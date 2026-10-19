package group1;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.Timer;

/**
 * Represents the main playable game area and engine for Bank Heist.
 * 
 * Renders the board, manages the game loop, handles player input, controls movement of the thief and guard,
 * processes item interactions, and checks win and loss conditions.
 * 
 * 
 * @author group 1
 * @version 1.1
 * */
public class Board extends JPanel implements GameSession {

    // Board Dimenstions
    private final int rowCount = 14;
    private final int columnCount = 25;
    private final int cellSize = 64;
    private final int boardWidth = columnCount * cellSize;
    private final int boardHeight = rowCount * cellSize;

    private Thief thief;
    private DiamondSpawner diamondSpawner;
    private final Random random = new Random();

    private final List<Guard> guards = new ArrayList<>();

    // Total score of the player
    private int balance = 0;
    private long cashCollected;
    private int diamondsCollected;
    private long penaltiesIncurred;
    private boolean finished;
    private boolean processingTick;
    private boolean escapeRequested;

    // Tracks the required money stacks to unlock the exit
    private int staticMoneyCount = 0;
    
    // Tracks the remaining ticks the thief is immobilized
    private int stuckCount = 0;

    // Used for Guard speed control
    private int tickCount = 0;

    private boolean paused = false;
    private Interface gameInterface = null;
    private double thiefAngle = 0;
    private double guardAngle = 0;
    private GameEvents gameEvents;

    // Cell Sets for different cells in the map and collision detection
    HashSet<Cell> walls;
    HashSet<Cell> emptyCells;
    HashSet<Cell> exitCells;
    HashSet<Cell> thiefCell;
    HashSet<Cell> interactableCells;
    HashSet<Cell> diamondCells;
    HashSet<Cell> guardCells;

    // The currently active bonus diamond
    private Cell activeDiamond = null;

    // Images used for rendering
    private final Image wallImage;
    private final Image emptyImage;
    private final Image moneyImage;
    private final Image exitLockedImage;
    private final Image exitOpenImage;
    private final Image thiefImage;
    private final Image diamondImage;
    private final Image inkDartImage;
    private final Image guardImage;
    private final Image thiefBlurImage;
    private final Image guardBlurImage;
    private final Image glueImage;

    // Map layout
    private final String[] boardMap;

    // Singleton
    private static Board instance;

    // Trackers for smooth dash animations
    private VisualState thiefVisual = new VisualState();
    private java.util.Map<Guard, VisualState> guardVisuals = new java.util.HashMap<>();

    private final java.util.List<PickupGhost> pickupGhosts = new java.util.ArrayList<>();
    private double thiefPickupPulse = 0.0;

    private long thiefWallBumpMs = 0;
    private int thiefWallBumpDCol = 0;
    private int thiefWallBumpDRow = 0;

    private Timer gameTimer;
    private int inputRow;
    private int inputCol;

    /**
     * Private constructor using the Singleton pattern.
     * 
     * Initializes the board dimensions, loads all images, starts the game timers.
     * 
     * @param BoardMap String array containing the layout of the board
     */
    private Board(String[] BoardMap) {
        this.boardMap = BoardMap.clone();

        wallImage = new ImageIcon(getClass().getResource("/group1/Wall.png")).getImage();
        emptyImage = new ImageIcon(getClass().getResource("/group1/Empty.png")).getImage();
        moneyImage = new ImageIcon(getClass().getResource("/group1/Money.png")).getImage();
        exitLockedImage = new ImageIcon(getClass().getResource("/group1/RedExitCell.png")).getImage();
        exitOpenImage = new ImageIcon(getClass().getResource("/group1/GreenExitCell.png")).getImage();
        thiefImage = new ImageIcon(getClass().getResource("/group1/Thief.png")).getImage();
        diamondImage = new ImageIcon(getClass().getResource("/group1/Diamond.png")).getImage();
        inkDartImage = new ImageIcon(getClass().getResource("/group1/InkDart.png")).getImage();
        guardImage = new ImageIcon(getClass().getResource("/group1/Guard.png")).getImage();
        thiefBlurImage = new ImageIcon(getClass().getResource("/group1/ThiefBlur.png")).getImage();
        guardBlurImage = new ImageIcon(getClass().getResource("/group1/GuardBlur.png")).getImage();
        glueImage = new ImageIcon(getClass().getResource("/group1/Glue.png")).getImage();

        setPreferredSize(new Dimension(boardWidth, boardHeight));
        setBackground(Color.WHITE);

        loadBoard();

        setFocusable(true);
        setupKeyBindings();
        setupDiamondSpawner();
        gameMovement();

        // timer for dealing with animations
        Timer animationTimer = new Timer(16, e -> {
            if (paused) return;

            double speed = 0.17;
            thiefVisual.update(speed);
            for (VisualState vs : guardVisuals.values()) {
                vs.update(speed);
            }

            if (!pickupGhosts.isEmpty()) {
                for (PickupGhost ghost : pickupGhosts) ghost.age += 0.06;
                pickupGhosts.removeIf(g -> g.age >= 1.0);
            }
            if (thiefPickupPulse > 0.0) {
                thiefPickupPulse -= 0.08;
                if (thiefPickupPulse < 0.0) thiefPickupPulse = 0.0;
            }
            repaint();
        });
        animationTimer.start();
    }

    private static class PickupGhost {
        final Image image;
        final int x;
        final int y;
        double age = 0.0;

        PickupGhost(Image image, int x, int y) {
            this.image = image;
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Retrieves the single active instance of the Board (Singleton).
     * 
     * If no board instance exists, creats one using the given board layout.
     * 
     * @param BoardMap An array of strings representing the grid layout of the level
     * @return the active Board instance
     */
    public static Board getInstance(String[] BoardMap) {
        if (instance == null) {
            instance = new Board(BoardMap);
        }
        return instance;
    }

    /**
     * Creates a Cell at the given position using the specified image.
     * 
     * Converts position to pixel coordinates.
     * 
     * @param image the image used to render the cell
     * @param col the column index of the cell on the board
     * @param row the row index of the cell on the board
     * @return a new Cell with given position and dimensions
     */
    private Cell makeCell(Image image, int col, int row){
        Position position = new Position(col * cellSize, row * cellSize);
        Dimension dimension = new Dimension(cellSize, cellSize);

        return new Cell(image, position, dimension);
    }

    /**
     * Parses the boardMap array to build the game.
     * 
     * Converts specific characters into their respective Cell objects and creates the Thief and Guard entity.
     */
    private void loadBoard() {
        walls = new HashSet<>();
        emptyCells = new HashSet<>();
        exitCells = new HashSet<>();
        thiefCell = new HashSet<>();
        interactableCells = new HashSet<>();
        diamondCells = new HashSet<>();
        guardVisuals.clear();

        guards.clear();
        guardVisuals.clear();
        guardCells = new HashSet<>();

        thief = null;
        staticMoneyCount = 0;
        guardVisuals.clear();
        thiefAngle = 0;
        guardAngle = 0;
        inputRow = 0;
        inputCol = 0;

        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < columnCount; c++) {
                String row = boardMap[r];
                char boardMapChar = row.charAt(c);

                switch (boardMapChar) {
                    case 'X' -> {
                        walls.add(makeCell(wallImage, c, r));
                    }
                    case 'T' -> {
                        thief = new Thief(r, c);
                        thiefVisual.setInitialPosition(c, r, cellSize, 0);
                        thiefCell.add(makeCell(thiefImage, c, r));
                        emptyCells.add(makeCell(emptyImage, c, r));
                    }
                    case 'E' -> {
                        emptyCells.add(makeCell(emptyImage, c, r));
                        exitCells.add(makeCell(exitLockedImage, c, r));
                    }
                    case 'G' -> {
                        emptyCells.add(makeCell(emptyImage, c, r));
                        Position pos = new Position(c, r);

                        Guard guard = new Guard(guards.size() + 1, pos);
                        guards.add(guard);

                        VisualState vs = new VisualState();
                        vs.setInitialPosition(c, r, cellSize, 180);
                        guardVisuals.put(guard, vs);
                        guardCells.add(makeCell(guardImage, c, r));
                    }
                    // using itemFactory to make it easier to introduce items
                    case 'M', 'D', 'S' -> {
                        Cell item = ItemFactory.createItem(
                                boardMapChar,
                                new Position(c * cellSize, r * cellSize),
                                new Dimension(cellSize, cellSize),
                                moneyImage,
                                inkDartImage,
                                glueImage
                        );

                        if (item != null) {
                            emptyCells.add(makeCell(emptyImage, c, r));
                            interactableCells.add(item);

                            if (boardMapChar == 'M') {
                                staticMoneyCount++;
                            }
                        }
                    }
                    default -> {
                        emptyCells.add(makeCell(emptyImage, c, r));
                    }
                }
            }
        }

        updateExitCells(staticMoneyCount <= 0);
    }

    /**
     * 
     * @param g2d
     * @param mainImg
     * @param blurImg
     * @param vs
     * @param angle
     */
    private void drawEntityWithTrail(Graphics2D g2d, Image mainImg, Image blurImg, VisualState vs, double angle) {
        if (vs.progress < 1.0) {

            for (int i = 1; i <= 5; i++) {
                float alpha = 0.6f * (1.0f - (i / 6.0f));
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

                double spacing = i * 5 * (1.0 - vs.progress);
                int ox = (int) (vs.curX - (vs.dirX * spacing));
                int oy = (int) (vs.curY - (vs.dirY * spacing));

                java.awt.geom.AffineTransform old = g2d.getTransform();
                g2d.rotate(Math.toRadians(angle), ox + cellSize / 2, oy + cellSize / 2);
                g2d.drawImage(blurImg, ox, oy, cellSize, cellSize, null);
                g2d.setTransform(old);
            }
        }

        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        java.awt.geom.AffineTransform old = g2d.getTransform();
        g2d.rotate(Math.toRadians(angle), vs.curX + cellSize / 2, vs.curY + cellSize / 2);
        g2d.drawImage(mainImg, (int) vs.curX, (int) vs.curY, cellSize, cellSize, null);
        g2d.setTransform(old);
    }

    /**
     * Draws all the cells, items, and moving entities onto the screen.
     * 
     * @param g The graphics object 
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (Cell empty : emptyCells) {
            g.drawImage(empty.getImage(), empty.getX(), empty.getY(), cellSize, cellSize, null);
        }
        for (Cell wall : walls) {
            g.drawImage(wall.getImage(), wall.getX(), wall.getY(), wall.getWidth(), wall.getHeight(), null);
        }
        for (Cell exit : exitCells) {
            g.drawImage(exit.getImage(), exit.getX(), exit.getY(), exit.getWidth(), exit.getHeight(), null);
        }
        for (Cell item : interactableCells) {
            g.drawImage(item.getImage(), item.getX(), item.getY(), item.getWidth(), item.getHeight(), null);
        }
        for (Guard guard : guardVisuals.keySet()) {
            VisualState vs = guardVisuals.get(guard);
            drawEntityWithTrail((Graphics2D) g, guardImage, guardBlurImage, vs, vs.angle);

            if (guard.getStuckCount() > 0) {
                drawStuckTint((Graphics2D) g, (int) vs.curX, (int) vs.curY, false);
            }
        }

        drawPickupGhosts((Graphics2D) g);

        Graphics2D g2d = (Graphics2D) g;
        int shakeX = 0;
        int shakeY = 0;
        long bumpAge = System.currentTimeMillis() - thiefWallBumpMs;
        if (bumpAge >= 0 && bumpAge < 320 && (thiefWallBumpDCol != 0 || thiefWallBumpDRow != 0)) {
            float decay = 1f - (bumpAge / 320f);
            int amp = (int) (10 * decay * Math.sin(bumpAge / 22.0));
            shakeX = amp * thiefWallBumpDCol;
            shakeY = amp * thiefWallBumpDRow;
        }

        java.awt.geom.AffineTransform preBump = g2d.getTransform();
        if (shakeX != 0 || shakeY != 0) {
            g2d.translate(shakeX, shakeY);
        }

        if (thiefPickupPulse > 0.0) {
            drawThiefWithAbsorbPulse(g2d);
        } else {
            drawEntityWithTrail(g2d, thiefImage, thiefBlurImage, thiefVisual, thiefAngle);
        }
        if (stuckCount > 0) {
            drawStuckTint(g2d, (int) thiefVisual.curX, (int) thiefVisual.curY, true);
        }

        if (shakeX != 0 || shakeY != 0) {
            g2d.setTransform(preBump);
        }
    }

    private void drawStuckTint(Graphics2D g, int px, int py, boolean isThief) {
        long ms = System.currentTimeMillis();
        int wobbleX = (int) (3 * Math.sin(ms / 40.0));
        int wobbleY = (int) (2 * Math.cos(ms / 33.0));

        java.awt.Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.48f));
        g.setColor(isThief ? new Color(255, 140, 0) : new Color(160, 40, 220));
        g.fillRect(px + wobbleX, py + wobbleY, cellSize, cellSize);

        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f));
        g.setColor(isThief ? new Color(255, 200, 50) : new Color(200, 100, 255));
        int dotSize = 8;
        g.fillOval(px + 10 + wobbleX, py + 14 + wobbleY, dotSize, dotSize);
        g.fillOval(px + cellSize - 18 + wobbleX, py + 12 + wobbleY, dotSize, dotSize);
        g.fillOval(px + cellSize / 2 - 4 + wobbleX, py + cellSize - 18 + wobbleY, dotSize, dotSize);

        g.setComposite(old);
    }

    private void drawPickupGhosts(Graphics2D g2d) {
        if (pickupGhosts.isEmpty()) {
            return;
        }

        java.awt.Composite oldComp = g2d.getComposite();
        for (PickupGhost ghost : pickupGhosts) {
            float t = (float) ghost.age;
            if (t >= 1f) {
                continue;
            }

            float alpha = 1f - t;
            float scale = 1f - 0.6f * t;
            int size = (int) (cellSize * scale);
            int offset = (cellSize - size) / 2;
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2d.drawImage(ghost.image, ghost.x + offset, ghost.y + offset, size, size, null);
        }
        g2d.setComposite(oldComp);
    }

    private void drawThiefWithAbsorbPulse(Graphics2D g2d) {
        double pulse = 1.0 + 0.18 * thiefPickupPulse;
        int cx = (int) (thiefVisual.curX + cellSize / 2);
        int cy = (int) (thiefVisual.curY + cellSize / 2);

        java.awt.Composite oldComp = g2d.getComposite();
        float ringAlpha = (float) (0.45 * thiefPickupPulse);
        if (ringAlpha > 0f) {
            int ringSize = (int) (cellSize * (1.0 + (1.0 - thiefPickupPulse) * 0.9));
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ringAlpha));
            g2d.setColor(new Color(255, 230, 120));
            g2d.fillOval(cx - ringSize / 2, cy - ringSize / 2, ringSize, ringSize);
        }
        g2d.setComposite(oldComp);

        java.awt.geom.AffineTransform old = g2d.getTransform();
        g2d.translate(cx, cy);
        g2d.scale(pulse, pulse);
        g2d.translate(-cx, -cy);
        drawEntityWithTrail(g2d, thiefImage, thiefBlurImage, thiefVisual, thiefAngle);
        g2d.setTransform(old);
    }

    /**
     * @return The current monetary balance of the player.
     * 
     * @return the current balance.
     */
    public int getBalance() {
        return balance;
    }

    public long getCashCollected() { return cashCollected; }
    public int getDiamondsCollected() { return diamondsCollected; }
    public long getPenaltiesIncurred() { return penaltiesIncurred; }

    public void collectCash(int amount) {
        cashCollected += amount;
        addBalance(amount);
    }

    public void collectDiamond() {
        diamondsCollected++;
        addBalance(ScoringRules.DIAMOND_VALUE);
    }

    public void incurPenalty(int amount) {
        penaltiesIncurred += amount;
        addBalance(-amount);
    }

    /**
     * Links the game board to the UI manager.
     * 
     * @param iface The interface instance deal with the HUD and menu
     */
    public void setInterface(Interface iface) {
        this.gameInterface = iface;
    }

    /**
     * Toggles the paused state of the game.
     * When the game us paused, all timers are paused.
     * 
     * @param paused true to pause game, false to resume
     */
    public void setPaused(boolean paused) {
    this.paused = paused;

    if (gameTimer != null) {
        if (paused) {
            gameTimer.stop();
        } else if (!finished && !gameTimer.isRunning()) {
            gameTimer.start();
        }
    }

    if (diamondSpawner != null) {
        if (paused) {
            diamondSpawner.stop();
        } else {
            diamondSpawner.start();
        }
    }
}

    public void setGameEvents(GameEvents events) {
        this.gameEvents = events;
    }

    /**
     * Resets the board to its intial state.
     * 
     * Clears the score, restores the board layout, resets movement, restarts timers and redraws the board.
     */
    public void reset() {
        thiefAngle = 0;
        guardAngle = 0;
        balance = 0;
        cashCollected = 0;
        diamondsCollected = 0;
        penaltiesIncurred = 0;
        finished = false;
        escapeRequested = false;
        staticMoneyCount = 0;
        activeDiamond = null;

        stuckCount = 0;
        for (Guard guard : guards) { guard.freeze(0); }

        pickupGhosts.clear();
        thiefPickupPulse = 0.0;

        thiefWallBumpMs = 0;
        thiefWallBumpDCol = 0;
        thiefWallBumpDRow = 0;

        if (diamondSpawner != null) {
            diamondSpawner.stop();
        }

        loadBoard();
        inputRow = 0;
        inputCol = 0;

        if (!paused && gameTimer != null && !gameTimer.isRunning()) {
    gameTimer.start();
}
        if (!paused && diamondSpawner != null) {
            diamondSpawner.start();
        }
        repaint();
    }

    /**
     * Updates the theif's current monetary balance and checks for loss conditions.
     * 
     * If a negative balance occurs, the game state is immediately set to GAME_OVER.
     * 
     * @param amount The value to add or subtract from the balance
     */
    public void addBalance(int amount) {
        balance = balance + amount;
        System.out.println("Balance updated! Current Balance: $" + balance);
        if (balance < 0) {
            System.out.println("DEFEAT: Score dropped below zero!");
            triggerGameOver();
        }
    }

    /** 
     * Resets the diamond timer after a collection.
     * 
     * Used to manage random spawning and despawning of diamond rewards.
    */
    public void restartDiamondTimer() {
        if (diamondSpawner != null) {
            diamondSpawner.restart();
        }
    }

    /**
     * Decrements the count of static money bags remaining on the board.
     */
    public void decrementStaticMoney() {
        staticMoneyCount--;
        if (staticMoneyCount == 0) {
            updateExitCells(true);
            System.out.println("MISSION COMPLETE: All static money collected! The Exit is now open.");
        }
    }

    private void updateExitCells(boolean open) {
        Image exitImage = open ? exitOpenImage : exitLockedImage;
        for (Cell exit : exitCells) {
            exit.setImage(exitImage);
        }
    }

    /**
     * Retrieves the current number of static CashStacks remaining on the board for testing
     * @return The number of uncollected CashStacks
     */
    int getStaticMoneyCount() {
        return this.staticMoneyCount;
    }


    /**
     * Handles input and moves Thief on the grid.
     * 
     * Deals with collision detection, item detection, and win/loss conditions.
     * 
     * @param dRow Change in row direction
     * @param dCol Change in column direction
     */
    public void moveThief(int dRow, int dCol) {
        if (thief == null || paused || finished) {
            return;
        }
        
        // Checks if the theif is supposed to be stuck in glue
        if (stuckCount  > 0){
            stuckCount --;
            inputRow = 0;
            inputCol = 0;
            return;
        }

        if (dRow == 0 && dCol == 0) {
            return; // do nothing when game is paused or there is no input
        }

        // checking the movement tile before moving
        int newRow = thief.getRow() + dRow;
        int newCol = thief.getCol() + dCol;
        Position target = new Position(newCol, newRow);

        if (!isInside(target)) {
            return;
        }
        if (isWall(target)) {
            return;
        }

        // Visual rotation
        if (dRow == -1) {
            thiefAngle = 0;
        } else if (dRow == 1) {
            thiefAngle = 180;
        } else if (dCol == -1) {
            thiefAngle = 270;
        } else if (dCol == 1) {
            thiefAngle = 90;
        }

        int oldCol = thief.getCol();
        int oldRow = thief.getRow();

        thief.move(dRow, dCol);

        thiefVisual.startDash(oldCol, oldRow, thief.getCol(), thief.getRow(), cellSize, thiefAngle);

        int col = thief.getCol() * cellSize;
        int row = thief.getRow() * cellSize;

        // Checks if the Thief stepped into an interactable cell
        Cell triggeredItem = null;
        for (Cell item : interactableCells) {
            if (item.getX() == col && item.getY() == row) {
                triggeredItem = item;
                break;
            }
        }

        if (triggeredItem != null) {
            Interactable interactable = triggeredItem.getInteractable();
            interactable.onCollect(this);
            if (finished) {
                return;
            }

            if (interactable instanceof RareDiamond) {
                this.restartDiamondTimer();
            }

            pickupGhosts.add(new PickupGhost(triggeredItem.getImage(), col, row));
            thiefPickupPulse = 1.0;

            interactableCells.remove(triggeredItem);
            emptyCells.add(makeCell(emptyImage, col, row));

            if (triggeredItem == activeDiamond) {
                activeDiamond = null;
            }
        }

        // get old position before clearing
        //Cell oldThief = thiefCell.iterator().next();
        emptyCells.add(makeCell(emptyImage, oldCol, oldRow));

        thiefCell.clear();
        thiefCell.add(makeCell(thiefImage, col, row));

        // reset the input so the thief can move one step at a time
        inputRow = 0;
        inputCol = 0;

        // Check if the thief moved onto the guard's position
        Position thiefPos = new Position(thief.getCol(), thief.getRow());

        for (Guard guard : guards) 
            {
                if (guard != null && guard.getPosition().equals(thiefPos)) {
                    System.out.println("CAUGHT! The guard captured the thief.");
                    triggerGameOver();
                    return;
                }
            }

        // Collision wins precedence over escape; emit exactly one terminal event.
        for (Cell exit : exitCells) {
            if (exit.getX() == col && exit.getY() == row && staticMoneyCount <= 0) {
                if (processingTick) {
                    escapeRequested = true;
                } else {
                    finish(true);
                }
                return;
            }
        }

    }


    /**
     * @return thiefRow current row index of the thief for testing
     */
    int getThiefRow() {
        return thief.getRow();
    }

    /**
     * @return thiefCol current column index of the thief for testing
     */
    int getThiefCol() {
        return thief.getCol();
    }


    private void tryTriggerWallShake(int dRow, int dCol) {
        if (thief == null || paused) {
            return;
        }

        int newRow = thief.getRow() + dRow;
        int newCol = thief.getCol() + dCol;
        Position target = new Position(newCol, newRow);
        if ((!isInside(target) || isWall(target)) && (dRow != 0 || dCol != 0)) {
            long nowMs = System.currentTimeMillis();
            if (nowMs - thiefWallBumpMs >= 300L) {
                thiefWallBumpMs = nowMs;
                thiefWallBumpDCol = dCol;
                thiefWallBumpDRow = dRow;
            }
        }
    }

    /**
     * Configures keyboard bindings for thief movement.
     *
     * Binds keyboard inputs (WASD), moves by 1 cell.
     */
    private void setupKeyBindings() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke("pressed W"), "up");
        im.put(KeyStroke.getKeyStroke("pressed S"), "down");
        im.put(KeyStroke.getKeyStroke("pressed A"), "left");
        im.put(KeyStroke.getKeyStroke("pressed D"), "right");

        am.put("up", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                inputRow = -1;
                inputCol = 0;
                tryTriggerWallShake(-1, 0);
            }
        });
        am.put("down", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                inputRow = 1;
                inputCol = 0;
                tryTriggerWallShake(1, 0);
            }
        });
        am.put("left", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                inputRow = 0;
                inputCol = -1;
                tryTriggerWallShake(0, -1);
            }
        });
        am.put("right", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                inputRow = 0;
                inputCol = 1;
                tryTriggerWallShake(0, 1);
            }
        });
    }


    private static final double GUARD_SPEED = 1.5; // higher = slower
    private double guardAccumulator = GUARD_SPEED;
    /**
     * Handles the main game movement loop.
     * 
     * Moves the thief based on player input and updates the guard position
     * while checking for collisions, and repaints the board.
     */
    private void gameMovement() {
        gameTimer = new Timer(500, e -> {
            if (paused || finished) {
                return;
            }
            processingTick = true;
            escapeRequested = false;
            try {
                tickCount++;

                if (thief != null) {
                    moveThief(inputRow, inputCol);
                }

                if (finished) {
                    return;
                }

                if (thief != null) {
                    Position thiefPos = new Position(thief.getCol(), thief.getRow());

                    updateGuardCells();

                    guardAccumulator += 1;

                    boolean shouldGuardsMove = false;
                    if (guardAccumulator >= GUARD_SPEED) {
                        shouldGuardsMove = true;
                        guardAccumulator -= GUARD_SPEED;
                    }

                    for (Guard guard : guards) {
                        if (guard.isFrozen()) {
                            guard.tickFreeze();
                        } else if (shouldGuardsMove) {
                            Position oldPos = guard.getPosition();
                            guard.update(thiefPos, this);
                            Position newPos = guard.getPosition();

                            double guardAngle = 0;
                            if (newPos.getY() < oldPos.getY()) {
                                guardAngle = 0;
                            } else if (newPos.getY() > oldPos.getY()) {
                                guardAngle = 180;
                            } else if (newPos.getX() < oldPos.getX()) {
                                guardAngle = 270;
                            } else if (newPos.getX() > oldPos.getX()) {
                                guardAngle = 90;
                            }

                            VisualState vs = guardVisuals.computeIfAbsent(guard, k -> new VisualState());
                            vs.startDash(
                                    oldPos.getX(),
                                    oldPos.getY(),
                                    newPos.getX(),
                                    newPos.getY(),
                                    cellSize,
                                    guardAngle
                            );

                            updateGuardCells();
                        }

                        int gCol = guard.getPosition().getX();
                        int gRow = guard.getPosition().getY();
                        int gPixelX = gCol * cellSize;
                        int gPixelY = gRow * cellSize;

                        Cell triggeredItem = null;
                        for (Cell item : interactableCells) {
                            if (item.getX() == gPixelX && item.getY() == gPixelY) {
                                triggeredItem = item;
                                break;
                            }
                        }

                        if (triggeredItem != null && triggeredItem.getInteractable() instanceof GlueTrap) {
                            guard.freeze(3);
                            interactableCells.remove(triggeredItem);
                            emptyCells.add(makeCell(emptyImage, gCol, gRow));
                            System.out.println("The Guard stepped in a Glue Trap!");
                        }

                        if (guard.getPosition().equals(thiefPos)) {
                            System.out.println("CAUGHT! The guard captured the thief.");
                            triggerGameOver();
                            return;
                        }
                    }

                    updateGuardCells();
                }

                if (escapeRequested && !finished) {
                    finish(true);
                }
                repaint();
            } finally {
                processingTick = false;
                escapeRequested = false;
            }
        });


    }
    /**
     * Updates the state of the diamond on the board.
     * If a diamond is currently active, it is removed.
     */
    public void updateDiamondState() {
        if (hasActiveDiamond()) {
            removeActiveDiamond();
        } else {
            createRandomDiamond();
        }
        repaint();
    }

    /**
     * Checks every 5 seconds if a diamond exists.
     * If no diamond exists, it attempts to spawn one.
     * If a diamond is already on the board, it removes the diamond.
     */
    private void setupDiamondSpawner() {
        diamondSpawner = new DiamondSpawner(this);
        diamondSpawner.start();
    }

    /**
     * Returns whether a bonus diamond is currently active on the board.
     * 
     * @return true if a bonus diamond is currently on the board
     */
    public boolean hasActiveDiamond() {
        return activeDiamond != null;
    }

    /**
     * Creates a new diamond on the board.
     */
    public void createRandomDiamond() {
        spawnRandomDiamond();
    }

    /**
     * Removes the diamond from the board.
     */
    public void removeActiveDiamond() {
        despawnDiamond();
    }

    /**
     * Attempts to spawn a diamond collectible at a random valid location on the board
     */
    private void spawnRandomDiamond() {
        if (paused || activeDiamond != null) {
            return;
        }

        // an attempt to find a valid tile
        for (int attempts = 0; attempts < 200; attempts++) {
            int r = random.nextInt(rowCount);
            int c = random.nextInt(columnCount);

            Position p = new Position(c, r);

            if (!isSpawnable(p)) {
                continue;
            }

            activeDiamond = makeCell(diamondImage, c, r);
            activeDiamond.setInteractable(new RareDiamond());
            interactableCells.add(activeDiamond);
            return;
        }
    }

    /** 
     * Removes the currently active diamond from the baord and replaces its cell with an empty floor tile.
     */
    private void despawnDiamond() {
        if (activeDiamond == null) {
            return;
        }
        interactableCells.remove(activeDiamond);
        emptyCells.add(makeCell(emptyImage, activeDiamond.getX(), activeDiamond.getY()));
        activeDiamond = null;
    }

    /**
     * Retrieves the set of interactable cells to allow for testing
     * @return HashSet containing all interactable cells on the board
     */
    HashSet<Cell> getInteractableCells() {
        return this.interactableCells;
    }

    /**
     * Applies the freeze effect to a moving entity for the 
     * number of movement cycles the entity remains frozen.
     * 
     * @param ticks number of cycles to freeze for
     */
    public void applyFreeze(int ticks) {
        this.stuckCount = ticks;
    }

    /**
     * Returns the remaining number of ticks the thief is immobilized.
     * @return the stuck count
     */
    public int getStuckCount() {
        return stuckCount;
    }

    /**
     * Updates the guards location on the grid
     */
    private void updateGuardCells() {
        guardCells.clear();

        for (Guard guard : guards) {
            int col = guard.getPosition().getX();
            int row = guard.getPosition().getY();
            guardCells.add(makeCell(guardImage, col, row));
        }
    }

    /**
     * Checks if a position is inside the board.
     * 
     * @param p Position to check
     * @return true if the position is inside the board
     */
    public boolean isInside(Position p) {
        int col = p.getX();
        int row = p.getY();

        return col >= 0 && col < columnCount && row >= 0 && row < rowCount;
    }

    private int toPixelX(Position p) {
        return p.getX() * cellSize;
    }

    private int toPixelY(Position p) {
        return p.getY() * cellSize;
    }

    private boolean containsCell(HashSet<Cell> cells, int x, int y) {
        for (Cell cell : cells) {
            if (cell.getX() == x && cell.getY() == y) {
                return true;
            }
        }
        return false;
    }


    /**
     * Checks if the given position contains a wall.
     * 
     * @param p Position to check
     * @return true if the position is a wall
     */
    public boolean isWall(Position p) {
        return containsCell(walls, toPixelX(p), toPixelY(p));
    }

    /**
     * Checks if a position is currently occupied.
     * 
     * @param p Position to check
     * @return true if the position is occupied
     */
    public boolean isOccupied(Position p) {
        int pixelX = toPixelX(p);
        int pixelY = toPixelY(p);

        return containsCell(thiefCell, pixelX, pixelY) || containsCell(guardCells, pixelX, pixelY);
    }

    /**
     * Checks if a position is currently occupied by guard.
     * 
     * @param p Position to check
     * @return true if the position is occupied by guard
     */
    public boolean isOccupiedByGuard(Position p) { return containsCell(guardCells, toPixelX(p), toPixelY(p)); }

    /**
     * Determines if a random item can be spawned on the specific position.
     * 
     * A valid spawn location must be inside the board and must not overlap with walls,
     * the exit, the thief, the guard or another interactable item.
     * 
     * @param p The position to be checked
     * @return True if the position is a safe spawn location
     */
    private boolean isSpawnable(Position p) {

        if (!isInside(p) || isWall(p)) {
            return false;
        }

        int pixelX = toPixelX(p);
        int pixelY = toPixelY(p);

        if (containsCell(exitCells, pixelX, pixelY)) {
            return false;
        }
        if (containsCell(thiefCell, pixelX, pixelY)) {
            return false;
        }
        if (containsCell(guardCells, pixelX, pixelY)) {
            return false;
        }
        if (containsCell(interactableCells, pixelX, pixelY)) {
            return false;
        }

        return true;
    }

    /**
     * Ends the game and displays game over screen.
     * 
     * Stops all active timers and updates user interface state.
     */
    private void triggerGameOver() {
        finish(false);
    }

    private void finish(boolean won) {
        if (finished) {
            return;
        }
        finished = true;
        escapeRequested = false;
        paused = true;

        if (diamondSpawner != null) {
            diamondSpawner.stop();
        }

        if (gameTimer != null) {
            gameTimer.stop();
        }

        if (gameEvents != null) {
            if (won) {
                gameEvents.onWin();
            } else {
                gameEvents.onGameOver();
            }
        }
    }

    /**
     * 
     */
    private class VisualState {
        double startX, startY;
        double curX, curY;
        double targetX, targetY;
        double dirX, dirY;
        double angle = 0;
        double progress = 1.0;

        /**
         * 
         * @param col
         * @param row
         * @param cellSize
         * @param initialAngle
         */
        public void setInitialPosition(int col, int row, int cellSize, double initialAngle) {
            this.curX = col * cellSize;
            this.curY = row * cellSize;
            this.startX = curX;
            this.startY = curY;
            this.targetX = curX;
            this.targetY = curY;
            this.angle = initialAngle;
            this.progress = 1.0;
        }

        /**
         * 
         * @param oldCol
         * @param oldRow
         * @param newCol
         * @param newRow
         * @param cellSize
         * @param newAngle
         */
        public void startDash(int oldCol, int oldRow, int newCol, int newRow, int cellSize, double newAngle) {
            this.startX = oldCol * cellSize;
            this.startY = oldRow * cellSize;
            this.curX = startX;
            this.curY = startY;
            this.targetX = newCol * cellSize;
            this.targetY = newRow * cellSize;
            this.dirX = newCol - oldCol;
            this.dirY = newRow - oldRow;
            this.angle = newAngle;
            this.progress = 0.0;
        }

        /**
         * 
         * @param speed
         */
        public void update(double speed) {
            if (progress < 1.0) {
                progress += speed;
                if (progress > 1.0) progress = 1.0;
                this.curX = startX + (targetX - startX) * progress;
                this.curY = startY + (targetY - startY) * progress;
            }
        }
    }

     /** Singleton instance reset for testing. */
    public static void resetInstance() { instance = null; }
    /** For tests */
    List<Guard> getGuards() { return guards;} 
}
