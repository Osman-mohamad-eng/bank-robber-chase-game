package group1;

/**
 * Represents the playable character (thief).
 *
 * The thief moves around the board collecting money and avoiding traps
 * and the guard. The thief is moved using keyboard inputs
 * (W, A, S, D).
 *
 * The goal is to collect all required money bags and
 * reach the exit before being caught by the guard.
 * 
 * @author Mohamad, Daniil
 * @version 1.1
 */
public class Thief extends MovingEntity{
    /**
     * Creates a thief at the specified starting position.
     * 
     * @param startRow starting row position of the thief on the board
     * @param startCol starting column position of the thief on the board
     */
    public Thief(int startRow, int startCol) {
        super(0, new Position(startCol, startRow));
    }

    /**
     * Moves the thief on the board while ensuring the position stays within the board boundaries.
     * 
     * @param dRow change in row position
     * @param dCol change in column position
     */
    public void move(int dRow, int dCol) {
        int newCol = getPosition().getX() + dCol;
        int newRow = getPosition().getY() + dRow;

        setPosition(new Position(newCol, newRow));
    }

    /**
     * Returns the current column position of the thief.
     * 
     * @return the column index of the thief
     */
    public int getCol() {
        return getPosition().getX();
    }

    /**
     * Returns the current row position of the thief.
     * 
     * @return the row index of the thief
     */
    public int getRow() {
        return getPosition().getY();
    }
}