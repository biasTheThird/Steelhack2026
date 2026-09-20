package net;

import java.io.Serializable;
import java.util.List;

import morph.MorphData;

/**
 * The whole protocol. Messages go over ObjectOutputStream, which means server and
 * clients have to be built from the same source - fine for a LAN game, and it
 * saves writing a parser.
 *
 * The server is authoritative about everything: who is drawing, what the answer
 * is, whose guess counted, and what the clock says. A client never decides any of
 * it, which is also why the answer is only ever sent to the drawer and then to
 * everyone at the reveal.
 */
public final class Msg {

    public enum Phase {
        LOBBY, CHOOSING, LOADING, PLAYING, REVEAL
    }

    public enum ChatKind {
        SAY, SYSTEM, CORRECT, CLOSE
    }

    /* ----- client to server ----- */

    public static final class Join implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String name;

        public Join(String name) {
            this.name = name;
        }
    }

    public static final class Say implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String text;

        public Say(String text) {
            this.text = text;
        }
    }

    public static final class Pick implements Serializable {
        private static final long serialVersionUID = 1L;
        public final int index;

        public Pick(int index) {
            this.index = index;
        }
    }

    public static final class AddPerson implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String person;
        public final List<byte[]> images;

        public AddPerson(String person, List<byte[]> images) {
            this.person = person;
            this.images = images;
        }
    }

    /* ----- server to client ----- */

    public static final class Welcome implements Serializable {
        private static final long serialVersionUID = 1L;
        public final int playerId;
        public final String name;

        public Welcome(int playerId, String name) {
            this.playerId = playerId;
            this.name = name;
        }
    }

    public static final class Standing implements Serializable {
        private static final long serialVersionUID = 1L;
        public final int id;
        public final String name;
        public final int points;
        public final boolean guessed;
        public final boolean drawer;

        public Standing(int id, String name, int points, boolean guessed, boolean drawer) {
            this.id = id;
            this.name = name;
            this.points = points;
            this.guessed = guessed;
            this.drawer = drawer;
        }
    }

    public static final class Players implements Serializable {
        private static final long serialVersionUID = 1L;
        public final List<Standing> standings;

        public Players(List<Standing> standings) {
            this.standings = standings;
        }
    }

    public static final class Chat implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String who;
        public final String text;
        public final ChatKind kind;

        public Chat(String who, String text, ChatKind kind) {
            this.who = who;
            this.text = text;
            this.kind = kind;
        }
    }

    /** Sent to the drawer only. */
    public static final class Choices implements Serializable {
        private static final long serialVersionUID = 1L;
        public final List<String> names;
        public final List<byte[]> previews;
        public final int seconds;

        public Choices(List<String> names, List<byte[]> previews, int seconds) {
            this.names = names;
            this.previews = previews;
            this.seconds = seconds;
        }
    }

    public static final class Loading implements Serializable {
        private static final long serialVersionUID = 1L;
        public final int percent;
        public final String note;

        public Loading(int percent, String note) {
            this.percent = percent;
            this.note = note;
        }
    }

    public static final class Begin implements Serializable {
        private static final long serialVersionUID = 1L;
        public final int round;
        public final String mask;
        public final int letters;
        public final int seconds;
        public final MorphData morph;
        public final String drawerName;
        /** Non-null only for the drawer, who already knows what they picked. */
        public final String answer;

        public Begin(int round, String mask, int letters, int seconds,
                     MorphData morph, String drawerName, String answer) {
            this.round = round;
            this.mask = mask;
            this.letters = letters;
            this.seconds = seconds;
            this.morph = morph;
            this.drawerName = drawerName;
            this.answer = answer;
        }
    }

    public static final class Tick implements Serializable {
        private static final long serialVersionUID = 1L;
        public final int secondsLeft;
        public final String mask;

        public Tick(int secondsLeft, String mask) {
            this.secondsLeft = secondsLeft;
            this.mask = mask;
        }
    }

    public static final class Over implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String answer;
        public final String note;

        public Over(String answer, String note) {
            this.answer = answer;
            this.note = note;
        }
    }

    public static final class Status implements Serializable {
        private static final long serialVersionUID = 1L;
        public final Phase phase;
        public final int round;
        public final String note;

        public Status(Phase phase, int round, String note) {
            this.phase = phase;
            this.round = round;
            this.note = note;
        }
    }

    private Msg() {
    }
}
