package code;

/**
 * One possible move from a state: the resulting state, the action name and its cost.
 */
public class Transition<S> {
    public final S state;       // state after the action
    public final String action; // action name, e.g. "left" or "collect"
    public final long stepCost; // cost of this single action

    public Transition(S state, String action, long stepCost) {
        this.state = state;
        this.action = action;
        this.stepCost = stepCost;
    }
}
