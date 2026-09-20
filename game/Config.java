package game;

/**
 * Every tunable number in one place. Nothing here is read from disk, so a change
 * needs a recompile - that is deliberate for a hackathon build.
 */
public final class Config {

    /** Default TCP port for the game server. */
    public static final int PORT = 5757;

    /** The morph field is a square of this many units on each side. */
    public static final int CANVAS = 400;

    /**
     * Upper bound on how many pixels take part in a morph. The assignment is
     * roughly O(n^2 / 2), so this is the main lever on how long a round takes to
     * load. 60000 gives a 200x200-ish grid and a few seconds on a modern laptop.
     */
    public static final int MAX_PIXELS = 60000;

    /** Seconds a round is playable once the morph starts. */
    public static final int ROUND_SECONDS = 30;

    /** Seconds the drawer has to pick a target before one is chosen for them. */
    public static final int CHOOSE_SECONDS = 20;

    /** Seconds the answer stays on screen after a round ends. */
    public static final int REVEAL_SECONDS = 6;

    /** How many targets the drawer chooses between. */
    public static final int CHOICES = 3;

    /** A newly added person is guaranteed to be the target within this many rounds. */
    public static final int GUARANTEE_ROUNDS = 7;

    /** Caps on what one person can upload in a single submission. */
    public static final int MAX_UPLOAD_IMAGES = 20;
    public static final long MAX_UPLOAD_BYTES = 8L * 1024 * 1024;
    public static final int MAX_NAME_LENGTH = 32;

    /**
     * Multiplier on the physics timestep, which sets how long the reveal takes.
     *
     * At 1.0 the morph settles in roughly thirteen seconds, far too fast to guess
     * against. Note that the closing rate goes as the square of the timestep, not
     * the timestep itself, so halving this makes the morph four times longer, not
     * twice. 0.5 lands around fifty seconds, which leaves slack inside a
     * seventy-five second round.
     *
     * The damping is per frame rather than per second, so this also assumes the
     * client is stepping at the 60 fps the canvas timer asks for.
     */
    public static final double MORPH_TIME_SCALE = 0.5;

    /** Physics, carried over from the original animation code. */
    public static final double DT = 0.025;
    public static final double TARGET_ATTRACTION = 1.0;
    public static final double DAMPING = 0.92;
    public static final double MAX_AVERAGE_DISTANCE = 0.25;

    /** Image folders, resolved relative to the server's working directory. */
    public static final String SOURCE_DIR = "SourceImages";
    public static final String TARGET_DIR = "TargetImage";
    public static final String USER_DIR = "UserImages";

    /** Points. */
    public static final int GUESS_BASE_POINTS = 50;
    public static final int GUESS_TIME_POINTS = 150;
    public static final int DRAWER_POINTS_PER_GUESS = 20;

    private Config() {
    }
}
