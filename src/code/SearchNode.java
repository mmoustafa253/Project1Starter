package code;

public class SearchNode<S> {
    public final S state;
    public final SearchNode<S> parent;
    public final String action;
    public final int depth;
    public final long pathCost;

    public SearchNode(S state, SearchNode<S> parent, String action, long pathCost) {
        this.state = state;
        this.parent = parent;
        this.action = action;
        this.pathCost = pathCost;
        if (parent == null) {
            this.depth = 0;
        } else {
            this.depth = parent.depth + 1;
        }
    }
}
