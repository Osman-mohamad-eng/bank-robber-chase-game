package group1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class InkDartTest {

    private InkDart inkDart;
    private InkDart secondInkDart;
    private Board realBoard;

    // Sets up a InkDart and board before each test
    @BeforeEach
    public void setUp() {
        inkDart = new InkDart();
        secondInkDart = new InkDart();
        
        BoardFactory factory = new BoardFactory();
        realBoard = factory.makeBoard(1); 
    }

    // Checks that an InkDart is not collected when initialized 
    @Test
    public void testInitialState() {
        assertFalse(inkDart.isCollected(), "InkDart should not be collected");
    }

    // Checks that hitting an InkDart reduces balance and updates teh item state
    @Test
    public void testOnCollectDecreasesBalance() {
        int initialBalance = realBoard.getBalance();
        inkDart.onCollect(realBoard);
        assertEquals(initialBalance - 150, realBoard.getBalance(), "Board balance should decrease 150 after hitting an InkDart.");
        assertTrue(inkDart.isCollected(), "InkDart should be marked as collected after onCollect is called.");
    }
    

    // Checks that the balance updates correctly when multiple InkDarts are triggered.
    @Test
    public void testMultipleCollections() {
        int initialBalance = realBoard.getBalance();
        inkDart.onCollect(realBoard);
        secondInkDart.onCollect(realBoard);
        assertEquals(initialBalance - 300, realBoard.getBalance(), "Balance should decrease by 300 if the trap is triggered twice.");
        assertTrue(inkDart.isCollected());
    }
}
