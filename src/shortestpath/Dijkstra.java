package shortestpath;

import graph.Edge;
import graph.WeightedGraph;
import metrics.OperationCounts;

import java.util.Arrays;
import java.util.PriorityQueue;

import static metrics.OperationCounts.Operation.*;

public final class Dijkstra {
    private Dijkstra() {
    }

    /** Finds distances and paths on a directed or undirected non-negative graph. */
    public static ShortestPathResult compute(WeightedGraph graph, int source) {
        return compute(graph, source, null);
    }

    public static ShortestPathResult compute(WeightedGraph graph, int source, OperationCounts counts) {
        if (source < 0 || source >= graph.vertexCount()) {
            throw new IllegalArgumentException("Source out of range: " + source);
        }
        for (Edge edge : graph.edges()) {
            if (counts != null) counts.increment(EDGE_EXAMINATIONS);
            if (edge.weight() < 0) {
                throw new IllegalArgumentException("Dijkstra requires non-negative edge weights");
            }
        }

        long[] distances = new long[graph.vertexCount()];
        int[] predecessors = new int[graph.vertexCount()];
        Arrays.fill(distances, ShortestPathResult.UNREACHABLE);
        Arrays.fill(predecessors, -1);
        PriorityQueue<State> queue = new PriorityQueue<>((first, second) -> {
            if (counts != null) counts.increment(KEY_COMPARISONS);
            return Long.compare(first.distance(), second.distance());
        });
        distances[source] = 0;
        queue.add(new State(source, 0));
        if (counts != null) counts.increment(FRONTIER_ADDS);

        while (!queue.isEmpty()) {
            State current = queue.remove();
            if (counts != null) counts.increment(FRONTIER_REMOVES);
            if (current.distance() != distances[current.vertex()]) {
                continue;
            }
            for (Edge edge : graph.neighbors(current.vertex())) {
                if (counts != null) {
                    counts.increment(EDGE_EXAMINATIONS);
                    counts.increment(RELAXATION_ATTEMPTS);
                }
                long candidate = current.distance() + edge.weight();
                if (candidate < distances[edge.to()]) {
                    distances[edge.to()] = candidate;
                    predecessors[edge.to()] = current.vertex();
                    queue.add(new State(edge.to(), candidate));
                    if (counts != null) {
                        counts.increment(SUCCESSFUL_RELAXATIONS);
                        counts.increment(FRONTIER_ADDS);
                    }
                }
            }
        }

        return new ShortestPathResult(source, distances, predecessors);
    }

    private record State(int vertex, long distance) {
    }
}
