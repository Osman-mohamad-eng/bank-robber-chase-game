package group1;

import java.awt.event.ActionEvent;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GlueTrapIntegrationTest {
    private Board testBoard;

    // Sets up the test without the GUI
    @BeforeAll
    public static void initHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    @BeforeEach
    public void setUp() {
        Board.resetInstance();

        String[] map = new String[14];
        map[0] = "XXXXXXXXXXXXXXXXXXXXXXXXX";
        map[1] = "XTS                      X";
        for (int i = 2; i < 13; i++) {
            map[i] = "X                       X";
        }
        map[13] = "XXXXXXXXXXXXXXXXXXXXXXXXX";

        testBoard = Board.getInstance(map);
    }

    // Automatically verifies the Thief's 1-tick penalty
    @Test
    public void testThiefGlueInteraction() {
        assertEquals(0, testBoard.getStuckCount(), "Thief should start with 0 stuck ticks.");
        testBoard.moveThief(0, 1);
        assertEquals(1, testBoard.getStuckCount(), "Thief should be stuck for 1 tick after moving onto the trap.");
    }

    // Verifies the Guard's independent state tracker exists
@Test
public void testGuardGlueState() {
    Board.resetInstance();

    String[] guardMap = new String[14];
    guardMap[0] = "XXXXXXXXXXXXXXXXXXXXXXXXX";
    guardMap[1] = "XT  G                   X";
    for (int i = 2; i < 13; i++) {
        guardMap[i] = "X                       X";
    }
    guardMap[13] = "XXXXXXXXXXXXXXXXXXXXXXXXX";

    Board guardBoard = Board.getInstance(guardMap);
    Guard guard = guardBoard.getGuards().get(0);

    assertEquals(0, guard.getStuckCount(),
            "Guard should have an independent stuck counter initialized at 0.");
}

    // Thief cannot move while stuck
    @Test
    public void testThiefCannotMoveWhileFrozen() {
        testBoard.applyFreeze(1);
        testBoard.moveThief(0, 1);
        // still frozen — position should not have changed
        assertEquals(0, testBoard.getStuckCount(), "stuckCount should have decremented");
    }

    // Thief freeze expires after 1 tick and movement resumes
    @Test
    public void testThiefFreezeExpiresAfterOneTick() {
        testBoard.moveThief(0, 1); // steps on glue, stuckCount = 1
        assertEquals(1, testBoard.getStuckCount());

        testBoard.moveThief(0, 1); // blocked tick — decrements stuckCount to 0
        assertEquals(0, testBoard.getStuckCount(), "freeze should expire after 1 blocked tick");
    }

    // Guard frozen for 3 ticks after stepping on a glue trap
    @Test
    public void testGuardFrozenForThreeTicksAfterGlue() throws Exception {
        Board.resetInstance();

        String[] guardMap = new String[14];
        guardMap[0]  = "XXXXXXXXXXXXXXXXXXXXXXXXX";
        guardMap[1]  = "XT  SG                  X";
        for (int i = 2; i < 13; i++) {
            guardMap[i] = "X                       X";
        }
        guardMap[13] = "XXXXXXXXXXXXXXXXXXXXXXXXX";

        Board guardBoard = Board.getInstance(guardMap);
        Field timerField = Board.class.getDeclaredField("gameTimer");
        timerField.setAccessible(true);
        javax.swing.Timer timer = (javax.swing.Timer) timerField.get(guardBoard);
        timer.stop();
        Guard guard = guardBoard.getGuards().get(0);
        guard.freeze(0);
        Field accField = Board.class.getDeclaredField("guardAccumulator");
        accField.setAccessible(true);
        accField.setDouble(guardBoard, 1.0);
        guardBoard.setPaused(false);
        timer.getActionListeners()[0].actionPerformed(new ActionEvent(guardBoard, 0, ""));
        assertEquals(3, guard.getStuckCount(),
                "guard should be frozen for 3 ticks after stepping on a glue trap");
    }

    // Guard stuck count decrements each tick
    @Test
    public void testGuardStuckCountDecrementsEachTick() throws Exception {
        Board.resetInstance();

        String[] guardMap = new String[14];
        guardMap[0]  = "XXXXXXXXXXXXXXXXXXXXXXXXX";
        guardMap[1]  = "XT  G                   X";
        for (int i = 2; i < 13; i++) {
            guardMap[i] = "X                       X";
        }
        guardMap[13] = "XXXXXXXXXXXXXXXXXXXXXXXXX";

        Board guardBoard = Board.getInstance(guardMap);
        Guard guard = guardBoard.getGuards().get(0);
        guard.freeze(3);

        Field timerField = Board.class.getDeclaredField("gameTimer");
        timerField.setAccessible(true);
        javax.swing.Timer timer = (javax.swing.Timer) timerField.get(guardBoard);
        timer.stop();

        guardBoard.setPaused(false);

        for (int expected = 2; expected >= 0; expected--) {
            timer.getActionListeners()[0].actionPerformed(new ActionEvent(guardBoard, 0, ""));
            assertEquals(expected, guard.getStuckCount(),
                    "guard stuck count should be " + expected + " after this tick");
        }
    }
}