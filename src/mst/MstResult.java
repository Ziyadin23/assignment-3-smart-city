package mst;

import graph.Edge;

import java.util.List;

/** Selected edges and total weight of a minimum spanning tree or forest. */
public record MstResult(List<Edge> edges, long totalWeight, int componentCount) {
    public MstResult {
        edges = List.copyOf(edges);
    }

    /** A single vertex is connected; an empty graph has zero components. */
    public boolean connected() {
        return componentCount == 1;
    }
}
