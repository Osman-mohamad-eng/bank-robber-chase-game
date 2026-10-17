package group1;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
/**
 * Tests for GuardTest class.
 * 
 * @author Dev
 * @version 1.0
 */
public class GuardTest {

    private Guard guard;
    private Board realBoard;

    @BeforeEach
    public void setUp() {
        guard = new Guard(1, new Position(5, 5));

        BoardFactory factory = new BoardFactory();
        realBoard = factory.makeBoard(1);
    }

    @Test
    public void testInitialPosition() {
        assertEquals(new Position(5,5), guard.getPosition(),
                "Guard should start at the given position.");
    }

    @Test
    public void testUpdateMovesTowardThief() {
        Position thief = new Position(6,5);

        guard.update(thief, realBoard);

        assertEquals(new Position(6,5), guard.getPosition(),
                "Guard should move one step toward the thief.");
    }

    @Test
    public void testMultipleUpdates() {
        Position thief = new Position(8,5);

        guard.update(thief, realBoard);
        guard.update(thief, realBoard);

        assertNotEquals(new Position(5,5), guard.getPosition(),
                "Guard should move closer after multiple updates.");
    }
}