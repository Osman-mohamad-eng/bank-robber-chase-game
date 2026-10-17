package group1;

/**
 * Defines the game operations needed by the UI layer.
 */
public interface GameSession {
    int getBalance();

    void setPaused(boolean paused);

    void reset();

    void setGameEvents(GameEvents events);
}
