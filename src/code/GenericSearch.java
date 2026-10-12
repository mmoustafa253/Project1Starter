package code;

import java.util.*;

public class GenericSearch {

    public static <S> SearchResult<S> search(GenericSearchProblem<S> problem, String strategy) {
        if (strategy.equals("UC")) {
            return uniformCostSearch(problem);
        } else if (strategy.equals("ID")) {
            return iterativeDeepeningSearch(problem);
        } else if (strategy.equals("AS")) {
            return AStarSearch.search(problem);
        } else {
            throw new IllegalArgumentException("Unknown search strategy: " + strategy);
        }
    }

    private static <S> SearchResult<S> uniformCostSearch(GenericSearchProblem<S> problem) {
        S initial = problem.initialState();
        SearchNode<S> root = new SearchNode<>(initial, null, null, 0L);

        PriorityQueue<SearchNode<S>> frontier = new PriorityQueue<>(
            Comparator.comparingLong(node -> node.pathCost)
        );
        frontier.add(root);

        Map<S, Long> bestPathCosts = new HashMap<>();
        bestPathCosts.put(initial, 0L);

        long expandedNodes = 0;

        while (!frontier.isEmpty()) {
            SearchNode<S> current = frontier.poll();

            if (current.pathCost > bestPathCosts.getOrDefault(current.state, Long.MAX_VALUE)) {
                continue;
            }

            expandedNodes++;

            if (problem.isGoal(current.state)) {
                return new SearchResult<>(current, expandedNodes);
            }

            for (Transition<S> trans : problem.successors(current.state)) {
                long newCost = current.pathCost + trans.stepCost;

                if (newCost < bestPathCosts.getOrDefault(trans.state, Long.MAX_VALUE)) {
                    bestPathCosts.put(trans.state, newCost);
                    frontier.add(new SearchNode<>(trans.state, current, trans.action, newCost));
                }
            }
        }

        return new SearchResult<>(null, expandedNodes);
    }

    private static <S> SearchResult<S> iterativeDeepeningSearch(GenericSearchProblem<S> problem) {
        long totalExpanded = 0;
        int maxDepth = problem.maximumDepth();

        for (int depthLimit = 0; depthLimit <= maxDepth; depthLimit++) {
            // DFS with depth limit
            SearchResult<S> result = depthLimitedSearch(problem, depthLimit);
            totalExpanded += result.expandedNodes;

            if (result.goal != null) {
                return new SearchResult<>(result.goal, totalExpanded);
            }
        }

        return new SearchResult<>(null, totalExpanded);
    }

    private static <S> SearchResult<S> depthLimitedSearch(GenericSearchProblem<S> problem, int depthLimit) {
        Stack<SearchNode<S>> frontier = new Stack<>();
        S initial = problem.initialState();
        SearchNode<S> root = new SearchNode<>(initial, null, null, 0L);
        frontier.push(root);

        // Track visited states in current iteration (state -> minimum depth reached for this state)
        Map<S, Integer> visitedAtDepth = new HashMap<>();
        long expandedNodes = 0;

        while (!frontier.isEmpty()) {
            SearchNode<S> current = frontier.pop();

            // Check if we've seen this state at an equal or shallower depth in this iteration
            if (visitedAtDepth.containsKey(current.state) && visitedAtDepth.get(current.state) <= current.depth) {
                continue;
            }
            visitedAtDepth.put(current.state, current.depth);

            expandedNodes++;

            if (problem.isGoal(current.state)) {
                return new SearchResult<>(current, expandedNodes);
            }

            if (current.depth < depthLimit) {
                // Expand successors in reverse order if needed to maintain standard DFS order, or standard order
                List<Transition<S>> successors = problem.successors(current.state);
                // To match typical traversal order when using a Stack, iterate backwards or add normally
                for (int i = successors.size() - 1; i >= 0; i--) {
                    Transition<S> trans = successors.get(i);
                    SearchNode<S> child = new SearchNode<>(trans.state, current, trans.action, current.pathCost + trans.stepCost);
                    
                    // Optional check: ensure child depth does not exceed limit
                    if (child.depth <= depthLimit) {
                        frontier.push(child);
                    }
                }
            }
        }

        return new SearchResult<>(null, expandedNodes);
    }
}