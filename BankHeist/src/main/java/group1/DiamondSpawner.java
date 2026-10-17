package group1;
import javax.swing.Timer;
public class DiamondSpawner {
    private final Board board;
    private final Timer timer;

    // time interval for removing diamond
    private static final int DEFAULT_DIAMOND_SPAWN_DELAY = 5000;
    /**
     * Handles timed spawning and despawning of the bonus diamond.
     *
     *  keeps the timer logic separate from the Board class.
     */
    public DiamondSpawner(Board board) {
        this(board, DEFAULT_DIAMOND_SPAWN_DELAY);
    }
    public DiamondSpawner(Board board, int delay) {
        this.board = board;
        timer = new Timer(delay, e -> this.board.updateDiamondState());
    }

    public void start() {
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    public void restart() {
        timer.restart();
    }

    public boolean isRunning() {
        return timer.isRunning();
    }
}
