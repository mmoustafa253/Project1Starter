
package code;

import java.util.*;

public class GenericSearch {

    public static <S> SearchResult<S> search(
            GenericSearchProblem<S> problem, String strategy) {

        switch (strategy) {
            case "UC":
                return uniformCostSearch(problem);
            case "ID":
                return iterativeDeepening(problem);
            case "AS":
                return aStarSearch(problem);
            default:
                throw new IllegalArgumentException(
                    "Invalid strategy: " + strategy
                );
        }
    }

    // ---------------------------------------------------------
    // Uniform Cost Search (UCS)
    // ---------------------------------------------------------

    private static <S> SearchResult<S> uniformCostSearch(
            GenericSearchProblem<S> problem) {

        PriorityQueue<SearchNode<S>> frontier = new PriorityQueue<>(
            Comparator.comparingLong(n -> n.pathCost)
        );

        Map<S, Long> explored = new HashMap<>();

        SearchNode<S> root = new SearchNode<>(
            problem.initialState(), null, null, 0L
        );

        frontier.add(root);
        long expandedCount = 0;

        while (!frontier.isEmpty()) {
            SearchNode<S> current = frontier.poll();

            Long oldCost = explored.get(current.state);

            // Skip a state already expanded at an equal or lower cost.
            if (oldCost != null && current.pathCost >= oldCost) {
                continue;
            }

            // Goal nodes are not counted as expanded.
            if (problem.isGoal(current.state)) {
                return new SearchResult<>(current, expandedCount);
            }

            explored.put(current.state, current.pathCost);
            expandedCount++;

            for (Transition<S> t : problem.successors(current.state)) {
                SearchNode<S> child = new SearchNode<>(
                    t.state,
                    current,
                    t.action,
                    current.pathCost + t.stepCost
                );

                frontier.add(child);
            }
        }

        return new SearchResult<>(null, expandedCount);
    }

    // ---------------------------------------------------------
    // Iterative Deepening Search (IDS)
    // ---------------------------------------------------------

    private static <S> SearchResult<S> iterativeDeepening(
            GenericSearchProblem<S> problem) {

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

    private static <S> SearchResult<S> depthLimitedSearch(
            GenericSearchProblem<S> problem, int limit) {

        Stack<SearchNode<S>> frontier = new Stack<>();

        SearchNode<S> root = new SearchNode<>(
            problem.initialState(), null, null, 0L
        );

        frontier.push(root);
        long expandedCount = 0;

        while (!frontier.isEmpty()) {
            SearchNode<S> current = frontier.pop();

            // Goal nodes are not counted as expanded.
            if (problem.isGoal(current.state)) {
                return new SearchResult<>(current, expandedCount);
            }

            if (current.depth < limit) {
                List<Transition<S>> successors =
                    problem.successors(current.state);

                // Count the node because its successors are generated.
                expandedCount++;

                // Reverse order preserves left-to-right DFS traversal.
                for (int i = successors.size() - 1; i >= 0; i--) {
                    Transition<S> t = successors.get(i);

                    // Do not revisit states already on the current path.
                    if (!isOnPath(current, t.state)) {
                        SearchNode<S> child = new SearchNode<>(
                            t.state,
                            current,
                            t.action,
                            current.pathCost + t.stepCost
                        );

                        frontier.push(child);
                    }
                }
            }
        }

        return new SearchResult<>(null, expandedCount);
    }

    // Checks whether a state already exists on the current path.
    private static <S> boolean isOnPath(
            SearchNode<S> node, S state) {

        while (node != null) {
            if (Objects.equals(node.state, state)) {
                return true;
            }

            node = node.parent;
        }

        return false;
    }

    // ---------------------------------------------------------
    // A* Search
    // f(n) = g(n) + h(n)
    // g(n) = cost so far
    // h(n) = estimated remaining cost
    // ---------------------------------------------------------

    private static class AStarEntry<S>
            implements Comparable<AStarEntry<S>> {

        SearchNode<S> node;
        long f;

        AStarEntry(SearchNode<S> node, long f) {
            this.node = node;
            this.f = f;
        }

        @Override
        public int compareTo(AStarEntry<S> other) {
            return Long.compare(this.f, other.f);
        }
    }

    private static <S> SearchResult<S> aStarSearch(
            GenericSearchProblem<S> problem) {

        S start = problem.initialState();

        SearchNode<S> root = new SearchNode<>(
            start, null, null, 0L
        );

        PriorityQueue<AStarEntry<S>> frontier =
            new PriorityQueue<>();

        frontier.add(new AStarEntry<>(
            root, addSafely(0L, problem.heuristic(start))
        ));

        Map<S, Long> bestG = new HashMap<>();
        bestG.put(start, 0L);

        long expandedCount = 0;

        while (!frontier.isEmpty()) {
            SearchNode<S> current = frontier.poll().node;

            Long bestCost = bestG.get(current.state);

            // Skip an outdated entry if a cheaper path was found.
            if (bestCost == null || current.pathCost > bestCost) {
                continue;
            }

            // Goal nodes are not counted as expanded.
            if (problem.isGoal(current.state)) {
                return new SearchResult<>(current, expandedCount);
            }

            List<Transition<S>> successors =
                problem.successors(current.state);

            expandedCount++;

            for (Transition<S> t : successors) {
                long newG = current.pathCost + t.stepCost;
                Long oldG = bestG.get(t.state);

                // Update only when a cheaper path is found.
                if (oldG == null || newG < oldG) {
                    bestG.put(t.state, newG);

                    SearchNode<S> child = new SearchNode<>(
                        t.state,
                        current,
                        t.action,
                        newG
                    );

                    long f = addSafely(
                        newG, problem.heuristic(t.state)
                    );

                    frontier.add(new AStarEntry<>(child, f));
                }
            }
        }

        return new SearchResult<>(null, expandedCount);
    }

    // Prevents overflow when calculating g + h.
    private static long addSafely(long g, long h) {
        if (h >= Long.MAX_VALUE - g) {
            return Long.MAX_VALUE;
        }

        return g + h;
    }
}
