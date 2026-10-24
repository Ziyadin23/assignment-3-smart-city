package mst;

import graph.Edge;
import graph.WeightedGraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class KruskalTest {
    private static int passed;

    public static void main(String[] args) {
        if (!KruskalTest.class.desiredAssertionStatus()) {
            throw new IllegalStateException("Tests require assertions: run with -ea");
        }

        run("known graph has the expected MST edges and cost", KruskalTest::knownGraph);
        run("disconnected graph returns a minimum spanning forest", KruskalTest::disconnectedGraph);
        run("isolated vertices remain separate components", KruskalTest::isolatedVertices);
        run("single vertex is connected with no selected edges", KruskalTest::singleVertex);
        run("empty graph has zero weight and components", KruskalTest::emptyGraph);
        run("equal weights allow multiple valid MSTs", KruskalTest::multipleMsts);
        run("parallel edges use the cheaper connection", KruskalTest::parallelEdges);
        run("negative and zero weights are valid", KruskalTest::negativeWeights);
        run("self-loops are excluded even when cheap", KruskalTest::selfLoops);
        run("large weights are sorted and summed without integer overflow", KruskalTest::largeWeights);
        run("directed graphs are rejected", KruskalTest::directedGraph);
        run("input order is preserved and results remain immutable", KruskalTest::immutableResult);
        run("random forests match exhaustive minimum-weight search", KruskalTest::exhaustiveComparison);
        System.out.println("KruskalTest: " + passed + " tests passed");
    }

    private static void knownGraph() {
        WeightedGraph graph = new WeightedGraph(4, false);
        graph.addEdge(0, 1, 7);
        graph.addEdge(1, 2, 4);
        graph.addEdge(2, 3, 6);
        graph.addEdge(0, 3, 12);

        MstResult result = Kruskal.compute(graph);
        assert result.edges().equals(List.of(new Edge(1, 2, 4), new Edge(2, 3, 6), new Edge(0, 1, 7)));
        assert result.totalWeight() == 17;
        assert result.connected();
        assertForest(graph, result);
    }

    private static void disconnectedGraph() {
        WeightedGraph graph = new WeightedGraph(6, false);
        graph.addEdge(0, 1, 4);
        graph.addEdge(1, 2, 1);
        graph.addEdge(0, 2, 8);
        graph.addEdge(3, 4, 3);

        MstResult result = Kruskal.compute(graph);
        assert result.totalWeight() == 8;
        assert result.componentCount() == 3;
        assert !result.connected();
        assertForest(graph, result);
    }

    private static void isolatedVertices() {
        WeightedGraph graph = new WeightedGraph(4, false);
        MstResult result = Kruskal.compute(graph);
        assert result.edges().isEmpty();
        assert result.totalWeight() == 0;
        assert result.componentCount() == 4;
        assert !result.connected();
        assertForest(graph, result);
    }

    private static void singleVertex() {
        WeightedGraph graph = new WeightedGraph(1, false);
        MstResult result = Kruskal.compute(graph);
        assert result.edges().isEmpty();
        assert result.totalWeight() == 0;
        assert result.componentCount() == 1;
        assert result.connected();
        assertForest(graph, result);
    }

    private static void emptyGraph() {
        WeightedGraph graph = new WeightedGraph(0, false);
        MstResult result = Kruskal.compute(graph);
        assert result.edges().isEmpty();
        assert result.totalWeight() == 0;
        assert result.componentCount() == 0;
        assert !result.connected();
        assertForest(graph, result);
    }

    private static void multipleMsts() {
        WeightedGraph graph = new WeightedGraph(4, false);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 3, 1);
        graph.addEdge(3, 0, 1);
        graph.addEdge(0, 2, 2);

        MstResult result = Kruskal.compute(graph);
        assert result.totalWeight() == 3;
        assert result.edges().size() == 3;
        assert result.connected();
        assertForest(graph, result);
    }

    private static void parallelEdges() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(0, 1, 9);
        graph.addEdge(0, 1, 2);
        graph.addEdge(1, 0, 2);
        graph.addEdge(1, 2, 5);
        graph.addEdge(0, 2, 10);

        MstResult result = Kruskal.compute(graph);
        assert result.totalWeight() == 7;
        assertForest(graph, result);
    }

    private static void negativeWeights() {
        WeightedGraph graph = new WeightedGraph(4, false);
        graph.addEdge(0, 1, -4);
        graph.addEdge(1, 2, 0);
        graph.addEdge(2, 3, -2);
        graph.addEdge(0, 3, 5);
        graph.addEdge(0, 2, 3);

        MstResult result = Kruskal.compute(graph);
        assert result.totalWeight() == -6;
        assertForest(graph, result);
    }

    private static void selfLoops() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(0, 0, -100);
        graph.addEdge(1, 1, -100);
        graph.addEdge(0, 1, 2);
        graph.addEdge(1, 2, 3);

        MstResult result = Kruskal.compute(graph);
        assert result.totalWeight() == 5;
        assert result.edges().stream().noneMatch(edge -> edge.from() == edge.to());
        assertForest(graph, result);
    }

    private static void largeWeights() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(0, 1, Integer.MAX_VALUE);
        graph.addEdge(1, 2, Integer.MAX_VALUE);
        graph.addEdge(0, 2, Integer.MAX_VALUE);
        MstResult result = Kruskal.compute(graph);
        assert result.totalWeight() == 2L * Integer.MAX_VALUE;
        assertForest(graph, result);

        WeightedGraph extremes = new WeightedGraph(3, false);
        extremes.addEdge(0, 1, Integer.MAX_VALUE);
        extremes.addEdge(1, 2, Integer.MIN_VALUE);
        extremes.addEdge(0, 2, 0);
        MstResult minimum = Kruskal.compute(extremes);
        assert minimum.totalWeight() == (long) Integer.MIN_VALUE;
        assertForest(extremes, minimum);
    }

    private static void directedGraph() {
        WeightedGraph graph = new WeightedGraph(2, true);
        graph.addEdge(0, 1, 1);
        try {
            Kruskal.compute(graph);
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected directed graph rejection");
    }

    private static void immutableResult() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(0, 2, 8);
        graph.addEdge(0, 1, 2);
        graph.addEdge(1, 2, 3);
        List<Edge> originalOrder = List.copyOf(graph.edges());
        MstResult result = Kruskal.compute(graph);
        assert graph.edges().equals(originalOrder);
        assert Kruskal.compute(graph).equals(result);
        assertForest(graph, result);

        graph.addEdge(0, 2, 1);
        assert result.totalWeight() == 5;
        assert result.edges().size() == 2;
        assert Kruskal.compute(graph).totalWeight() == 3;
        try {
            result.edges().clear();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("Result edges must be immutable");
    }

    private static void exhaustiveComparison() {
        Random random = new Random(24);
        for (int sample = 0; sample < 60; sample++) {
            WeightedGraph graph = new WeightedGraph(1 + random.nextInt(5), false);
            for (int from = 0; from < graph.vertexCount(); from++) {
                for (int to = from + 1; to < graph.vertexCount(); to++) {
                    if (random.nextBoolean()) {
                        graph.addEdge(from, to, random.nextInt(11) - 5);
                    }
                }
            }
            MstResult result = Kruskal.compute(graph);
            assertForest(graph, result);
            assert result.totalWeight() == exhaustiveMinimum(graph) : "Wrong weight for sample " + sample;
        }
    }

    private static long exhaustiveMinimum(WeightedGraph graph) {
        int requiredEdges = graph.vertexCount() - componentCount(graph.vertexCount(), graph.edges());
        long minimum = Long.MAX_VALUE;
        for (int mask = 0; mask < (1 << graph.edgeCount()); mask++) {
            if (Integer.bitCount(mask) != requiredEdges) {
                continue;
            }
            List<Edge> subset = new ArrayList<>();
            long weight = 0;
            for (int index = 0; index < graph.edgeCount(); index++) {
                if ((mask & (1 << index)) != 0) {
                    Edge edge = graph.edges().get(index);
                    subset.add(edge);
                    weight += edge.weight();
                }
            }
            if (componentCount(graph.vertexCount(), subset) == graph.vertexCount() - requiredEdges) {
                minimum = Math.min(minimum, weight);
            }
        }
        return minimum;
    }

    private static void assertForest(WeightedGraph graph, MstResult result) {
        List<Edge> remaining = new ArrayList<>(graph.edges());
        long weight = 0;
        for (Edge edge : result.edges()) {
            assert remaining.remove(edge) : "Selected an edge not present in the input";
            weight += edge.weight();
        }
        int components = componentCount(graph.vertexCount(), graph.edges());
        assert result.totalWeight() == weight;
        assert result.componentCount() == components;
        assert result.connected() == (components == 1);
        assert componentCount(graph.vertexCount(), result.edges()) == components;
        assert result.edges().size() == graph.vertexCount() - components : "Result must be acyclic";
    }

    private static int componentCount(int vertexCount, List<Edge> edges) {
        boolean[] visited = new boolean[vertexCount];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        int components = 0;
        for (int start = 0; start < vertexCount; start++) {
            if (visited[start]) {
                continue;
            }
            components++;
            visited[start] = true;
            queue.add(start);
            while (!queue.isEmpty()) {
                int current = queue.remove();
                for (Edge edge : edges) {
                    if (edge.from() == current && !visited[edge.to()]) {
                        visited[edge.to()] = true;
                        queue.add(edge.to());
                    }
                    if (edge.to() == current && !visited[edge.from()]) {
                        visited[edge.from()] = true;
                        queue.add(edge.from());
                    }
                }
            }
        }
        return components;
    }

    private static void run(String name, Runnable test) {
        test.run();
        passed++;
        System.out.println("PASS: " + name);
    }
}
