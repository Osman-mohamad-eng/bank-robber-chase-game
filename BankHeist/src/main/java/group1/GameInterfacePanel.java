package group1;

/**
 * Clearer alias for the main Swing game interface.
 */
public class GameInterfacePanel extends Interface {
    public GameInterfacePanel(GameSession gameSession) {
        super(gameSession);
    }

    public GameInterfacePanel(Board board) {
        super(board);
    }
}
