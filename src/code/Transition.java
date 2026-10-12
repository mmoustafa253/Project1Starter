package code;

public class Transition<S> {
    public final S state;
    public final String action;
    public final long stepCost;

    public Transition(S state, String action, long stepCost) {
        this.state = state;
        this.action = action;
        this.stepCost = stepCost;
    }
}