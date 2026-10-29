package mst;

import graph.Edge;
import graph.WeightedGraph;
import metrics.OperationCounts;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import static metrics.OperationCounts.Operation.*;

public final class Prim {
    private Prim() {
    }

    /** Computes an MST, restarting in each component for disconnected graphs. */
    public static MstResult compute(WeightedGraph graph) {
        return compute(graph, null);
    }

    public static MstResult compute(WeightedGraph graph, OperationCounts counts) {
        if (graph.isDirected()) {
            throw new IllegalArgumentException("Prim requires an undirected graph");
        }

        boolean[] visited = new boolean[graph.vertexCount()];
        PriorityQueue<Edge> frontier = new PriorityQueue<>((first, second) -> {
            if (counts != null) counts.increment(KEY_COMPARISONS);
            return Integer.compare(first.weight(), second.weight());
        });
        List<Edge> selected = new ArrayList<>();
        long totalWeight = 0;
        int componentCount = 0;

        for (int start = 0; start < graph.vertexCount(); start++) {
            if (visited[start]) {
                continue;
            }
            componentCount++;
            visit(graph, start, visited, frontier, counts);
            while (!frontier.isEmpty()) {
                Edge edge = frontier.remove();
                if (counts != null) counts.increment(FRONTIER_REMOVES);
                if (visited[edge.to()]) {
                    continue;
                }
                selected.add(edge);
                totalWeight += edge.weight();
                visit(graph, edge.to(), visited, frontier, counts);
            }
        }

        return new MstResult(selected, totalWeight, componentCount);
    }

    private static void visit(WeightedGraph graph, int vertex, boolean[] visited,
                              PriorityQueue<Edge> frontier, OperationCounts counts) {
        visited[vertex] = true;
        for (Edge edge : graph.neighbors(vertex)) {
            if (counts != null) counts.increment(EDGE_EXAMINATIONS);
            if (!visited[edge.to()]) {
                frontier.add(edge);
                if (counts != null) counts.increment(FRONTIER_ADDS);
            }
        }
    }
}
