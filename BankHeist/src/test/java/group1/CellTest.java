package group1;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for Cell class.
 * 
 * @author Daniil
 * @version 1.0
 */

public class CellTest {
    private Cell cell;
    private Image image;

    /**
     * Initializes a Cell instance before each test.
     * 
     * Creates an image and assigns values.
     */

    @BeforeEach
    public void setUp() {
        image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        cell = new Cell(image, new Position(6, 4), new Dimension(64, 64)); 
    }

    /**
     * Tests that the constructor assigns all fields correctly.
     * 
     * Verifies fields are stored and returned correctly by getters.
     */
    @Test
    public void testConstructorStoresValues() {
        assertSame(image, cell.getImage());
        assertEquals(6, cell.getX());
        assertEquals(4, cell.getY());
        assertEquals(64, cell.getWidth());
        assertEquals(64, cell.getHeight());
    }

    /** Tests that the image setter changes image correctly */    
    @Test
    public void testImageSetterChangesImage() {
        Image newImage = new BufferedImage(32,32, BufferedImage.TYPE_INT_ARGB);
        cell.setImage(newImage);

        assertSame(newImage, cell.getImage());
    }

    /** Tests that initial interactable item is null */
    @Test
    public void testInteractableIsNull() {
        assertNull(cell.getInteractable());
    }

    /** Tests that hasInteractable initially returns false */
    @Test
    public void testInitialHasInteractableIsFalse() {
        assertFalse(cell.hasInteractable());
    }


    /** Tests that the interactable setter changes item correctly */
    @Test
    public void testInteractableSetterChangesItem() {
        CashStack cashStack = new CashStack();
        cell.setInteractable(cashStack);

        assertSame(cashStack, cell.getInteractable());
    }

    /** Tests that hasInteractable returns false when none exists */
    @Test
    public void testHasInteractableFalseWhenInteractableIsNull() {
        cell.setInteractable(null);
        assertFalse(cell.hasInteractable());
    }

    /** Tests that hasInteractable returns false when it is collected */
    @Test
    public void testHasInteractableFalseWhenInteractableIsCollected() {
        CashStack cashStack = new CashStack();
        cell.setInteractable(cashStack);

        BoardFactory factory = new BoardFactory();
        Board board = factory.makeBoard(1);

        cashStack.onCollect(board);

        assertFalse(cell.hasInteractable());
    }

    /** Test that hasInteractable returns true when it exists and has not been collected */
    @Test
    public void testHasInteractableTrueWhenInteractableExistsAndNotCollected() {
        CashStack cashStack = new CashStack();
        cell.setInteractable(cashStack);

        assertTrue(cell.hasInteractable());
    }
}