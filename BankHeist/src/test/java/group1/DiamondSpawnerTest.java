package group1;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


/**
 * Test cases for the DiamondSpawner class.
 *
 * This class tests the timer behavior of the spawner starting,
 * stopping, and restarting the spawning process.
 */
public class DiamondSpawnerTest {
    private Board createTestBoard() {
        String[] map = new String[14];
        for (int i = 0; i < 14; i++) {
            // 25 spaces
            map[i] = "                         ";
        }
        return Board.getInstance(map);
    }


    @Test
    void testStartSpawner() {
        Board board = createTestBoard();
        DiamondSpawner spawner = new DiamondSpawner(board);
        spawner.start();
        assertTrue(spawner.isRunning());
        spawner.stop();
    }


    @Test
    void testStopSpawner() {
        Board board = createTestBoard();
        DiamondSpawner spawner = new DiamondSpawner(board);
        spawner.start();
        spawner.stop();
        assertFalse(spawner.isRunning());
    }


    @Test
    void testRestartSpawner() {
        Board board = createTestBoard();
        DiamondSpawner spawner = new DiamondSpawner(board);
        spawner.stop();
        spawner.restart();
        assertTrue(spawner.isRunning());
        spawner.stop();
    }
}