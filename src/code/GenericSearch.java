package code;

import java.util.*;

public class GenericSearch {

    public static <S> SearchResult<S> search(GenericSearchProblem<S> problem, String strategy) {
        switch (strategy) {
            case "UC":
                return uniformCostSearch(problem);
            case "ID":
                return iterativeDeepening(problem);
            case "AS":
                return AStarSearch.search(problem);
            default:
                throw new IllegalArgumentException("Invalid strategy: " + strategy);
        }
    }

    private static <S> SearchResult<S> uniformCostSearch(GenericSearchProblem<S> problem) {
        PriorityQueue<SearchNode<S>> frontier = new PriorityQueue<>(
            Comparator.comparingLong(n -> n.pathCost)
        );
        Map<S, Long> explored = new HashMap<>();

        SearchNode<S> root = new SearchNode<>(problem.initialState(), null, null, 0L);
        frontier.add(root);
        long expandedCount = 0;

        while (!frontier.isEmpty()) {
            SearchNode<S> current = frontier.poll();

            if (problem.isGoal(current.state)) {
                return new SearchResult<>(current, expandedCount);
            }

            if (!explored.containsKey(current.state) || current.pathCost < explored.get(current.state)) {
                explored.put(current.state, current.pathCost);
                expandedCount++;

                for (Transition<S> t : problem.successors(current.state)) {
                    SearchNode<S> child = new SearchNode<>(
                        t.state, current, t.action, current.pathCost + t.stepCost
                    );
                    frontier.add(child);
                }
            }
        }

        return new SearchResult<>(null, expandedCount);
    }

    private static <S> SearchResult<S> iterativeDeepening(GenericSearchProblem<S> problem) {
        long totalExpanded = 0;
        int maxDepth = problem.maximumDepth();

        for (int depth = 0; depth <= maxDepth; depth++) {
            SearchResult<S> result = depthLimitedSearch(problem, depth);
            totalExpanded += result.expandedNodes;

            if (result.goal != null) {
                return new SearchResult<>(result.goal, totalExpanded);
            }
        }

        return new SearchResult<>(null, totalExpanded);
    }

    private static <S> SearchResult<S> depthLimitedSearch(GenericSearchProblem<S> problem, int limit) {
        Stack<SearchNode<S>> frontier = new Stack<>();
        SearchNode<S> root = new SearchNode<>(problem.initialState(), null, null, 0L);
        frontier.push(root);
        long expandedCount = 0;

        while (!frontier.isEmpty()) {
            SearchNode<S> current = frontier.pop();

            if (problem.isGoal(current.state)) {
                return new SearchResult<>(current, expandedCount);
            }

            if (current.depth < limit) {
                expandedCount++;
                List<Transition<S>> successors = problem.successors(current.state);
                
                // Push in reverse order so left-to-right traversal matches standard DFS
                for (int i = successors.size() - 1; i >= 0; i--) {
                    Transition<S> t = successors.get(i);
                    SearchNode<S> child = new SearchNode<>(
                        t.state, current, t.action, current.pathCost + t.stepCost
                    );
                    frontier.push(child);
                }
            }
        }

        return new SearchResult<>(null, expandedCount);
    }
}
