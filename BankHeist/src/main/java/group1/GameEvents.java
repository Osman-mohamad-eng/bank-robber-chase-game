package group1;

/**
 * Receives game outcome notifications from the active game session.
 */
public interface GameEvents {
    void onWin();

    void onGameOver();
}
