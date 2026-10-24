package mst;

import dsu.DisjointSetUnion;
import graph.Edge;
import graph.WeightedGraph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Kruskal {
    private Kruskal() {
    }

    /** Computes an MST for a connected graph, or a minimum spanning forest otherwise. */
    public static MstResult compute(WeightedGraph graph) {
        if (graph.isDirected()) {
            throw new IllegalArgumentException("Kruskal requires an undirected graph");
        }

        List<Edge> sortedEdges = new ArrayList<>(graph.edges());
        sortedEdges.sort(Comparator.comparingInt(Edge::weight));
        DisjointSetUnion components = new DisjointSetUnion(graph.vertexCount());
        List<Edge> selected = new ArrayList<>();
        long totalWeight = 0;

        for (Edge edge : sortedEdges) {
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
