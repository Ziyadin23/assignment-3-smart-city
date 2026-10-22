package graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A graph with fixed vertex IDs from 0 to vertexCount - 1. */
public final class WeightedGraph {
    private final boolean directed;
    private final List<List<Edge>> adjacency;
    private final List<Edge> edges = new ArrayList<>();

    public WeightedGraph(int vertexCount, boolean directed) {
        if (vertexCount < 0) {
            throw new IllegalArgumentException("Vertex count must be non-negative");
        }
        this.directed = directed;
        this.adjacency = new ArrayList<>(vertexCount);
        for (int vertex = 0; vertex < vertexCount; vertex++) {
            adjacency.add(new ArrayList<>());
        }
    }

    public int vertexCount() {
        return adjacency.size();
    }

    public int edgeCount() {
        return edges.size();
    }

    public boolean isDirected() {
        return directed;
    }

    public void addEdge(int from, int to, int weight) {
        validateVertex(from);
        validateVertex(to);

        Edge edge = new Edge(from, to, weight);
        edges.add(edge);
        adjacency.get(from).add(edge);
        if (!directed && from != to) {
            adjacency.get(to).add(new Edge(to, from, weight));
        }
    }

    /** Read-only live view; each added edge appears once, including undirected edges. */
    public List<Edge> edges() {
        return Collections.unmodifiableList(edges);
    }

    /** Read-only live view of edges leaving this vertex. */
    public List<Edge> neighbors(int vertex) {
        validateVertex(vertex);
        return Collections.unmodifiableList(adjacency.get(vertex));
    }

    private void validateVertex(int vertex) {
        if (vertex < 0 || vertex >= vertexCount()) {
            throw new IllegalArgumentException("Vertex out of range: " + vertex);
        }
    }
}
