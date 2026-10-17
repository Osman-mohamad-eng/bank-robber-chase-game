package group1;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// PlaytimeManager tests: timing, pauses/ resume, formatTime

public class PlaytimeManagerTest {

    private PlaytimeManager pm;

    @BeforeEach
    public void setUp() {
        pm = new PlaytimeManager();
    }

    // getElapsed()

    @Test
    public void testElapsedIsZeroBeforeStart() {
        assertEquals(0, pm.getElapsed(),
                "elapsed time should be 0 before start() is called");
    }

    @Test
    public void testElapsedIncreasesWhileRunning() throws InterruptedException {
        pm.start();
        Thread.sleep(50);
        long elapsed = pm.getElapsed();
        assertTrue(elapsed >= 30,
                "elapsed should be at least ~30ms after sleeping 50ms, got: " + elapsed);
    }

    @Test
    public void testElapsedFreezeAfterPause() throws InterruptedException {
        pm.start();
        Thread.sleep(50);
        pm.pause();
        long frozen = pm.getElapsed();
        Thread.sleep(50);
        assertEquals(frozen, pm.getElapsed(),
                "elapsed should not change after pause()");
    }

    @Test
    public void testElapsedAccumulatesAcrossSessions() throws InterruptedException {
        pm.start();
        Thread.sleep(50);
        pm.pause();
        long first = pm.getElapsed();

        pm.start();
        Thread.sleep(50);
        pm.pause();
        long second = pm.getElapsed();

        assertTrue(second > first,
                "accumulated time should grow across start/ pause cycless");
    }

    // start() / pause() edge cases

    @Test
    public void testPauseWhenNotRunningIsNoOp() {
        pm.pause(); // should not throw or change state
        assertEquals(0, pm.getElapsed());
        assertEquals(0, pm.getFinalTime());
    }

    @Test
    public void testDoublePauseDoesNotAccumulateExtra() throws InterruptedException {
        pm.start();
        Thread.sleep(50);
        pm.pause();
        long afterFirst = pm.getElapsed();
        pm.pause(); // second pause while already paused
        assertEquals(afterFirst, pm.getElapsed(),
                "pausing again while already paused should not change anything");
    }

    @Test
    public void testDoubleStartResetsStartTime() throws InterruptedException {
        pm.start();
        Thread.sleep(50);
        // Calling start() again resets the session start time without accumulating the previous time
        pm.start();
        long elapsed = pm.getElapsed();
        // The accumulated time from the first session is not paused,
        // but the new startTime is reset, so elapsed should be small
        assertTrue(elapsed < 100,
                "after a second start(), elapsed should reset the session clock");
    }

    // reset()

    @Test
    public void testResetClearsEverything() throws InterruptedException {
        pm.start();
        Thread.sleep(50);
        pm.pause();
        pm.reset();

        assertEquals(0, pm.getElapsed(),
                "elapsed should be 0 after reset()");
        assertEquals(0, pm.getFinalTime(),
                "final time should be 0 after reset()");
    }

    @Test
    public void testResetWhileRunning() throws InterruptedException {
        pm.start();
        Thread.sleep(50);
        pm.reset();
        // After reset, running is false so getElapsed returns accumulatedTime = 0
        assertEquals(0, pm.getElapsed());
    }

    @Test
    public void testStartAfterResetWorks() throws InterruptedException {
        pm.start();
        Thread.sleep(50);
        pm.pause();
        pm.reset();
        pm.start();
        Thread.sleep(50);
        long elapsed = pm.getElapsed();
        assertTrue(elapsed >= 30 && elapsed < 200,
                "timer should track fresh time after reset + start");
    }

    // getFinalTime()

    @Test
    public void testFinalTimeIsZeroBeforeAnyPause() {
        assertEquals(0, pm.getFinalTime(),
                "final time should be 0 before any pause");
    }

    @Test
    public void testFinalTimeSetOnPause() throws InterruptedException {
        pm.start();
        Thread.sleep(50);
        pm.pause();
        long ft = pm.getFinalTime();
        assertTrue(ft >= 30,
                "final time should reflect accumulated time  at pause");
    }

    @Test
    public void testFinalTimeUpdatesOnSubsequentPause() throws InterruptedException {
        pm.start();
        Thread.sleep(50);
        pm.pause();
        long first = pm.getFinalTime();

        pm.start();
        Thread.sleep(50);
        pm.pause();
        long second = pm.getFinalTime();

        assertTrue(second > first,
                "final time should grow with each pause");
    }

    @Test
    public void testFinalTimeDoesNotChangeWhileRunning() throws InterruptedException {
        pm.start();
        Thread.sleep(50);
        pm.pause();
        long ft = pm.getFinalTime();

        pm.start();
        Thread.sleep(50);
        // Don't pause — finalTime should still be the old value
        assertEquals(ft, pm.getFinalTime(),
                "getFinalTime() should not change until next pause()");
    }

    // formatTime()

    @Test
    public void testFormatTimeZero() {
        assertEquals("00:00", pm.formatTime(0));
    }

    @Test
    public void testFormatTimeOneSecond() {
        assertEquals("00:01", pm.formatTime(1000));
    }

    @Test
    public void testFormatTimeOneMinute() {
        assertEquals("01:00", pm.formatTime(60_000));
    }

    @Test
    public void testFormatTimeMixedMinutesAndSeconds() {
        // 3 minutes 45 seconds = 225_000 ms
        assertEquals("03:45", pm.formatTime(225_000));
    }

    @Test
    public void testFormatTimeIgnoresSubSecondMillis() {
        // 1500ms = 1.5s → should show 00:01
        assertEquals("00:01", pm.formatTime(1500));
    }

    @Test
    public void testFormatTimeWrapsAt60Minutes() {
        // 61 minutes = 3,660,000 ms → (61 % 60) = 1 min, 0 sec
        assertEquals("01:00", pm.formatTime(3_660_000));
    }

    @Test
    public void testFormatTimeLargeValue() {
        // 59 minutes 59 seconds = 3,599,000 ms
        assertEquals("59:59", pm.formatTime(3_599_000));
    }

    @Test
    public void testFormatTimeExactlyOneHour() {
        // 60 minutes = 3,600,000 ms → wraps to 00:00
        assertEquals("00:00", pm.formatTime(3_600_000));
    }

    @Test
    public void testFormatTimePadsSingleDigits() {
        // 5 seconds
        assertEquals("00:05", pm.formatTime(5_000));
        // 9 minutes 9 seconds
        assertEquals("09:09", pm.formatTime(549_000));
    }
}
