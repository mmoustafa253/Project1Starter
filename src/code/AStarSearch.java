package code;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * A* search: always expands the node with the smallest f = g + h,
 * where g = cost so far and h = heuristic estimate of the remaining cost.
 * With an admissible h, the first goal removed from the frontier is optimal.
 */
public class AStarSearch {

    // Frontier entry: a node and its priority f = g + h.
    private static class Entry<S> implements Comparable<Entry<S>> {
        private final SearchNode<S> node;
        private final long estimatedCost; // f = g + h

        private Entry(SearchNode<S> node, long heuristic) {
            this.node = node;
            // Cap at Long.MAX_VALUE so a huge h ("unreachable") cannot overflow.
            if (heuristic >= Long.MAX_VALUE - node.pathCost) {
                this.estimatedCost = Long.MAX_VALUE;
            } else {
                this.estimatedCost = node.pathCost + heuristic;
            }
        }

        // Smallest f first; on a tie, smaller g first.
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
        Map<S, Long> bestCost = new HashMap<>(); // cheapest g found for each state
        S initial = problem.initialState();
        SearchNode<S> root = new SearchNode<>(initial, null, null, 0L);
        frontier.add(new Entry<>(root, problem.heuristic(initial)));
        bestCost.put(initial, 0L);
        long expandedNodes = 0;

        while (!frontier.isEmpty()) {
            SearchNode<S> current = frontier.poll().node;

            // Outdated entry: a cheaper path to this state was found later, skip it.
            if (current.pathCost > bestCost.get(current.state)) {
                continue;
            }
            // Goal test on removal (not on generation) guarantees optimality.
            if (problem.isGoal(current.state)) {
                return new SearchResult<>(current, expandedNodes);
            }

            expandedNodes++;
            for (Transition<S> transition : problem.successors(current.state)) {
                long newCost = current.pathCost + transition.stepCost;
                Long oldCost = bestCost.get(transition.state);
                // Add the child only if the state is new or reached more cheaply.
                // No closed set: improved states are reopened, so A* stays optimal
                // even if the heuristic is admissible but not consistent.
                if (oldCost == null || newCost < oldCost) {
                    bestCost.put(transition.state, newCost);
                    SearchNode<S> child = new SearchNode<>(transition.state,
                            current, transition.action, newCost);
                    frontier.add(new Entry<>(child, problem.heuristic(child.state)));
                }
            }
        }
        // Frontier empty: no solution.
        return new SearchResult<>(null, expandedNodes);
    }
}
