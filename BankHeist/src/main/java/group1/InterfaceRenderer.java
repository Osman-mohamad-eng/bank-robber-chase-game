package group1;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.util.List;

/**
 * Renders non-interactive UI screens and HUD overlays for the game interface.
 */
final class InterfaceRenderer {

    static final class ScreenMotion {
        final float titleProgress;
        final float resultProgress;
        final float valueProgress;
        final float pauseTextProgress;
        final float startTransitionProgress;
        final float[] pillarProgress;

        ScreenMotion(float titleProgress, float resultProgress, float valueProgress,
                float pauseTextProgress, float startTransitionProgress, float[] pillarProgress) {
            this.titleProgress = titleProgress;
            this.resultProgress = resultProgress;
            this.valueProgress = valueProgress;
            this.pauseTextProgress = pauseTextProgress;
            this.startTransitionProgress = startTransitionProgress;
            this.pillarProgress = pillarProgress;
        }
    }

    private static final int CHASE_GUARD_COUNT = 2;

    private float chaseThiefX = -1;
    private float chaseThiefY;
    private float chaseThiefDx;
    private float chaseThiefDy;
    private final float[] chaseGuardX = new float[CHASE_GUARD_COUNT];
    private final float[] chaseGuardY = new float[CHASE_GUARD_COUNT];
    private final float[] chaseGuardDx = new float[CHASE_GUARD_COUNT];
    private final float[] chaseGuardDy = new float[CHASE_GUARD_COUNT];
    private long chaseLastMs = 0;
    private final java.util.Random chaseRandom = new java.util.Random(42);

    static final class ScorePopup {
        final int amount;
        float ageMs = 0f;
        static final float LIFETIME_MS = 900f;

        ScorePopup(int amount) {
            this.amount = amount;
        }

        boolean isAlive() {
            return ageMs < LIFETIME_MS;
        }
    }

    private static final String FONT_FAMILY = "Arial";
    private static final Font FONT_MENU_TITLE = new Font(FONT_FAMILY, Font.BOLD, 96);
    private static final Font FONT_PAUSED = new Font(FONT_FAMILY, Font.BOLD, 72);
    private static final Font FONT_END_TITLE = new Font(FONT_FAMILY, Font.BOLD, 88);
    private static final Font FONT_END_SCORE = new Font(FONT_FAMILY, Font.PLAIN, 48);
    private static final Font FONT_TUTORIAL = new Font(FONT_FAMILY, Font.BOLD, 48);
    private static final Font FONT_SCORE = new Font(FONT_FAMILY, Font.BOLD, 28);
    private static final Font FONT_TIME = new Font(FONT_FAMILY, Font.BOLD, 28);

    private static final int HUD_SCORE_X = 0;
    private static final int HUD_TIME_X = 250;
    private static final int HUD_HEADER_Y = 1;
    private static final int HUD_HEADER_W = 280;
    private static final int HUD_HEADER_H = 70;

    private static final int CENTER_X = UIConstants.SCR_W / 2;
    private static final int MENU_TITLE_CX = CENTER_X;
    private static final int MENU_TITLE_CY = 300;
    private static final int MENU_TITLE_W = 850;
    private static final int MENU_TITLE_H = 160 * 192;
    private static final int START_WIPE_MAX_R = 1100;
    private static final int PAUSE_TEXT_W = 400;
    private static final int PAUSE_TEXT_H = 170;
    private static final int PAUSE_TEXT_Y = 250;
    private static final int END_TITLE_Y = 260;
    private static final int END_SCORE_Y = 340;
    private static final int WIN_HEADER_CY = 170;
    private static final int WIN_HEADER_W = 1000;
    private static final int WIN_HEADER_H = 260;
    private static final int GAME_OVER_HEADER_W = 860;
    private static final int GAME_OVER_HEADER_H = 220;
    private static final int WIN_PILLAR_W = 280;
    private static final int WIN_PILLAR_H = 1000;
    private static final int WIN_PILLAR_Y = 450;
    private static final int WIN_LEFT_OUTER_PILLAR_X = 100;
    private static final int WIN_LEFT_INNER_PILLAR_X = 370;
    private static final int WIN_RIGHT_INNER_PILLAR_X = 1230;
    private static final int WIN_RIGHT_OUTER_PILLAR_X = 1500;
    private static final int WIN_RESULT_Y = 380;
    private static final int WIN_SCORE_X = 675;
    private static final int WIN_TIME_X = 925;
    private static final int WIN_RESULT_W = HUD_HEADER_W;
    private static final int WIN_RESULT_H = HUD_HEADER_H;

    private static final int CHASE_SPRITE_SIZE = 170;
    private static final int MENU_CHASE_Y = UIConstants.SCR_H - 70;

    void render(Graphics2D g, Interface.GameState state, Image mainMenuBackground, Image tutorialBackground,
            Image titleImage, Image pausedTextImage, Image winHeaderImage, Image gameOverHeaderImage, Image pillarImage,
            Image scoreHeaderImage, Image timeHeaderImage, int balance, long finalTimeMs,
            String currentElapsedText, Image menuThiefImage, Image menuGuardImage, long menuAnimMs,
            ScreenMotion motion,
            List<ScorePopup> scorePopups, float scoreHeaderPulse, float timeHeaderPulse,
            long idleAnimMs) {
        drawCurrentScreen(g, state, mainMenuBackground, tutorialBackground, titleImage, pausedTextImage, winHeaderImage,
                gameOverHeaderImage, pillarImage, scoreHeaderImage, timeHeaderImage, balance, finalTimeMs,
                menuThiefImage, menuGuardImage, menuAnimMs, motion);
        drawHud(g, state, scoreHeaderImage, timeHeaderImage, balance, currentElapsedText, scorePopups,
                scoreHeaderPulse, timeHeaderPulse);
    }

    private void drawCurrentScreen(Graphics2D g, Interface.GameState state, Image mainMenuBackground,
            Image tutorialBackground, Image titleImage, Image pausedTextImage, Image winHeaderImage, Image gameOverHeaderImage,
            Image pillarImage, Image scoreHeaderImage, Image timeHeaderImage, int balance, long finalTimeMs,
            Image menuThiefImage, Image menuGuardImage, long menuAnimMs, ScreenMotion motion) {
        switch (state) {
            case MAIN_MENU -> drawMainMenu(g, mainMenuBackground, titleImage, menuThiefImage, menuGuardImage, menuAnimMs, motion);
            case PAUSED -> drawPaused(g, pausedTextImage, motion);
            case WIN -> drawWin(g, winHeaderImage, pillarImage, scoreHeaderImage, timeHeaderImage, balance, finalTimeMs, motion);
            case GAME_OVER -> drawGameOver(g, gameOverHeaderImage, scoreHeaderImage, timeHeaderImage, balance, finalTimeMs, motion);
            case TUTORIAL -> drawTutorial(g, tutorialBackground);
            case PLAYING -> drawStartTransition(g, titleImage, motion.startTransitionProgress);
        }
    }

    private void drawHud(Graphics2D g, Interface.GameState state, Image scoreHeaderImage, Image timeHeaderImage,
            int balance, String currentElapsedText, List<ScorePopup> scorePopups,
            float scoreHeaderPulse, float timeHeaderPulse) {
        if (state == Interface.GameState.PLAYING || state == Interface.GameState.PAUSED) {
            drawScoreHeader(g, scoreHeaderImage, balance, scoreHeaderPulse);
            drawTimeHeader(g, timeHeaderImage, currentElapsedText, timeHeaderPulse);
            drawScorePopups(g, scorePopups);
        }
    }

    private void drawMainMenu(Graphics2D g, Image backgroundImage, Image titleImage, Image menuThiefImage,
            Image menuGuardImage, long animMs, ScreenMotion motion) {
        g.setColor(new Color(10, 10, 30));
        g.fillRect(0, 0, UIConstants.SCR_W, UIConstants.SCR_H);

        if (backgroundImage != null) {
            g.drawImage(backgroundImage, 0, 0, UIConstants.SCR_W, UIConstants.SCR_H, null);
        }

        drawChaseLoop(g, menuThiefImage, menuGuardImage, animMs, MENU_CHASE_Y);

        if (titleImage != null) {
            drawAnimatedDropImage(g, titleImage, MENU_TITLE_CX, MENU_TITLE_CY, MENU_TITLE_W, MENU_TITLE_H, motion.titleProgress, 160);
        } else {
            g.setColor(Color.WHITE);
            g.setFont(FONT_MENU_TITLE);
            drawCenteredText(g, "BANK HEIST", CENTER_X, 310 + Math.round(-160f * (1f - motion.titleProgress)));
        }
    }

    private void drawSmoothLayer(Graphics2D g, Image image, float cx, float cy,
            int baseW, int baseH, float scale) {
        drawSmoothLayer(g, image, cx, cy, baseW, baseH, scale, 0.0);
    }

    private void drawSmoothLayer(Graphics2D g, Image image, float cx, float cy,
            int baseW, int baseH, float scale, double rotationDeg) {
        if (image == null) {
            return;
        }

        int[] dims = getFitDimensions(image, baseW, baseH);
        AffineTransform old = g.getTransform();
        g.translate(cx, cy);
        if (rotationDeg != 0.0) {
            g.rotate(Math.toRadians(rotationDeg));
        }
        g.scale(scale, scale);
        g.drawImage(image, -dims[0] / 2, -dims[1] / 2, dims[0], dims[1], null);
        g.setTransform(old);
    }

    private void drawChaseLoop(Graphics2D g, Image thiefImage, Image guardImage, long animMs, int baseY) {
        if (thiefImage == null || guardImage == null) {
            return;
        }

        long now = System.currentTimeMillis();
        float half = CHASE_SPRITE_SIZE / 2f;
        float thiefSpeed = 190f; // px/s
        float guardSpeed = 155f; // slightly slower so it never fully catches up

        if (chaseThiefX < 0) {
            double angle = chaseRandom.nextDouble() * Math.PI * 2;
            chaseThiefX = UIConstants.SCR_W * 0.35f;
            chaseThiefY = UIConstants.SCR_H * 0.5f;
            chaseThiefDx = thiefSpeed * (float) Math.cos(angle);
            chaseThiefDy = thiefSpeed * (float) Math.sin(angle);
            for (int gi = 0; gi < CHASE_GUARD_COUNT; gi++) {
                float spread = (gi - (CHASE_GUARD_COUNT - 1) * 0.5f) * CHASE_SPRITE_SIZE * 1.4f;
                float perpX = -chaseThiefDy / thiefSpeed;
                float perpY = chaseThiefDx / thiefSpeed;
                chaseGuardX[gi] = chaseThiefX - 160 * chaseThiefDx / thiefSpeed + perpX * spread;
                chaseGuardY[gi] = chaseThiefY - 160 * chaseThiefDy / thiefSpeed + perpY * spread;
                chaseGuardDx[gi] = chaseThiefDx;
                chaseGuardDy[gi] = chaseThiefDy;
            }
            chaseLastMs = now;
        }

        float dt = Math.min((now - chaseLastMs) / 1000f, 0.08f);
        chaseLastMs = now;

        chaseThiefX += chaseThiefDx * dt;
        chaseThiefY += chaseThiefDy * dt;
        if (chaseThiefX < half) {
            chaseThiefX = half;
            chaseThiefDx = Math.abs(chaseThiefDx);
        }
        if (chaseThiefX > UIConstants.SCR_W - half) {
            chaseThiefX = UIConstants.SCR_W - half;
            chaseThiefDx = -Math.abs(chaseThiefDx);
        }
        if (chaseThiefY < half) {
            chaseThiefY = half;
            chaseThiefDy = Math.abs(chaseThiefDy);
        }
        if (chaseThiefY > UIConstants.SCR_H - half) {
            chaseThiefY = UIConstants.SCR_H - half;
            chaseThiefDy = -Math.abs(chaseThiefDy);
        }

        float minGuardThief = CHASE_SPRITE_SIZE * 1.5f;
        float minGuardGuard = CHASE_SPRITE_SIZE * 1.2f;

        for (int gi = 0; gi < CHASE_GUARD_COUNT; gi++) {
            float tdx = chaseThiefX - chaseGuardX[gi];
            float tdy = chaseThiefY - chaseGuardY[gi];
            float dist = (float) Math.sqrt(tdx * tdx + tdy * tdy);
            if (dist > 1f) {
                tdx /= dist;
                tdy /= dist;
            }

            float steer = Math.min(1.2f * dt, 1f);
            chaseGuardDx[gi] = chaseGuardDx[gi] * (1f - steer) + tdx * guardSpeed * steer;
            chaseGuardDy[gi] = chaseGuardDy[gi] * (1f - steer) + tdy * guardSpeed * steer;
            float gspd = (float) Math.sqrt(chaseGuardDx[gi] * chaseGuardDx[gi] + chaseGuardDy[gi] * chaseGuardDy[gi]);
            if (gspd > 0f) {
                chaseGuardDx[gi] = chaseGuardDx[gi] / gspd * guardSpeed;
                chaseGuardDy[gi] = chaseGuardDy[gi] / gspd * guardSpeed;
            }

            chaseGuardX[gi] += chaseGuardDx[gi] * dt;
            chaseGuardY[gi] += chaseGuardDy[gi] * dt;

            pushGuardFromThief(gi, minGuardThief, guardSpeed);

            if (chaseGuardX[gi] < half) {
                chaseGuardX[gi] = half;
                chaseGuardDx[gi] = Math.abs(chaseGuardDx[gi]);
            }
            if (chaseGuardX[gi] > UIConstants.SCR_W - half) {
                chaseGuardX[gi] = UIConstants.SCR_W - half;
                chaseGuardDx[gi] = -Math.abs(chaseGuardDx[gi]);
            }
            if (chaseGuardY[gi] < half) {
                chaseGuardY[gi] = half;
                chaseGuardDy[gi] = Math.abs(chaseGuardDy[gi]);
            }
            if (chaseGuardY[gi] > UIConstants.SCR_H - half) {
                chaseGuardY[gi] = UIConstants.SCR_H - half;
                chaseGuardDy[gi] = -Math.abs(chaseGuardDy[gi]);
            }
        }

        for (int a = 0; a < CHASE_GUARD_COUNT; a++) {
            for (int b = a + 1; b < CHASE_GUARD_COUNT; b++) {
                pushGuardsApart(a, b, minGuardGuard, guardSpeed);
            }
        }

        double t = animMs / 1000.0;
        float thiefBob = (float) (Math.sin(t * 10.0) * 4.0);

        double thiefAngleDeg = Math.toDegrees(Math.atan2(chaseThiefDx, -chaseThiefDy));
        for (int gi = 0; gi < CHASE_GUARD_COUNT; gi++) {
            float guardBob = (float) (Math.sin(t * 10.0 + Math.PI * (gi + 1)) * 4.0);
            double guardAngleDeg = Math.toDegrees(Math.atan2(chaseGuardDx[gi], -chaseGuardDy[gi]));
            drawSmoothLayer(g, guardImage, chaseGuardX[gi], chaseGuardY[gi] + guardBob,
                    CHASE_SPRITE_SIZE, CHASE_SPRITE_SIZE, 1.0f, guardAngleDeg);
        }
        drawSmoothLayer(g, thiefImage, chaseThiefX, chaseThiefY + thiefBob,
                CHASE_SPRITE_SIZE, CHASE_SPRITE_SIZE, 1.0f, thiefAngleDeg);
    }

    private void pushGuardFromThief(int gi, float minGap, float guardSpeed) {
        float sx = chaseGuardX[gi] - chaseThiefX;
        float sy = chaseGuardY[gi] - chaseThiefY;
        float sep = (float) Math.sqrt(sx * sx + sy * sy);
        if (sep < minGap && sep > 0f) {
            float nx = sx / sep;
            float ny = sy / sep;
            chaseGuardX[gi] = chaseThiefX + nx * minGap;
            chaseGuardY[gi] = chaseThiefY + ny * minGap;
            float dot = chaseGuardDx[gi] * nx + chaseGuardDy[gi] * ny;
            if (dot < 0f) {
                chaseGuardDx[gi] -= dot * nx;
                chaseGuardDy[gi] -= dot * ny;
                float s = (float) Math.sqrt(chaseGuardDx[gi] * chaseGuardDx[gi] + chaseGuardDy[gi] * chaseGuardDy[gi]);
                if (s > 0f) {
                    chaseGuardDx[gi] = chaseGuardDx[gi] / s * guardSpeed;
                    chaseGuardDy[gi] = chaseGuardDy[gi] / s * guardSpeed;
                }
            }
        }
    }

    private void pushGuardsApart(int a, int b, float minDist, float speed) {
        float sx = chaseGuardX[b] - chaseGuardX[a];
        float sy = chaseGuardY[b] - chaseGuardY[a];
        float sep = (float) Math.sqrt(sx * sx + sy * sy);
        if (sep < minDist && sep > 0f) {
            float nx = sx / sep;
            float ny = sy / sep;
            float push = (minDist - sep) * 0.5f;
            chaseGuardX[a] -= nx * push;
            chaseGuardY[a] -= ny * push;
            chaseGuardX[b] += nx * push;
            chaseGuardY[b] += ny * push;
            float dotA = chaseGuardDx[a] * nx + chaseGuardDy[a] * ny;
            if (dotA > 0f) {
                chaseGuardDx[a] -= dotA * nx;
                chaseGuardDy[a] -= dotA * ny;
                float sA = (float) Math.sqrt(chaseGuardDx[a] * chaseGuardDx[a] + chaseGuardDy[a] * chaseGuardDy[a]);
                if (sA > 0f) {
                    chaseGuardDx[a] = chaseGuardDx[a] / sA * speed;
                    chaseGuardDy[a] = chaseGuardDy[a] / sA * speed;
                }
            }
            float dotB = chaseGuardDx[b] * (-nx) + chaseGuardDy[b] * (-ny);
            if (dotB > 0f) {
                chaseGuardDx[b] += dotB * nx;
                chaseGuardDy[b] += dotB * ny;
                float sB = (float) Math.sqrt(chaseGuardDx[b] * chaseGuardDx[b] + chaseGuardDy[b] * chaseGuardDy[b]);
                if (sB > 0f) {
                    chaseGuardDx[b] = chaseGuardDx[b] / sB * speed;
                    chaseGuardDy[b] = chaseGuardDy[b] / sB * speed;
                }
            }
        }
    }

    private void drawScorePopups(Graphics2D g, List<ScorePopup> popups) {
        if (popups == null || popups.isEmpty()) {
            return;
        }

        Font popupFont = new Font(FONT_FAMILY, Font.BOLD, 28);
        g.setFont(popupFont);
        FontMetrics fm = g.getFontMetrics();
        Composite oldComp = g.getComposite();

        int anchorX = HUD_SCORE_X + (HUD_HEADER_W * 3 / 5);
        int anchorY = HUD_HEADER_Y + (HUD_HEADER_H / 4) + 40;

        for (ScorePopup p : popups) {
            float t = p.ageMs / ScorePopup.LIFETIME_MS;
            if (t >= 1f) {
                continue;
            }

            float alpha = 1f - t;
            int dy = (int) (-60f * t);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            String text = (p.amount >= 0 ? "+" : "") + p.amount;
            Color color = p.amount >= 0 ? new Color(71, 255, 98) : new Color(255, 81, 81);

            g.setColor(color);
            g.drawString(text, anchorX - fm.stringWidth(text) / 2, anchorY + dy);
        }
        g.setComposite(oldComp);
    }

    private void drawPaused(Graphics2D g, Image pausedTextImage, ScreenMotion motion) {
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, UIConstants.SCR_W, UIConstants.SCR_H);

        if (pausedTextImage != null) {
            drawAnimatedDropImage(g, pausedTextImage, CENTER_X, PAUSE_TEXT_Y, PAUSE_TEXT_W, PAUSE_TEXT_H,
                    motion.pauseTextProgress, 130);
        } else {
            g.setColor(Color.WHITE);
            g.setFont(FONT_PAUSED);
            drawCenteredText(g, "PAUSED", CENTER_X, PAUSE_TEXT_Y + Math.round(-130f * (1f - motion.pauseTextProgress)));
        }
    }

    private void drawWin(Graphics2D g, Image winHeaderImage, Image pillarImage, Image scoreHeaderImage,
            Image timeHeaderImage, int balance, long finalTimeMs, ScreenMotion motion) {
        g.setColor(new Color(120, 92, 38, 210));
        g.fillRect(0, 0, UIConstants.SCR_W, UIConstants.SCR_H);

        drawWinPillar(g, pillarImage, WIN_LEFT_OUTER_PILLAR_X, getPillarProgress(motion, 0));
        drawWinPillar(g, pillarImage, WIN_LEFT_INNER_PILLAR_X, getPillarProgress(motion, 1));
        drawWinPillar(g, pillarImage, WIN_RIGHT_INNER_PILLAR_X, getPillarProgress(motion, 2));
        drawWinPillar(g, pillarImage, WIN_RIGHT_OUTER_PILLAR_X, getPillarProgress(motion, 3));

        if (winHeaderImage != null) {
            drawAnimatedDropImage(g, winHeaderImage, CENTER_X, WIN_HEADER_CY, WIN_HEADER_W, WIN_HEADER_H,
                    motion.titleProgress, 160);
        } else {
            g.setColor(new Color(255, 215, 0));
            g.setFont(FONT_END_TITLE);
            drawCenteredText(g, "HEIST SUCCESSFUL", CENTER_X, END_TITLE_Y + Math.round(-160f * (1f - motion.titleProgress)));
        }

        drawWinScoreHeader(g, scoreHeaderImage, balance, motion.resultProgress, motion.valueProgress);
        drawWinTimeHeader(g, timeHeaderImage, finalTimeMs, motion.resultProgress, motion.valueProgress);
    }

    private void drawGameOver(Graphics2D g, Image gameOverHeaderImage, Image scoreHeaderImage, Image timeHeaderImage,
            int balance, long finalTimeMs, ScreenMotion motion) {
        g.setColor(new Color(60, 0, 0, 210));
        g.fillRect(0, 0, UIConstants.SCR_W, UIConstants.SCR_H);

        if (gameOverHeaderImage != null) {
            drawAnimatedDropImage(g, gameOverHeaderImage, CENTER_X, WIN_HEADER_CY, GAME_OVER_HEADER_W, GAME_OVER_HEADER_H,
                    motion.titleProgress, 160);
        } else {
            g.setColor(Color.RED);
            g.setFont(FONT_END_TITLE);
            drawCenteredText(g, "HEIST UNSUCCESSFUL", CENTER_X, END_TITLE_Y + Math.round(-160f * (1f - motion.titleProgress)));
        }

        drawWinScoreHeader(g, scoreHeaderImage, balance, motion.resultProgress, motion.valueProgress);
        drawWinTimeHeader(g, timeHeaderImage, finalTimeMs, motion.resultProgress, motion.valueProgress);
    }

    private void drawTutorial(Graphics2D g, Image tutorialBackground) {
        if (tutorialBackground != null) {
            g.drawImage(tutorialBackground, 0, 0, UIConstants.SCR_W, UIConstants.SCR_H, null);
        } else {
            g.setColor(new Color(10, 10, 30));
            g.fillRect(0, 0, UIConstants.SCR_W, UIConstants.SCR_H);
            g.setColor(Color.WHITE);
            g.setFont(FONT_TUTORIAL);
            drawCenteredText(g, "TUTORIAL", CENTER_X, 200);
        }
    }

    private void drawStartTransition(Graphics2D g, Image titleImage, float progress) {
        if (progress >= 1f) {
            return;
        }

        float revealProgress = easeOutBack(progress);
        float fadeProgress = easeOutCubic(progress);
        float cover = 1f - fadeProgress;
        float holeScale = revealProgress;
        int holeR = Math.round(START_WIPE_MAX_R * holeScale);
        Composite oldComp = g.getComposite();
        Shape oldClip = g.getClip();

        Area outside = new Area(new Rectangle(0, 0, UIConstants.SCR_W, UIConstants.SCR_H));
        outside.subtract(new Area(new Ellipse2D.Float(CENTER_X - holeR, UIConstants.SCR_H / 2f - holeR, holeR * 2f, holeR * 2f)));

        g.setColor(new Color(24, 11, 5, 245));
        g.fill(outside);

        int glowR = holeR + 34;
        g.setClip(new Ellipse2D.Float(CENTER_X - glowR, UIConstants.SCR_H / 2f - glowR, glowR * 2f, glowR * 2f));
        g.setColor(new Color(255, 173, 71, 70));
        g.fillOval(CENTER_X - glowR, UIConstants.SCR_H / 2 - glowR, glowR * 2, glowR * 2);
        g.setClip(oldClip);

        g.setStroke(new BasicStroke(24f));
        g.setColor(new Color(255, 168, 58, Math.min(255, Math.round(255f * cover))));
        g.drawOval(CENTER_X - holeR, UIConstants.SCR_H / 2 - holeR, holeR * 2, holeR * 2);

        double rotationDeg = 28.0 * cover;
        float titleScale = 0.68f + 0.32f * holeScale;
        AffineTransform old = g.getTransform();
        g.translate(CENTER_X, UIConstants.SCR_H / 2f);
        g.rotate(Math.toRadians(rotationDeg));
        g.scale(titleScale, titleScale);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, 1.2f * cover)));
        if (titleImage != null) {
            int[] dims = getFitDimensions(titleImage, MENU_TITLE_W, MENU_TITLE_H);
            g.drawImage(titleImage, -dims[0] / 2, -dims[1] / 2, dims[0], dims[1], null);
        } else {
            g.setColor(Color.WHITE);
            g.setFont(FONT_MENU_TITLE);
            drawCenteredText(g, "BANK HEIST", 0, 20);
        }
        g.setTransform(old);
        g.setComposite(oldComp);
    }

    private float easeOutCubic(float t) {
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }

    private void drawScoreHeader(Graphics2D g, Image scoreHeaderImage, int balance, float pulse) {
        int[] dims = getFitDimensions(scoreHeaderImage, HUD_HEADER_W, HUD_HEADER_H);
        float scale = 1f + 0.10f * pulse;
        int cx = HUD_SCORE_X + dims[0] / 2;
        int cy = HUD_HEADER_Y + dims[1] / 2;
        AffineTransform old = g.getTransform();
        g.translate(cx, cy);
        g.scale(scale, scale);
        if (scoreHeaderImage != null) {
            g.drawImage(scoreHeaderImage, -dims[0] / 2, -dims[1] / 2, dims[0], dims[1], null);
        }
        g.setColor(new Color(160, 215, 255));
        g.setFont(FONT_SCORE);
        FontMetrics fm = g.getFontMetrics();
        String text = String.valueOf(balance);
        int tx = -fm.stringWidth(text) / 2;
        int ty = (fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(text, tx, ty);
        g.setTransform(old);
    }

    private void drawTimeHeader(Graphics2D g, Image timeHeaderImage, String currentElapsedText, float pulse) {
        int[] dims = getFitDimensions(timeHeaderImage, HUD_HEADER_W, HUD_HEADER_H);
        float scale = 1f + 0.10f * pulse;
        int cx = HUD_TIME_X + dims[0] / 2;
        int cy = HUD_HEADER_Y + dims[1] / 2;
        AffineTransform old = g.getTransform();
        g.translate(cx, cy);
        g.scale(scale, scale);
        if (timeHeaderImage != null) {
            g.drawImage(timeHeaderImage, -dims[0] / 2, -dims[1] / 2, dims[0], dims[1], null);
        }
        g.setColor(Color.WHITE);
        g.setFont(FONT_TIME);
        FontMetrics fm = g.getFontMetrics();
        String text = currentElapsedText;
        int tx = -fm.stringWidth(text) / 2;
        int ty = (fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(text, tx, ty);
        g.setTransform(old);
    }

    private int[] drawFitImage(Graphics2D g, Image image, int x, int y, int maxWidth, int maxHeight) {
        int[] dims = getFitDimensions(image, maxWidth, maxHeight);

        if (image != null) {
            g.drawImage(image, x, y, dims[0], dims[1], null);
        }

        return dims;
    }

    private int[] getFitDimensions(Image image, int maxWidth, int maxHeight) {
        int drawWidth = maxWidth;
        int drawHeight = maxHeight;

        if (image != null) {
            int iw = image.getWidth(null);
            int ih = image.getHeight(null);
            if (iw > 0 && ih > 0) {
                float scale = Math.min((float) maxWidth / iw, (float) maxHeight / ih);
                drawWidth = (int) (iw * scale);
                drawHeight = (int) (ih * scale);
            }
        }

        return new int[]{drawWidth, drawHeight};
    }

    private void drawCenteredFitImage(Graphics2D g, Image image, int centerX, int centerY, int maxWidth, int maxHeight) {
        int[] dims = getFitDimensions(image, maxWidth, maxHeight);
        int x = centerX - dims[0] / 2;
        int y = centerY - dims[1] / 2;
        g.drawImage(image, x, y, dims[0], dims[1], null);
    }

    private void drawAnimatedDropImage(Graphics2D g, Image image, int centerX, int centerY, int maxWidth, int maxHeight,
            float progress, int startOffsetY) {
        if (progress <= 0f) {
            return;
        }
        drawCenteredFitImage(g, image, centerX, centerY + Math.round(-startOffsetY * (1f - progress)), maxWidth, maxHeight);
    }

    private void drawWinPillar(Graphics2D g, Image pillarImage, int centerX, float progress) {
        if (progress <= 0f) {
            return;
        }
        if (pillarImage != null) {
            Composite oldComp = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
            drawCenteredFitImage(g, pillarImage, centerX, WIN_PILLAR_Y + Math.round(320f * (1f - progress)), WIN_PILLAR_W, WIN_PILLAR_H);
            g.setComposite(oldComp);
        }
    }

    private void drawWinScoreHeader(Graphics2D g, Image scoreHeaderImage, int balance, float boxProgress, float valueProgress) {
        drawWinHeaderBox(g, scoreHeaderImage, WIN_SCORE_X, WIN_RESULT_Y, WIN_RESULT_W, WIN_RESULT_H,
                String.valueOf(Math.round(balance * valueProgress)), new Color(160, 215, 255), boxProgress);
    }

    private void drawWinTimeHeader(Graphics2D g, Image timeHeaderImage, long finalTimeMs, float boxProgress, float valueProgress) {
        drawWinHeaderBox(g, timeHeaderImage, WIN_TIME_X, WIN_RESULT_Y, WIN_RESULT_W, WIN_RESULT_H,
                formatTime(Math.round(finalTimeMs * valueProgress)), Color.WHITE, boxProgress);
    }

    private void drawWinHeaderBox(Graphics2D g, Image image, int centerX, int centerY, int maxWidth, int maxHeight,
            String text, Color textColor, float progress) {
        if (progress <= 0f) {
            return;
        }
        int[] dims = getFitDimensions(image, maxWidth, maxHeight);
        float scale = 0.72f + 0.28f * easeOutBack(progress);
        int drawWidth = Math.round(dims[0] * scale);
        int drawHeight = Math.round(dims[1] * scale);
        int yOffset = Math.round(-80f * (1f - progress));
        int x = centerX - drawWidth / 2;
        int y = centerY + yOffset - drawHeight / 2;

        if (image != null) {
            g.drawImage(image, x, y, drawWidth, drawHeight, null);
        }

        g.setColor(textColor);
        g.setFont(FONT_TIME);
        drawCenteredTextInBox(g, text, centerX, centerY + yOffset);
    }

    private float getPillarProgress(ScreenMotion motion, int index) {
        if (motion.pillarProgress == null || index >= motion.pillarProgress.length) {
            return 1f;
        }
        return motion.pillarProgress[index];
    }

    private float easeOutBack(float t) {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float inv = t - 1f;
        return 1f + c3 * inv * inv * inv + c1 * inv * inv;
    }

    private String formatTime(long millis) {
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void drawCenteredText(Graphics2D g, String text, int centerX, int baselineY) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, centerX - fm.stringWidth(text) / 2, baselineY);
    }

    private void drawCenteredTextInBox(Graphics2D g, String text, int centerX, int centerY) {
        FontMetrics fm = g.getFontMetrics();
        int x = centerX - fm.stringWidth(text) / 2;
        int y = centerY + (fm.getAscent() - fm.getDescent()) / 2;
        g.drawString(text, x, y);
    }
}
