package group1;

import java.awt.Dimension;
import java.awt.Image;

/**
 * Creates items and traps used on the board.
 *
 * This removes item creation from Board class and makes
 * it easier to add new item types later.
 * 
 * @author Mohamad, Daniil
 * @version 1.1
 */
public class ItemFactory {

        /**
         * Creates an item based on the symbol from the map.
         *
         * @param type the character representing the item
         * @param position the x and y-positions on the board
         * @param dimension the width and height of item
         * @param moneyImage image for money
         * @param dartImage image for ink traps
         * @param glueImage image for glue traps
         * @return Cell containing the correct item, or null
         */
        public static Cell createItem(char type, Position position, Dimension dimension, Image moneyImage, Image dartImage, Image glueImage)

        {
            switch (type) {
                case 'M' ->
                {
                    Cell money = new Cell(moneyImage, position, dimension);
                    money.setInteractable(new CashStack());
                    return money;
                }

                case 'D' ->
                {
                    Cell dart = new Cell(dartImage, position, dimension);
                    dart.setInteractable(new InkDart());
                    return dart;
                }

                case 'S' ->
                {
                    Cell glue = new Cell(glueImage, position, dimension);
                    glue.setInteractable(new GlueTrap());
                    return glue;
                }

                default ->
                {
                    return null;
                }
            }
        }
    }

