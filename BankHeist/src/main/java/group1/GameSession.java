package group1;

/**
 * Defines the game operations needed by the UI layer.
 */
public interface GameSession {
    int getBalance();

    default long getCashCollected() { return 0; }

    default int getDiamondsCollected() { return 0; }

    default long getPenaltiesIncurred() { return 0; }

    void setPaused(boolean paused);

    void reset();

    void setGameEvents(GameEvents events);
}
