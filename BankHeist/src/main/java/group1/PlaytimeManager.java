package group1;

/**
 * Tracks gameplay time across pause/ resume.
 *
 * Call start() when gameplay begins (or resumes), pause() when gameplay is
 * paused, and reset() when starting a fresh game. Use getElapsed() for
 * the live running total and getFinalTime() for the final end game value.
 * 
 * @author Keith
 * @version 1.0
 */
public class PlaytimeManager {

    private long startTime;
    private long accumulatedTime;
    private long finalTime;
    private boolean running;

    /**
     * Marks the beginning of a play session.
     * Records the current system time as the sessions start.
     */
    public void start() {
        startTime = System.currentTimeMillis();
        running = true;
    }

    /**
     * Ends the current play session and accumulates elapsed time.
     * Also freezes finalTime to the accumulated total (used for end screens).
     */
    public void pause() {
        if (running) {
            accumulatedTime += System.currentTimeMillis() - startTime;
            finalTime = accumulatedTime;
            running = false;
        }
    }

    /**
     * Resets all time tracking to zero.
     * Call this before starting a brand-new game.
     */
    public void reset() {
        accumulatedTime = 0;
        finalTime = 0;
        running = false;
    }

    /**
     * Returns the total elapsed gameplay time in milliseconds.
     * Includes the current session's contribution if the timer is running.
     *
     * @return elapsed time in milliseconds
     */
    public long getElapsed() {
        if (running) {
            return accumulatedTime + (System.currentTimeMillis() - startTime);
        }
        return accumulatedTime;
    }

    /**
     * Returns the frozen final time set at the last pause() call.
     * Use this on Win/ Game Over screens.
     *
     * @return final time in milliseconds
     */
    public long getFinalTime() {
        return finalTime;
    }

    /**
     * Formats an elapsed time in milliseconds as a MM:SS string.
     *
     * @param millis elapsed time in milliseconds
     * @return formatted time string in MM:SS format
     */
    public String formatTime(long millis) {
        long seconds = (millis / 1000) % 60;
        long minutes  = (millis / (1000 * 60)) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
