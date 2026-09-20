package game;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.imageio.ImageIO;

import morph.ImageField;

/**
 * Owns both pools of pictures.
 *
 * Sources come from SourceImages and are picked at random each round; nobody
 * guesses them, they are just the clay. Targets are the answer, so they are
 * grouped by the name of the person in the photo: the built-in ones take their
 * name from the filename, and player-added ones take the name that was typed in.
 *
 * Every method is synchronized because uploads arrive on network threads while
 * the round loop is reading.
 */
public final class ImageLibrary {

    /** One entry in the drawer's three choices. */
    public static final class Option {
        public final String person;
        public final File file;

        Option(String person, File file) {
            this.person = person;
            this.file = file;
        }
    }

    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(
            java.util.Arrays.asList("jpg", "jpeg", "png", "gif", "bmp"));

    private final List<File> sources = new ArrayList<>();
    private final Map<String, List<File>> people = new LinkedHashMap<>();
    private final Map<String, Integer> deadlines = new HashMap<>();
    private final Random random = new Random();

    private final File sourceDir;
    private final File targetDir;
    private final File userDir;

    public ImageLibrary() {
        this(new File(Config.SOURCE_DIR), new File(Config.TARGET_DIR), new File(Config.USER_DIR));
    }

    public ImageLibrary(File sourceDir, File targetDir, File userDir) {
        this.sourceDir = sourceDir;
        this.targetDir = targetDir;
        this.userDir = userDir;
        rescan();
    }

    /* ----- loading ----- */

    public synchronized void rescan() {
        sources.clear();
        people.clear();

        for (File file : imagesIn(sourceDir)) {
            sources.add(file);
        }

        for (File file : imagesIn(targetDir)) {
            addTarget(displayNameOf(file), file);
        }

        File[] folders = userDir.listFiles();
        if (folders != null) {
            for (File folder : folders) {
                if (!folder.isDirectory()) {
                    continue;
                }
                String person = folder.getName().replace('_', ' ').trim();
                for (File file : imagesIn(folder)) {
                    addTarget(person, file);
                }
            }
        }

        if (sources.isEmpty()) {
            // Without a source pool there is nothing to morph, so fall back to the
            // targets rather than refusing to start.
            for (List<File> files : people.values()) {
                sources.addAll(files);
            }
        }
    }

    private void addTarget(String person, File file) {
        if (person == null || person.isEmpty()) {
            return;
        }
        List<File> files = people.get(person);
        if (files == null) {
            files = new ArrayList<>();
            people.put(person, files);
        }
        files.add(file);
    }

    private static List<File> imagesIn(File dir) {
        List<File> out = new ArrayList<>();
        File[] found = dir.listFiles();
        if (found == null) {
            return out;
        }
        for (File file : found) {
            if (file.isFile() && isImage(file.getName())) {
                out.add(file);
            }
        }
        Collections.sort(out);
        return out;
    }

    private static boolean isImage(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        return IMAGE_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase());
    }

    /**
     * Filename to answer. "kobe_bryant2.jpg" becomes "Kobe Bryant". A filename with
     * nothing name-like left in it falls back to the raw stem, which is a good signal
     * to go and rename the file.
     */
    static String displayNameOf(File file) {
        String stem = file.getName();
        int dot = stem.lastIndexOf('.');
        if (dot > 0) {
            stem = stem.substring(0, dot);
        }

        String cleaned = stem
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("[0-9]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (cleaned.isEmpty()) {
            cleaned = stem.replaceAll("\\s+", " ").trim();
        }
        if (cleaned.isEmpty()) {
            return null;
        }

        StringBuilder out = new StringBuilder(cleaned.length());
        boolean startOfWord = true;
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);
            out.append(startOfWord ? Character.toUpperCase(c) : Character.toLowerCase(c));
            startOfWord = c == ' ';
        }
        return out.toString();
    }

    /* ----- round setup ----- */

    public synchronized boolean ready() {
        return !sources.isEmpty() && !people.isEmpty();
    }

    public synchronized String readinessProblem() {
        if (sources.isEmpty()) {
            return "No images in " + sourceDir.getPath() + ". Drop a few photos in there and restart the server.";
        }
        if (people.isEmpty()) {
            return "No images in " + targetDir.getPath() + ". Each filename becomes the answer, so name them after their subject.";
        }
        return null;
    }

    public synchronized File randomSource() {
        if (sources.isEmpty()) {
            return null;
        }
        return sources.get(random.nextInt(sources.size()));
    }

    /**
     * Builds the drawer's choices for a round.
     *
     * A person whose guarantee comes due this round takes over the whole list, so
     * whichever option the drawer picks, that person is the answer. Otherwise a
     * still-pending person gets one slot, giving the drawer a chance to resolve the
     * guarantee early, and the rest are random.
     */
    public synchronized List<Option> choices(int round) {
        List<Option> chosen = new ArrayList<>();
        Set<String> used = new HashSet<>();

        String due = mostUrgentDue(round);
        if (due != null) {
            fillFrom(due, chosen, Config.CHOICES);
            used.add(due);

            // A person who only uploaded one photo cannot fill three slots, so the next
            // most urgent person gets the remainder rather than a random stranger.
            while (chosen.size() < Config.CHOICES) {
                String next = nextPendingExcluding(used);
                if (next == null) {
                    break;
                }
                fillFrom(next, chosen, Config.CHOICES - chosen.size());
                used.add(next);
            }
        } else {
            String pending = nextPendingExcluding(used);
            if (pending != null) {
                fillFrom(pending, chosen, 1);
                used.add(pending);
            }
        }

        List<String> pool = new ArrayList<>(people.keySet());
        Collections.shuffle(pool, random);
        for (String person : pool) {
            if (chosen.size() >= Config.CHOICES) {
                break;
            }
            if (used.contains(person)) {
                continue;
            }
            fillFrom(person, chosen, 1);
            used.add(person);
        }

        // Fewer than three people in the library: repeat photos rather than show
        // fewer options, so the drawer always has a real pick to make.
        while (chosen.size() < Config.CHOICES && !chosen.isEmpty() && totalImages() >= Config.CHOICES) {
            for (String person : pool) {
                if (chosen.size() >= Config.CHOICES) {
                    break;
                }
                List<File> files = people.get(person);
                for (File file : files) {
                    if (chosen.size() >= Config.CHOICES) {
                        break;
                    }
                    if (!alreadyChosen(chosen, file)) {
                        chosen.add(new Option(person, file));
                    }
                }
            }
            break;
        }

        Collections.shuffle(chosen, random);
        return chosen;
    }

    private boolean alreadyChosen(List<Option> chosen, File file) {
        for (Option option : chosen) {
            if (option.file.equals(file)) {
                return true;
            }
        }
        return false;
    }

    private int totalImages() {
        int total = 0;
        for (List<File> files : people.values()) {
            total += files.size();
        }
        return total;
    }

    private void fillFrom(String person, List<Option> chosen, int wanted) {
        List<File> files = people.get(person);
        if (files == null || files.isEmpty()) {
            return;
        }
        List<File> shuffled = new ArrayList<>(files);
        Collections.shuffle(shuffled, random);
        for (int i = 0; i < shuffled.size() && i < wanted; i++) {
            chosen.add(new Option(person, shuffled.get(i)));
        }
    }

    private String mostUrgentDue(int round) {
        String best = null;
        int bestDeadline = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> entry : deadlines.entrySet()) {
            int deadline = entry.getValue();
            if (deadline <= round && deadline < bestDeadline && people.containsKey(entry.getKey())) {
                bestDeadline = deadline;
                best = entry.getKey();
            }
        }
        return best;
    }

    private String nextPendingExcluding(Set<String> skip) {
        String best = null;
        int bestDeadline = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> entry : deadlines.entrySet()) {
            if (skip.contains(entry.getKey()) || !people.containsKey(entry.getKey())) {
                continue;
            }
            if (entry.getValue() < bestDeadline) {
                bestDeadline = entry.getValue();
                best = entry.getKey();
            }
        }
        return best;
    }

    /** Called once a person has actually been the answer. */
    public synchronized void guaranteeMet(String person) {
        deadlines.remove(person);
    }

    public synchronized int roundsUntilDue(String person, int round) {
        Integer deadline = deadlines.get(person);
        if (deadline == null) {
            return -1;
        }
        return Math.max(0, deadline - round);
    }

    /* ----- uploads ----- */

    public static final class AddResult {
        public final boolean accepted;
        public final String person;
        public final int saved;
        public final int roundsUntilDue;
        public final String message;

        AddResult(boolean accepted, String person, int saved, int roundsUntilDue, String message) {
            this.accepted = accepted;
            this.person = person;
            this.saved = saved;
            this.roundsUntilDue = roundsUntilDue;
            this.message = message;
        }
    }

    /**
     * Saves a batch of photos under a person's name and books them a slot inside the
     * next {@link Config#GUARANTEE_ROUNDS} rounds.
     *
     * Deadlines are staggered so several people added at once each get their own
     * round. If the whole window is already booked the newcomer shares the last
     * round, which is the only case where the guarantee can slip.
     */
    public synchronized AddResult addPerson(String rawName, List<byte[]> images, int currentRound) {
        String person = sanitiseName(rawName);
        if (person == null) {
            return new AddResult(false, null, 0, -1,
                    "That name has no letters in it. Try the person's name as you'd want it guessed.");
        }
        if (images == null || images.isEmpty()) {
            return new AddResult(false, person, 0, -1, "Pick at least one photo.");
        }
        if (images.size() > Config.MAX_UPLOAD_IMAGES) {
            return new AddResult(false, person, 0, -1,
                    "That's more than " + Config.MAX_UPLOAD_IMAGES + " photos. Send them in smaller batches.");
        }

        File folder = new File(userDir, person.replace(' ', '_'));
        if (!folder.isDirectory() && !folder.mkdirs()) {
            return new AddResult(false, person, 0, -1, "Couldn't create " + folder.getPath() + " on the server.");
        }

        int saved = 0;
        int rejected = 0;
        for (byte[] bytes : images) {
            if (bytes == null || bytes.length == 0 || bytes.length > Config.MAX_UPLOAD_BYTES) {
                rejected++;
                continue;
            }
            try {
                BufferedImage field = ImageField.load(bytes);
                File out = new File(folder, System.currentTimeMillis() + "-" + saved + ".png");
                ImageIO.write(field, "png", out);
                addTarget(person, out);
                saved++;
            } catch (IOException e) {
                rejected++;
            }
        }

        if (saved == 0) {
            return new AddResult(false, person, 0, -1,
                    "None of those files could be read as images.");
        }

        int deadline = bookDeadline(currentRound);
        deadlines.put(person, deadline);

        String note = person + " is in, with " + saved + (saved == 1 ? " photo" : " photos");
        if (rejected > 0) {
            note = note + " (" + rejected + " skipped)";
        }

        return new AddResult(true, person, saved, Math.max(1, deadline - currentRound), note);
    }

    private int bookDeadline(int currentRound) {
        Set<Integer> taken = new HashSet<>(deadlines.values());
        for (int round = currentRound + 1; round <= currentRound + Config.GUARANTEE_ROUNDS; round++) {
            if (!taken.contains(round)) {
                return round;
            }
        }
        return currentRound + Config.GUARANTEE_ROUNDS;
    }

    static String sanitiseName(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.replaceAll("[^A-Za-z0-9 '\\-]", " ").replaceAll("\\s+", " ").trim();
        if (trimmed.length() > Config.MAX_NAME_LENGTH) {
            trimmed = trimmed.substring(0, Config.MAX_NAME_LENGTH).trim();
        }
        if (GuessChecker.normalise(trimmed).isEmpty()) {
            return null;
        }

        StringBuilder out = new StringBuilder(trimmed.length());
        boolean startOfWord = true;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            out.append(startOfWord ? Character.toUpperCase(c) : Character.toLowerCase(c));
            startOfWord = c == ' ' || c == '-';
        }
        return out.toString();
    }
}
