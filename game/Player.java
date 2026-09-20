package game;

public final class Player {

    public final int id;
    public final String name;

    public int points;
    public boolean guessedThisRound;
    public boolean hasDrawn;

    public Player(int id, String name) {
        this.id = id;
        this.name = name;
    }
}
