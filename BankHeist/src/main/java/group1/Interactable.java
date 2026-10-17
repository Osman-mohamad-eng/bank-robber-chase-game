package group1;

/**
 * Abstract base class for all collectible items and traps on the board.
 * 
 * @author Brian
 * @version 1.0
 */

public abstract class Interactable {
    protected boolean isCollected = false;

    /**
     * @return True if the item has already been triggered/collected, false otherwise.
     */
    public boolean isCollected() {
        return isCollected;
    }

    /**
     * Triggers the item's unique effect on the game board.
     * @param board The game board where the collection occurs.
     */
    public final void onCollect(Board board) {
        if (isCollected) {
            return;
        }
        
        applyEffect(board);
        this.isCollected = true;
    }

    /**
     * Subclasses must implement their specific collection effects here.
     * @param board The game board where the effect is applied.
     */
    protected abstract void applyEffect(Board board);
}
