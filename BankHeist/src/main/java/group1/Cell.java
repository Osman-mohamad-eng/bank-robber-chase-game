package group1;
import java.awt.Dimension;
import java.awt.Image;
/**
 * Represents a single tile (cell) on the game board.
 *
 * A cell stores its position, size, image and an optional interactable item
 * Cells may contain objects such as CashStack, Diamond, GlueTrap or InkDart.
 *
 * Cells are used to detect interactions with the thief.
 * 
 * @author Daniil, Brian
 * @version 1.1
 */
public class Cell {
    private final Position position;
    private final Dimension dimension;

    private Interactable item;
    private Image image;

    /**
     * Constructs a cell with given image, position and size (in pixels).
     * 
     * @param image image used to render the cell
     * @param position x and y-coordinates of the cell on the board
     * @param dimension height and width dimensions of the cell
     */

    public Cell(Image image, Position position, Dimension dimension) {
        this.image = image;
        this.position = position;
        this.dimension = dimension;
    }

    /**
     * Returns the image used to render this cell.
     * 
     * @return the cell image
     */

    public Image getImage() { return image; }

    /**
     * Returns the x-coordinate of the cell.
     * 
     * @return the x position
     */
    public int getX() { return position.getX(); }

    /**
     * Returns the y-coordinate of the cell.
     * 
     * @return the y position
     */
    public int getY() { return position.getY(); }

     /**
     * Returns the width of the cell.
     * 
     * @return the cell width
     */
    public int getWidth() { return dimension.width; }

     /**
     * Returns the height of the cell.
     * 
     * @return the cell height
     */
    public int getHeight() { return dimension.height; }

    /**
     * Returns the interactable object stored in this cell.
     * 
     * @return the interactable object, or null if none exists
     */
    public Interactable getInteractable() { return item; }

    /**
     * Sets the image used to render this cell
     * 
     * @param image the new image for the cell
     */
    public void setImage(Image image) { this.image = image; }
  
    /**
     * Sets the interactable object for this cell.
     * 
     * @param item the interactable object to place inside the cell
     */
    public void setInteractable(Interactable item) { this.item = item; }

    /**
     * Checks whether this cell contains an interactable item that has not been collected yet.
     * 
     * @return true if an interactable exists and is not collected
     */
    public boolean hasInteractable() { return item != null && !item.isCollected(); }
}