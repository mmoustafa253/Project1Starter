
package tests;

import code.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class SearchFrameworkTests {

    // ---------------------------------------------------------
    // Mock state
    // ---------------------------------------------------------

    static class IntegerState {
        int id;

        IntegerState(int id) {
            this.id = id;
        }

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

    // ---------------------------------------------------------
    // Mock graph problem
    //
    //          (1) A ----10----> (2) Goal
    //         /                   ^
    //      2 /                    | 15
    //       /                     |
    //    (0) Start ---------------+
    //
    // Expected:
    // UCS: cheapest path, cost 12
    // IDS: shallowest path, directGoal, cost 15
    // A*: cheapest path, cost 12
    // ---------------------------------------------------------

    static class MockGraphProblem
            extends GenericSearchProblem<IntegerState> {

        @Override
        public IntegerState initialState() {
            return new IntegerState(0);
        }

        @Override
        public boolean isGoal(IntegerState state) {
            return state.id == 2;
        }

        @Override
        public List<Transition<IntegerState>> successors(
                IntegerState state) {

            List<Transition<IntegerState>> list =
                    new ArrayList<>();

            if (state.id == 0) {
                list.add(new Transition<>(
                    new IntegerState(1), "toA", 2L
                ));

                list.add(new Transition<>(
                    new IntegerState(2), "directGoal", 15L
                ));
            } else if (state.id == 1) {
                list.add(new Transition<>(
                    new IntegerState(2), "toGoal", 10L
                ));
            }

            return list;
        }

        @Override
        public long heuristic(IntegerState state) {
            return 0L;
        }

        @Override
        public boolean dominates(
                IntegerState first, IntegerState second) {
            return false;
        }

        @Override
        public int maximumDepth() {
            return 5;
        }
    }

    // ---------------------------------------------------------
    // Test 1: Uniform Cost Search
    // ---------------------------------------------------------

    @Test
    public void testUniformCostSearch() {

        MockGraphProblem problem = new MockGraphProblem();

        SearchResult<IntegerState> result =
                GenericSearch.search(problem, "UC");

        assertNotNull(result.goal,
                "UCS should find the goal");

        assertEquals(12L, result.goal.pathCost,
                "UCS should find the cheapest path");

        assertEquals(2, result.goal.depth,
                "The cheapest path should contain two actions");

        assertEquals("toA,toGoal",
                result.goal.parent.action + ","
                        + result.goal.action);

        assertEquals(2L, result.expandedNodes,
                "UCS should expand the start and A");
    }

    // ---------------------------------------------------------
    // Test 2: Iterative Deepening Search
    // ---------------------------------------------------------

    @Test
    public void testIterativeDeepeningSearch() {

        MockGraphProblem problem = new MockGraphProblem();

        SearchResult<IntegerState> result =
                GenericSearch.search(problem, "ID");

        assertNotNull(result.goal,
                "IDS should find the goal");

        assertEquals(15L, result.goal.pathCost,
                "IDS should return the shallowest solution");

        assertEquals(1, result.goal.depth,
                "IDS should find the direct one-action path");

        assertEquals("directGoal", result.goal.action,
                "IDS should select the direct goal");

        assertEquals(1L, result.expandedNodes,
                "IDS should count expansions across depth limits");
    }

    // ---------------------------------------------------------
    // Test 3: A* Search
    // ---------------------------------------------------------

    @Test
    public void testAStarSearch() {

        MockGraphProblem problem = new MockGraphProblem();

        SearchResult<IntegerState> result =
                GenericSearch.search(problem, "AS");

        assertNotNull(result.goal,
                "A* should find the goal");

        assertEquals(12L, result.goal.pathCost,
                "A* should find the cheapest path");

        assertEquals(2, result.goal.depth,
                "The cheapest path should contain two actions");

        assertEquals("toA,toGoal",
                result.goal.parent.action + ","
                        + result.goal.action);

        assertEquals(2L, result.expandedNodes,
                "A* should expand the start and A");
    }

    // ---------------------------------------------------------
    // Test 4: Invalid strategy
    // ---------------------------------------------------------

    @Test
    public void testInvalidStrategy() {

        MockGraphProblem problem = new MockGraphProblem();

        assertThrows(
            IllegalArgumentException.class,
            () -> GenericSearch.search(problem, "INVALID"),
            "An invalid strategy should throw an exception"
        );
    }

    // ---------------------------------------------------------
    // Test 5: IDS handles cycles
    // ---------------------------------------------------------

    static class CyclicGraphProblem
            extends GenericSearchProblem<IntegerState> {

        @Override
        public IntegerState initialState() {
            return new IntegerState(0);
        }

        @Override
        public boolean isGoal(IntegerState state) {
            return state.id == 2;
        }

        @Override
        public List<Transition<IntegerState>> successors(
                IntegerState state) {

            List<Transition<IntegerState>> list =
                    new ArrayList<>();

            if (state.id == 0) {
                list.add(new Transition<>(
                    new IntegerState(1), "toA", 1L
                ));
            } else if (state.id == 1) {
                list.add(new Transition<>(
                    new IntegerState(0), "backToStart", 1L
                ));

                list.add(new Transition<>(
                    new IntegerState(2), "toGoal", 1L
                ));
            }

            return list;
        }

        @Override
        public long heuristic(IntegerState state) {
            return 0L;
        }

        @Override
        public boolean dominates(
                IntegerState first, IntegerState second) {
            return false;
        }

        @Override
        public int maximumDepth() {
            return 5;
        }
    }

    @Test
    public void testIterativeDeepeningHandlesCycles() {

        CyclicGraphProblem problem = new CyclicGraphProblem();

        SearchResult<IntegerState> result =
                GenericSearch.search(problem, "ID");

        assertNotNull(result.goal,
                "IDS should find the goal despite the cycle");

        assertEquals(2, result.goal.state.id,
                "The solution should reach the goal state");

        assertEquals(2, result.goal.depth,
                "The solution should take two actions");

        assertEquals(2L, result.goal.pathCost,
                "The solution should cost two");
    }
}
