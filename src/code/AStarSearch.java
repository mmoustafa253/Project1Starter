package code;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public class AStarSearch {
    private static class Entry<S> implements Comparable<Entry<S>> {
        private final SearchNode<S> node;
        private final long estimatedCost;

        private Entry(SearchNode<S> node, long heuristic) {
            this.node = node;
            if (heuristic >= Long.MAX_VALUE - node.pathCost) {
                this.estimatedCost = Long.MAX_VALUE;
            } else {
                this.estimatedCost = node.pathCost + heuristic;
            }
        }

        @Override
        public int compareTo(Entry<S> other) {
            int comparison = Long.compare(estimatedCost, other.estimatedCost);
            if (comparison == 0) {
                return Long.compare(node.pathCost, other.node.pathCost);
            }
            return comparison;
        }
    }

    public static <S> SearchResult<S> search(GenericSearchProblem<S> problem) {
        PriorityQueue<Entry<S>> frontier = new PriorityQueue<>();
        Map<S, Long> bestCost = new HashMap<>();
        S initial = problem.initialState();
        SearchNode<S> root = new SearchNode<>(initial, null, null, 0L);
        frontier.add(new Entry<>(root, problem.heuristic(initial)));
        bestCost.put(initial, 0L);
        long expandedNodes = 0;

        while (!frontier.isEmpty()) {
            SearchNode<S> current = frontier.poll().node;
            if (current.pathCost > bestCost.get(current.state)) {
                continue;
            }
            if (problem.isGoal(current.state)) {
                return new SearchResult<>(current, expandedNodes);
            }

            expandedNodes++;
            for (Transition<S> transition : problem.successors(current.state)) {
                long newCost = current.pathCost + transition.stepCost;
                Long oldCost = bestCost.get(transition.state);
                if (oldCost == null || newCost < oldCost) {
                    bestCost.put(transition.state, newCost);
                    SearchNode<S> child = new SearchNode<>(transition.state,
                            current, transition.action, newCost);
                    frontier.add(new Entry<>(child, problem.heuristic(child.state)));
                }
            }
        }
        return new SearchResult<>(null, expandedNodes);
    }
}
