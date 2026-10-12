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
                return aStarSearch(problem);
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

            Long oldCost = explored.get(current.state);

            if (oldCost == null || current.pathCost < oldCost) {
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

    // ---------------------------------------------------------------------
    // A* search
    //
    // Idea: always expand the node that looks cheapest overall, where
    //   f = g + h
    //   g = cost already paid to reach the node
    //   h = estimated cost still needed to reach the goal (problem.heuristic)
    // ---------------------------------------------------------------------

    // One item in the A* frontier: a node plus its f value.
    private static class AStarEntry<S> implements Comparable<AStarEntry<S>> {
        SearchNode<S> node;
        long f;

        AStarEntry(SearchNode<S> node, long f) {
            this.node = node;
            this.f = f;
        }

        // The priority queue gives back the entry with the smallest f first.
        public int compareTo(AStarEntry<S> other) {
            return Long.compare(this.f, other.f);
        }
    }

    private static <S> SearchResult<S> aStarSearch(GenericSearchProblem<S> problem) {
        S start = problem.initialState();
        SearchNode<S> root = new SearchNode<>(start, null, null, 0L);

        // Frontier: nodes waiting to be expanded, smallest f first.
        PriorityQueue<AStarEntry<S>> frontier = new PriorityQueue<>();
        frontier.add(new AStarEntry<>(root, addSafely(0, problem.heuristic(start))));

        // For every state seen so far: the cheapest g found to reach it.
        HashMap<S, Long> bestG = new HashMap<>();
        bestG.put(start, 0L);

        long expanded = 0;

        while (!frontier.isEmpty()) {
            SearchNode<S> current = frontier.poll().node;

            // Old copy: we already found a cheaper way to this state, so skip it.
            if (current.pathCost > bestG.get(current.state)) {
                continue;
            }

            expanded++;

            // Goal is checked when the node is taken out, not when it is created.
            if (problem.isGoal(current.state)) {
                return new SearchResult<>(current, expanded);
            }

            for (Transition<S> t : problem.successors(current.state)) {
                long newG = current.pathCost + t.stepCost;
                Long oldG = bestG.get(t.state); // null if never seen

                // Add the child if it is new, or if we found a cheaper way to it.
                // (Even if that state was expanded before - this keeps A* optimal.)
                if (oldG == null || newG < oldG) {
                    bestG.put(t.state, newG);
                    SearchNode<S> child = new SearchNode<>(t.state, current, t.action, newG);
                    long f = addSafely(newG, problem.heuristic(t.state));
                    frontier.add(new AStarEntry<>(child, f));
                }
            }
        }

        // Frontier is empty and no goal was found.
        return new SearchResult<>(null, expanded);
    }

    // Returns g + h, but stops at the largest long instead of overflowing
    // (the heuristic may return a huge number for "goal unreachable").
    private static long addSafely(long g, long h) {
        if (h >= Long.MAX_VALUE - g) {
            return Long.MAX_VALUE;
        }
        return g + h;
    }
}