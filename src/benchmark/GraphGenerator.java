package benchmark;

import graph.WeightedGraph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class GraphGenerator {
    private GraphGenerator() {
    }

    public static WeightedGraph connectedUndirected(int vertices, int edges, long seed) {
        if (vertices < 1 || edges < vertices - 1) {
            throw new IllegalArgumentException("A connected graph needs V >= 1 and E >= V - 1");
        }
        validate(vertices, edges, (long) vertices * (vertices - 1) / 2);
        WeightedGraph graph = new WeightedGraph(vertices, false);
        Random random = new Random(seed);
        List<Integer> order = shuffledVertices(vertices, random);
        Set<Long> pairs = new HashSet<>();
        for (int index = 1; index < vertices; index++) {
            add(graph, order.get(index), order.get(random.nextInt(index)), pairs, random);
        }
        while (graph.edgeCount() < edges) {
            add(graph, random.nextInt(vertices), random.nextInt(vertices), pairs, random);
        }
        return graph;
    }

    public static WeightedGraph dag(int vertices, int edges, long seed) {
        validate(vertices, edges, (long) vertices * (vertices - 1) / 2);
        WeightedGraph graph = new WeightedGraph(vertices, true);
        Random random = new Random(seed);
        List<Integer> order = shuffledVertices(vertices, random);
        Set<Long> pairs = new HashSet<>();
        while (graph.edgeCount() < edges) {
            int first = random.nextInt(vertices);
            int second = random.nextInt(vertices);
            add(graph, order.get(Math.min(first, second)), order.get(Math.max(first, second)), pairs, random);
        }
        return graph;
    }

    public static WeightedGraph cyclicDirected(int vertices, int edges, long seed) {
        if (vertices < 3 || edges < 3) {
            throw new IllegalArgumentException("A guaranteed three-vertex cycle needs V >= 3 and E >= 3");
        }
        validate(vertices, edges, (long) vertices * (vertices - 1));
        WeightedGraph graph = new WeightedGraph(vertices, true);
        Random random = new Random(seed);
        Set<Long> pairs = new HashSet<>();
        add(graph, 0, 1, pairs, random);
        add(graph, 1, 2, pairs, random);
        add(graph, 2, 0, pairs, random);
        while (graph.edgeCount() < edges) {
            add(graph, random.nextInt(vertices), random.nextInt(vertices), pairs, random);
        }
        return graph;
    }

    private static List<Integer> shuffledVertices(int count, Random random) {
        List<Integer> order = new ArrayList<>();
        for (int vertex = 0; vertex < count; vertex++) order.add(vertex);
        Collections.shuffle(order, random);
        return order;
    }

    private static void add(WeightedGraph graph, int from, int to, Set<Long> pairs, Random random) {
        if (from == to) return;
        if (!graph.isDirected() && from > to) {
            int temporary = from;
            from = to;
            to = temporary;
        }
        long key = ((long) from << 32) | (to & 0xffffffffL);
        if (pairs.add(key)) graph.addEdge(from, to, 1 + random.nextInt(100));
    }

    private static void validate(int vertices, int edges, long maximum) {
        if (vertices < 0 || edges < 0 || edges > maximum) {
            throw new IllegalArgumentException("Invalid vertex/edge count for a simple graph");
        }
    }
}
