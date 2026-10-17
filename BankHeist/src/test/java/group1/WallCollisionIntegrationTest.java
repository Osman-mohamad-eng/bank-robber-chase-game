package group1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WallCollisionIntegrationTest {
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
        customMap[1] = "XT                      X"; 
        for (int i = 2; i < 13; i++) customMap[i] = "X                       X";
        customMap[13] = "XXXXXXXXXXXXXXXXXXXXXXXXX";
        testBoard = Board.getInstance(customMap);
    }

    // Verifies that the Thief cannot move into a Wall
    @Test
    public void testThiefCannotMoveIntoWall() {
        assertEquals(1, testBoard.getThiefRow());
        assertEquals(1, testBoard.getThiefCol());
        testBoard.moveThief(0, -1);
        assertEquals(1, testBoard.getThiefRow(), "Row should remain unchanged.");
        assertEquals(1, testBoard.getThiefCol(), "Column should remain unchanged after hitting a wall.");
    }
}