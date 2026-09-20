package game;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import morph.ImageField;
import morph.MorphBuilder;
import morph.MorphData;
import net.Msg;

/**
 * The game, run on one thread on the server. Clients only ever render what this
 * class tells them, and only this class knows the answer while a round is live.
 *
 * Each round goes: pick a drawer, hand them three targets, build the morph while
 * everyone watches a progress bar, run the clock, then reveal.
 */
public final class RoundEngine implements Runnable {

    private final Transport transport;
    private final ImageLibrary library;
    private final Random random = new Random();

    private final Map<Integer, Player> players = new LinkedHashMap<>();
    private final List<Integer> turnOrder = new ArrayList<>();

    private volatile boolean running = true;

    private Msg.Phase phase = Msg.Phase.LOBBY;
    private int round;
    private int drawerId = -1;
    private int turnCursor = -1;

    private List<ImageLibrary.Option> choices = new ArrayList<>();
    private int picked = -1;

    private String answer;
    private final Set<Integer> revealedLetters = new HashSet<>();
    private int correctThisRound;
    private long roundEndsAt;

    public RoundEngine(Transport transport, ImageLibrary library) {
        this.transport = transport;
        this.library = library;
    }

    public void stop() {
        running = false;
    }

    /* ----- roster ----- */

    public synchronized Player addPlayer(int id, String name) {
        Player player = new Player(id, name);
        players.put(id, player);
        turnOrder.add(id);
        sendPlayers();
        return player;
    }

    public synchronized void removePlayer(int id) {
        Player player = players.remove(id);
        turnOrder.remove(Integer.valueOf(id));
        if (player != null) {
            say(player.name + " left.");
        }
        if (id == drawerId && phase != Msg.Phase.LOBBY) {
            // Nothing to guess toward once the picker is gone.
            roundEndsAt = 0;
            say("The round ended early.");
        }
        sendPlayers();
    }

    public synchronized int playerCount() {
        return players.size();
    }

    /* ----- inbound player actions ----- */

    public synchronized void onPick(int playerId, int index) {
        if (phase == Msg.Phase.CHOOSING && playerId == drawerId
                && index >= 0 && index < choices.size()) {
            picked = index;
        }
    }

    public synchronized void onSay(int playerId, String rawText) {
        Player player = players.get(playerId);
        if (player == null || rawText == null) {
            return;
        }

        String text = rawText.trim();
        if (text.isEmpty()) {
            return;
        }
        if (text.length() > 240) {
            text = text.substring(0, 240);
        }

        boolean live = phase == Msg.Phase.PLAYING && answer != null;

        if (live && playerId == drawerId) {
            if (GuessChecker.isCorrect(text, answer) || GuessChecker.isClose(text, answer)) {
                whisper(playerId, "You picked it, so keep it to yourself.", Msg.ChatKind.SYSTEM);
                return;
            }
            transport.toAll(new Msg.Chat(player.name, text, Msg.ChatKind.SAY));
            return;
        }

        if (live && !player.guessedThisRound) {
            if (GuessChecker.isCorrect(text, answer)) {
                award(player);
                return;
            }
            if (GuessChecker.isClose(text, answer)) {
                whisper(playerId, "\"" + text + "\" is close.", Msg.ChatKind.CLOSE);
                transport.toAll(new Msg.Chat(player.name, text, Msg.ChatKind.SAY));
                return;
            }
        }

        transport.toAll(new Msg.Chat(player.name, text, Msg.ChatKind.SAY));
    }

    public synchronized void onAddPerson(int playerId, String person, List<byte[]> images) {
        Player player = players.get(playerId);
        String who = player == null ? "Someone" : player.name;

        ImageLibrary.AddResult result = library.addPerson(person, images, round);
        if (!result.accepted) {
            whisper(playerId, result.message, Msg.ChatKind.SYSTEM);
            return;
        }

        say(who + " added " + result.saved + (result.saved == 1 ? " photo of " : " photos of ") + result.person + ".");
        whisper(playerId,
                result.person + " will be the target within " + result.roundsUntilDue
                        + (result.roundsUntilDue == 1 ? " round." : " rounds."),
                Msg.ChatKind.SYSTEM);
    }

    private void award(Player player) {
        player.guessedThisRound = true;
        correctThisRound++;

        long remaining = Math.max(0, roundEndsAt - System.currentTimeMillis());
        double fraction = remaining / (Config.ROUND_SECONDS * 1000.0);
        int points = Config.GUESS_BASE_POINTS + (int) (Config.GUESS_TIME_POINTS * fraction);
        if (correctThisRound == 1) {
            points += 25;
        }
        player.points += points;

        whisper(player.id, "You got it. +" + points, Msg.ChatKind.CORRECT);
        say(player.name + " got it.");
        sendPlayers();
    }

    /* ----- the loop ----- */

    @Override
    public void run() {
        while (running) {
            if (!readyToPlay()) {
                sleep(500);
                continue;
            }

            ImageLibrary.Option option = openChoosing();
            if (option == null) {
                continue;
            }

            MorphData morph = buildMorph(option);
            if (morph == null) {
                continue;
            }

            playRound(option, morph);
            revealRound(option);
        }
    }

    private boolean readyToPlay() {
        String problem;
        int count;
        synchronized (this) {
            problem = library.readinessProblem();
            count = players.size();
        }

        if (problem != null) {
            announceLobby(problem);
            return false;
        }
        if (count < 2) {
            announceLobby("Waiting for one more player.");
            return false;
        }
        return true;
    }

    private String lastLobbyNote;

    private void announceLobby(String note) {
        synchronized (this) {
            phase = Msg.Phase.LOBBY;
            if (note.equals(lastLobbyNote)) {
                return;
            }
            lastLobbyNote = note;
        }
        transport.toAll(new Msg.Status(Msg.Phase.LOBBY, round, note));
    }

    /** Starts a round, offers three targets, and waits for the drawer to choose. */
    private ImageLibrary.Option openChoosing() {
        String drawerName;
        List<ImageLibrary.Option> options;

        synchronized (this) {
            lastLobbyNote = null;
            round++;
            drawerId = nextDrawer();
            if (drawerId < 0) {
                return null;
            }

            for (Player player : players.values()) {
                player.guessedThisRound = false;
            }
            players.get(drawerId).hasDrawn = true;

            choices = library.choices(round);
            picked = -1;
            answer = null;
            revealedLetters.clear();
            correctThisRound = 0;
            phase = Msg.Phase.CHOOSING;

            drawerName = players.get(drawerId).name;
            options = new ArrayList<>(choices);
        }

        if (options.isEmpty()) {
            announceLobby("No targets to choose from. Add photos in " + Config.TARGET_DIR + ".");
            return null;
        }

        sendPlayers();
        transport.toAll(new Msg.Status(Msg.Phase.CHOOSING, round, drawerName + " is picking a target."));
        transport.toOne(drawerId, buildChoices(options));

        long deadline = System.currentTimeMillis() + Config.CHOOSE_SECONDS * 1000L;
        int lastSecond = -1;

        while (running) {
            synchronized (this) {
                if (picked >= 0) {
                    return choices.get(picked);
                }
                if (!players.containsKey(drawerId)) {
                    return null;
                }
            }

            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                break;
            }

            int second = (int) Math.ceil(remaining / 1000.0);
            if (second != lastSecond) {
                lastSecond = second;
                transport.toAll(new Msg.Tick(second, ""));
            }
            sleep(100);
        }

        synchronized (this) {
            if (choices.isEmpty()) {
                return null;
            }
            if (picked >= 0) {
                // A pick landed right on the deadline. It still counts.
                return choices.get(picked);
            }
            picked = random.nextInt(choices.size());
            say(players.containsKey(drawerId)
                    ? players.get(drawerId).name + " ran out of time, so one was picked for them."
                    : "A target was picked automatically.");
            return choices.get(picked);
        }
    }

    private Msg.Choices buildChoices(List<ImageLibrary.Option> options) {
        List<String> names = new ArrayList<>();
        List<byte[]> previews = new ArrayList<>();

        for (ImageLibrary.Option option : options) {
            names.add(option.person);
            try {
                BufferedImage field = ImageField.load(option.file);
                previews.add(ImageField.thumbnail(field, 150));
            } catch (IOException e) {
                previews.add(null);
            }
        }

        return new Msg.Choices(names, previews, Config.CHOOSE_SECONDS);
    }

    /** The expensive part: pairing every source pixel with a target pixel. */
    private MorphData buildMorph(ImageLibrary.Option option) {
        File sourceFile = library.randomSource(option.person);
        if (sourceFile == null) {
            announceLobby("No source images to morph from.");
            return null;
        }

        synchronized (this) {
            phase = Msg.Phase.LOADING;
        }
        transport.toAll(new Msg.Status(Msg.Phase.LOADING, round, "Building the morph."));
        transport.toAll(new Msg.Loading(0, "Reading the pictures"));

        BufferedImage source;
        BufferedImage target;
        try {
            source = ImageField.load(sourceFile);
            target = ImageField.load(option.file);
        } catch (IOException e) {
            say("Couldn't read one of the images, skipping this round.");
            return null;
        }

        final int[] lastSent = { -10 };
        MorphData morph = MorphBuilder.build(source, target, percent -> {
            if (percent - lastSent[0] >= 2) {
                lastSent[0] = percent;
                transport.toAll(new Msg.Loading(percent, "Matching pixels"));
            }
        });

        transport.toAll(new Msg.Loading(100, "Ready"));
        return morph;
    }

    private void playRound(ImageLibrary.Option option, MorphData morph) {
        String drawerName;
        String mask;

        synchronized (this) {
            answer = option.person;
            revealedLetters.clear();
            correctThisRound = 0;
            phase = Msg.Phase.PLAYING;
            roundEndsAt = System.currentTimeMillis() + Config.ROUND_SECONDS * 1000L;
            drawerName = players.containsKey(drawerId) ? players.get(drawerId).name : "Someone";
            mask = GuessChecker.mask(answer, revealedLetters);
        }

        int letters = GuessChecker.letterCount(option.person);

        // Only the drawer is told what they picked.
        synchronized (this) {
            for (Player player : players.values()) {
                String answerForPlayer = player.id == drawerId ? option.person : null;
                transport.toOne(player.id, new Msg.Begin(round, mask, letters,
                        Config.ROUND_SECONDS, morph, drawerName, answerForPlayer));
            }
        }

        int lastSecond = -1;
        boolean firstHintDropped = false;
        boolean secondHintDropped = false;

        while (running) {
            long remaining;
            boolean everyoneGuessed;

            synchronized (this) {
                remaining = roundEndsAt - System.currentTimeMillis();
                everyoneGuessed = allGuessersDone();
            }

            if (remaining <= 0 || everyoneGuessed) {
                break;
            }

            double elapsedFraction = 1.0 - (remaining / (Config.ROUND_SECONDS * 1000.0));
            boolean maskChanged = false;

            synchronized (this) {
                if (!firstHintDropped && elapsedFraction > 0.55) {
                    firstHintDropped = true;
                    maskChanged = revealOneLetter();
                }
                if (!secondHintDropped && elapsedFraction > 0.8) {
                    secondHintDropped = true;
                    maskChanged = revealOneLetter() || maskChanged;
                }
                if (maskChanged) {
                    mask = GuessChecker.mask(answer, revealedLetters);
                }
            }

            int second = (int) Math.ceil(remaining / 1000.0);
            if (second != lastSecond || maskChanged) {
                lastSecond = second;
                transport.toAll(new Msg.Tick(second, mask));
            }
            sleep(100);
        }
    }

    private void revealRound(ImageLibrary.Option option) {
        String note;

        synchronized (this) {
            phase = Msg.Phase.REVEAL;
            library.guaranteeMet(option.person);

            Player drawer = players.get(drawerId);
            if (drawer != null && correctThisRound > 0) {
                drawer.points += Config.DRAWER_POINTS_PER_GUESS * correctThisRound;
            }

            int guessers = Math.max(0, players.size() - 1);
            if (correctThisRound == 0) {
                note = "Nobody got it.";
            } else if (correctThisRound >= guessers) {
                note = "Everyone got it.";
            } else {
                note = correctThisRound + " of " + guessers + " got it.";
            }

            answer = null;
        }

        transport.toAll(new Msg.Over(option.person, note));
        sendPlayers();
        sleep(Config.REVEAL_SECONDS * 1000L);
    }

    /* ----- helpers ----- */

    private boolean allGuessersDone() {
        int guessers = 0;
        int done = 0;
        for (Player player : players.values()) {
            if (player.id == drawerId) {
                continue;
            }
            guessers++;
            if (player.guessedThisRound) {
                done++;
            }
        }
        return guessers > 0 && done == guessers;
    }

    private boolean revealOneLetter() {
        if (answer == null) {
            return false;
        }
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < answer.length(); i++) {
            if (Character.isLetterOrDigit(answer.charAt(i)) && !revealedLetters.contains(i)) {
                candidates.add(i);
            }
        }
        // Leave at least a third of the name hidden.
        if (candidates.size() <= Math.max(1, GuessChecker.letterCount(answer) / 3)) {
            return false;
        }
        revealedLetters.add(candidates.get(random.nextInt(candidates.size())));
        return true;
    }

    private int nextDrawer() {
        if (turnOrder.isEmpty()) {
            return -1;
        }
        for (int step = 0; step < turnOrder.size(); step++) {
            turnCursor = (turnCursor + 1) % turnOrder.size();
            int candidate = turnOrder.get(turnCursor);
            if (players.containsKey(candidate)) {
                return candidate;
            }
        }
        return -1;
    }

    private void sendPlayers() {
        List<Msg.Standing> standings = new ArrayList<>();
        synchronized (this) {
            for (Player player : players.values()) {
                standings.add(new Msg.Standing(player.id, player.name, player.points,
                        player.guessedThisRound, player.id == drawerId));
            }
        }
        standings.sort(Comparator.comparingInt((Msg.Standing s) -> -s.points));
        transport.toAll(new Msg.Players(new ArrayList<>(standings)));
    }

    private void say(String text) {
        transport.toAll(new Msg.Chat(null, text, Msg.ChatKind.SYSTEM));
    }

    private void whisper(int playerId, String text, Msg.ChatKind kind) {
        transport.toOne(playerId, new Msg.Chat(null, text, kind));
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Used by the server to greet a player who joins mid-round. */
    public synchronized Msg.Status currentStatus() {
        String note;
        switch (phase) {
            case CHOOSING:
                note = "A target is being picked.";
                break;
            case LOADING:
                note = "Building the morph.";
                break;
            case PLAYING:
                note = "Round in progress. You're in from the next one.";
                break;
            case REVEAL:
                note = "Round just ended.";
                break;
            default:
                note = "Waiting to start.";
                break;
        }
        return new Msg.Status(phase, round, note);
    }
}
