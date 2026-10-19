package group1;

/**
 * Represents the base collectible currency in Bank Heist
 * Allows various rewards and traps to define collection behaviours
 * 
 * @author Brian
 * @version 1.0
 */
public class CashStack extends Interactable {

    /**
     * Constructs a standard Money stack with a default value of 100
     */
    public CashStack() {
    }

    /**
     * Triggers the collection effect, adding balance and decrementing the required cash stacks.
     * @param board The game board where the collection occurs.
     */
    @Override
    protected void applyEffect(Board board) {
        board.collectCash(ScoringRules.CASH_STACK_VALUE);
        board.decrementStaticMoney();
    }
}
