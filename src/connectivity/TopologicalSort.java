package connectivity;

import graph.Edge;
import graph.WeightedGraph;
import metrics.OperationCounts;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import static metrics.OperationCounts.Operation.*;

public final class TopologicalSort {
    private TopologicalSort() {
    }

    /** Uses Kahn's algorithm to order a directed graph or report a cycle. */
    public static TopologicalResult compute(WeightedGraph graph) {
        return compute(graph, null);
    }

    public static TopologicalResult compute(WeightedGraph graph, OperationCounts counts) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("Topological ordering requires a directed graph");
        }

        int[] indegree = new int[graph.vertexCount()];
        for (Edge edge : graph.edges()) {
            if (counts != null) counts.increment(EDGE_EXAMINATIONS);
            indegree[edge.to()]++;
        }
        ArrayDeque<Integer> ready = new ArrayDeque<>();
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            if (indegree[vertex] == 0) {
                ready.add(vertex);
                if (counts != null) counts.increment(FRONTIER_ADDS);
            }
        }

        List<Integer> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            int vertex = ready.remove();
            if (counts != null) counts.increment(FRONTIER_REMOVES);
            order.add(vertex);
            for (Edge edge : graph.neighbors(vertex)) {
                if (counts != null) counts.increment(EDGE_EXAMINATIONS);
                indegree[edge.to()]--;
                if (indegree[edge.to()] == 0) {
                    ready.add(edge.to());
                    if (counts != null) counts.increment(FRONTIER_ADDS);
                }
            }
        }

        if (order.size() != graph.vertexCount()) {
            return new TopologicalResult(List.of(), false);
        }
        return new TopologicalResult(order, true);
    }
}
