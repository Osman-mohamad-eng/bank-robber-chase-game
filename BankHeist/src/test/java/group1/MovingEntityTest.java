package group1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for MovingEntityTest class.
 *
 * @author Dev
 * @version 1.0
 */
public class MovingEntityTest {

    private Guard guard;
    private Board realBoard;

    @BeforeEach
    public void setUp() {
        guard = new Guard(1, new Position(1, 1));

        BoardFactory factory = new BoardFactory();
        realBoard = factory.makeBoard(1);
    }

    @Test
    public void testMoveToChangesPosition() {
        guard.moveTo(new Position(2, 2));

        assertEquals(new Position(2, 2), guard.getPosition());
    }

    @Test
    public void testCanMoveInsideBoard() {
        boolean canMove = guard.canMoveTo(new Position(3, 3), realBoard);

        assertTrue(canMove);
    }

    @Test
    public void testCannotMoveOutsideBoard() {
        boolean canMove = guard.canMoveTo(new Position(-1, -1), realBoard);

        assertFalse(canMove);
    }

    @Test
    public void testGetId() {
        Guard g = new Guard(99, new Position(1, 1));
        assertEquals(99, g.getId());
    }
}
