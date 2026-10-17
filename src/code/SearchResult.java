package code;

/**
 * What a search returns: the goal node found and how many nodes were expanded.
 */
public class SearchResult<S> {
    public final SearchNode<S> goal;    // goal node, or null if there is no solution
    public final long expandedNodes;    // number of nodes expanded during the search

    public SearchResult(SearchNode<S> goal, long expandedNodes) {
        this.goal = goal;
        this.expandedNodes = expandedNodes;
    }
}
