package game;

/** How the round loop reaches players. Implemented by the server. */
public interface Transport {

    void toAll(Object message);

    void toOne(int playerId, Object message);
}
