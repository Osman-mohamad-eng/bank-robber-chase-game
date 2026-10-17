package group1;

/** 
 * Represents a high-value bonus reward that appears randomly
 * Collecting a diamond resets the spawner timer and increases the score by 500.
 * 
 * @author Brian
 * @version 1.0
*/
public class RareDiamond extends Interactable {
    private static final int REWARD_VALUE = 500;
    private int value;

    /**
     * Constructs a Diamond with a high reward value of 500
     */
    public RareDiamond() {
        this.value = REWARD_VALUE;
    }

    /**
     * Triggers the diamond's reward and restarts the spawn timer
     * @param board The game board where the collection occurs
     */
    @Override
    protected void applyEffect(Board board) {
        board.addBalance(this.value);
    }
}