package shortestpath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable single-source distances and predecessor links. */
public final class ShortestPathResult {
    public static final long UNREACHABLE = Long.MAX_VALUE;

    private final int source;
    private final long[] distances;
    private final int[] predecessors;

    ShortestPathResult(int source, long[] distances, int[] predecessors) {
        this.source = source;
        this.distances = distances.clone();
        this.predecessors = predecessors.clone();
    }

    public int source() {
        return source;
    }

    public int vertexCount() {
        return distances.length;
    }

    public long distanceTo(int vertex) {
        validateVertex(vertex);
        return distances[vertex];
    }

    public boolean reachable(int vertex) {
        return distanceTo(vertex) != UNREACHABLE;
    }

    /** Returns source-to-destination vertices, or an empty list if unreachable. */
    public List<Integer> pathTo(int vertex) {
        if (!reachable(vertex)) {
            return List.of();
        }
        List<Integer> path = new ArrayList<>();
        for (int current = vertex; current != -1; current = predecessors[current]) {
            path.add(current);
        }
        Collections.reverse(path);
        return List.copyOf(path);
    }

    private void validateVertex(int vertex) {
        if (vertex < 0 || vertex >= distances.length) {
            throw new IllegalArgumentException("Vertex out of range: " + vertex);
        }
    }
}
