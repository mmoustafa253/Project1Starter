package code;

import java.util.List;

/**
 * Generic search problem. S is the state type.
 * Any problem (e.g. CaveExplorer) extends this so the search algorithms can solve it.
 */
public abstract class GenericSearchProblem<S> {
    // The state the search starts from.
    public abstract S initialState();

    // True if the state solves the problem.
    public abstract boolean isGoal(S state);

    // All states reachable in one action, with the action name and its cost.
    public abstract List<Transition<S>> successors(S state);

    // Estimated remaining cost to a goal (used by A*). Must never overestimate.
    public abstract long heuristic(S state);

    // True if first is at least as good as second (same situation, more or equal resources).
    public abstract boolean dominates(S first, S second);

    // Upper limit on solution depth (used by iterative deepening).
    public abstract int maximumDepth();
}
