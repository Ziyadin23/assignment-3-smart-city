package shortestpath;

import graph.Edge;
import graph.WeightedGraph;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

public final class Dijkstra {
    private Dijkstra() {
    }

    /** Finds distances and paths on a directed or undirected non-negative graph. */
    public static ShortestPathResult compute(WeightedGraph graph, int source) {
        if (source < 0 || source >= graph.vertexCount()) {
            throw new IllegalArgumentException("Source out of range: " + source);
        }
        for (Edge edge : graph.edges()) {
            if (edge.weight() < 0) {
                throw new IllegalArgumentException("Dijkstra requires non-negative edge weights");
            }
        }

        long[] distances = new long[graph.vertexCount()];
        int[] predecessors = new int[graph.vertexCount()];
        Arrays.fill(distances, ShortestPathResult.UNREACHABLE);
        Arrays.fill(predecessors, -1);
        PriorityQueue<State> queue = new PriorityQueue<>(Comparator.comparingLong(State::distance));
        distances[source] = 0;
        queue.add(new State(source, 0));

        while (!queue.isEmpty()) {
            State current = queue.remove();
            if (current.distance() != distances[current.vertex()]) {
                continue;
            }
            for (Edge edge : graph.neighbors(current.vertex())) {
                long candidate = current.distance() + edge.weight();
                if (candidate < distances[edge.to()]) {
                    distances[edge.to()] = candidate;
                    predecessors[edge.to()] = current.vertex();
                    queue.add(new State(edge.to(), candidate));
                }
            }
        }

        return new ShortestPathResult(source, distances, predecessors);
    }

    private record State(int vertex, long distance) {
    }
}
