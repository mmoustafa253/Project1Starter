package tests;

import code.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.ArrayList;

public class SearchFrameworkTests {

    // A simple mock state representing an integer node in a graph
    static class IntegerState {
        int id;
        IntegerState(int id) { this.id = id; }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof IntegerState)) return false;
            return id == ((IntegerState) o).id;
        }
        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }
    }

    // A mock search problem representing a small graph:
    // Start (0) -> A (1) with cost 2
    // A (1) -> Goal (2) with cost 10  (Total cost = 12, 2 actions)
    // Start (0) -> Goal (2) with cost 15 (Total cost = 15, 1 action)
    static class MockGraphProblem extends GenericSearchProblem<IntegerState> {
        @Override
        public IntegerState initialState() {
            return new IntegerState(0);
        }

        @Override
        public boolean isGoal(IntegerState state) {
            return state.id == 2;
        }

        @Override
        public List<Transition<IntegerState>> successors(IntegerState state) {
            List<Transition<IntegerState>> list = new ArrayList<>();
            if (state.id == 0) {
                list.add(new Transition<>(new IntegerState(1), "toA", 2L));
                list.add(new Transition<>(new IntegerState(2), "directGoal", 15L));
            } else if (state.id == 1) {
                list.add(new Transition<>(new IntegerState(2), "toGoal", 10L));
            }
            return list;
        }

        @Override
        public long heuristic(IntegerState state) {
            return 0;
        }

        @Override
        public boolean dominates(IntegerState first, IntegerState second) {
            return false;
        }

        @Override
        public int maximumDepth() {
            return 5;
        }
    }

    @Test
    public void testUniformCostSearch() {
        MockGraphProblem problem = new MockGraphProblem();
        SearchResult<IntegerState> result = GenericSearch.search(problem, "UC");

        assertNotNull(result.goal, "UC should find a solution");
        assertEquals(12L, result.goal.pathCost, "UC should find the path with the lowest cost (12)");
        assertEquals("toA,toGoal", result.goal.parent.action + "," + result.goal.action);
    }

    @Test
    public void testIterativeDeepeningSearch() {
        MockGraphProblem problem = new MockGraphProblem();
        SearchResult<IntegerState> result = GenericSearch.search(problem, "ID");

        assertNotNull(result.goal, "ID should find a solution");
        assertEquals("directGoal", result.goal.action, "ID should choose the path with fewer actions");
    }
}