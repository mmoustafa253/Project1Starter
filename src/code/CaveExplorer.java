package code;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CaveExplorer extends GenericSearchProblem<CaveState> {
    private int height;
    private int width;
    private int[][] grid;
    private int startX;
    private int startY;
    private int initialEnergy;
    private int initialRope;
    private int[][] doors;
    private int[][] keys;

    public CaveExplorer(String initString) {
        String[] sections = initString.split(";");

        String[] dimensions = sections[0].split(",");
        this.height = Integer.parseInt(dimensions[0]);
        this.width = Integer.parseInt(dimensions[1]);

        String[] startingPosition = sections[1].split(",");
        this.startX = Integer.parseInt(startingPosition[0]);
        this.startY = Integer.parseInt(startingPosition[1]);

        String[] startingResources = sections[2].split(",");
        this.initialEnergy = Integer.parseInt(startingResources[0]);
        this.initialRope = Integer.parseInt(startingResources[1]);

        this.grid = new int[this.height][this.width];
        for (int row = 0; row < this.height; row++) {
            String[] cells = sections[row + 3].split(",");
            for (int column = 0; column < this.width; column++) {
                this.grid[row][column] = Integer.parseInt(cells[column]);
            }
        }

        String[] doorCoordinates = sections[this.height + 3].split(",");
        int doorCount = doorCoordinates.length / 2;
        this.doors = new int[doorCount][2];
        for (int i = 0; i < doorCount; i++) {
            this.doors[i][0] = Integer.parseInt(doorCoordinates[2 * i]);
            this.doors[i][1] = Integer.parseInt(doorCoordinates[2 * i + 1]);
        }

        String[] keyCoordinates = sections[this.height + 4].split(",");
        int keyCount = keyCoordinates.length / 2;
        this.keys = new int[keyCount][2];
        for (int i = 0; i < keyCount; i++) {
            this.keys[i][0] = Integer.parseInt(keyCoordinates[2 * i]);
            this.keys[i][1] = Integer.parseInt(keyCoordinates[2 * i + 1]);
        }
    }
    @Override
    public CaveState initialState() {
        return new CaveState(startX, startY, initialEnergy, initialRope, 3,
                false, new boolean[doors.length], new boolean[keys.length]);
    }

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

    @Override
    public List<Transition<CaveState>> successors(CaveState state) {
        List<Transition<CaveState>> transitions = new ArrayList<>();
        if (state.getLives() < 1) {
            return transitions;
        }

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

    private void addMovement(List<Transition<CaveState>> transitions,
            CaveState state, int nextX, int nextY, String action,
            int ropeUsed, int livesUsed) {
        if (nextX < 0 || nextX >= width || nextY < 0 || nextY >= height) {
            return;
        }
        int energyUsed = grid[nextY][nextX];
        if (energyUsed == 0 || state.getEnergy() < energyUsed
                || state.getRope() < ropeUsed || state.getLives() <= livesUsed) {
            return;
        }

        // All movement pays the destination difficulty, including jumpdown.
        CaveState next = new CaveState(nextX, nextY,
                state.getEnergy() - energyUsed, state.getRope() - ropeUsed,
                state.getLives() - livesUsed, state.hasKey(),
                copyDoorsOpen(state), copyKeysCollected(state));
        long stepCost = energyUsed + 501L * livesUsed;
        transitions.add(new Transition<>(next, action, stepCost));
    }

    private boolean[] copyDoorsOpen(CaveState state) {
        boolean[] doorsOpen = new boolean[doors.length];
        for (int i = 0; i < doors.length; i++) {
            doorsOpen[i] = state.isDoorOpen(i);
        }
        return doorsOpen;
    }

    private boolean[] copyKeysCollected(CaveState state) {
        boolean[] keysCollected = new boolean[keys.length];
        for (int i = 0; i < keys.length; i++) {
            keysCollected[i] = state.isKeyCollected(i);
        }
        return keysCollected;
    }

    // Temporary lower bound until Person 3 supplies CaveHeuristic.estimate.
    // Each unopened door away from our position must be entered at least once.
    // Sum only those destination costs, ignoring other movement and lost lives.
    // This cannot exceed the remaining energy + 501 * lives cost of a solution.
    @Override
    public long heuristic(CaveState state) {
        long estimate = 0;
        for (int i = 0; i < doors.length; i++) {
            if (!state.isDoorOpen(i)
                    && (doors[i][0] != state.getX() || doors[i][1] != state.getY())) {
                estimate += grid[doors[i][1]][doors[i][0]];
            }
        }
        return estimate;
    }

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

    @Override
    public int maximumDepth() {
        // Every move costs at least one energy; each item action happens once.
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

    public static String solve(String initString, String strategy) {
        CaveExplorer problem = new CaveExplorer(initString);
        SearchResult<CaveState> result = GenericSearch.search(problem, strategy);
        if (result.goal == null) {
            return "No Solution";
        }

        List<String> actions = new ArrayList<>();
        SearchNode<CaveState> node = result.goal;
        while (node.parent != null) {
            actions.add(node.action);
            node = node.parent;
        }
        Collections.reverse(actions);

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
