package group1;

/**
 * Represents a sticky trap that immobilizes the Thief for one tick
 * 
 * @author Brian
 * @version 1.0
 */
public class GlueTrap extends Interactable {
    private static final int FREEZE_DURATION_TICKS = 1;

    /**
     * Triggers the trap effect when collected by the thief.
     * Skips exactly one movement tick
     * @param board The game board where the freeze status is applied 
     */
    @Override
    protected void applyEffect(Board board) {
        board.applyFreeze(FREEZE_DURATION_TICKS); 
    }
}