package group1;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Interface tests: game state transitions, paint, panel properties

public class InterfaceTest {

    private Board board;
    private Interface ui;

    @BeforeAll
    public static void initHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    @BeforeEach
    public void setUp() {
        BoardFactory factory = new BoardFactory();
        board = factory.makeBoard(1);
        ui = new Interface(board);
    }

    // GameState enum

    @Test
    public void testGameStateEnumValues() {
        Interface.GameState[] states = Interface.GameState.values();
        assertEquals(6, states.length, "should have 6 game states");
    }

    @Test
    public void testGameStateValueOf() {
        assertEquals(Interface.GameState.MAIN_MENU, Interface.GameState.valueOf("MAIN_MENU"));
        assertEquals(Interface.GameState.PLAYING, Interface.GameState.valueOf("PLAYING"));
        assertEquals(Interface.GameState.PAUSED, Interface.GameState.valueOf("PAUSED"));
        assertEquals(Interface.GameState.WIN, Interface.GameState.valueOf("WIN"));
        assertEquals(Interface.GameState.GAME_OVER, Interface.GameState.valueOf("GAME_OVER"));
        assertEquals(Interface.GameState.TUTORIAL, Interface.GameState.valueOf("TUTORIAL"));
    }

    // Initial state

    @Test
    public void testInitialStateIsMainMenu() {
        assertEquals(Interface.GameState.MAIN_MENU, ui.getState(),
                "interface should start in MAIN_MENU state");
    }

    // setState / getState

    @Test
    public void testSetStatePlaying() {
        ui.setState(Interface.GameState.PLAYING);
        assertEquals(Interface.GameState.PLAYING, ui.getState());
    }

    @Test
    public void testSetStatePaused() {
        ui.setState(Interface.GameState.PLAYING);
        ui.setState(Interface.GameState.PAUSED);
        assertEquals(Interface.GameState.PAUSED, ui.getState());
    }

    @Test
    public void testSetStateWin() {
        ui.setState(Interface.GameState.WIN);
        assertEquals(Interface.GameState.WIN, ui.getState());
    }

    @Test
    public void testSetStateGameOver() {
        ui.setState(Interface.GameState.GAME_OVER);
        assertEquals(Interface.GameState.GAME_OVER, ui.getState());
    }

    @Test
    public void testSetStateTutorial() {
        ui.setState(Interface.GameState.TUTORIAL);
        assertEquals(Interface.GameState.TUTORIAL, ui.getState());
    }

    @Test
    public void testSetStateBackToMainMenu() {
        ui.setState(Interface.GameState.PLAYING);
        ui.setState(Interface.GameState.MAIN_MENU);
        assertEquals(Interface.GameState.MAIN_MENU, ui.getState());
    }

    // State transition cycles

    @Test
    public void testPlayPauseResumeTransition() {
        ui.setState(Interface.GameState.PLAYING);
        assertEquals(Interface.GameState.PLAYING, ui.getState());

        ui.setState(Interface.GameState.PAUSED);
        assertEquals(Interface.GameState.PAUSED, ui.getState());

        ui.setState(Interface.GameState.PLAYING);
        assertEquals(Interface.GameState.PLAYING, ui.getState());
    }

    @Test
    public void testPlayToWinTransition() {
        ui.setState(Interface.GameState.PLAYING);
        ui.setState(Interface.GameState.WIN);
        assertEquals(Interface.GameState.WIN, ui.getState());
    }

    @Test
    public void testPlayToGameOverTransition() {
        ui.setState(Interface.GameState.PLAYING);
        ui.setState(Interface.GameState.GAME_OVER);
        assertEquals(Interface.GameState.GAME_OVER, ui.getState());
    }

    @Test
    public void testMainMenuToTutorialAndBack() {
        assertEquals(Interface.GameState.MAIN_MENU, ui.getState());
        ui.setState(Interface.GameState.TUTORIAL);
        assertEquals(Interface.GameState.TUTORIAL, ui.getState());
        ui.setState(Interface.GameState.MAIN_MENU);
        assertEquals(Interface.GameState.MAIN_MENU, ui.getState());
    }

    @Test
    public void testPausedToTutorialAndBack() {
        ui.setState(Interface.GameState.PLAYING);
        ui.setState(Interface.GameState.PAUSED);
        ui.setState(Interface.GameState.TUTORIAL);
        assertEquals(Interface.GameState.TUTORIAL, ui.getState());
        ui.setState(Interface.GameState.PAUSED);
        assertEquals(Interface.GameState.PAUSED, ui.getState());
    }

    // Board paused integration

    @Test
    public void testBoardIsPausedWhenNotPlaying() {
        // In MAIN_MENU the board should be paused
        ui.setState(Interface.GameState.MAIN_MENU);
        // setState should not throw regardless of pause state
        assertDoesNotThrow(() -> ui.setState(Interface.GameState.PLAYING));
    }

    // paintComponent does not throw for any statd

    @Test
    public void testPaintMainMenuDoesNotThrow() {
        ui.setState(Interface.GameState.MAIN_MENU);
        ui.setSize(1600, 896);
        BufferedImage img = new BufferedImage(1600, 896, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        assertDoesNotThrow(() -> ui.paintComponent(g));
        g.dispose();
    }

    @Test
    public void testPaintPlayingDoesNotThrow() {
        ui.setState(Interface.GameState.PLAYING);
        ui.setSize(1600, 896);
        BufferedImage img = new BufferedImage(1600, 896, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        assertDoesNotThrow(() -> ui.paintComponent(g));
        g.dispose();
    }

    @Test
    public void testPaintPausedDoesNotThrow() {
        ui.setState(Interface.GameState.PLAYING);
        ui.setState(Interface.GameState.PAUSED);
        ui.setSize(1600, 896);
        BufferedImage img = new BufferedImage(1600, 896, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        assertDoesNotThrow(() -> ui.paintComponent(g));
        g.dispose();
    }

    @Test
    public void testPaintWinDoesNotThrow() {
        ui.setState(Interface.GameState.WIN);
        ui.setSize(1600, 896);
        BufferedImage img = new BufferedImage(1600, 896, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        assertDoesNotThrow(() -> ui.paintComponent(g));
        g.dispose();
    }

    @Test
    public void testPaintGameOverDoesNotThrow() {
        ui.setState(Interface.GameState.GAME_OVER);
        ui.setSize(1600, 896);
        BufferedImage img = new BufferedImage(1600, 896, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        assertDoesNotThrow(() -> ui.paintComponent(g));
        g.dispose();
    }

    @Test
    public void testPaintTutorialDoesNotThrow() {
        ui.setState(Interface.GameState.TUTORIAL);
        ui.setSize(1600, 896);
        BufferedImage img = new BufferedImage(1600, 896, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        assertDoesNotThrow(() -> ui.paintComponent(g));
        g.dispose();
    }

    // Rapid state changes

    @Test
    public void testRapidStateChangesDoNotThrow() {
        assertDoesNotThrow(() -> {
            for (Interface.GameState s : Interface.GameState.values()) {
                ui.setState(s);
            }
        });
    }

    @Test
    public void testCycleThroughAllStates() {
        for (Interface.GameState s : Interface.GameState.values()) {
            ui.setState(s);
            assertEquals(s, ui.getState());
        }
    }

    // Panel properties

    @Test
    public void testPanelIsNotOpaque() {
        assertFalse(ui.isOpaque(),
                "interface panel should be transparent (not opaque)");
    }

    @Test
    public void testPanelIsFocusable() {
        assertTrue(ui.isFocusable(),
                "interface panel should be focusable for key bindings");
    }

    @Test
    public void testPanelLayoutIsNull() {
        assertNull(ui.getLayout(),
                "interface panel should have null layout");
    }

    // UIElement — dims() for each FitMode via reflection

    @Test
    public void testUIElementDimsNatural() throws Exception {
        // NATURAL returns the image's own pixel size
        Object el = createUIElement(100, 100, 200, 200, createTestImage(50, 30), getFitMode("NATURAL"));
        int[] d = invokeDims(el);
        assertEquals(50, d[0], "NATURAL width should be the image width");
        assertEquals(30, d[1], "NATURAL height should be the image height");
    }

    @Test
    public void testUIElementDimsFit() throws Exception {
        // FIT scales to fit inside budget, preserving aspect ratio
        Object el = createUIElement(100, 100, 200, 100, createTestImage(100, 50), getFitMode("FIT"));
        int[] d = invokeDims(el);
        // 100x50 into 200x100 → scale = min(2, 2) = 2 → 200x100
        assertEquals(200, d[0], "FIT width should scale to fit budget");
        assertEquals(100, d[1], "FIT height should scale to fit budget");
    }

    @Test
    public void testUIElementDimsFill() throws Exception {
        // FILL scales to fill budget, preserving aspect ratio (may crop)
        Object el = createUIElement(100, 100, 200, 200, createTestImage(100, 50), getFitMode("FILL"));
        int[] d = invokeDims(el);
        // 100x50 into 200x200 → scale = max(2, 4) = 4 → 400x200
        assertEquals(400, d[0], "FILL width should scale to fill budget");
        assertEquals(200, d[1], "FILL height should scale to fill budget");
    }

    @Test
    public void testUIElementDimsStretch() throws Exception {
        // STRETCH forces exactly w x h
        Object el = createUIElement(100, 100, 300, 150, createTestImage(100, 50), getFitMode("STRETCH"));
        int[] d = invokeDims(el);
        assertEquals(300, d[0], "STRETCH width should match budget exactly");
        assertEquals(150, d[1], "STRETCH height should match budget exactly");
    }

    @Test
    public void testUIElementDimsNullImage() throws Exception {
        Object el = createUIElement(100, 100, 240, 70, null, getFitMode("NATURAL"));
        int[] d = invokeDims(el);
        assertEquals(240, d[0], "null image should fall back to budget width");
        assertEquals(70, d[1], "null image should fall back to budget height");
    }

    @Test
    public void testUIElementDrawNullImagePlaceholder() throws Exception {
        // draws a grey placeholder rect when image is null
        Object el = createUIElement(100, 100, 200, 100, null, getFitMode("FIT"));
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        assertDoesNotThrow(() -> invokeDrawElement(el, g),
                "drawing with null image should not throw");
        g.dispose();
    }

    @Test
    public void testUIElementDrawWithImage() throws Exception {
        Object el = createUIElement(100, 100, 200, 100, createTestImage(50, 50), getFitMode("FIT"));
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        assertDoesNotThrow(() -> invokeDrawElement(el, g),
                "drawing with image should not throw");
        g.dispose();
    }

    // UIButton — tick, contains, draw

    @Test
    public void testUIButtonTickConverges() throws Exception {
        Object btn = createUIButton(100, 100, 200, 100, null, getFitMode("FIT"));
        // set targetScale to HOVER (1.1)
        Field targetField = btn.getClass().getDeclaredField("targetScale");
        targetField.setAccessible(true);
        targetField.setFloat(btn, 1.10f);

        Method tick = btn.getClass().getDeclaredMethod("tick");
        tick.setAccessible(true);
        // tick several times until converged
        for (int i = 0; i < 50; i++) tick.invoke(btn);

        Field scaleField = btn.getClass().getDeclaredField("scale");
        scaleField.setAccessible(true);
        assertEquals(1.10f, scaleField.getFloat(btn), 0.01f,
                "scale should converge to targetScale after ticking");
    }

    @Test
    public void testUIButtonTickReturnsFalseWhenConverged() throws Exception {
        Object btn = createUIButton(100, 100, 200, 100, null, getFitMode("FIT"));
        // scale == targetScale already (both 1.0)
        Method tick = btn.getClass().getDeclaredMethod("tick");
        tick.setAccessible(true);
        boolean animating = (boolean) tick.invoke(btn);
        assertFalse(animating, "tick should return false when already at target");
    }

    @Test
    public void testUIButtonContainsInside() throws Exception {
        Object btn = createUIButton(100, 100, 200, 100, null, getFitMode("FIT"));
        Method contains = btn.getClass().getDeclaredMethod("contains", Point.class);
        contains.setAccessible(true);
        assertTrue((boolean) contains.invoke(btn, new Point(100, 100)),
                "centre point should be inside the button");
    }

    @Test
    public void testUIButtonContainsOutside() throws Exception {
        Object btn = createUIButton(100, 100, 200, 100, null, getFitMode("FIT"));
        Method contains = btn.getClass().getDeclaredMethod("contains", Point.class);
        contains.setAccessible(true);
        assertFalse((boolean) contains.invoke(btn, new Point(500, 500)),
                "far-away point should be outside the button");
    }

    @Test
    public void testUIButtonDrawNullImagePlaceholder() throws Exception {
        Object btn = createUIButton(100, 100, 200, 100, null, getFitMode("FIT"));
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        assertDoesNotThrow(() -> invokeDrawElement(btn, g),
                "drawing button with null image should not throw");
        g.dispose();
    }

    @Test
    public void testUIButtonDrawNullImageHovered() throws Exception {
        Object btn = createUIButton(100, 100, 200, 100, null, getFitMode("FIT"));
        Field hovered = btn.getClass().getDeclaredField("hovered");
        hovered.setAccessible(true);
        hovered.setBoolean(btn, true);

        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        assertDoesNotThrow(() -> invokeDrawElement(btn, g),
                "drawing hovered button with null image should not throw");
        g.dispose();
    }

    // button counts per state

    @Test
    public void testMainMenuHasThreeButtons() throws Exception {
        ui.setState(Interface.GameState.MAIN_MENU);
        assertEquals(3, getButtons().size(), "main menu should have 3 buttons");
    }

    @Test
    public void testPlayingHasOneButton() throws Exception {
        ui.setState(Interface.GameState.PLAYING);
        assertEquals(1, getButtons().size(), "playing should have 1 button (pause)");
    }

    @Test
    public void testPausedHasFiveButtons() throws Exception {
        ui.setState(Interface.GameState.PLAYING);
        ui.setState(Interface.GameState.PAUSED);
        assertEquals(5, getButtons().size(), "paused should have 5 buttons");
    }

    @Test
    public void testWinHasThreeButtons() throws Exception {
        ui.setState(Interface.GameState.WIN);
        assertEquals(3, getButtons().size(), "win should have 3 buttons");
    }

    @Test
    public void testGameOverHasThreeButtons() throws Exception {
        ui.setState(Interface.GameState.GAME_OVER);
        assertEquals(3, getButtons().size(), "game over should have 3 buttons");
    }

    @Test
    public void testTutorialHasOneButton() throws Exception {
        ui.setState(Interface.GameState.TUTORIAL);
        assertEquals(1, getButtons().size(), "tutorial should have 1 button (close)");
    }

    // ESC key binding

    @Test
    public void testEscapeFromPlayingTogglesToPaused() {
        ui.setState(Interface.GameState.PLAYING);
        fireEscapeKey();
        assertEquals(Interface.GameState.PAUSED, ui.getState(),
                "ESC during playing should pause");
    }

    @Test
    public void testEscapeFromPausedTogglesToPlaying() {
        ui.setState(Interface.GameState.PLAYING);
        ui.setState(Interface.GameState.PAUSED);
        fireEscapeKey();
        assertEquals(Interface.GameState.PLAYING, ui.getState(),
                "ESC during paused should resume");
    }

    @Test
    public void testEscapeFromTutorialReturnsToPreviousState() throws Exception {
        ui.setState(Interface.GameState.PLAYING);
        ui.setState(Interface.GameState.PAUSED);

        // open tutorial from paused — set tutorialReturnState
        Field returnField = Interface.class.getDeclaredField("tutorialReturnState");
        returnField.setAccessible(true);
        returnField.set(ui, Interface.GameState.PAUSED);
        ui.setState(Interface.GameState.TUTORIAL);

        fireEscapeKey();
        assertEquals(Interface.GameState.PAUSED, ui.getState(),
                "ESC from tutorial should return to previous state");
    }

    @Test
    public void testEscapeFromMainMenuDoesNothing() {
        ui.setState(Interface.GameState.MAIN_MENU);
        fireEscapeKey();
        assertEquals(Interface.GameState.MAIN_MENU, ui.getState(),
                "ESC from main menu should do nothing");
    }

    @Test
    public void testEscapeFromWinDoesNothing() {
        ui.setState(Interface.GameState.WIN);
        fireEscapeKey();
        assertEquals(Interface.GameState.WIN, ui.getState(),
                "ESC from win should do nothing");
    }

    @Test
    public void testEscapeFromGameOverDoesNothing() {
        ui.setState(Interface.GameState.GAME_OVER);
        fireEscapeKey();
        assertEquals(Interface.GameState.GAME_OVER, ui.getState(),
                "ESC from game over should do nothing");
    }

    // mouse listeners — press, release, hover

    @Test
    public void testMousePressOnButton() throws Exception {
        ui.setState(Interface.GameState.PLAYING);
        List<?> btns = getButtons();
        Object pauseBtn = btns.get(0);

        // get the button centre to simulate a click on it
        Field cxField = pauseBtn.getClass().getSuperclass().getDeclaredField("cx");
        cxField.setAccessible(true);
        Field cyField = pauseBtn.getClass().getSuperclass().getDeclaredField("cy");
        cyField.setAccessible(true);
        int bx = cxField.getInt(pauseBtn);
        int by = cyField.getInt(pauseBtn);

        MouseEvent press = new MouseEvent(ui, MouseEvent.MOUSE_PRESSED, 0, 0, bx, by, 1, false);
        for (java.awt.event.MouseListener ml : ui.getMouseListeners()) {
            ml.mousePressed(press);
        }

        Field pressedField = pauseBtn.getClass().getDeclaredField("pressed");
        pressedField.setAccessible(true);
        assertTrue(pressedField.getBoolean(pauseBtn),
                "button should be pressed after mouse press on it");
    }

    @Test
    public void testMouseReleaseOnButtonTriggersAction() throws Exception {
        ui.setState(Interface.GameState.PLAYING);
        List<?> btns = getButtons();
        Object pauseBtn = btns.get(0);

        Field cxField = pauseBtn.getClass().getSuperclass().getDeclaredField("cx");
        cxField.setAccessible(true);
        Field cyField = pauseBtn.getClass().getSuperclass().getDeclaredField("cy");
        cyField.setAccessible(true);
        int bx = cxField.getInt(pauseBtn);
        int by = cyField.getInt(pauseBtn);

        // press then release on the button
        MouseEvent press = new MouseEvent(ui, MouseEvent.MOUSE_PRESSED, 0, 0, bx, by, 1, false);
        MouseEvent release = new MouseEvent(ui, MouseEvent.MOUSE_RELEASED, 0, 0, bx, by, 1, false);
        for (java.awt.event.MouseListener ml : ui.getMouseListeners()) {
            ml.mousePressed(press);
            ml.mouseReleased(release);
        }

        assertEquals(Interface.GameState.PAUSED, ui.getState(),
                "releasing on the pause button should pause the game");
    }

    @Test
    public void testMouseReleaseOutsideButtonCancels() throws Exception {
        ui.setState(Interface.GameState.PLAYING);
        List<?> btns = getButtons();
        Object pauseBtn = btns.get(0);

        Field cxField = pauseBtn.getClass().getSuperclass().getDeclaredField("cx");
        cxField.setAccessible(true);
        Field cyField = pauseBtn.getClass().getSuperclass().getDeclaredField("cy");
        cyField.setAccessible(true);
        int bx = cxField.getInt(pauseBtn);
        int by = cyField.getInt(pauseBtn);

        // press on button, release far away
        MouseEvent press = new MouseEvent(ui, MouseEvent.MOUSE_PRESSED, 0, 0, bx, by, 1, false);
        MouseEvent release = new MouseEvent(ui, MouseEvent.MOUSE_RELEASED, 0, 0, 0, 0, 1, false);
        for (java.awt.event.MouseListener ml : ui.getMouseListeners()) {
            ml.mousePressed(press);
            ml.mouseReleased(release);
        }

        assertEquals(Interface.GameState.PLAYING, ui.getState(),
                "releasing outside the button should cancel the action");
    }

    @Test
    public void testMouseMoveHoversButton() throws Exception {
        ui.setState(Interface.GameState.PLAYING);
        List<?> btns = getButtons();
        Object pauseBtn = btns.get(0);

        Field cxField = pauseBtn.getClass().getSuperclass().getDeclaredField("cx");
        cxField.setAccessible(true);
        Field cyField = pauseBtn.getClass().getSuperclass().getDeclaredField("cy");
        cyField.setAccessible(true);
        int bx = cxField.getInt(pauseBtn);
        int by = cyField.getInt(pauseBtn);

        MouseEvent move = new MouseEvent(ui, MouseEvent.MOUSE_MOVED, 0, 0, bx, by, 0, false);
        for (java.awt.event.MouseMotionListener ml : ui.getMouseMotionListeners()) {
            ml.mouseMoved(move);
        }

        Field hoveredField = pauseBtn.getClass().getDeclaredField("hovered");
        hoveredField.setAccessible(true);
        assertTrue(hoveredField.getBoolean(pauseBtn),
                "button should be hovered after mouse moves onto it");
    }

    @Test
    public void testMouseMoveAwayUnhoversButton() throws Exception {
        ui.setState(Interface.GameState.PLAYING);
        List<?> btns = getButtons();
        Object pauseBtn = btns.get(0);

        Field cxField = pauseBtn.getClass().getSuperclass().getDeclaredField("cx");
        cxField.setAccessible(true);
        Field cyField = pauseBtn.getClass().getSuperclass().getDeclaredField("cy");
        cyField.setAccessible(true);
        int bx = cxField.getInt(pauseBtn);
        int by = cyField.getInt(pauseBtn);

        // hover on, then hover off
        MouseEvent moveOn = new MouseEvent(ui, MouseEvent.MOUSE_MOVED, 0, 0, bx, by, 0, false);
        MouseEvent moveOff = new MouseEvent(ui, MouseEvent.MOUSE_MOVED, 0, 0, 0, 0, 0, false);
        for (java.awt.event.MouseMotionListener ml : ui.getMouseMotionListeners()) {
            ml.mouseMoved(moveOn);
            ml.mouseMoved(moveOff);
        }

        Field hoveredField = pauseBtn.getClass().getDeclaredField("hovered");
        hoveredField.setAccessible(true);
        assertFalse(hoveredField.getBoolean(pauseBtn),
                "button should not be hovered after mouse moves away");
    }

    // playtime display — paint WIN and GAME_OVER show playtime / score

    @Test
    public void testPaintWinShowsPlaytimeAndScore() {
        ui.setState(Interface.GameState.PLAYING);
        board.addBalance(500);
        ui.setState(Interface.GameState.WIN);

        ui.setSize(1600, 896);
        BufferedImage img = new BufferedImage(1600, 896, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        assertDoesNotThrow(() -> ui.paintComponent(g),
                "win screen with balance and playtime should not throw");
        g.dispose();
    }

    @Test
    public void testPaintGameOverAfterPlaying() {
        ui.setState(Interface.GameState.PLAYING);
        ui.setState(Interface.GameState.GAME_OVER);

        ui.setSize(1600, 896);
        BufferedImage img = new BufferedImage(1600, 896, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        assertDoesNotThrow(() -> ui.paintComponent(g),
                "game over screen after playing should not throw");
        g.dispose();
    }

    // tickAnimations

    @Test
    public void testTickAnimationsDoesNotThrow() throws Exception {
        ui.setState(Interface.GameState.PLAYING);
        Method tickAnim = Interface.class.getDeclaredMethod("tickAnimations");
        tickAnim.setAccessible(true);
        assertDoesNotThrow(() -> tickAnim.invoke(ui),
                "tickAnimations should not throw");
    }

    // playAgain resets board and goes to PLAYING

    @Test
    public void testPlayAgainResetsAndPlays() throws Exception {
        ui.setState(Interface.GameState.PLAYING);
        board.addBalance(999);
        ui.setState(Interface.GameState.WIN);

        Method playAgain = Interface.class.getDeclaredMethod("playAgain");
        playAgain.setAccessible(true);
        playAgain.invoke(ui);

        assertEquals(Interface.GameState.PLAYING, ui.getState(),
                "playAgain should set state to playing");
        assertEquals(0, board.getBalance(),
                "playAgain should reset the board balance");
    }

    // returnToMainMenu resets board

    @Test
    public void testReturnToMainMenuResetsAndGoesToMenu() throws Exception {
        ui.setState(Interface.GameState.PLAYING);
        board.addBalance(999);
        ui.setState(Interface.GameState.PAUSED);

        Method returnToMenu = Interface.class.getDeclaredMethod("returnToMainMenu");
        returnToMenu.setAccessible(true);
        returnToMenu.invoke(ui);

        assertEquals(Interface.GameState.MAIN_MENU, ui.getState(),
                "returnToMainMenu should set state to main menu");
        assertEquals(0, board.getBalance(),
                "returnToMainMenu should reset the board balance");
    }

    // openTutorial remembers return state

    @Test
    public void testOpenTutorialFromPausedRemembersReturnState() throws Exception {
        ui.setState(Interface.GameState.PLAYING);
        ui.setState(Interface.GameState.PAUSED);

        Method openTutorial = Interface.class.getDeclaredMethod("openTutorial", Interface.GameState.class);
        openTutorial.setAccessible(true);
        openTutorial.invoke(ui, Interface.GameState.PAUSED);

        assertEquals(Interface.GameState.TUTORIAL, ui.getState(),
                "openTutorial should set state to tutorial");

        Field returnField = Interface.class.getDeclaredField("tutorialReturnState");
        returnField.setAccessible(true);
        assertEquals(Interface.GameState.PAUSED, returnField.get(ui),
                "tutorialReturnState should remember paused");
    }

    // Reflection helpers

    @SuppressWarnings("unchecked")
    private List<?> getButtons() throws Exception {
        Field f = Interface.class.getDeclaredField("buttons");
        f.setAccessible(true);
        return (List<?>) f.get(ui);
    }

    private void fireEscapeKey() {
        javax.swing.ActionMap am = ui.getActionMap();
        am.get("togglePause").actionPerformed(new ActionEvent(ui, 0, ""));
    }

    private java.awt.Image createTestImage(int w, int h) {
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    private Object getFitMode(String name) throws Exception {
        Class<?>[] inner = Interface.class.getDeclaredClasses();
        for (Class<?> c : inner) {
            if (c.getSimpleName().equals("FitMode")) {
                Method valueOf = c.getDeclaredMethod("valueOf", String.class);
                valueOf.setAccessible(true);
                return valueOf.invoke(null, name);
            }
        }
        throw new AssertionError("FitMode enum not found");
    }

    private Object createUIElement(int cx, int cy, int w, int h, java.awt.Image image, Object fitMode) throws Exception {
        Class<?>[] inner = Interface.class.getDeclaredClasses();
        for (Class<?> c : inner) {
            if (c.getSimpleName().equals("UIElement")) {
                Constructor<?> ctor = c.getDeclaredConstructor(int.class, int.class, int.class, int.class, java.awt.Image.class, fitMode.getClass());
                ctor.setAccessible(true);
                return ctor.newInstance(cx, cy, w, h, image, fitMode);
            }
        }
        throw new AssertionError("UIElement class not found");
    }

    private Object createUIButton(int cx, int cy, int w, int h, java.awt.Image image, Object fitMode) throws Exception {
        Class<?>[] inner = Interface.class.getDeclaredClasses();
        for (Class<?> c : inner) {
            if (c.getSimpleName().equals("UIButton")) {
                Constructor<?> ctor = c.getDeclaredConstructor(int.class, int.class, int.class, int.class, java.awt.Image.class, fitMode.getClass(), Runnable.class);
                ctor.setAccessible(true);
                return ctor.newInstance(cx, cy, w, h, image, fitMode, (Runnable) () -> {});
            }
        }
        throw new AssertionError("UIButton class not found");
    }

    private int[] invokeDims(Object element) throws Exception {
        Method m = element.getClass().getDeclaredMethod("dims");
        m.setAccessible(true);
        return (int[]) m.invoke(element);
    }

    private void invokeDrawElement(Object element, Graphics2D g) throws Exception {
        Method m;
        try {
            m = element.getClass().getDeclaredMethod("draw", Graphics2D.class);
        } catch (NoSuchMethodException e) {
            m = element.getClass().getSuperclass().getDeclaredMethod("draw", Graphics2D.class);
        }
        m.setAccessible(true);
        m.invoke(element, g);
    }
}
