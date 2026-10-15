package tests;

import code.CaveExplorer;
import code.CaveState;
import code.Transition;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CaveDomainTests {
    private CaveExplorer problem() {
        return new CaveExplorer("3,3;1,1;20,1;1,2,3;4,5,6;7,8,9;2,1;1,1;");
    }

    private Transition<CaveState> find(List<Transition<CaveState>> transitions,
            String action) {
        for (Transition<CaveState> transition : transitions) {
            if (transition.action.equals(action)) {
                return transition;
            }
        }
        return null;
    }

    @Test
    public void parsesVariableHeightAndMultipleObjects() {
        CaveExplorer cave = new CaveExplorer(
                "4,3;1,2;30,4;1,2,3;4,5,6;7,8,9;10,11,12;2,0,0,3;0,0,2,3;");
        assertEquals(4, cave.getHeight());
        assertEquals(3, cave.getWidth());
        assertEquals(11, cave.getDifficulty(1, 3));
        assertEquals(2, cave.getDoorCount());
        assertEquals(0, cave.getDoorX(1));
        assertEquals(3, cave.getDoorY(1));
        assertEquals(2, cave.getKeyCount());
        assertEquals(2, cave.getKeyX(1));
        assertEquals(3, cave.getKeyY(1));
        CaveState start = cave.initialState();
        assertEquals(1, start.getX());
        assertEquals(2, start.getY());
        assertEquals(30, start.getEnergy());
        assertEquals(4, start.getRope());
        assertEquals(3, start.getLives());
        assertFalse(start.hasKey());
        assertFalse(start.isDoorOpen(1));
        assertFalse(start.isKeyCollected(1));
    }

    @Test
    public void movementPaysDestinationAndRespectsResources() {
        CaveExplorer cave = problem();
        CaveState start = cave.initialState();
        List<Transition<CaveState>> transitions = cave.successors(start);
        Transition<CaveState> right = find(transitions, "right");
        assertNotNull(right); // A closed door can be entered.
        assertEquals(14, right.state.getEnergy());
        assertEquals(1, right.state.getRope());
        assertEquals(3, right.state.getLives());
        assertEquals(6, right.stepCost);
        Transition<CaveState> climb = find(transitions, "climbdown");
        assertNotNull(climb);
        assertEquals(12, climb.state.getEnergy());
        assertEquals(0, climb.state.getRope());
        assertEquals(3, climb.state.getLives());
        assertEquals(8, climb.stepCost);
        Transition<CaveState> jump = find(transitions, "jumpdown");
        assertNotNull(jump);
        assertEquals(12, jump.state.getEnergy());
        assertEquals(1, jump.state.getRope());
        assertEquals(2, jump.state.getLives());
        assertEquals(509, jump.stepCost);
        assertEquals(20, start.getEnergy());

        CaveState limited = new CaveState(1, 1, 5, 0, 1, false,
                new boolean[1], new boolean[1]);
        transitions = cave.successors(limited);
        assertNotNull(find(transitions, "left"));
        assertNull(find(transitions, "right"));
        assertNull(find(transitions, "climbup"));
        assertNull(find(transitions, "climbdown"));
        assertNull(find(transitions, "jumpdown"));
    }

    @Test
    public void exactEnergyIsEnoughAndWallsAndBoundariesAreBlocked() {
        CaveExplorer cave = new CaveExplorer(
                "3,3;0,0;2,1;1,2,0;0,1,0;1,1,1;1,0;1,1;");
        List<Transition<CaveState>> transitions = cave.successors(cave.initialState());
        assertEquals(1, transitions.size());
        assertEquals("right", transitions.get(0).action);
        assertEquals(0, transitions.get(0).state.getEnergy());
    }

    @Test
    public void collectAndUnlockAreFreeAndConsumeTheCorrectKey() {
        CaveExplorer cave = problem();
        CaveState emptyEnergy = new CaveState(1, 1, 0, 0, 1, false,
                new boolean[1], new boolean[1]);
        Transition<CaveState> collect = find(cave.successors(emptyEnergy), "collect");
        assertNotNull(collect);
        assertEquals(0, collect.stepCost);
        assertEquals(0, collect.state.getEnergy());
        assertEquals(1, collect.state.getLives());
        assertTrue(collect.state.hasKey());
        assertTrue(collect.state.isKeyCollected(0));
        assertFalse(emptyEnergy.isKeyCollected(0));
        assertNull(find(cave.successors(collect.state), "collect"));
        CaveState usedKey = new CaveState(1, 1, 0, 0, 1, false,
                new boolean[1], new boolean[]{true});
        assertNull(find(cave.successors(usedKey), "collect"));

        CaveState atDoor = new CaveState(2, 1, 0, 0, 1, true,
                new boolean[1], new boolean[]{true});
        Transition<CaveState> unlock = find(cave.successors(atDoor), "unlock");
        assertNotNull(unlock);
        assertEquals(0, unlock.stepCost);
        assertFalse(unlock.state.hasKey());
        assertTrue(unlock.state.isDoorOpen(0));
        assertFalse(atDoor.isDoorOpen(0));
        assertTrue(cave.isGoal(unlock.state));
        assertNull(find(cave.successors(unlock.state), "unlock"));
        assertFalse(cave.isGoal(new CaveState(2, 1, 0, 0, 0, false,
                new boolean[]{true}, new boolean[]{true})));
    }

    @Test
    public void dominanceRequiresMatchingPositionAndItemStatus() {
        CaveExplorer cave = problem();
        CaveState strong = cave.initialState();
        CaveState weak = new CaveState(1, 1, 10, 0, 2, false,
                new boolean[1], new boolean[1]);
        assertTrue(cave.dominates(strong, weak));
        assertTrue(cave.dominates(strong, strong));
        assertFalse(cave.dominates(weak, strong));
        assertFalse(cave.dominates(strong, new CaveState(0, 1, 10, 0, 2,
                false, new boolean[1], new boolean[1])));
        assertFalse(cave.dominates(strong, new CaveState(1, 1, 10, 0, 2,
                true, new boolean[1], new boolean[1])));
        assertFalse(cave.dominates(strong, new CaveState(1, 1, 10, 0, 2,
                false, new boolean[]{true}, new boolean[1])));
        assertFalse(cave.dominates(strong, new CaveState(1, 1, 10, 0, 2,
                false, new boolean[1], new boolean[]{true})));
        assertEquals(22, cave.maximumDepth());
    }

    @Test
    public void statesRemainStableAsMapKeys() {
        boolean[] doors = {false};
        boolean[] keys = {false};
        CaveState first = new CaveState(1, 1, 20, 1, 3, false, doors, keys);
        CaveState equal = new CaveState(1, 1, 20, 1, 3, false, doors, keys);
        Map<CaveState, String> map = new HashMap<>();
        map.put(first, "found");
        doors[0] = true;
        keys[0] = true;
        assertEquals(first, equal);
        assertEquals(first.hashCode(), equal.hashCode());
        assertEquals("found", map.get(equal));
        assertFalse(first.isDoorOpen(0));
        assertFalse(first.isKeyCollected(0));
    }
}
