package code;

public class CaveHeuristic {
    // Energy bound: take the largest of the energy needed to reach each closed
    // door, the sum of required door-entry costs, and the cheapest key-to-door
    // trip when no key is carried. Distances respect walls and cell difficulties
    // but ignore rope and lives, so each bound can only underestimate energy.
    // Life bound: reaching a door d rows below us requires at least d downward
    // moves. Even if all remaining rope is spent going down, at least d - rope
    // jumps are needed. Take the largest bound over all closed doors.
    // Every solution pays both bounds, so energy + 501 * jumps is admissible.
    // Unreachable doors, too few keys, or too few lives imply no solution.
    public static long estimate(CaveExplorer problem, CaveState state) {
        int closedDoors = 0;
        int minimumJumps = 0;
        long energyBound = 0;
        long doorEntryCost = 0;

        for (int i = 0; i < problem.getDoorCount(); i++) {
            if (state.isDoorOpen(i)) {
                continue;
            }
            closedDoors++;
            long distance = problem.minimumEnergyToItem(i, state.getX(), state.getY());
            if (distance == Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            energyBound = Math.max(energyBound, distance);
            int doorX = problem.getDoorX(i);
            int doorY = problem.getDoorY(i);
            if (doorX != state.getX() || doorY != state.getY()) {
                doorEntryCost += problem.getDifficulty(doorX, doorY);
            }
            int jumps = doorY - state.getY() - state.getRope();
            minimumJumps = Math.max(minimumJumps, jumps);
        }
        if (closedDoors == 0 && state.getLives() > 0) {
            return 0;
        }
        if (state.getLives() <= minimumJumps) {
            return Long.MAX_VALUE;
        }

        int availableKeys = 0;
        long firstKeyTrip = Long.MAX_VALUE;
        for (int i = 0; i < problem.getKeyCount(); i++) {
            if (state.isKeyCollected(i)) {
                continue;
            }
            availableKeys++;
            if (!state.hasKey()) {
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
            availableKeys++;
        } else {
            if (firstKeyTrip == Long.MAX_VALUE) {
                return Long.MAX_VALUE;
            }
            energyBound = Math.max(energyBound, firstKeyTrip);
        }
        if (availableKeys < closedDoors) {
            return Long.MAX_VALUE;
        }
        energyBound = Math.max(energyBound, doorEntryCost);
        return energyBound + 501L * minimumJumps;
    }

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
                break;
            }
            visited[currentY][currentX] = true;
            updateDistance(problem, distance, currentX, currentY, currentX - 1, currentY);
            updateDistance(problem, distance, currentX, currentY, currentX + 1, currentY);
            updateDistance(problem, distance, currentX, currentY, currentX, currentY - 1);
            updateDistance(problem, distance, currentX, currentY, currentX, currentY + 1);
        }
        return distance;
    }

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
