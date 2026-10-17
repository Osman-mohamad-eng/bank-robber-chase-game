package group1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 * Tests for PositionTest class.
 *
 * @author Dev
 * @version 1.0
 */
public class PositionTest {

    @Test
    public void testInitialCoordinates() {
        Position p = new Position(2, 3);

        assertEquals(2, p.getX());
        assertEquals(3, p.getY());
    }

    @Test
    public void testAddCreatesShiftedPosition() {
        Position p = new Position(1, 1);
        Position shifted = p.add(2, 3);

        assertEquals(new Position(3, 4), shifted);
    }

    @Test
    public void testDistanceCalculation() {
        Position p1 = new Position(0, 0);
        Position p2 = new Position(3, 4);

        assertEquals(5.0, p1.distanceTo(p2));
    }

    @Test
    public void testToString() {
        Position p = new Position(2, 3);
        assertEquals("(2, 3)", p.toString());
    }
}
