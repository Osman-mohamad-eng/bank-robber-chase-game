package group1;

/**
 * Represents a security hazard trap on the board.
 * Penalizes the theif by deducting a set amount from the score
 * 
 * @author Brian
 * @version 1.0
 */
public class InkDart extends Interactable {
    private static final int PENALTY_VALUE = -150;
    private int value;

    /**
     * Constructs an InkDart with a default penalty value of -150.
     */
    public InkDart() {
        this.value = PENALTY_VALUE;
    }

    /**
     * Triggers the trap's penalty, subtracting from the board balance.
     * Does not affect the static money count
     * @param board The game board where the penalty occurs
     */
    @Override
    protected void applyEffect(Board board) {
        board.addBalance(this.value);
    }
}
