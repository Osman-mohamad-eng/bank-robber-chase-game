package group1;

import java.awt.Dimension;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Integration tests for the exit condition.
//  verifies that the thief can only escape through the exit after
// collecting all money bags. Tests both the blocked and winning cases
// using a real Board and Interface connected together

public class ExitIntegrationTest {

    private Board board;
    private Interface ui;

    @BeforeAll
    public static void initHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    @BeforeEach
    public void setUp() throws Exception {
        Field instanceField = Board.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        BoardFactory factory = new BoardFactory();
        board = factory.makeBoard(1);

        ui = new Interface(board);
        board.setInterface(ui);
        ui.setState(Interface.GameState.PLAYING);
    }

    @Test
    public void testExitWithAllMoneyCollectedTriggersWin() throws Exception {
        // All money already collected
        setStaticMoneyCount(0);

        // Teleport thief one step left of the exit (col 22, row 11) and move right into it
        teleportThief(22, 11);
        moveThief(0, 1);

        assertEquals(Interface.GameState.WIN, ui.getState(),
                "reaching the exit with all money collected should trigger WIN");
    }

    @Test
    public void testExitWithMoneyRemainingDoesNotWin() throws Exception {
        // Some money still on the board
        setStaticMoneyCount(3);

        teleportThief(22, 11);
        moveThief(0, 1);

        assertEquals(Interface.GameState.PLAYING, ui.getState(),
                "reaching the exit with money remaining should not trigger WIN");
    }

    @Test
    public void testExitWithOneMoneyRemainingDoesNotWin() throws Exception {
        setStaticMoneyCount(1);

        teleportThief(22, 11);
        moveThief(0, 1);

        assertEquals(Interface.GameState.PLAYING, ui.getState(),
                "reaching the exit with 1 money bag remaining should not trigger WIN");
    }

    @Test
    public void testExitBeforeAndAfterCollectingLastMoney() throws Exception {
        // First attempt — one money bag left, exit should be blocked
        setStaticMoneyCount(1);
        teleportThief(22, 11);
        moveThief(0, 1);
        assertEquals(Interface.GameState.PLAYING, ui.getState(),
                "exit should be blocked while money remains");

        // Simulate collecting the last money bag
        board.decrementStaticMoney();

        // Move away and back to exit
        teleportThief(22, 11);
        moveThief(0, 1);
        assertEquals(Interface.GameState.WIN, ui.getState(),
                "exit should open after all money is collected");
    }

    @Test
    public void testExitWithZeroBalanceStillWins() throws Exception {
        // Balance doesn't need to be positive — only money count matters for exit
        setStaticMoneyCount(0);
        board.addBalance(0);

        teleportThief(22, 11);
        moveThief(0, 1);

        assertEquals(Interface.GameState.WIN, ui.getState(),
                "exit should trigger WIN regardless of balance when money count is 0");
    }

    // set the number of remaining money bags
    private void setStaticMoneyCount(int count) throws Exception {
        Field f = Board.class.getDeclaredField("staticMoneyCount");
        f.setAccessible(true);
        f.setInt(board, count);
    }

    // move the thief to a specific grid position
    private void teleportThief(int col, int row) throws Exception {
        Field thiefField = Board.class.getDeclaredField("thief");
        thiefField.setAccessible(true);
        Thief thief = (Thief) thiefField.get(board);

        Field posField = MovingEntity.class.getDeclaredField("position");
        posField.setAccessible(true);
        posField.set(thief, new Position(col, row));

        Field thiefCellField = Board.class.getDeclaredField("thiefCell");
        thiefCellField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.HashSet<Cell> thiefCell = (java.util.HashSet<Cell>) thiefCellField.get(board);
        thiefCell.clear();

        Field cellSizeField = Board.class.getDeclaredField("cellSize");
        cellSizeField.setAccessible(true);
        int cellSize = cellSizeField.getInt(board);

        thiefCell.add(new Cell(null, new Position (col * cellSize, row * cellSize), new Dimension(cellSize, cellSize)));
    }

    // invoke moveThief directly
    private void moveThief(int dRow, int dCol) throws Exception {
        Field inputRowField = Board.class.getDeclaredField("inputRow");
        inputRowField.setAccessible(true);
        inputRowField.setInt(board, dRow);
        Field inputColField = Board.class.getDeclaredField("inputCol");
        inputColField.setAccessible(true);
        inputColField.setInt(board, dCol);

        Method mt = Board.class.getDeclaredMethod("moveThief", int.class, int.class);
        mt.setAccessible(true);
        mt.invoke(board, dRow, dCol);
    }
}
