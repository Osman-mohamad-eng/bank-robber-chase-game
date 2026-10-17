package group1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GlueTrapTest {
    
    private GlueTrap glueTrap;
    private Board realBoard;

    // Sets up a glue trap and board before each test
    @BeforeEach
    public void setUp() {
        glueTrap = new GlueTrap();
        BoardFactory factory = new BoardFactory();
        realBoard = factory.makeBoard(1); 
    }

    // Checks that the glue trap is not collected initially
    @Test
    public void testInitialState() {
        assertFalse(glueTrap.isCollected(), "GlueTrap should not be collected");
    }

    // Confirms that triggering the trap updates its state and applies a freeze to the board
    @Test
    public void testOnCollectAppliesFreeze() {
        glueTrap.onCollect(realBoard);
        assertTrue(glueTrap.isCollected(), "GlueTrap should be marked as collected");
        assertEquals(1, realBoard.getStuckCount(), "Board's stuckCount should be 1 after triggering a GlueTrap.");
    }
}
