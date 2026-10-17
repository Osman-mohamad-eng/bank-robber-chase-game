package group1;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

// Manages all non-gameplay screens and the in-game HUD
//
// UIElement - image + position + FitMode; non-interactive, just draws itself
// UIButton - extends UIElement adds hover/press scale animation, hitbox, and click action
//
// FitMode options:
//   NATURAL draw at image's own pixel size (w/h budget ignored)
//   FIT scale to fit inside wh, keep aspect ratio (may letterbox)
//   FILL scale to fill wh, keep aspect ratio (may crop)
//   STRETCH force exactly wh (distorts if aspect ratio differs)
//
// Note: to edit the UI, change constants in the LAYOUT section. Image paths are in loadImages()

/**
 * Manages all non-gameplay screens and the in-game HUD for Bank Heist.
 *
 * Screens controlled include Main Menu, Pause, Win, Game Over, and the in-game HUD
 * (score header, playtime header, and pause button).
 *
 * UI is built from two internals: UIElement, a non-interactive
 * image drawn at a position using a FitMode, and UIButton,
 * which extends UIElement with hover/press scale animation, a hitbox,
 * and a click action.
 *
 * To edit the layout, change constants in the LAYOUT section.
 * Image paths are defined in loadImages().
 *
 * @author Keith
 * @version 1.0
 */
public class Interface extends JPanel {

    /**
     * The possible screens and gameplay states of the UI.
     */
    public enum GameState { MAIN_MENU, PLAYING, PAUSED, WIN, GAME_OVER, TUTORIAL }

    /**
     * Controls how a UIElement scales its image to fill its display budget.
     */
    private enum FitMode {
        // Draws at the image's own pixel size; the w/h budget is ignored.
        NATURAL,
        // Scales to fit inside the w/h budget, preserving aspect ratio (may letterbox).
        FIT,
        // Scales to fill the w/h budget, preserving aspect ratio (may crop).
        FILL,
        // Forces exactly w/h, ignoring aspect ratio (may distort).
        STRETCH,
    }

    /**
     * A non-interactive UI element that draws an image at a screen position using a FitMode.
     * Draws a grey placeholder rectangle if no image is provided.
     */
    private static class UIElement {
        int cx, cy; // centre position on screen
        int w, h; // display budget (used by FIT / FILL / STRETCH)
        Image image;
        FitMode fitMode;

        /**
         * Constructs a UIElement at the given screen position with the specified image and fit mode.
         *
         * @param cx      horizontal centre position on screen
         * @param cy      vertical centre position on screen
         * @param w       display width budget (used by FIT, FILL, STRETCH)
         * @param h       display height budget (used by FIT, FILL, STRETCH)
         * @param image   image to draw; a grey placeholder is drawn if null
         * @param fitMode how to scale the image into the w/h budget
         */
        UIElement(int cx, int cy, int w, int h, Image image, FitMode fitMode) {
            this.cx = cx; this.cy = cy;
            this.w = w; this.h = h;
            this.image = image;
            this.fitMode = fitMode;
        }

        /**
         * Computes the draw dimensions according to the current FitMode.
         * Subclasses override this to apply an additional scale factor.
         *
         * @return an int array {drawWidth, drawHeight}
         */
        protected int[] dims() {
            int bw = w > 0 ? w : 240;
            int bh = h > 0 ? h : 70;

            if (image == null) return new int[]{bw, bh};

            int iw = image.getWidth(null);
            int ih = image.getHeight(null);

            if (iw <= 0 || ih <= 0) return new int[]{bw, bh};

            return switch (fitMode) {
                case NATURAL -> new int[]{iw, ih};

                case FIT -> {
                    float s = Math.min((float) bw / iw, (float) bh / ih);
                    yield new int[]{(int)(iw * s), (int)(ih * s)};
                }

                case FILL -> {
                    float s = Math.max((float) bw / iw, (float) bh / ih);
                    yield new int[]{(int)(iw * s), (int)(ih * s)};
                }

                case STRETCH -> new int[]{bw, bh};
            };
        }

        void draw(Graphics2D g) {
            int[] d = dims();
            int x = cx - d[0] / 2;
            int y = cy - d[1] / 2;

            if (image != null) {
                g.drawImage(image, x, y, d[0], d[1], null);
            } else {
                // Placeholder
                g.setColor(new Color(120, 120, 120, 100));
                g.fillRect(x, y, d[0], d[1]);
            }
        }
    }


    /**
     * An interactive button extending UIElement with hover and press scale animation.
     *
     * The image is scaled by the FitMode, then multiplied by the current animation scale.
     * The hitbox is always matching the final animated draw size, centred on (cx, cy).
     */
    private static class UIButton extends UIElement {
        final Runnable action;
        boolean hovered = false;
        boolean pressed = false;
        float scale = NORMAL_SCALE;
        float targetScale = NORMAL_SCALE;
        float entranceScale = 1f;
        int entranceOffsetY = 0;
        float entranceProgress = 1f;

        /**
         * Constructs a UIButton at the given screen position with the specified image, fit mode, and click action.
         *
         * @param cx      horizontal centre position on screen
         * @param cy      vertical centre position on screen
         * @param w       display width budget
         * @param h       display height budget
         * @param image   image to draw; a placeholder is drawn if null
         * @param fitMode how to scale the image into the w/h budget
         * @param action  the action to run when this button is clicked
         */
        UIButton(int cx, int cy, int w, int h, Image image, FitMode fitMode, Runnable action) {
            super(cx, cy, w, h, image, fitMode);
            this.action = action;
        }

        /**
         * Advances the scale animation one step toward the target scale.
         *
         * @return true if the animation is still in progress, false if it has reached the target
         */
        boolean tick() {
            float diff = targetScale - scale;

            if (Math.abs(diff) < 0.0015f) { scale = targetScale; return false; }

            scale += diff * LERP_SPEED;
            return true;
        }

        // Applies the hover/press animation scale on top of the FitMode dims
        @Override
        protected int[] dims() {
            int[] base = super.dims();

            return new int[]{(int)(base[0] * scale * entranceScale), (int)(base[1] * scale * entranceScale)};
        }

        /**
         * Checks whether the given point falls within this button's current animated hitbox.
         *
         * @param p the point to test, in screen coordinates
         * @return true if p is inside the hitbox, false else
         */
        boolean contains(Point p) {
            if (entranceProgress < 0.98f) {
                return false;
            }
            int[] d = dims();

            return new Rectangle(cx - d[0] / 2, cy + entranceOffsetY - d[1] / 2, d[0], d[1]).contains(p);
        }

        @Override
        void draw(Graphics2D g) {
            if (entranceProgress <= 0f) {
                return;
            }
            int[] d = dims();
            int x = cx - d[0] / 2;
            int y = cy + entranceOffsetY - d[1] / 2;

            if (image != null) {
                g.drawImage(image, x, y, d[0], d[1], null);
            } else {
                // Placeholder rounded rect, brightens on hover
                g.setColor(hovered ? new Color(210, 210, 210) : new Color(150, 150, 150));
                g.fillRoundRect(x, y, d[0], d[1], 16, 16);
                g.setColor(Color.DARK_GRAY);
                g.setStroke(new BasicStroke(2f));
                g.drawRoundRect(x, y, d[0], d[1], 16, 16);
            }
        }
    }

    private record ButtonSpec(int cx, int cy, int w, int h, Image image, Runnable action) {
    }

    private record ScreenSequence(long titleDelayMs, long titleDurationMs,
            long resultDelayMs, long resultDurationMs,
            long valueDelayMs, long valueDurationMs,
            long buttonDelayMs, long buttonStepMs, long buttonDurationMs,
            long pauseTextDelayMs, long pauseTextDurationMs,
            long pillarDelayMs, long pillarStepMs, long pillarDurationMs,
            int pillarCount) {
    }

    // ANIMATION CONSTANTS
    private static final float HOVER_SCALE = 1.10f;
    private static final float PRESS_SCALE = 0.93f;
    private static final float NORMAL_SCALE = 1.00f;
    private static final float LERP_SPEED = 0.35f;

    // Screen - matches the board size exactly
    private static final int CX = UIConstants.SCR_W / 2;
    // Main Menu
    private static final int MENU_PLAY_CX = CX, MENU_PLAY_CY = 550;
    private static final int MENU_HELP_CX = CX, MENU_HELP_CY = 670;   // tutorial button (was close)
    private static final int MENU_CLOSE_CX = CX, MENU_CLOSE_CY = 790; // close moved down

    // Paused screen
    private static final int PAUSE_RESUME_CX = CX, PAUSE_RESUME_CY = 400;
    private static final int PAUSE_MENU_CX = CX, PAUSE_MENU_CY = 515;
    private static final int PAUSE_TUTORIAL_CX = CX, PAUSE_TUTORIAL_CY = 635; // tutorial button
    private static final int PAUSE_CLOSE_CX = CX, PAUSE_CLOSE_CY = 735;       // close moved down

    // Win / Game Over screen
    private static final int END_AGAIN_CX = CX, END_AGAIN_CY = 460;
    private static final int END_MENU_CX = CX, END_MENU_CY = 575;
    private static final int END_CLOSE_CX = CX, END_CLOSE_CY = 675;

    // FIELDS
    private GameState state;
    private final GameSession gameSession;
    private final List<UIButton> buttons = new ArrayList<>();
    private final EnumMap<GameState, List<ButtonSpec>> buttonLayouts = new EnumMap<>(GameState.class);
    private final EnumMap<GameState, ScreenSequence> screenSequences = new EnumMap<>(GameState.class);
    private final Timer animTimer;
    private final InterfaceRenderer renderer = new InterfaceRenderer();

    // Background image (drawn stretched to fill the screen)
    private Image imgBckMainMenu;

    // Main menu title image.
    private Image imgTitle;

    // Raw images referenced by buildButtons() and drawScoreHeader()
    private Image imgHeaderScore;
    private Image imgBtnPlay;
    private Image imgBtnPlayAgain;
    private Image imgBtnMainMenu;
    private Image imgBtnSquarePause;
    private Image imgBtnSquarePlay;
    private Image imgBtnSquareClose;
    private Image imgBtnSquareHelp;
    private Image imgBckTutorial;
    private Image imgPillar;
    private Image imgHeaderHeistSuccessful;
    private Image imgHeaderHeistUnsuccessful;
    private Image imgPausedText;

    // Where to return when closing the tutorial
    private GameState tutorialReturnState = GameState.MAIN_MENU;

    private Image imgHeaderPlaytime;
    private final PlaytimeManager playtime = new PlaytimeManager();

    private Image imgMenuThief;
    private Image imgMenuGuard;

    private long stateEnteredAtMs = System.currentTimeMillis();
    private long startTransitionStartedAtMs = -1L;
    private boolean startTransitionRunning = false;
    private static final long START_TRANSITION_DURATION_MS = 560L;

    private final List<InterfaceRenderer.ScorePopup> scorePopups = new ArrayList<>();
    private int prevBalance = 0;
    private float displayedBalance = 0f;

    private float scoreHeaderPulse = 0f;
    private float timeHeaderPulse = 0f;
    private String prevElapsedText = "";

    // SETUP
    /**
     * Constructs the game interface panel, loads all UI images, sets up mouse and keyboard listeners,
     * and sets the initial state to MAIN_MENU.
     *
     * @param gameSession the game session this interface is paired with
     */
    public Interface(GameSession gameSession) {
        this.gameSession = gameSession;
        setOpaque(false);
        setLayout(null);
        setFocusable(true);

        animTimer = new Timer(16, e -> tickAnimations());
        animTimer.start();

        loadImages();
        gameSession.setGameEvents(new GameEvents() {
            @Override
            public void onWin() {
                setState(GameState.WIN);
            }

            @Override
            public void onGameOver() {
                setState(GameState.GAME_OVER);
            }
        });
        configureButtonLayouts();
        configureScreenSequences();
        setupMouseListeners();
        setupKeyBindings();
        setState(GameState.MAIN_MENU);
        warmUpTransitions();
    }

    /**
     * Convenience constructor retained for compatibility with existing code and tests.
     *
     * @param board board-backed game session
     */
    public Interface(Board board) {
        this((GameSession) board);
    }


    // IMAGE LOADING
    /**
     * Loads an image from the given relative path.
     *
     * @param path the classpath-relative path to the image file
     * @return the loaded Image, or null if the resource is not found
     */
    private Image tryLoad(String path) {
        try {
            var res = getClass().getResource(path);
            if (res != null) return new ImageIcon(res).getImage();
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Loads all UI images from the classpath and builds the non-interactive title text in main menu.
     */
    private void loadImages() {
        imgBckMainMenu = tryLoad("/group1/interface/BckMainMenu.png");
        imgHeaderScore = tryLoad("/group1/interface/HeaderScore.png");
        imgBtnPlay = tryLoad("/group1/interface/BtnPlay.png");
        imgBtnPlayAgain = tryLoad("/group1/interface/BtnPlayAgain.png");
        imgBtnMainMenu = tryLoad("/group1/interface/BtnMainMenu.png");
        imgBtnSquarePause = tryLoad("/group1/interface/BtnSquarePause.png");
        imgBtnSquarePlay = tryLoad("/group1/interface/BtnSquarePlay.png");
        imgBtnSquareClose = tryLoad("/group1/interface/BtnSquareClose.png");
        imgBtnSquareHelp = tryLoad("/group1/interface/BtnSquareHelp.png");
        imgBckTutorial = tryLoad("/group1/interface/BckTutorial.png");
        imgHeaderPlaytime = tryLoad("/group1/interface/HeaderPlaytime.png");
        imgPillar = tryLoad("/group1/interface/Pillar.png");
        imgHeaderHeistSuccessful = tryLoad("/group1/interface/HeaderHeistSuccessful.png");
        imgHeaderHeistUnsuccessful = tryLoad("/group1/interface/HeaderHeistUnsuccessful.png");
        imgPausedText = tryLoad("/group1/interface/TxtPaused.png");

        imgTitle = tryLoad("/group1/interface/TxtBankHeist.png");

        imgMenuThief = tryLoad("/group1/interface/Thief.png");
        imgMenuGuard = tryLoad("/group1/interface/Guard.png");
    }

    // STATE MANAGEMENT
    /**
     * Transitions the UI to a new GameState, rebuilds the button layout,
     * updates the board's paused state, and manages playtime tracking.
     *
     * @param newState the GameState to transition to
     */
    public void setState(GameState newState) {
        GameState previousState = this.state;

        // Playtime Logic
        if (newState == GameState.PLAYING) {
            if (this.state != GameState.PAUSED && this.state != GameState.TUTORIAL) {
                // New game/ Reset
                playtime.reset();
            }
            playtime.start();
        } else if (this.state == GameState.PLAYING) {
            playtime.pause();
        }

        this.state = newState;
        stateEnteredAtMs = System.currentTimeMillis();
        startTransitionRunning = shouldRunStartTransition(previousState, newState);
        if (startTransitionRunning) {
            startTransitionStartedAtMs = stateEnteredAtMs;
        } else {
            startTransitionStartedAtMs = -1L;
        }
        if (newState == GameState.MAIN_MENU || newState == GameState.PLAYING) {
            scorePopups.clear();
            prevBalance = gameSession.getBalance();
            displayedBalance = prevBalance;
            scoreHeaderPulse = 0f;
            timeHeaderPulse = 0f;
            prevElapsedText = playtime.formatTime(playtime.getElapsed());
        }
        buildButtons();
        syncPausedState();
        repaint();
    }

    /**
     * Returns the current GameState.
     *
     * @return the active GameState
     */
    public GameState getState() { return state; }


    private ButtonSpec buttonSpec(int cx, int cy, int w, int h, Image img, Runnable action) {
        return new ButtonSpec(cx, cy, w, h, img, action);
    }

    private void configureButtonLayouts() {
        buttonLayouts.put(GameState.MAIN_MENU, List.of(
                buttonSpec(UIConstants.MENU_PLAY_CX, UIConstants.MENU_PLAY_CY, UIConstants.BTN_W, UIConstants.BTN_H, imgBtnPlay,
                        this::startNewGame),
                buttonSpec(UIConstants.MENU_HELP_CX, UIConstants.MENU_HELP_CY, UIConstants.SQ_W, UIConstants.SQ_H, imgBtnSquareHelp,
                        () -> openTutorial(GameState.MAIN_MENU)),
                buttonSpec(UIConstants.MENU_CLOSE_CX, UIConstants.MENU_CLOSE_CY, UIConstants.SQ_W, UIConstants.SQ_H, imgBtnSquareClose,
                        this::exitGame)));

        buttonLayouts.put(GameState.PLAYING, List.of(
                buttonSpec(UIConstants.HUD_PAUSE_CX, UIConstants.HUD_CY, UIConstants.SQ_W, UIConstants.SQ_H, imgBtnSquarePause,
                        () -> setState(GameState.PAUSED))));

        buttonLayouts.put(GameState.PAUSED, List.of(
                buttonSpec(UIConstants.HUD_PAUSE_CX, UIConstants.HUD_CY, UIConstants.SQ_W, UIConstants.SQ_H, imgBtnSquarePlay,
                        () -> setState(GameState.PLAYING)),
                buttonSpec(UIConstants.PAUSE_RESUME_CX, UIConstants.PAUSE_RESUME_CY, UIConstants.BTN_W, UIConstants.BTN_H, imgBtnPlay,
                        () -> setState(GameState.PLAYING)),
                buttonSpec(UIConstants.PAUSE_MENU_CX, UIConstants.PAUSE_MENU_CY, UIConstants.BTN_W, UIConstants.BTN_H, imgBtnMainMenu,
                        this::returnToMainMenu),
                buttonSpec(UIConstants.PAUSE_TUTORIAL_CX, UIConstants.PAUSE_TUTORIAL_CY, UIConstants.SQ_W, UIConstants.SQ_H, imgBtnSquareHelp,
                        () -> openTutorial(GameState.PAUSED)),
                buttonSpec(UIConstants.PAUSE_CLOSE_CX, UIConstants.PAUSE_CLOSE_CY, UIConstants.SQ_W, UIConstants.SQ_H, imgBtnSquareClose,
                        this::exitGame)));

        buttonLayouts.put(GameState.TUTORIAL, List.of(
                buttonSpec(UIConstants.HUD_PAUSE_CX, UIConstants.HUD_CY, UIConstants.SQ_W, UIConstants.SQ_H, imgBtnSquareClose,
                        () -> setState(tutorialReturnState))));

        buttonLayouts.put(GameState.WIN, List.of(
                buttonSpec(UIConstants.WIN_AGAIN_CX, UIConstants.WIN_AGAIN_CY, UIConstants.BTN_W, UIConstants.BTN_H, imgBtnPlayAgain,
                        this::playAgain),
                buttonSpec(UIConstants.WIN_MENU_CX, UIConstants.WIN_MENU_CY, UIConstants.BTN_W, UIConstants.BTN_H, imgBtnMainMenu,
                        this::returnToMainMenu),
                buttonSpec(UIConstants.WIN_CLOSE_CX, UIConstants.WIN_CLOSE_CY, UIConstants.SQ_W, UIConstants.SQ_H, imgBtnSquareClose,
                        this::exitGame)));

        List<ButtonSpec> endScreenButtons = List.of(
                buttonSpec(UIConstants.WIN_AGAIN_CX, UIConstants.WIN_AGAIN_CY, UIConstants.BTN_W, UIConstants.BTN_H, imgBtnPlayAgain,
                        this::playAgain),
                buttonSpec(UIConstants.WIN_MENU_CX, UIConstants.WIN_MENU_CY, UIConstants.BTN_W, UIConstants.BTN_H, imgBtnMainMenu,
                        this::returnToMainMenu),
                buttonSpec(UIConstants.WIN_CLOSE_CX, UIConstants.WIN_CLOSE_CY, UIConstants.SQ_W, UIConstants.SQ_H, imgBtnSquareClose,
                        this::exitGame));
        buttonLayouts.put(GameState.GAME_OVER, endScreenButtons);
    }

    private void configureScreenSequences() {
        screenSequences.put(GameState.MAIN_MENU, new ScreenSequence(
                0, 380,
                0, 0,
                0, 0,
                240, 120, 280,
                0, 0,
                0, 0, 0,
                0));

        screenSequences.put(GameState.PAUSED, new ScreenSequence(
                0, 0,
                0, 0,
                0, 0,
                180, 110, 280,
                0, 320,
                0, 0, 0,
                0));

        screenSequences.put(GameState.WIN, new ScreenSequence(
                260, 320,
                470, 260,
                560, 450,
                720, 110, 280,
                0, 0,
                0, 90, 320,
                4));

        screenSequences.put(GameState.GAME_OVER, new ScreenSequence(
                0, 320,
                220, 260,
                310, 450,
                500, 110, 280,
                0, 0,
                0, 0, 0,
                0));
    }

    // BUTTON LAYOUT
    /**
     * Clears and rebuilds the button list for the current GameState.
     * All coordinates and sizes come from the LAYOUT constants.
     */

    private void buildButtons() {
        buttons.clear();
        List<ButtonSpec> specs = buttonLayouts.get(state);
        if (specs == null) {
            return;
        }

        ScreenSequence sequence = screenSequences.get(state);
        for (ButtonSpec spec : specs) {
            UIButton btn = new UIButton(spec.cx(), spec.cy(), spec.w(), spec.h(), spec.image(), FitMode.FIT, spec.action());
            if (sequence != null && sequence.buttonDurationMs() > 0) {
                btn.entranceScale = 0.72f;
                btn.entranceOffsetY = -70;
                btn.entranceProgress = 0f;
            }
            buttons.add(btn);
        }
    }

    /**
     * Resets the board and transitions the UI to the Main Menu.
     */
    private void returnToMainMenu() { gameSession.reset(); setState(GameState.MAIN_MENU); }

    /**
     * Resets the board and starts a new game immediately.
     */
    private void playAgain() {
        startNewGame();
    }

    private void startNewGame() {
        gameSession.reset();
        setState(GameState.PLAYING);
    }

    private void exitGame() {
        System.exit(0);
    }

    /**
     * Opens the tutorial screen, remembering which state to return to when closed.
     *
     * @param returnTo the GameState to transition back to when the tutorial is closed
     */
    private void openTutorial(GameState returnTo) {
        tutorialReturnState = returnTo;
        setState(GameState.TUTORIAL);
    }


    // ANIMATION
    /**
     * Advances all button animations one step and repaints the panel if anything changed
     * or the game is currently active.
     */
    private void tickAnimations() {
        boolean dirty = (state == GameState.PLAYING || state == GameState.PAUSED
                || state == GameState.MAIN_MENU || state == GameState.GAME_OVER
                || state == GameState.WIN);
        long stateElapsed = System.currentTimeMillis() - stateEnteredAtMs;

        if (updateStartTransition()) {
            dirty = true;
        }

        for (UIButton btn : buttons) {
            if (btn.tick()) dirty = true;
        }

        if (updateButtonEntranceAnimations(stateElapsed)) {
            dirty = true;
        }

        int currentBalance = gameSession.getBalance();
        if (currentBalance != prevBalance) {
            int delta = currentBalance - prevBalance;
            scorePopups.add(new InterfaceRenderer.ScorePopup(delta));
            prevBalance = currentBalance;
            scoreHeaderPulse = 1f;
            dirty = true;
        }

        if (Math.abs(displayedBalance - currentBalance) > 0.01f) {
            float diff = currentBalance - displayedBalance;
            displayedBalance += diff * 0.18f;
            if (Math.abs(diff) < 0.6f) displayedBalance = currentBalance;
            dirty = true;
        }

        if (state == GameState.PLAYING) {
            String elapsedText = playtime.formatTime(playtime.getElapsed());
            if (!elapsedText.equals(prevElapsedText)) {
                prevElapsedText = elapsedText;
                timeHeaderPulse = 1f;
                dirty = true;
            }
        }

        if (scoreHeaderPulse > 0f) {
            scoreHeaderPulse -= 0.07f;
            if (scoreHeaderPulse < 0f) scoreHeaderPulse = 0f;
            dirty = true;
        }
        if (timeHeaderPulse > 0f) {
            timeHeaderPulse -= 0.07f;
            if (timeHeaderPulse < 0f) timeHeaderPulse = 0f;
            dirty = true;
        }

        if (!scorePopups.isEmpty()) {
            for (InterfaceRenderer.ScorePopup p : scorePopups) p.ageMs += 16f;
            scorePopups.removeIf(p -> !p.isAlive());
            dirty = true;
        }

        if (dirty) repaint();
    }

    private boolean updateButtonEntranceAnimations(long stateElapsed) {
        ScreenSequence sequence = screenSequences.get(state);
        if (sequence == null || sequence.buttonDurationMs() <= 0) {
            return false;
        }
        // need to fix something is broken

        boolean dirty = false;
        for (int i = 0; i < buttons.size(); i++) {
            UIButton btn = buttons.get(i);
            float progress = easedProgress(stateElapsed,
                    sequence.buttonDelayMs() + (long) i * sequence.buttonStepMs(),
                    sequence.buttonDurationMs());
            int newOffsetY = Math.round(-70f * (1f - progress));
            float newScale = 0.72f + 0.28f * easeOutBack(progress);

            if (Math.abs(btn.entranceScale - newScale) > 0.001f
                    || btn.entranceOffsetY != newOffsetY
                    || Math.abs(btn.entranceProgress - progress) > 0.001f) {
                btn.entranceScale = newScale;
                btn.entranceOffsetY = newOffsetY;
                btn.entranceProgress = progress;
                dirty = true;
            }
        }
        return dirty;
    }

    private InterfaceRenderer.ScreenMotion buildScreenMotion(long stateElapsed) {
        ScreenSequence sequence = screenSequences.get(state);
        if (sequence == null) {
            return new InterfaceRenderer.ScreenMotion(1f, 1f, 1f, 1f, getStartTransitionProgress(), new float[0]);
        }

        float titleProgress = sequence.titleDurationMs() > 0
                ? easedProgress(stateElapsed, sequence.titleDelayMs(), sequence.titleDurationMs()) : 1f;
        float resultProgress = sequence.resultDurationMs() > 0
                ? easedProgress(stateElapsed, sequence.resultDelayMs(), sequence.resultDurationMs()) : 1f;
        float valueProgress = sequence.valueDurationMs() > 0
                ? easedProgress(stateElapsed, sequence.valueDelayMs(), sequence.valueDurationMs()) : 1f;
        float pauseTextProgress = sequence.pauseTextDurationMs() > 0
                ? easedProgress(stateElapsed, sequence.pauseTextDelayMs(), sequence.pauseTextDurationMs()) : 1f;
        float[] pillarProgress = new float[sequence.pillarCount()];
        for (int i = 0; i < pillarProgress.length; i++) {
            pillarProgress[i] = sequence.pillarDurationMs() > 0
                    ? easedProgress(stateElapsed,
                    sequence.pillarDelayMs() + (long) i * sequence.pillarStepMs(),
                    sequence.pillarDurationMs()) : 1f;
        }

        return new InterfaceRenderer.ScreenMotion(titleProgress, resultProgress, valueProgress, pauseTextProgress,
                getStartTransitionProgress(), pillarProgress);
    }

    private float easedProgress(long elapsedMs, long delayMs, long durationMs) {
        if (durationMs <= 0) {
            return 1f;
        }
        float raw = (elapsedMs - delayMs) / (float) durationMs;
        raw = Math.max(0f, Math.min(1f, raw));
        return easeOutCubic(raw);
    }

    private float easeOutCubic(float t) {
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    private float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float inv = t - 1f;
        return 1f + c3 * inv * inv * inv + c1 * inv * inv;
    }

    private boolean shouldRunStartTransition(GameState previousState, GameState newState) {
        if (System.getProperty("surefire.test.class.path") != null) {
            return false;
        }

        return newState == GameState.PLAYING
                && !GraphicsEnvironment.isHeadless()
                && previousState != GameState.PAUSED
                && previousState != GameState.TUTORIAL;
    }

    private boolean updateStartTransition() {
        if (!startTransitionRunning) {
            return false;
        }

        if (getStartTransitionProgress() >= 1f) {
            startTransitionRunning = false;
            syncPausedState();
            return true;
        }

        syncPausedState();
        return true;
    }

    private float getStartTransitionProgress() {
        if (!startTransitionRunning || startTransitionStartedAtMs < 0L) {
            return 1f;
        }

        long elapsedMs = System.currentTimeMillis() - startTransitionStartedAtMs;
        return clampProgress(elapsedMs, START_TRANSITION_DURATION_MS);
    }

    private void syncPausedState() {
        gameSession.setPaused(state != GameState.PLAYING || startTransitionRunning);
    }

    private float clampProgress(long elapsedMs, long durationMs) {
        if (durationMs <= 0L) {
            return 1f;
        }
        float raw = elapsedMs / (float) durationMs;
        return Math.max(0f, Math.min(1f, raw));
    }


    // DRAWING
    /**
     * Renders the screen overlay for the current GameState,
     * then draws the HUD (score, playtime) and all buttons on top.
     *
     * @param g the Graphics context used for drawing
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        long now = System.currentTimeMillis();
        long stateElapsed = now - stateEnteredAtMs;

        int hudBalance = (int) Math.round(displayedBalance);

        renderer.render(
                g2d,
                state,
                imgBckMainMenu,
                imgBckTutorial,
                imgTitle,
                imgPausedText,
                imgHeaderHeistSuccessful,
                imgHeaderHeistUnsuccessful,
                imgPillar,
                imgHeaderScore,
                imgHeaderPlaytime,
                hudBalance,
                playtime.getFinalTime(),
                playtime.formatTime(playtime.getElapsed()),
                imgMenuThief,
                imgMenuGuard,
                state == GameState.MAIN_MENU ? stateElapsed : 0L,
                buildScreenMotion(stateElapsed),
                scorePopups,
                scoreHeaderPulse,
                timeHeaderPulse,
                now);
        drawButtons(g2d);
        g2d.dispose();
    }

    private void drawButtons(Graphics2D g) {
        for (UIButton btn : buttons) btn.draw(g);
    }

    private void warmUpTransitions() {
        BufferedImage warmUp = new BufferedImage(UIConstants.SCR_W, UIConstants.SCR_H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = warmUp.createGraphics();
        renderer.render(
                g2d,
                GameState.PLAYING,
                imgBckMainMenu,
                imgBckTutorial,
                imgTitle,
                imgPausedText,
                imgHeaderHeistSuccessful,
                imgHeaderHeistUnsuccessful,
                imgPillar,
                imgHeaderScore,
                imgHeaderPlaytime,
                0,
                0L,
                "00:00",
                imgMenuThief,
                imgMenuGuard,
                0L,
                new InterfaceRenderer.ScreenMotion(1f, 1f, 1f, 1f, 0.25f, new float[0]),
                scorePopups,
                0f,
                0f,
                0L);
        renderer.render(
                g2d,
                GameState.PLAYING,
                imgBckMainMenu,
                imgBckTutorial,
                imgTitle,
                imgPausedText,
                imgHeaderHeistSuccessful,
                imgHeaderHeistUnsuccessful,
                imgPillar,
                imgHeaderScore,
                imgHeaderPlaytime,
                0,
                0L,
                "00:00",
                imgMenuThief,
                imgMenuGuard,
                0L,
                new InterfaceRenderer.ScreenMotion(1f, 1f, 1f, 1f, 0.7f, new float[0]),
                scorePopups,
                0f,
                0f,
                0L);
        g2d.dispose();
    }


    // INPUT
    /**
     * Registers mouse press, release, and move listeners to drive button hover
     * and press animations and to trigger click actions.
     */
    private void setupMouseListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                for (UIButton btn : buttons) {
                    if (btn.contains(e.getPoint())) {
                        btn.pressed = true;
                        btn.targetScale = PRESS_SCALE;

                        break;
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                for (UIButton btn : buttons) {

                    if (btn.pressed) {
                        btn.pressed = false;

                        if (btn.contains(e.getPoint())) {
                            btn.targetScale = HOVER_SCALE;
                            btn.action.run();
                        } else {
                            btn.targetScale = NORMAL_SCALE;
                            btn.hovered = false;
                        }

                        break;
                    }
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                for (UIButton btn : buttons) {
                    boolean was = btn.hovered;
                    btn.hovered = btn.contains(e.getPoint());
                    if (btn.hovered != was && !btn.pressed) {
                        btn.targetScale = btn.hovered ? HOVER_SCALE : NORMAL_SCALE;
                    }
                }
            }
        });
    }

    /**
     * Binds the Escape key to toggle between PLAYING and PAUSED states.
     */
    private void setupKeyBindings() {
        InputMap  im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke("pressed ESCAPE"), "togglePause");
        am.put("togglePause", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (state == GameState.PLAYING) setState(GameState.PAUSED);
                else if (state == GameState.PAUSED) setState(GameState.PLAYING);
                else if (state == GameState.TUTORIAL) setState(tutorialReturnState);
            }
        });
    }
}
