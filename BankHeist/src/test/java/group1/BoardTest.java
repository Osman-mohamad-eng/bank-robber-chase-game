package group1;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Board tests: balance, pausing, reset, visual state animation

public class BoardTest {


    private Board board;

    @BeforeAll
    public static void initHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    @BeforeEach
    public void setUp() throws Exception {
        // Resets the singleton so each test gets a fresh Board
        Field instanceField = Board.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        BoardFactory factory = new BoardFactory();
        board = factory.makeBoard(1);
    }

    // getBalance / addBalance

    @Test
    public void testInitialBalanceIsZero() {
        assertEquals(0, board.getBalance(),
                "initial balance should be 0");
    }

    @Test
    public void testAddPositiveBalance() {
        board.addBalance(100);
        assertEquals(100, board.getBalance());
    }

    @Test
    public void testAddMultipleBalances() {
        board.addBalance(50);
        board.addBalance(30);
        assertEquals(80, board.getBalance());
    }

    @Test
    public void testAddNegativeBalanceReduces() {
        board.addBalance(200);
        board.addBalance(-50);
        assertEquals(150, board.getBalance());
    }

    @Test
    public void testNegativeBalanceTriggersGameOver() throws Exception {
        // Set up an Interface so triggerGameOver() can call gameInterface.setState(GAME_OVER)
        Interface ui = new Interface(board);
        board.setInterface(ui);
        ui.setState(Interface.GameState.PLAYING);

        board.addBalance(-10);

        // The balance should be negativee and game state should be GAME_OVER
        assertTrue(board.getBalance() < 0);
        assertEquals(Interface.GameState.GAME_OVER, ui.getState(),
                "negative balance should trigger GAME_OVER state");
    }

    @Test
    public void testAddZeroBalance() {
        board.addBalance(0);
        assertEquals(0, board.getBalance());
    }

    // setPaused

    @Test
    public void testSetPausedTrue() throws Exception {
        board.setPaused(true);
        Field pausedField = Board.class.getDeclaredField("paused");
        pausedField.setAccessible(true);
        assertTrue((boolean) pausedField.get(board),
                "board should be paused after setPaused(true)");
    }

    @Test
    public void testSetPausedFalse() throws Exception {
        board.setPaused(true);
        board.setPaused(false);
        Field pausedField = Board.class.getDeclaredField("paused");
        pausedField.setAccessible(true);
        assertFalse((boolean) pausedField.get(board),
                "board should not be paused after setPaused(false)");
    }

    @Test
    public void testSetPausedStopsDiamondSpawner() throws Exception {
        board.setPaused(true);
        Field spawnerField = Board.class.getDeclaredField("diamondSpawner");
        spawnerField.setAccessible(true);
        DiamondSpawner spawner = (DiamondSpawner) spawnerField.get(board);
        assertFalse(spawner.isRunning(),
                "diamond spawner should be stopped when paused");
    }

    @Test
    public void testSetPausedResumesDiamondSpawner() throws Exception {
        board.setPaused(true);
        board.setPaused(false);
        Field spawnerField = Board.class.getDeclaredField("diamondSpawner");
        spawnerField.setAccessible(true);
        DiamondSpawner spawner = (DiamondSpawner) spawnerField.get(board);
        assertTrue(spawner.isRunning(),
                "diamond spawner should resume when unpaused");
    }

    // setInterface

    @Test
    public void testSetInterface() throws Exception {
        Interface ui = new Interface(board);
        board.setInterface(ui);

        Field ifaceField = Board.class.getDeclaredField("gameInterface");
        ifaceField.setAccessible(true);
        assertSame(ui, ifaceField.get(board),
                "setInterface should store the given Interface reference");
    }

    @Test
    public void testSetInterfaceNull() throws Exception {
        board.setInterface(null);
        Field ifaceField = Board.class.getDeclaredField("gameInterface");
        ifaceField.setAccessible(true);
        assertNull(ifaceField.get(board));
    }

    // reset

    @Test
    public void testResetClearsBalance() {
        board.addBalance(500);
        board.reset();
        assertEquals(0, board.getBalance(),
                "balance should be 0 after reset");
    }

    @Test
    public void testResetClearsAngles() throws Exception {
        Field thiefAngleField = Board.class.getDeclaredField("thiefAngle");
        thiefAngleField.setAccessible(true);
        thiefAngleField.setDouble(board, 90.0);

        board.reset();

        assertEquals(0.0, thiefAngleField.getDouble(board),
                "thief angle should reset to 0");
    }

    @Test
    public void testResetClearsGuardAngle() throws Exception {
        Field guardAngleField = Board.class.getDeclaredField("guardAngle");
        guardAngleField.setAccessible(true);
        guardAngleField.setDouble(board, 180.0);

        board.reset();

        assertEquals(0.0, guardAngleField.getDouble(board),
                "guard angle should reset to 0");
    }

    @Test
    public void testResetClearsStuckCount() throws Exception {
        board.applyFreeze(5);
        board.reset();

        Field stuckField = Board.class.getDeclaredField("stuckCount");
        stuckField.setAccessible(true);
        assertEquals(0, stuckField.getInt(board),
                "stuckCount should be 0 after reset");
    }

    @Test
    public void testResetClearsActiveDiamond() {
        board.reset();
        assertFalse(board.hasActiveDiamond(),
                "no active diamond after reset");
    }

    @Test
    public void testResetClearsInputState() throws Exception {
        Field inputRowField = Board.class.getDeclaredField("inputRow");
        inputRowField.setAccessible(true);
        Field inputColField = Board.class.getDeclaredField("inputCol");
        inputColField.setAccessible(true);

        inputRowField.setInt(board, 1);
        inputColField.setInt(board, -1);

        board.reset();

        assertEquals(0, inputRowField.getInt(board), "inputRow should be 0 after reset");
        assertEquals(0, inputColField.getInt(board), "inputCol should be 0 after reset");
    }

    // VisualState inner class (tested via reflection)

    @Test
    public void testVisualStateSetInitialPosition() throws Exception {
        Object vs = createVisualState();
        invokeSetInitialPosition(vs, 3, 5, 64, 90.0);

        assertEquals(3 * 64.0, getDoubleField(vs, "curX"));
        assertEquals(5 * 64.0, getDoubleField(vs, "curY"));
        assertEquals(3 * 64.0, getDoubleField(vs, "startX"));
        assertEquals(5 * 64.0, getDoubleField(vs, "startY"));
        assertEquals(3 * 64.0, getDoubleField(vs, "targetX"));
        assertEquals(5 * 64.0, getDoubleField(vs, "targetY"));
        assertEquals(90.0, getDoubleField(vs, "angle"));
        assertEquals(1.0, getDoubleField(vs, "progress"));
    }

    @Test
    public void testVisualStateStartDash() throws Exception {
        Object vs = createVisualState();
        invokeStartDash(vs, 1, 1, 2, 1, 64, 90.0);

        assertEquals(1 * 64.0, getDoubleField(vs, "startX"));
        assertEquals(1 * 64.0, getDoubleField(vs, "startY"));
        assertEquals(2 * 64.0, getDoubleField(vs, "targetX"));
        assertEquals(1 * 64.0, getDoubleField(vs, "targetY"));
        assertEquals(1.0, getDoubleField(vs, "dirX"));
        assertEquals(0.0, getDoubleField(vs, "dirY"));
        assertEquals(90.0, getDoubleField(vs, "angle"));
        assertEquals(0.0, getDoubleField(vs, "progress"));
    }

    @Test
    public void testVisualStateUpdateProgressIncreases() throws Exception {
        Object vs = createVisualState();
        invokeStartDash(vs, 0, 0, 1, 0, 64, 0.0);

        invokeUpdate(vs, 0.3);

        double progress = getDoubleField(vs, "progress");
        assertEquals(0.3, progress, 0.001);
    }

    @Test
    public void testVisualStateUpdateClampedAtOne() throws Exception {
        Object vs = createVisualState();
        invokeStartDash(vs, 0, 0, 1, 0, 64, 0.0);

        invokeUpdate(vs, 1.5); // overshoot

        assertEquals(1.0, getDoubleField(vs, "progress"), 0.001,
                "progress should not go above 1.0");
    }

    @Test
    public void testVisualStateInterpolatesPosition() throws Exception {
        Object vs = createVisualState();
        invokeStartDash(vs, 0, 0, 2, 0, 64, 90.0);

        // At 50% progress, curX should be midway between 0 and 128
        invokeUpdate(vs, 0.5);

        double curX = getDoubleField(vs, "curX");
        assertEquals(64.0, curX, 1.0,
                "at 50% progress, curX should be halfway");
    }

    @Test
    public void testVisualStateFullDashReachesTarget() throws Exception {
        Object vs = createVisualState();
        invokeStartDash(vs, 1, 2, 3, 4, 64, 180.0);

        invokeUpdate(vs, 1.0);

        assertEquals(3 * 64.0, getDoubleField(vs, "curX"), 0.001);
        assertEquals(4 * 64.0, getDoubleField(vs, "curY"), 0.001);
    }

    @Test
    public void testVisualStateUpdateNoOpWhenComplete() throws Exception {
        Object vs = createVisualState();
        invokeSetInitialPosition(vs, 5, 5, 64, 0.0);

        // progress is already 1.0 after setInitialPosition
        double beforeX = getDoubleField(vs, "curX");
        invokeUpdate(vs, 0.5);
        double afterX = getDoubleField(vs, "curX");

        assertEquals(beforeX, afterX,
                "already at destination, update should do nothing");
    }

    @Test
    public void testVisualStateDiagonalDash() throws Exception {
        Object vs = createVisualState();
        invokeStartDash(vs, 0, 0, 3, 3, 64, 45.0);

        invokeUpdate(vs, 1.0);

        assertEquals(3 * 64.0, getDoubleField(vs, "curX"), 0.001);
        assertEquals(3 * 64.0, getDoubleField(vs, "curY"), 0.001);
    }

    // paintComponent smoke test (exercises drawEntityWithTrail)

    @Test
    public void testPaintComponentDoesNotThrow() {
        board.setSize(1600, 896);
        BufferedImage img = new BufferedImage(1600, 896, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        assertDoesNotThrow(() -> board.paintComponent(g));
        g.dispose();
    }

    @Test
    public void testPaintComponentAfterReset() {
        board.reset();
        board.setSize(1600, 896);
        BufferedImage img = new BufferedImage(1600, 896, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        assertDoesNotThrow(() -> board.paintComponent(g));
        g.dispose();
    }

    // applyFreeze

    @Test
    public void testApplyFreeze() throws Exception {
        board.applyFreeze(3);
        Field stuckField = Board.class.getDeclaredField("stuckCount");
        stuckField.setAccessible(true);
        assertEquals(3, stuckField.getInt(board));
    }

    @Test
    public void testApplyFreezeZero() throws Exception {
        board.applyFreeze(0);
        Field stuckField = Board.class.getDeclaredField("stuckCount");
        stuckField.setAccessible(true);
        assertEquals(0, stuckField.getInt(board));
    }

    // Diamond spawning/despawning

    @Test
    public void testHasActiveDiamondInitiallyFalse() {
        assertFalse(board.hasActiveDiamond());
    }

    @Test
    public void testCreateAndRemoveDiamond() {
        board.createRandomDiamond();
        // Diamond may or may not spawn (depends on random position finding a valid spot)
        // If it spawned, remove should work
        if (board.hasActiveDiamond()) {
            board.removeActiveDiamond();
            assertFalse(board.hasActiveDiamond());
        }
    }

    @Test
    public void testRemoveActiveDiamondWhenNoneExistsIsNoOp() {
        assertDoesNotThrow(() -> board.removeActiveDiamond());
        assertFalse(board.hasActiveDiamond());
    }

    @Test
    public void testRestartDiamondTimerDoesNotThrow() {
        assertDoesNotThrow(() -> board.restartDiamondTimer());
    }

    // moveThief — rotation, stuck, paused, guard collision

    @Test
    public void testMoveThiefUpSetsAngleZero() throws Exception {
        Board.resetInstance();
        Board b = Board.getInstance(openMap("T"));
        b.moveThief(-1, 0);

        Field angle = Board.class.getDeclaredField("thiefAngle");
        angle.setAccessible(true);
        assertEquals(0.0, angle.getDouble(b), "up should set thiefAngle to 0");
    }

    @Test
    public void testMoveThiefDownSetsAngle180() throws Exception {
        Board.resetInstance();
        Board b = Board.getInstance(openMap("T"));
        b.moveThief(1, 0);

        Field angle = Board.class.getDeclaredField("thiefAngle");
        angle.setAccessible(true);
        assertEquals(180.0, angle.getDouble(b), "down should set thiefAngle to 180");
    }

    @Test
    public void testMoveThiefLeftSetsAngle270() throws Exception {
        Board.resetInstance();
        Board b = Board.getInstance(openMap("T"));
        // thief starts at col 2, move left
        b.moveThief(0, -1);

        Field angle = Board.class.getDeclaredField("thiefAngle");
        angle.setAccessible(true);
        assertEquals(270.0, angle.getDouble(b), "left should set thiefAngle to 270");
    }

    @Test
    public void testMoveThiefRightSetsAngle90() throws Exception {
        Board.resetInstance();
        Board b = Board.getInstance(openMap("T"));
        b.moveThief(0, 1);

        Field angle = Board.class.getDeclaredField("thiefAngle");
        angle.setAccessible(true);
        assertEquals(90.0, angle.getDouble(b), "right should set thiefAngle to 90");
    }

    @Test
    public void testMoveThiefWhilePausedIsNoOp() throws Exception {
        Board.resetInstance();
        Board b = Board.getInstance(openMap("T"));
        b.setPaused(true);

        int rowBefore = b.getThiefRow();
        int colBefore = b.getThiefCol();
        b.moveThief(0, 1);

        assertEquals(rowBefore, b.getThiefRow(), "row should not change when paused");
        assertEquals(colBefore, b.getThiefCol(), "col should not change when paused");
    }

    @Test
    public void testMoveThiefZeroInputIsNoOp() throws Exception {
        Board.resetInstance();
        Board b = Board.getInstance(openMap("T"));

        int rowBefore = b.getThiefRow();
        int colBefore = b.getThiefCol();
        b.moveThief(0, 0);

        assertEquals(rowBefore, b.getThiefRow(), "row should not change with zero input");
        assertEquals(colBefore, b.getThiefCol(), "col should not change with zero input");
    }

    @Test
    public void testMoveThiefStuckDecrementsAndBlocks() throws Exception {
        Board.resetInstance();
        Board b = Board.getInstance(openMap("T"));
        b.applyFreeze(2);

        int colBefore = b.getThiefCol();
        b.moveThief(0, 1);

        assertEquals(1, b.getStuckCount(), "stuckCount should decrement by 1");
        assertEquals(colBefore, b.getThiefCol(), "thief should not move while stuck");
    }

    @Test
    public void testMoveThiefIntoWallBlocks() throws Exception {
        Board.resetInstance();
        Board b = Board.getInstance(openMap("T"));
        // thief at row 2, col 2 — row 0 is all walls, moving up twice hits wall
        b.moveThief(-1, 0); // row 1
        int rowAfterFirst = b.getThiefRow();
        b.moveThief(-1, 0); // row 0 is wall

        assertEquals(rowAfterFirst, b.getThiefRow(), "thief should not move into a wall");
    }

    @Test
    public void testMoveThiefGuardCollisionTriggersGameOver() throws Exception {
        Board.resetInstance();
        // place guard one cell right of thief
        String[] map = openMap("TG");
        Board b = Board.getInstance(map);
        Interface ui = new Interface(b);
        b.setInterface(ui);
        ui.setState(Interface.GameState.PLAYING);

        b.moveThief(0, 1);

        assertEquals(Interface.GameState.GAME_OVER, ui.getState(),
                "moving onto guard should trigger game over");
    }

    // Helper method to perform out of bounds tests
    private void teleportThief(int col, int row) throws Exception{
        Field thiefField = Board.class.getDeclaredField("thief");
        thiefField.setAccessible(true);
        Thief thief = (Thief) thiefField.get(board);
        
        Field positionField = MovingEntity.class.getDeclaredField("position");
        positionField.setAccessible(true);
        positionField.set(thief, new Position(col, row));
    }

    @Test
    public void testCannotMoveLeftOutOfBounds() throws Exception {
        teleportThief(0, 0);
        board.moveThief(0, -1);

        assertEquals(0, board.getThiefRow());
        assertEquals(0, board.getThiefCol());
    }

    @Test
    public void testCannotMoveUpOutOfBounds() throws Exception {
        teleportThief(0, 0);
        board.moveThief(-1, 0);

        assertEquals(0, board.getThiefRow());
        assertEquals(0, board.getThiefCol());
    }

    @Test
    public void testCannotMoveRightOutOfBounds() throws Exception {
        teleportThief(24, 0);
        board.moveThief(0, 1);

        assertEquals(0, board.getThiefRow());
        assertEquals(24, board.getThiefCol());
    }

    @Test
    public void testCannotMoveDownOutOfBounds() throws Exception {
        teleportThief(0, 13);
        board.moveThief(1, 0);

        assertEquals(13, board.getThiefRow());
        assertEquals(0, board.getThiefCol());
    }

    // key bindings — WASD actions set inputRow / inputCol

    @Test
    public void testKeyBindingWSetsInputUp() throws Exception {
        javax.swing.ActionMap am = board.getActionMap();
        am.get("up").actionPerformed(new java.awt.event.ActionEvent(board, 0, ""));

        Field inputRow = Board.class.getDeclaredField("inputRow");
        inputRow.setAccessible(true);
        Field inputCol = Board.class.getDeclaredField("inputCol");
        inputCol.setAccessible(true);

        assertEquals(-1, inputRow.getInt(board), "W should set inputRow to -1");
        assertEquals(0, inputCol.getInt(board), "W should set inputCol to 0");
    }

    @Test
    public void testKeyBindingSSetsInputDown() throws Exception {
        javax.swing.ActionMap am = board.getActionMap();
        am.get("down").actionPerformed(new java.awt.event.ActionEvent(board, 0, ""));

        Field inputRow = Board.class.getDeclaredField("inputRow");
        inputRow.setAccessible(true);
        Field inputCol = Board.class.getDeclaredField("inputCol");
        inputCol.setAccessible(true);

        assertEquals(1, inputRow.getInt(board), "S should set inputRow to 1");
        assertEquals(0, inputCol.getInt(board), "S should set inputCol to 0");
    }

    @Test
    public void testKeyBindingASetsInputLeft() throws Exception {
        javax.swing.ActionMap am = board.getActionMap();
        am.get("left").actionPerformed(new java.awt.event.ActionEvent(board, 0, ""));

        Field inputRow = Board.class.getDeclaredField("inputRow");
        inputRow.setAccessible(true);
        Field inputCol = Board.class.getDeclaredField("inputCol");
        inputCol.setAccessible(true);

        assertEquals(0, inputRow.getInt(board), "A should set inputRow to 0");
        assertEquals(-1, inputCol.getInt(board), "A should set inputCol to -1");
    }

    @Test
    public void testKeyBindingDSetsInputRight() throws Exception {
        javax.swing.ActionMap am = board.getActionMap();
        am.get("right").actionPerformed(new java.awt.event.ActionEvent(board, 0, ""));

        Field inputRow = Board.class.getDeclaredField("inputRow");
        inputRow.setAccessible(true);
        Field inputCol = Board.class.getDeclaredField("inputCol");
        inputCol.setAccessible(true);

        assertEquals(0, inputRow.getInt(board), "D should set inputRow to 0");
        assertEquals(1, inputCol.getInt(board), "D should set inputCol to 1");
    }

    // spawnRandomDiamond — paused and already-active guards

    @Test
    public void testSpawnDiamondWhilePausedDoesNothing() throws Exception {
        Board.resetInstance();
        Board b = Board.getInstance(openMap("T"));
        b.setPaused(true);
        b.createRandomDiamond();
        assertFalse(b.hasActiveDiamond(),
                "diamond should not spawn while paused");
    }

    @Test
    public void testSpawnDiamondWhenAlreadyActiveDoesNothing() throws Exception {
        Board.resetInstance();
        Board b = Board.getInstance(openMap("T"));
        b.createRandomDiamond();
        if (b.hasActiveDiamond()) {
            // try again — should not replace the existing one
            int sizeBefore = b.getInteractableCells().size();
            b.createRandomDiamond();
            assertEquals(sizeBefore, b.getInteractableCells().size(),
                    "second spawn should not add another diamond");
        }
    }

    // isInside / isWall / isOccupied

    @Test
    public void testIsInsideValidPosition() {
        assertTrue(board.isInside(new Position(5, 5)),
                "position inside the board should return true");
    }

    @Test
    public void testIsInsideNegativeCol() {
        assertFalse(board.isInside(new Position(-1, 5)),
                "negative column should be outside");
    }

    @Test
    public void testIsInsideNegativeRow() {
        assertFalse(board.isInside(new Position(5, -1)),
                "negative row should be outside");
    }

    @Test
    public void testIsInsideBeyondColumns() {
        assertFalse(board.isInside(new Position(25, 5)),
                "column 25 should be outside the 25-column board");
    }

    @Test
    public void testIsInsideBeyondRows() {
        assertFalse(board.isInside(new Position(5, 14)),
                "row 14 should be outside the 14-row board");
    }

    @Test
    public void testIsWallTrue() {
        // row 0 col 0 is always a wall in level 1
        assertTrue(board.isWall(new Position(0, 0)),
                "corner cell should be a wall");
    }

    @Test
    public void testIsWallFalseOnEmpty() {
        // row 5 col 1 is empty in level 1
        assertFalse(board.isWall(new Position(1, 5)),
                "empty cell should not be a wall");
    }

    @Test
    public void testIsOccupiedByThief() {
        // thief starts at col 15, row 11 in level 1
        assertTrue(board.isOccupied(new Position(15, 11)),
                "thief cell should be occupied");
    }

    @Test
    public void testIsOccupiedEmptyCell() {
        assertFalse(board.isOccupied(new Position(1, 5)),
                "empty cell should not be occupied");
    }

    @Test
    public void testIsOccupiedByGuard() {
        // guard starts at col 12, row 5 in level 1
        assertTrue(board.isOccupied(new Position(12, 5)),
                "guard cell should be occupied");
    }

    // decrementStaticMoney

    @Test
    public void testDecrementStaticMoney() {
        int before = board.getStaticMoneyCount();
        board.decrementStaticMoney();
        assertEquals(before - 1, board.getStaticMoneyCount(),
                "static money count should decrease by 1");
    }

    @Test
    public void testDecrementStaticMoneyToZero() {
        // decrement until zero
        int count = board.getStaticMoneyCount();
        for (int i = 0; i < count; i++) {
            board.decrementStaticMoney();
        }
        assertEquals(0, board.getStaticMoneyCount(),
                "static money count should reach 0");
    }

    // getStuckCount / getGuardStuckCount

    @Test
    public void testGetStuckCountInitiallyZero() {
        assertEquals(0, board.getStuckCount(),
                "stuck count should start at 0");
    }

    @Test
    public void testGetGuardStuckCountInitiallyZero() {
        Guard guard = new Guard(1, new Position(0, 0));
        assertEquals(0, guard.getStuckCount());
    }

    // resetInstance singleton

    @Test
    public void testResetInstanceAllowsNewBoard() throws Exception {
        Board.resetInstance();
        Board newBoard = Board.getInstance(openMap("T"));
        assertNotNull(newBoard, "new board should be created after resetInstance");
    }

    // gameMovement timer tick

    @Test
    public void testGameTimerTickWhilePausedIsNoOp() throws Exception {
        board.setPaused(true);
        int rowBefore = board.getThiefRow();

        Field timerField = Board.class.getDeclaredField("gameTimer");
        timerField.setAccessible(true);
        javax.swing.Timer timer = (javax.swing.Timer) timerField.get(board);
        timer.stop();
        timer.getActionListeners()[0].actionPerformed(
                new java.awt.event.ActionEvent(board, 0, ""));

        assertEquals(rowBefore, board.getThiefRow(),
                "thief should not move on a timer tick while paused");
    }

    // drawEntityWithTrail mid-dash

    @Test
    public void testPaintComponentDuringDash() throws Exception {
        Board.resetInstance();
        Board b = Board.getInstance(openMap("T"));
        b.moveThief(0, 1); // start a dash

        b.setSize(1600, 896);
        BufferedImage img = new BufferedImage(1600, 896, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // set thiefVisual progress < 1 to exercise trail drawing
        Field vsField = Board.class.getDeclaredField("thiefVisual");
        vsField.setAccessible(true);
        Object vs = vsField.get(b);
        Field progressField = vs.getClass().getDeclaredField("progress");
        progressField.setAccessible(true);
        progressField.setDouble(vs, 0.5);

        assertDoesNotThrow(() -> b.paintComponent(g),
                "painting mid-dash should not throw");
        g.dispose();
    }

    // helper — builds a 14x25 open map with the given content at row 2

    private static String[] openMap(String content) {
        String[] map = new String[14];
        map[0]  = "XXXXXXXXXXXXXXXXXXXXXXXXX";
        StringBuilder row1 = new StringBuilder("X  ");
        row1.append(content);
        while (row1.length() < 24) row1.append(' ');
        row1.append('X');
        map[1] = row1.toString();
        // thief row
        StringBuilder row2 = new StringBuilder("X  ");
        while (row2.length() < 24) row2.append(' ');
        row2.append('X');
        String emptyRow = row2.toString();
        // place thief at row 2 if not already in content
        if (!content.contains("T")) {
            map[2] = "X T                     X";
        } else {
            map[2] = emptyRow;
        }
        for (int i = 3; i < 13; i++) map[i] = emptyRow;
        map[13] = "XXXXXXXXXXXXXXXXXXXXXXXXX";
        return map;
    }

    // Reflection helpers for VisualState

    private Object createVisualState() throws Exception {
        Class<?>[] innerClasses = Board.class.getDeclaredClasses();
        Class<?> vsClass = null;
        for (Class<?> c : innerClasses) {
            if (c.getSimpleName().equals("VisualState")) {
                vsClass = c;
                break;
            }
        }
        assertNotNull(vsClass, "VisualState inner class should exist.");
        // VisualState is a non-static inner class, needs Board as enclosing instance
        java.lang.reflect.Constructor<?> ctor = vsClass.getDeclaredConstructor(Board.class);
        ctor.setAccessible(true);
        return ctor.newInstance(board);
    }

    private void invokeSetInitialPosition(Object vs, int col, int row, int cellSize, double angle) throws Exception {
        Method m = vs.getClass().getDeclaredMethod("setInitialPosition", int.class, int.class, int.class, double.class);
        m.setAccessible(true);
        m.invoke(vs, col, row, cellSize, angle);
    }

    private void invokeStartDash(Object vs, int oldCol, int oldRow, int newCol, int newRow, int cellSize, double angle) throws Exception {
        Method m = vs.getClass().getDeclaredMethod("startDash", int.class, int.class, int.class, int.class, int.class, double.class);
        m.setAccessible(true);
        m.invoke(vs, oldCol, oldRow, newCol, newRow, cellSize, angle);
    }

    private void invokeUpdate(Object vs, double speed) throws Exception {
        Method m = vs.getClass().getDeclaredMethod("update", double.class);
        m.setAccessible(true);
        m.invoke(vs, speed);
    }

    private double getDoubleField(Object obj, String fieldName) throws Exception {
        Field f = obj.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.getDouble(obj);
    }
}
