package group1;

import group1.results.HeistResult;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SessionTelemetryTest {
    @Test
    void tracksGrossCollectionsAndPenaltiesWithoutCountingPickupsTwice() {
        Board.resetInstance();
        Board board = new BoardFactory().makeBoard(1);
        board.setPaused(true);
        CashStack cash = new CashStack();
        RareDiamond diamond = new RareDiamond();
        InkDart penalty = new InkDart();
        cash.onCollect(board);
        cash.onCollect(board);
        diamond.onCollect(board);
        diamond.onCollect(board);
        penalty.onCollect(board);
        assertEquals(100, board.getCashCollected());
        assertEquals(1, board.getDiamondsCollected());
        assertEquals(150, board.getPenaltiesIncurred());
        assertEquals(450, board.getBalance());
        board.reset();
        board.setPaused(true);
        assertEquals(0, board.getCashCollected());
        assertEquals(0, board.getDiamondsCollected());
        assertEquals(0, board.getPenaltiesIncurred());
    }

    @Test
    @SuppressWarnings("unchecked")
    void guardOnExitProducesOneLossAndNeverReportsWin() throws Exception {
        Board.resetInstance();
        Board board = new BoardFactory().makeBoard(1);
        board.setPaused(true);
        Field thiefField = Board.class.getDeclaredField("thief");
        thiefField.setAccessible(true);
        Field position = MovingEntity.class.getDeclaredField("position");
        position.setAccessible(true);
        position.set(thiefField.get(board), new Position(22, 11));
        Field guardsField = Board.class.getDeclaredField("guards");
        guardsField.setAccessible(true);
        List<Guard> guards = (List<Guard>) guardsField.get(board);
        guards.clear();
        guards.add(new Guard(1, new Position(23, 11)));
        Field remaining = Board.class.getDeclaredField("staticMoneyCount");
        remaining.setAccessible(true);
        remaining.setInt(board, 0);
        List<String> outcomes = new ArrayList<>();
        board.setGameEvents(new GameEvents() {
            public void onWin() { outcomes.add("WON"); }
            public void onGameOver() { outcomes.add("LOST"); }
        });
        board.setPaused(false);
        board.moveThief(0, 1);
        board.moveThief(0, -1);
        board.incurPenalty(150);
        assertEquals(List.of("LOST"), outcomes);
        board.setPaused(true);
    }

    @Test
    void guardReachingExitDuringSameMovementTickTakesPriorityOverEscape() throws Exception {
        Board.resetInstance();
        String[] map = new String[14];
        map[0] = "XXXXXXXXXXXXXXXXXXXXXXXXX";
        for (int row = 1; row < 13; row++) map[row] = "X                       X";
        map[13] = "XXXXXXXXXXXXXXXXXXXXXXXXX";
        char[] guardRow = map[10].toCharArray();
        guardRow[23] = 'G';
        map[10] = new String(guardRow);
        char[] escapeRow = map[11].toCharArray();
        escapeRow[22] = 'T';
        escapeRow[23] = 'E';
        map[11] = new String(escapeRow);

        Board board = Board.getInstance(map);
        board.setPaused(true);
        Field timerField = Board.class.getDeclaredField("gameTimer");
        timerField.setAccessible(true);
        javax.swing.Timer timer = (javax.swing.Timer) timerField.get(board);
        timer.stop();
        Field inputColumn = Board.class.getDeclaredField("inputCol");
        inputColumn.setAccessible(true);
        inputColumn.setInt(board, 1);
        Field accumulator = Board.class.getDeclaredField("guardAccumulator");
        accumulator.setAccessible(true);
        accumulator.setDouble(board, 1.5);
        List<String> outcomes = new ArrayList<>();
        board.setGameEvents(new GameEvents() {
            public void onWin() { outcomes.add("WON"); }
            public void onGameOver() { outcomes.add("LOST"); }
        });
        try {
            board.setPaused(false);
            javax.swing.SwingUtilities.invokeAndWait(() -> timer.getActionListeners()[0]
                    .actionPerformed(new java.awt.event.ActionEvent(board, 0, "tick")));
            assertEquals(new Position(23, 11), board.getGuards().get(0).getPosition());
            assertEquals(List.of("LOST"), outcomes,
                    "The complete movement tick must resolve guard capture before publishing escape");
        } finally {
            board.setPaused(true);
            timer.stop();
        }
    }

    @Test
    void reportsOneImmutableSnapshotPerAttemptAndIgnoresMenus() {
        System.setProperty("java.awt.headless", "true");
        FakeSession session = new FakeSession();
        List<HeistResult> results = new ArrayList<>();
        Interface ui = new Interface(session, results::add, UUID.randomUUID(), 3);
        ui.setState(Interface.GameState.WIN);
        assertTrue(results.isEmpty());
        ui.setState(Interface.GameState.PLAYING);
        ui.setState(Interface.GameState.PAUSED);
        assertTrue(results.isEmpty());
        ui.setState(Interface.GameState.PLAYING);
        session.events.onWin();
        session.events.onWin();
        assertEquals(1, results.size());
        HeistResult first = results.get(0);
        assertEquals(1000, first.cashCollected());
        assertEquals(1200, first.finalScore());
        assertEquals("WON", first.outcome());
        session.reset();
        assertEquals(1200, first.finalScore());
        ui.setState(Interface.GameState.PLAYING);
        session.events.onGameOver();
        assertEquals(2, results.size());
        assertNotEquals(first.sessionId(), results.get(1).sessionId());
        ui.setState(Interface.GameState.MAIN_MENU);
    }

    private static final class FakeSession implements GameSession {
        GameEvents events;
        int balance = 1200;
        public int getBalance() { return balance; }
        public long getCashCollected() { return 1000; }
        public int getDiamondsCollected() { return 1; }
        public long getPenaltiesIncurred() { return 300; }
        public void setPaused(boolean paused) { }
        public void reset() { balance = 0; }
        public void setGameEvents(GameEvents events) { this.events = events; }
    }
}
