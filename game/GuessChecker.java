package game;

import java.util.Set;

/**
 * Decides whether a chat line is the answer, and builds the row of blanks that
 * everyone stares at while they think.
 */
public final class GuessChecker {

    private GuessChecker() {
    }

    /** Lowercases and strips everything that is not a letter or digit. */
    public static String normalise(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = Character.toLowerCase(text.charAt(i));
            if (Character.isLetterOrDigit(c)) {
                out.append(c);
            }
        }
        return out.toString();
    }

    public static boolean isCorrect(String guess, String answer) {
        String a = normalise(guess);
        String b = normalise(answer);
        return !b.isEmpty() && a.equals(b);
    }

    /**
     * True for a guess that is one edit away, or that matches a single word of a
     * multi-word answer. Worth a nudge but not the points.
     */
    public static boolean isClose(String guess, String answer) {
        String a = normalise(guess);
        String b = normalise(answer);
        if (a.isEmpty() || b.isEmpty() || a.equals(b)) {
            return false;
        }

        if (b.length() >= 5 && editDistance(a, b) <= 1) {
            return true;
        }

        String[] words = answer.trim().split("\\s+");
        if (words.length > 1) {
            for (String word : words) {
                String w = normalise(word);
                if (w.length() >= 3 && w.equals(a)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int editDistance(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];

        for (int j = 0; j <= b.length(); j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(
                        Math.min(current[j - 1] + 1, previous[j] + 1),
                        previous[j - 1] + cost
                );
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }

        return previous[b.length()];
    }

    /**
     * Turns the answer into blanks. Letters in {@code revealed} (indexes into the
     * answer) show through; punctuation always shows; words are separated by a wide
     * gap so the shape of the name reads at a glance.
     *
     * "Kobe Bryant" with nothing revealed becomes "_ _ _ _   _ _ _ _ _ _".
     */
    public static String mask(String answer, Set<Integer> revealed) {
        if (answer == null || answer.isEmpty()) {
            return "";
        }

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < answer.length(); i++) {
            char c = answer.charAt(i);

            if (c == ' ') {
                out.append("   ");
                continue;
            }

            if (i > 0 && answer.charAt(i - 1) != ' ' && out.length() > 0) {
                out.append(' ');
            }

            if (Character.isLetterOrDigit(c)) {
                out.append(revealed != null && revealed.contains(i) ? c : '_');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /** Number of letters and digits in the answer, for the "14 letters" hint. */
    public static int letterCount(String answer) {
        return normalise(answer).length();
    }
}
