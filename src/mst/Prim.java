package mst;

import graph.Edge;
import graph.WeightedGraph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public final class Prim {
    private Prim() {
    }

    /** Computes an MST, restarting in each component for disconnected graphs. */
    public static MstResult compute(WeightedGraph graph) {
        if (graph.isDirected()) {
            throw new IllegalArgumentException("Prim requires an undirected graph");
        }

        boolean[] visited = new boolean[graph.vertexCount()];
        PriorityQueue<Edge> frontier = new PriorityQueue<>(Comparator.comparingInt(Edge::weight));
        List<Edge> selected = new ArrayList<>();
        long totalWeight = 0;
        int componentCount = 0;

        for (int start = 0; start < graph.vertexCount(); start++) {
            if (visited[start]) {
                continue;
            }
            componentCount++;
            visit(graph, start, visited, frontier);
            while (!frontier.isEmpty()) {
                Edge edge = frontier.remove();
                if (visited[edge.to()]) {
                    continue;
                }
                selected.add(edge);
                totalWeight += edge.weight();
                visit(graph, edge.to(), visited, frontier);
            }
        }

        return new MstResult(selected, totalWeight, componentCount);
    }

    private static void visit(WeightedGraph graph, int vertex, boolean[] visited,
                              PriorityQueue<Edge> frontier) {
        visited[vertex] = true;
        for (Edge edge : graph.neighbors(vertex)) {
            if (!visited[edge.to()]) {
                frontier.add(edge);
            }
        }
    }
}
