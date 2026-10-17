package group1;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Represents the security guard enemy in Bank Heist.
 *
 * The guard automatically chases the thief using a pathfinding algorithm. A
 * Breadth First Search is used to find the shortest path to get to the thief's
 * position on the board.
 *
 * if the guard catches the thief, the game ends.
 *
 * @author Dev
 * @version 1.0
 */
public class Guard extends MovingEntity {

    public Guard(int id, Position startPos) {
        super(id, startPos);
    }

    /**
     * finds the next position the guard should move to in order to catch the
     * thief.
     *
     * Uses Breadth First Search to explore the board and find the shortest path
     * to the thief.
     *
     * @param thiefPos the current position of the thief
     * @param board the game board used to check walls and valid tiles
     * @return the next step toward the thief
     */
    /**
     * Returns the next step toward the thief.
     */
    public Position chooseStepToward(Position thiefPos, Board board) {
        Map<Position, Position> parentMap = bfs(thiefPos, board);

        // No path found
        if (!parentMap.containsKey(thiefPos)) {
            return getPosition();
        }

        return getNextStep(thiefPos, parentMap);
    }

    /**
     * Performs BFS and builds parent relationships.
     */
    private Map<Position, Position> bfs(Position target, Board board) {
        Queue<Position> queue = new LinkedList<>();
        Map<Position, Position> parent = new HashMap<>();
        Set<Position> visited = new HashSet<>();

        Position start = getPosition();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Position current = queue.poll();

            // Early exit
            if (current.equals(target)) {
                return parent;
            }

            for (Position neighbor : getNeighbors(current)) {
                if (!visited.contains(neighbor)
                        && board.isInside(neighbor)
                        && !board.isWall(neighbor)
                        && (!board.isOccupiedByGuard(neighbor) || neighbor.equals(target))) {
                    visited.add(neighbor);
                    parent.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        return parent;
    }

    /**
     * Returns valid neighboring positions.
     */
    private List<Position> getNeighbors(Position current) {
        return List.of(
                current.add(1, 0),
                current.add(-1, 0),
                current.add(0, 1),
                current.add(0, -1)
        );
    }

    /**
     * Backtracks to find the immediate next step.
     */
    private Position getNextStep(Position target, Map<Position, Position> parent) {
        Position currentStep = target;

        while (parent.get(currentStep) != null
                && !parent.get(currentStep).equals(getPosition())) {
            currentStep = parent.get(currentStep);
        }

        return currentStep;
    }

    /**
     * Updating the guard's position during a game.
     *
     * The guard calculates the next step toward the thief and moves one tile
     * closer if a valid path exists.
     *
     * @param thiefPos the thief's current position
     * @param board the board used to validate movement
     */
    public void update(Position thiefPos, Board board) {
        Position next = chooseStepToward(thiefPos, board);

        if (!next.equals(getPosition())) {
            moveTo(next);
        }
    }

    private int stuckCount = 0;

    public void freeze(int ticks) {
        this.stuckCount = ticks;
    }

    public int getStuckCount() {
        return stuckCount;
    }

    public boolean isFrozen() {
        return stuckCount > 0;
    }

    public void tickFreeze() {
        if (stuckCount > 0) {
            stuckCount--;
        }
    }
}
