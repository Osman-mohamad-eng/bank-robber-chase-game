package group1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RareDiamondTest {
    
    private RareDiamond rareDiamond;
    private RareDiamond secondRareDiamond;
    private Board realBoard;

    // Sets up a RareDiamond and new board before each test
    @BeforeEach
    public void setUp() {
        rareDiamond = new RareDiamond();
        secondRareDiamond = new RareDiamond();
        BoardFactory factory = new BoardFactory();
        realBoard = factory.makeBoard(1); 
    }

    // Checks that a RareDiamond is not collected when created
    @Test
    public void testInitialState() {
        assertFalse(rareDiamond.isCollected(), "RareDiamond should not be collected");
    }

    // Checks that collecting a RareDiamond increases the board balance by 500 and updates the item state
    @Test
    public void testOnCollectIncreasesBalance() {
        int initialBalance = realBoard.getBalance();
        rareDiamond.onCollect(realBoard);
        assertEquals(initialBalance + 500, realBoard.getBalance(), "Board balance should increase by 500 after collecting a RareDiamond.");
        assertTrue(rareDiamond.isCollected(), "RareDiamond should be marked as collected after onCollect is called.");
    }
    
    // Checks that the balance updates correctly when multiple RareDiamonds are collected.
    @Test
    public void testMultipleCollections() {
        int initialBalance = realBoard.getBalance();
        rareDiamond.onCollect(realBoard);
        secondRareDiamond.onCollect(realBoard);
        assertEquals(initialBalance + 1000, realBoard.getBalance(), "Balance should increase by 1000 if the diamond is collected twice.");
        assertTrue(rareDiamond.isCollected());
    }
}
