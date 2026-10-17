package group1;

/**
 * Abstract class for all moving characters.
 *
 * Both the Thief and Guard inherit from this class.
 * It stores common properties such as position and ID
 * 
 * @author Dev, Mohamad
 * @version 1.0
 */
public abstract class MovingEntity {  // abstract

    private int id;
    private Position position;

    /**
     * Constructor for a moving entity.
     *
     * @param id unique identifier of the entity
     * @param position starting position of the entity
     */
    public MovingEntity(int id, Position position) {
        this.id = id;
        this.position = position;
    }

    /**
     * Returns the id of the entity.
     *
     * @return entity id
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the current position of the entity.
     *
     * @return current position
     */
    public Position getPosition() {
        return position;
    }

    /**
     * 
     * @param position
     */
    protected void setPosition(Position position) {
        this.position = position;
    }
   

    /**
     * Checks if the entity can move to a given position.
     *
     * @param p target position
     * @param board board used to check movement conditions
     * @return true if the position is valid for movement
     */
    public boolean canMoveTo(Position p, Board board) {
        return board.isInside(p) && !board.isWall(p) && !board.isOccupied(p);
    }

    /**
     * Moves the entity to the given position.
     *
     * @param p new position
     */
    public void moveTo(Position p) {
        setPosition(p);
    }
}
