package group1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;


/**
 * Test cases for the Thief class.
 *
 * This class tests movement behavior, including valid movement
 * and boundary conditions to ensure the thief cannot move outside the board.
 */
public class ThiefTest {
    @Test
    void testStartingPosition() {
        Thief thief = new Thief(2, 3);
        assertEquals(2, thief.getRow());
        assertEquals(3, thief.getCol());
    }

    @Test
    void testMoveDown() {
        Thief thief = new Thief(2, 3);
        thief.move(1, 0);
        assertEquals(3, thief.getRow());
        assertEquals(3, thief.getCol());
    }

    @Test
    void testMoveRight() {
        Thief thief = new Thief(2, 3);
        thief.move(0, 1);
        assertEquals(2, thief.getRow());
        assertEquals(4, thief.getCol());
    }

    @Test
    void testSeveralMoves() {
        Thief thief = new Thief(5, 5);
        thief.move(1, 0);
        thief.move(0, -1);
        thief.move(-1, 0);
        assertEquals(5, thief.getRow());
        assertEquals(4, thief.getCol());
    }
}