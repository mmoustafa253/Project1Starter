package code;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The cave explorer search problem.
 * Goal: open every door while keeping at least one life.
 * Step cost = energy used + 501 * lives used, so searches minimize lives first
 * (one life always outweighs the max energy of 500), then energy.
 */
public class CaveExplorer extends GenericSearchProblem<CaveState> {
    private int height;
    private int width;
    private int[][] grid;               // grid[row][column] = difficulty, 0 = wall
    private int startX;
    private int startY;
    private int initialEnergy;
    private int initialRope;
    private int[][] doors;              // doors[i] = {x, y}
    private int[][] keys;               // keys[i] = {x, y}
    private long[][][] energyToItems;   // precomputed energy distances for the heuristic

    // Parses "H,W;X,Y;EN,RL;row0;row1;...;doors;keys;" into the fields above.
    public CaveExplorer(String initString) {
        String[] sections = initString.split(";");

        // Grid size.
        String[] dimensions = sections[0].split(",");
        this.height = Integer.parseInt(dimensions[0]);
        this.width = Integer.parseInt(dimensions[1]);

        // Start position.
        String[] startingPosition = sections[1].split(",");
        this.startX = Integer.parseInt(startingPosition[0]);
        this.startY = Integer.parseInt(startingPosition[1]);

        // Starting energy and rope.
        String[] startingResources = sections[2].split(",");
        this.initialEnergy = Integer.parseInt(startingResources[0]);
        this.initialRope = Integer.parseInt(startingResources[1]);

        // Cell difficulties, one row per section.
        this.grid = new int[this.height][this.width];
        for (int row = 0; row < this.height; row++) {
            String[] cells = sections[row + 3].split(",");
            for (int column = 0; column < this.width; column++) {
                this.grid[row][column] = Integer.parseInt(cells[column]);
            }
        }

        // Door coordinates come in x,y pairs.
        String[] doorCoordinates = sections[this.height + 3].split(",");
        int doorCount = doorCoordinates.length / 2;
        this.doors = new int[doorCount][2];
        for (int i = 0; i < doorCount; i++) {
            this.doors[i][0] = Integer.parseInt(doorCoordinates[2 * i]);
            this.doors[i][1] = Integer.parseInt(doorCoordinates[2 * i + 1]);
        }

        // Key coordinates come in x,y pairs.
        String[] keyCoordinates = sections[this.height + 4].split(",");
        int keyCount = keyCoordinates.length / 2;
        this.keys = new int[keyCount][2];
        for (int i = 0; i < keyCount; i++) {
            this.keys[i][0] = Integer.parseInt(keyCoordinates[2 * i]);
            this.keys[i][1] = Integer.parseInt(keyCoordinates[2 * i + 1]);
        }
        // Compute once so the heuristic is fast during search.
        this.energyToItems = CaveHeuristic.calculateDistances(this);
    }

    // Start: given position, energy and rope, 3 lives, no key, all doors closed.
    @Override
    public CaveState initialState() {
        return new CaveState(startX, startY, initialEnergy, initialRope, 3,
                false, new boolean[doors.length], new boolean[keys.length]);
    }

    // Goal: at least one life left and every door open.
    @Override
    public boolean isGoal(CaveState state) {
        if (state.getLives() < 1) {
            return false;
        }
        for (int i = 0; i < doors.length; i++) {
            if (!state.isDoorOpen(i)) {
                return false;
            }
        }
        return true;
    }

    // All legal actions from this state.
    @Override
    public List<Transition<CaveState>> successors(CaveState state) {
        List<Transition<CaveState>> transitions = new ArrayList<>();
        if (state.getLives() < 1) {
            return transitions;
        }

        // Moves: (target x, target y, action, rope used, lives used).
        addMovement(transitions, state, state.getX() - 1, state.getY(),
                "left", 0, 0);
        addMovement(transitions, state, state.getX() + 1, state.getY(),
                "right", 0, 0);
        addMovement(transitions, state, state.getX(), state.getY() - 1,
                "climbup", 1, 0);
        addMovement(transitions, state, state.getX(), state.getY() + 1,
                "climbdown", 1, 0);
        addMovement(transitions, state, state.getX(), state.getY() + 1,
                "jumpdown", 0, 1);

        if (!state.hasKey()) {
            // collect: hands empty and an uncollected key is here. Costs nothing.
            for (int i = 0; i < keys.length; i++) {
                if (!state.isKeyCollected(i)
                        && keys[i][0] == state.getX()
                        && keys[i][1] == state.getY()) {
                    boolean[] keysCollected = copyKeysCollected(state);
                    keysCollected[i] = true;
                    CaveState next = new CaveState(state.getX(), state.getY(),
                            state.getEnergy(), state.getRope(), state.getLives(),
                            true, copyDoorsOpen(state), keysCollected);
                    transitions.add(new Transition<>(next, "collect", 0L));
                }
            }
        } else {
            // unlock: carrying a key and a closed door is here. Uses up the key.
            for (int i = 0; i < doors.length; i++) {
                if (!state.isDoorOpen(i)
                        && doors[i][0] == state.getX()
                        && doors[i][1] == state.getY()) {
                    boolean[] doorsOpen = copyDoorsOpen(state);
                    doorsOpen[i] = true;
                    CaveState next = new CaveState(state.getX(), state.getY(),
                            state.getEnergy(), state.getRope(), state.getLives(),
                            false, doorsOpen, copyKeysCollected(state));
                    transitions.add(new Transition<>(next, "unlock", 0L));
                }
            }
        }
        return transitions;
    }

    // Adds a move if legal: inside the grid, not a wall, enough energy for the
    // destination, enough rope, and (for a jump) at least one life left afterwards.
    private void addMovement(List<Transition<CaveState>> transitions,
            CaveState state, int nextX, int nextY, String action,
            int ropeUsed, int livesUsed) {
        if (nextX < 0 || nextX >= width || nextY < 0 || nextY >= height) {
            return;
        }
        int energyUsed = grid[nextY][nextX]; // moving costs the destination's difficulty
        if (energyUsed == 0 || state.getEnergy() < energyUsed
                || state.getRope() < ropeUsed || state.getLives() <= livesUsed) {
            return;
        }

        CaveState next = new CaveState(nextX, nextY,
                state.getEnergy() - energyUsed, state.getRope() - ropeUsed,
                state.getLives() - livesUsed, state.hasKey(),
                copyDoorsOpen(state), copyKeysCollected(state));
        long stepCost = energyUsed + 501L * livesUsed;
        transitions.add(new Transition<>(next, action, stepCost));
    }

    // Copy of the door flags, so a new state can change them safely.
    private boolean[] copyDoorsOpen(CaveState state) {
        boolean[] doorsOpen = new boolean[doors.length];
        for (int i = 0; i < doors.length; i++) {
            doorsOpen[i] = state.isDoorOpen(i);
        }
        return doorsOpen;
    }

    // Copy of the key flags, so a new state can change them safely.
    private boolean[] copyKeysCollected(CaveState state) {
        boolean[] keysCollected = new boolean[keys.length];
        for (int i = 0; i < keys.length; i++) {
            keysCollected[i] = state.isKeyCollected(i);
        }
        return keysCollected;
    }

    // A* heuristic (see CaveHeuristic for the explanation).
    @Override
    public long heuristic(CaveState state) {
        return CaveHeuristic.estimate(this, state);
    }

    // Cheapest energy from (x, y) to item index (doors first, then keys).
    long minimumEnergyToItem(int index, int x, int y) {
        return energyToItems[index][y][x];
    }

    // first dominates second: same position, key, doors and keys,
    // and at least as much energy, rope and lives.
    @Override
    public boolean dominates(CaveState first, CaveState second) {
        if (first.getX() != second.getX() || first.getY() != second.getY()
                || first.hasKey() != second.hasKey()) {
            return false;
        }
        for (int i = 0; i < doors.length; i++) {
            if (first.isDoorOpen(i) != second.isDoorOpen(i)) {
                return false;
            }
        }
        for (int i = 0; i < keys.length; i++) {
            if (first.isKeyCollected(i) != second.isKeyCollected(i)) {
                return false;
            }
        }
        return first.getEnergy() >= second.getEnergy()
                && first.getRope() >= second.getRope()
                && first.getLives() >= second.getLives();
    }

    // Depth limit for iterative deepening: moves cost at least 1 energy,
    // plus one collect per key and one unlock per door.
    @Override
    public int maximumDepth() {
        return initialEnergy + keys.length + doors.length;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

    public int getDifficulty(int x, int y) {
        return grid[y][x];
    }

    public int getDoorCount() {
        return doors.length;
    }

    public int getDoorX(int index) {
        return doors[index][0];
    }

    public int getDoorY(int index) {
        return doors[index][1];
    }

    public int getKeyCount() {
        return keys.length;
    }

    public int getKeyX(int index) {
        return keys[index][0];
    }

    public int getKeyY(int index) {
        return keys[index][1];
    }

    // Runs the chosen search and returns "plan;livesUsed;energyUsed;expandedNodes".
    public static String solve(String initString, String strategy) {
        CaveExplorer problem = new CaveExplorer(initString);
        SearchResult<CaveState> result = GenericSearch.search(problem, strategy);
        if (result.goal == null) {
            return "No Solution";
        }

        // Follow parent links from the goal back to the root, then reverse.
        List<String> actions = new ArrayList<>();
        SearchNode<CaveState> node = result.goal;
        while (node.parent != null) {
            actions.add(node.action);
            node = node.parent;
        }
        Collections.reverse(actions);

        // Join actions with commas.
        StringBuilder plan = new StringBuilder();
        for (int i = 0; i < actions.size(); i++) {
            if (i > 0) {
                plan.append(',');
            }
            plan.append(actions.get(i));
        }
        int livesUsed = 3 - result.goal.state.getLives();
        int energyUsed = problem.initialEnergy - result.goal.state.getEnergy();
        return plan + ";" + livesUsed + ";" + energyUsed + ";" + result.expandedNodes;
    }
}
