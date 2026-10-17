package group1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CashStackTest {
    
    private CashStack cashStack;
    private CashStack secondCashStack;
    private Board realBoard;

    @BeforeEach
    public void setUp() {
        cashStack = new CashStack();
        secondCashStack = new CashStack();
        
        BoardFactory factory = new BoardFactory();
        realBoard = factory.makeBoard(1); 
    }

    // Checks that a newly created CashStack is not marked as collected
    @Test
    public void testInitialState() {
        assertFalse(cashStack.isCollected(), "CashStack should not be collected.");
    }

    // Confirms that collecting a CashStack increases the board balance and updates the item state
    @Test
    public void testOnCollectIncreasesBalance() {
        int initialBalance = realBoard.getBalance();
        cashStack.onCollect(realBoard);
        assertEquals(initialBalance + 100, realBoard.getBalance(), "Board balance should increase by 100 after collecting the CashStack");
        assertTrue(cashStack.isCollected(), "CashStack should be marked as collected after onCollect is called.");
    }

    // Checks that the balance updates accordingly when multiple collections occur
    @Test
    public void testMultipleCollections() {
        int initialBalance = realBoard.getBalance();
        cashStack.onCollect(realBoard);
        secondCashStack.onCollect(realBoard);
        assertEquals(initialBalance + 200, realBoard.getBalance(), "Balance should increase by 200 if collected twice.");
        assertTrue(cashStack.isCollected());
    }
}
