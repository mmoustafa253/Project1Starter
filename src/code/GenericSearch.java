package code;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Generic search procedure: takes any problem and a strategy name
 * and returns the goal node found (or null) plus the expanded-node count.
 */
public class GenericSearch {

    // Picks the search algorithm: UC = uniform cost, ID = iterative deepening, AS = A*.
    public static <S> SearchResult<S> search(GenericSearchProblem<S> problem,
            String strategy) {
        if ("UC".equals(strategy)) {
            return uniformCostSearch(problem);
        } else if ("ID".equals(strategy)) {
            return iterativeDeepening(problem);
        } else if ("AS".equals(strategy)) {
            return AStarSearch.search(problem);
        }
        throw new IllegalArgumentException("Invalid strategy: " + strategy);
    }

    // Uniform cost search: always expands the node with the lowest path cost g.
    private static <S> SearchResult<S> uniformCostSearch(GenericSearchProblem<S> problem) {
        // Frontier ordered by path cost, cheapest first.
        Comparator<SearchNode<S>> compareCost = new Comparator<SearchNode<S>>() {
            @Override
            public int compare(SearchNode<S> first, SearchNode<S> second) {
                return Long.compare(first.pathCost, second.pathCost);
            }
        };
        PriorityQueue<SearchNode<S>> frontier = new PriorityQueue<>(compareCost);
        Map<S, Long> bestCost = new HashMap<>(); // cheapest cost found for each state
        S initial = problem.initialState();
        frontier.add(new SearchNode<>(initial, null, null, 0L));
        bestCost.put(initial, 0L);
        long expandedNodes = 0;

        while (!frontier.isEmpty()) {
            SearchNode<S> current = frontier.poll();
            // Outdated entry: a cheaper path to this state exists, skip it.
            if (current.pathCost > bestCost.get(current.state)) {
                continue;
            }
            // Goal test on removal, so the first goal found is the cheapest.
            if (problem.isGoal(current.state)) {
                return new SearchResult<>(current, expandedNodes);
            }

            expandedNodes++;
            for (Transition<S> transition : problem.successors(current.state)) {
                long newCost = current.pathCost + transition.stepCost;
                Long oldCost = bestCost.get(transition.state);
                // Only add the child if the state is new or reached more cheaply.
                if (oldCost == null || newCost < oldCost) {
                    bestCost.put(transition.state, newCost);
                    frontier.add(new SearchNode<>(transition.state, current,
                            transition.action, newCost));
                }
            }
        }
        return new SearchResult<>(null, expandedNodes);
    }

    // Iterative deepening: run depth-limited DFS with limit 0, 1, 2, ... until a goal is found.
    private static <S> SearchResult<S> iterativeDeepening(GenericSearchProblem<S> problem) {
        long expandedNodes = 0;
        for (int limit = 0; limit <= problem.maximumDepth(); limit++) {
            SearchResult<S> result = depthLimitedSearch(problem, limit);
            expandedNodes += result.expandedNodes; // count nodes from every iteration
            if (result.goal != null) {
                return new SearchResult<>(result.goal, expandedNodes);
            }
        }
        return new SearchResult<>(null, expandedNodes);
    }

    // Depth-first search that never goes deeper than the given limit.
    private static <S> SearchResult<S> depthLimitedSearch(
            GenericSearchProblem<S> problem, int limit) {
        Deque<SearchNode<S>> frontier = new ArrayDeque<>();  // used as a stack (LIFO)
        Map<S, Integer> bestDepth = new HashMap<>();         // shallowest depth seen per state
        List<SearchNode<S>> visited = new ArrayList<>();      // expanded nodes, for dominance checks
        frontier.push(new SearchNode<>(problem.initialState(), null, null, 0L));
        long expandedNodes = 0;

        while (!frontier.isEmpty()) {
            SearchNode<S> current = frontier.pop();
            // Skip a state already seen at the same or a shallower depth.
            Integer oldDepth = bestDepth.get(current.state);
            if (oldDepth != null && oldDepth <= current.depth) {
                continue;
            }
            if (problem.isGoal(current.state)) {
                return new SearchResult<>(current, expandedNodes);
            }
            bestDepth.put(current.state, current.depth);
            // Stop at the depth limit, or if an earlier node was at least as good.
            if (current.depth == limit || isDominated(problem, visited, current)) {
                continue;
            }

            rememberState(problem, visited, current);
            expandedNodes++;
            // Push children in reverse so they are popped in their original order.
            List<Transition<S>> transitions = problem.successors(current.state);
            for (int i = transitions.size() - 1; i >= 0; i--) {
                Transition<S> transition = transitions.get(i);
                frontier.push(new SearchNode<>(transition.state, current,
                        transition.action, current.pathCost + transition.stepCost));
            }
        }
        return new SearchResult<>(null, expandedNodes);
    }

    // True if an earlier node at the same or a shallower depth dominates this one
    // (same situation, at least as many resources), so this node cannot do better.
    private static <S> boolean isDominated(GenericSearchProblem<S> problem,
            List<SearchNode<S>> visited, SearchNode<S> current) {
        for (int i = 0; i < visited.size(); i++) {
            SearchNode<S> previous = visited.get(i);
            if (previous.depth <= current.depth
                    && problem.dominates(previous.state, current.state)) {
                return true;
            }
        }
        return false;
    }

    // Adds the node to the visited list and removes older nodes it dominates.
    private static <S> void rememberState(GenericSearchProblem<S> problem,
            List<SearchNode<S>> visited, SearchNode<S> current) {
        for (int i = visited.size() - 1; i >= 0; i--) {
            SearchNode<S> previous = visited.get(i);
            if (current.depth <= previous.depth
                    && problem.dominates(current.state, previous.state)) {
                visited.remove(i);
            }
        }
        visited.add(current);
    }
}
