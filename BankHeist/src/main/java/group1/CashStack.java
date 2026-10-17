package group1;

/**
 * Represents the base collectible currency in Bank Heist
 * Allows various rewards and traps to define collection behaviours
 * 
 * @author Brian
 * @version 1.0
 */
public class CashStack extends Interactable {
    private static final int REWARD_VALUE = 100;
    private int value;

    /**
     * Constructs a standard Money stack with a default value of 100
     */
    public CashStack() {
        this.value = REWARD_VALUE; 
    }

    /**
     * Triggers the collection effect, adding balance and decrementing the required cash stacks.
     * @param board The game board where the collection occurs.
     */
    @Override
    protected void applyEffect(Board board) {
        board.addBalance(this.value);
        board.decrementStaticMoney();
    }
}