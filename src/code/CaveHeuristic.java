package code;

/**
 * Admissible heuristic for A* on the cave problem.
 * Path cost = energy used + 501 * lives used, so h estimates both parts.
 */
public class CaveHeuristic {
    /*
     * HEURISTIC: h = energyBound + 501 * minimumJumps
     *
     * energyBound = the largest of three lower bounds on the energy still needed:
     *   1. Farthest door: every closed door must be reached, so we need at least
     *      the cheapest energy path to the farthest closed door.
     *   2. Door entries: every closed door cell must be entered at least once,
     *      so we pay at least the sum of their difficulties.
     *   3. Key first: if no key is carried, we must reach some key and then some
     *      door, so we pay at least the cheapest key -> door trip.
     *   Energy paths are computed with Dijkstra (moving costs the destination's
     *   difficulty, walls blocked) but ignore rope and lives, so they can only be
     *   cheaper than the real path. The max of lower bounds is still a lower bound.
     *
     * minimumJumps = lower bound on lives still to be lost:
     *   A door d rows below needs at least d downward moves. Rope covers at most
     *   "rope" of them (climbdown); the rest must be jumpdowns, each costing a life.
     *
     * Admissible: every real solution pays at least energyBound energy and at
     * least minimumJumps lives, so h <= real cost (energy + 501 * lives).
     * It accounts for both energy and lives, not just distance.
     *
     * Dead ends return Long.MAX_VALUE (no solution exists from this state):
     * an unreachable door, too few keys left, or too few lives for the jumps.
     */
    public static long estimate(CaveExplorer problem, CaveState state) {
        int closedDoors = 0;
        int minimumJumps = 0;
        long energyBound = 0;
        long doorEntryCost = 0;

        // Bounds 1 and 2, plus the jump bound, over all closed doors.
        for (int i = 0; i < problem.getDoorCount(); i++) {
            if (state.isDoorOpen(i)) {
                continue;
            }
            closedDoors++;
            long distance = problem.minimumEnergyToItem(i, state.getX(), state.getY());
            if (distance == Long.MAX_VALUE) {
                return Long.MAX_VALUE; // door cannot be reached
            }
            energyBound = Math.max(energyBound, distance);
            // Entering a door costs its difficulty (unless we already stand on it).
            int doorX = problem.getDoorX(i);
            int doorY = problem.getDoorY(i);
            if (doorX != state.getX() || doorY != state.getY()) {
                doorEntryCost += problem.getDifficulty(doorX, doorY);
            }
            // Downward moves the rope cannot cover must be jumps.
            int jumps = doorY - state.getY() - state.getRope();
            minimumJumps = Math.max(minimumJumps, jumps);
        }
        // All doors open: goal reached.
        if (closedDoors == 0 && state.getLives() > 0) {
            return 0;
        }
        // Each jump costs a life and we must keep at least one.
        if (state.getLives() <= minimumJumps) {
            return Long.MAX_VALUE;
        }

        // Bound 3: cheapest trip to an uncollected key and then to a closed door.
        int availableKeys = 0;
        long firstKeyTrip = Long.MAX_VALUE;
        for (int i = 0; i < problem.getKeyCount(); i++) {
            if (state.isKeyCollected(i)) {
                continue;
            }
            availableKeys++;
            if (!state.hasKey()) {
                // Keys are stored after the doors in the distance table.
                long toKey = problem.minimumEnergyToItem(
                        problem.getDoorCount() + i, state.getX(), state.getY());
                if (toKey == Long.MAX_VALUE) {
                    continue;
                }
                for (int j = 0; j < problem.getDoorCount(); j++) {
                    if (!state.isDoorOpen(j)) {
                        long toDoor = problem.minimumEnergyToItem(j,
                                problem.getKeyX(i), problem.getKeyY(i));
                        if (toDoor != Long.MAX_VALUE) {
                            firstKeyTrip = Math.min(firstKeyTrip, toKey + toDoor);
                        }
                    }
                }
            }
        }
        if (state.hasKey()) {
            availableKeys++; // the carried key also counts
        } else {
            if (firstKeyTrip == Long.MAX_VALUE) {
                return Long.MAX_VALUE; // no key can reach a door
            }
            energyBound = Math.max(energyBound, firstKeyTrip);
        }
        // Each door uses up one key.
        if (availableKeys < closedDoors) {
            return Long.MAX_VALUE;
        }
        energyBound = Math.max(energyBound, doorEntryCost);
        return energyBound + 501L * minimumJumps;
    }

    // Precomputes, for every door and key, the cheapest energy from each cell to it.
    // Index order: doors first, then keys. Done once per problem, so h is fast.
    static long[][][] calculateDistances(CaveExplorer problem) {
        int itemCount = problem.getDoorCount() + problem.getKeyCount();
        long[][][] distances = new long[itemCount][][];
        for (int i = 0; i < itemCount; i++) {
            int x;
            int y;
            if (i < problem.getDoorCount()) {
                x = problem.getDoorX(i);
                y = problem.getDoorY(i);
            } else {
                int keyIndex = i - problem.getDoorCount();
                x = problem.getKeyX(keyIndex);
                y = problem.getKeyY(keyIndex);
            }
            distances[i] = distancesTo(problem, x, y);
        }
        return distances;
    }

    // Dijkstra backwards from the target: distance[row][col] = cheapest energy
    // to walk from (col, row) to the target. Long.MAX_VALUE = unreachable.
    private static long[][] distancesTo(CaveExplorer problem, int targetX, int targetY) {
        int height = problem.getHeight();
        int width = problem.getWidth();
        long[][] distance = new long[height][width];
        boolean[][] visited = new boolean[height][width];
        for (int row = 0; row < height; row++) {
            for (int column = 0; column < width; column++) {
                distance[row][column] = Long.MAX_VALUE;
            }
        }
        distance[targetY][targetX] = 0;

        for (int i = 0; i < height * width; i++) {
            // Pick the unvisited cell with the smallest distance.
            int currentX = -1;
            int currentY = -1;
            long bestDistance = Long.MAX_VALUE;
            for (int row = 0; row < height; row++) {
                for (int column = 0; column < width; column++) {
                    if (!visited[row][column] && distance[row][column] < bestDistance) {
                        bestDistance = distance[row][column];
                        currentX = column;
                        currentY = row;
                    }
                }
            }
            if (currentX == -1) {
                break; // remaining cells are unreachable
            }
            visited[currentY][currentX] = true;
            // Relax the four neighbours.
            updateDistance(problem, distance, currentX, currentY, currentX - 1, currentY);
            updateDistance(problem, distance, currentX, currentY, currentX + 1, currentY);
            updateDistance(problem, distance, currentX, currentY, currentX, currentY - 1);
            updateDistance(problem, distance, currentX, currentY, currentX, currentY + 1);
        }
        return distance;
    }

    // Walking from next into current costs current's difficulty;
    // keep it if that gives next a cheaper distance. Walls and out-of-grid are skipped.
    private static void updateDistance(CaveExplorer problem, long[][] distance,
            int currentX, int currentY, int nextX, int nextY) {
        if (nextX < 0 || nextX >= problem.getWidth()
                || nextY < 0 || nextY >= problem.getHeight()
                || problem.getDifficulty(nextX, nextY) == 0) {
            return;
        }
        long newDistance = distance[currentY][currentX]
                + problem.getDifficulty(currentX, currentY);
        if (newDistance < distance[nextY][nextX]) {
            distance[nextY][nextX] = newDistance;
        }
    }
}
