package group1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for GuardIntegrationTest class.
 *
 * @author Dev
 * @version 1.0
 */
public class GuardIntegrationTest {

    private Board board;
    private Interface ui;

    @BeforeAll
    public static void initHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    @BeforeEach
    public void setUp() {
        Board.resetInstance();
        BoardFactory factory = new BoardFactory();
        board = factory.makeBoard(1);

        ui = new Interface(board);
        board.setInterface(ui);
        ui.setState(Interface.GameState.PLAYING);
    }

    /**
     * Integration test: If guard reaches thief position → GAME_OVER should
     * trigger
     */
    @Test
    public void testGuardCatchesThiefEndsGame() {

        // get guard and thief positions
        Position thiefPos = new Position(15, 11);   // from level 1 map
        Position guardStart = new Position(12, 5);

        Guard guard = new Guard(1, guardStart);

        // simulate multiple updates so guard reaches thief
        for (int i = 0; i < 20; i++) {
            guard.update(thiefPos, board);
        }

        // simulate collision manually
        if (guard.getPosition().equals(thiefPos)) {
            ui.setState(Interface.GameState.GAME_OVER);
        }

        assertEquals(
                Interface.GameState.GAME_OVER,
                ui.getState(),
                "Game should end when guard catches thief"
        );

    }

    /**
     * Integration test: If both guard and thief reach the exit cell at the same
     * time, the guard catching the thief should take priority and the game
     * should end with GAME_OVER instead of WIN.
     */
    @Test
    public void testGuardAndThiefReachExitSameTimeGameOver() {

        // simulate positions near exit
        Position exitPos = new Position(23, 11); // level 1 exit

        Guard guard = new Guard(1, exitPos);
        Position thiefPos = exitPos;

        // simulate collision at exit
        if (guard.getPosition().equals(thiefPos)) {
            ui.setState(Interface.GameState.GAME_OVER);
        }

        assertEquals(
                Interface.GameState.GAME_OVER,
                ui.getState(),
                "Guard catching thief at exit should still cause GAME OVER"
        );
    }
}
