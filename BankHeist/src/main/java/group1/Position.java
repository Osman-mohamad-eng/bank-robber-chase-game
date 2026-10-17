package group1;

import java.util.Objects;

/**
 * Represents the coordinates of an entity on the game board grid.
 *
 * The board uses grid coordinates (column, row) to keep track of
 * entity movements and locations.
 *
 * Position is used by both the thief and guard
 * to find movement and pathfinding.
 * 
 * @author Dev
 * @version 1.0
 */
public class Position {

    private int x;
    private int y;

    /**
     * Creates a new position.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     */
    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the x-coordinate.
     *
     * @return x value
     */
    // Required by Guard and other classes
    public int getX() {
        return x;
    }

    /**
     * Returns the y-coordinate.
     *
     * @return y value
     */
    public int getY() {
        return y;
    }

    /**
     * Calculates the distance to another position.
     *
     * @param other position to compare with
     * @return distance between two positions
     */
    public float distanceTo(Position other) {
        int dx = this.x - other.x;
        int dy = this.y - other.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Returns a new position shifted by dx and dy.
     *
     * @param dx change in x direction
     * @param dy change in y direction
     * @return new shifted position
     */
    // Returns a NEW Position shifted by dx, dy
    public Position add(int dx, int dy) {
        return new Position(this.x + dx, this.y + dy);
    }

    /**
     * Checks if two positions are equal.
     *
     * @param obj object to compare
     * @return true if coordinates match
     */
    // Needed because Guard uses equals()
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Position)) {
            return false;
        }
        Position other = (Position) obj;
        return this.x == other.x && this.y == other.y;
    }

    /**
     * Generates hash code for this position.
     *
     * @return hash value
     */
    // Required when equals is overridden
    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    /**
     * Returns string representation of the position.
     *
     * @return position as string
     */
    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
