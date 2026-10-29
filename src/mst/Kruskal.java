package mst;

import dsu.DisjointSetUnion;
import graph.Edge;
import graph.WeightedGraph;
import metrics.OperationCounts;

import java.util.ArrayList;
import java.util.List;

import static metrics.OperationCounts.Operation.*;

public final class Kruskal {
    private Kruskal() {
    }

    /** Computes an MST for a connected graph, or a minimum spanning forest otherwise. */
    public static MstResult compute(WeightedGraph graph) {
        return compute(graph, null);
    }

    public static MstResult compute(WeightedGraph graph, OperationCounts counts) {
        if (graph.isDirected()) {
            throw new IllegalArgumentException("Kruskal requires an undirected graph");
        }

        List<Edge> sortedEdges = new ArrayList<>(graph.edges());
        sortedEdges.sort((first, second) -> {
            if (counts != null) counts.increment(KEY_COMPARISONS);
            return Integer.compare(first.weight(), second.weight());
        });
        DisjointSetUnion components = new DisjointSetUnion(graph.vertexCount(), counts);
        List<Edge> selected = new ArrayList<>();
        long totalWeight = 0;

        for (Edge edge : sortedEdges) {
            if (counts != null) counts.increment(EDGE_EXAMINATIONS);
            if (components.union(edge.from(), edge.to())) {
                selected.add(edge);
                totalWeight += edge.weight();
                if (components.componentCount() == 1) {
                    break;
                }
            }
        }

        return new MstResult(selected, totalWeight, components.componentCount());
    }
}
