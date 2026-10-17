package code;

/**
 * A node in the search tree: a state plus how we got there.
 */
public class SearchNode<S> {
    public final S state;               // the state this node represents
    public final SearchNode<S> parent;  // node we came from (null for the root)
    public final String action;         // action that led here (null for the root)
    public final int depth;             // number of actions from the root
    public final long pathCost;         // total cost from the root to this node

    public SearchNode(S state, SearchNode<S> parent, String action, long pathCost) {
        this.state = state;
        this.parent = parent;
        this.action = action;
        this.pathCost = pathCost;
        // Root has depth 0; every child is one level deeper than its parent.
        if (parent == null) {
            this.depth = 0;
        } else {
            this.depth = parent.depth + 1;
        }
    }
}
