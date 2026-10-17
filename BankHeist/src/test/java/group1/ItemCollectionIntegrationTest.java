package group1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ItemCollectionIntegrationTest {
    private Board testBoard;

    // Sets up the test without the GUI
    @BeforeAll
    public static void initHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    // Resets the board and creates a custom map just for testing
    @BeforeEach
    public void setUp() {
        Board.resetInstance();

        String[] customMap = new String[14];
        customMap[0] = "XXXXXXXXXXXXXXXXXXXXXXXXX";
        customMap[1] = "XTM                     X";
        
        for (int i = 2; i < 13; i++) {
            customMap[i] = "X                       X";
        }
        customMap[13] = "XXXXXXXXXXXXXXXXXXXXXXXXX";

        testBoard = Board.getInstance(customMap);
    }

    // Verifies that the Thief moving onto a CashStack updates the balance, money count, and removes the item
    @Test
    public void testThiefCollectingCashStackUpdatesBoard() {
        assertEquals(0, testBoard.getBalance(), "Initial balance should be 0");
        assertEquals(1, testBoard.getStaticMoneyCount(), "There should be exactly 1 static money bag on the board.");
        testBoard.moveThief(0, 1);
        assertEquals(100, testBoard.getBalance(), "Board balance should increase by 100 after collection.");
        assertEquals(0, testBoard.getStaticMoneyCount(), "The static money count should decrease to 0.");
        assertTrue(testBoard.getInteractableCells().isEmpty(), "The CashStack should be removed from the interactables set.");
    }
}
