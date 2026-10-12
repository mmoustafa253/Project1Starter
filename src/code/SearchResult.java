package code;

public class SearchResult<S> {
    public final SearchNode<S> goal;
    public final long expandedNodes;

    public SearchResult(SearchNode<S> goal, long expandedNodes) {
        this.goal = goal;
        this.expandedNodes = expandedNodes;
    }
}