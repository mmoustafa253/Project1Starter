package code;

import java.util.List;

public abstract class GenericSearchProblem<S> {
    public abstract S initialState();
    public abstract boolean isGoal(S state);
    public abstract List<Transition<S>> successors(S state);
    public abstract long heuristic(S state);
    public abstract boolean dominates(S first, S second);
    public abstract int maximumDepth();
}