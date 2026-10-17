package group1;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for BoardFactory class.
 * 
 * @author Daniil
 * @version 1.0
 */
public class BoardFactoryTest {
    private BoardFactory factory;

    /** 
     * Instance reset before each test.
     * 
     * BoardFactory uses Singleton pattern, so it needs to be reset before each test.
     */
    @BeforeEach
    public void setUp(){
        Board.resetInstance();
        factory = new BoardFactory();
    }

    /** Tests that an invalid level throws exception. */
    @SuppressWarnings("")
    @Test
    public void testMakeBoardInvalidLevelThrowsException() {
       assertThrows(IllegalArgumentException.class, () -> factory.makeBoard(-1));
    }

    /** Tests that makeBoard returns a board. */
    @Test
    public void testMakeBoardReturnsBoard() {
        Board board = factory.makeBoard(1);

        assertNotNull(board);
    }

    /** Tests that makeBoard(1) returns the expected board. */
    @Test
    public void testMakeBoardReturnExpectedBoard1(){
        Board board = factory.makeBoard(1);

        assertTrue(board.isWall(new Position(2, 1)));
        assertTrue(board.isWall(new Position(2, 10)));
        assertTrue(board.isWall(new Position(8, 4)));

        assertTrue(board.isOccupied(new Position(15, 11))); //Thief
        assertTrue(board.isOccupied(new Position(12, 5))); //Guard
    }

    /** Tests that makeBoard(2) returns the expected board. */
    @Test
    public void testMakeBoardReturnExpectedBoard2(){
        Board board = factory.makeBoard(2);

        assertTrue(board.isWall(new Position(2, 4)));
        assertTrue(board.isWall(new Position(5, 4)));
        assertTrue(board.isWall(new Position(17, 2)));
        
        assertTrue(board.isOccupied(new Position(1, 1))); //Thief
        assertTrue(board.isOccupied(new Position(10, 7))); //Guard
    }

    /** Tests that makeBoard(3) returns the expected board. */
    @Test
    public void testMakeBoardReturnExpectedBoard3(){
        Board board = factory.makeBoard(3);

        assertTrue(board.isWall(new Position(0, 0)));
        assertTrue(board.isWall(new Position(11, 1)));
        assertTrue(board.isWall(new Position(2, 4)));
        assertTrue(board.isWall(new Position(22, 11)));
        
        assertTrue(board.isOccupied(new Position(1, 1))); //Thief
        assertTrue(board.isOccupied(new Position(23, 12))); //Guard
    }

    /** Tests singleton. */
    @Test
    public void testMakeBoardReturnsSameSingletonInstance() {
        Board board1 = factory.makeBoard(1);
        Board board2 = factory.makeBoard(2);

        assertSame(board1, board2);
    }
}
