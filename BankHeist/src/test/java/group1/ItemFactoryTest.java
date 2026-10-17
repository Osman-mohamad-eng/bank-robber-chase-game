package group1;

import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;


/**
 * Test cases for the ItemFactory class.
 *
 * These tests cases make sure the correct item is created based on the input symbol
 * and that invalid inputs return null.
 */
public class ItemFactoryTest {
    private Image dummyImage() {
        return new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
    }

    private Position dummyPosition(){
        return new Position(0,0);
    }

    private Dimension dummyDimension(){
        return new Dimension(40, 40);
    }
    @Test
    void testCreateMoneyItem() {
        Cell item = ItemFactory.createItem('M', dummyPosition(), dummyDimension(),
                dummyImage(), dummyImage(), dummyImage());
        assertNotNull(item);
    }


    @Test
    void testCreateDartItem() {
        Cell item = ItemFactory.createItem('D', dummyPosition(), dummyDimension(),
                dummyImage(), dummyImage(), dummyImage());
        assertNotNull(item);
    }


    @Test
    void testCreateTrapItem() {
        Cell item = ItemFactory.createItem('S', dummyPosition(), dummyDimension(),
                dummyImage(), dummyImage(), dummyImage());
        assertNotNull(item);
    }


    @Test
    void testInvalidItemReturnsNull() {
        Cell item = ItemFactory.createItem('X', dummyPosition(), dummyDimension(),
                dummyImage(), dummyImage(), dummyImage());
        assertNull(item);
    }
}