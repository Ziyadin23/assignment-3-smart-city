package mst;

import graph.Edge;
import graph.WeightedGraph;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Random;

public final class PrimTest {
    private static int passed;

    public static void main(String[] args) {
        if (!PrimTest.class.desiredAssertionStatus()) {
            throw new IllegalStateException("Tests require assertions: run with -ea");
        }
        run("known network has minimum cost 17", PrimTest::knownGraph);
        run("disconnected components and isolated vertices are included", PrimTest::disconnectedGraph);
        run("empty and single-vertex graphs follow MST conventions", PrimTest::boundaryGraphs);
        run("equal weights produce a valid minimum tree", PrimTest::equalWeights);
        run("stale queue edges cannot introduce a cycle", PrimTest::staleEdges);
        run("parallel roads and self-loops are handled", PrimTest::parallelEdges);
        run("negative and extreme weights are compared and summed safely", PrimTest::extremeWeights);
        run("directed input is rejected", PrimTest::directedGraph);
        run("input and immutable results are preserved", PrimTest::immutableResult);
        run("connected and disconnected random graphs match Kruskal", PrimTest::randomComparison);
        System.out.println("PrimTest: " + passed + " tests passed");
    }

    private static void knownGraph() {
        WeightedGraph graph = new WeightedGraph(4, false);
        graph.addEdge(0, 1, 7);
        graph.addEdge(1, 2, 4);
        graph.addEdge(2, 3, 6);
        graph.addEdge(0, 3, 12);
        MstResult result = Prim.compute(graph);
        assert result.totalWeight() == 17;
        assert result.connected();
        assertForest(graph, result);
    }

    private static void disconnectedGraph() {
        WeightedGraph graph = new WeightedGraph(6, false);
        graph.addEdge(1, 2, 4);
        graph.addEdge(2, 3, 1);
        graph.addEdge(1, 3, 8);
        graph.addEdge(4, 5, -2);
        MstResult result = Prim.compute(graph);
        assert result.totalWeight() == 3;
        assert result.componentCount() == 3;
        assert !result.connected();
        assertForest(graph, result);
    }

    private static void boundaryGraphs() {
        for (int count : new int[] {0, 1, 4}) {
            WeightedGraph graph = new WeightedGraph(count, false);
            MstResult result = Prim.compute(graph);
            assert result.totalWeight() == 0;
            assert result.edges().isEmpty();
            assert result.componentCount() == count;
            assert result.connected() == (count == 1);
            assertForest(graph, result);
        }
    }

    private static void equalWeights() {
        WeightedGraph graph = new WeightedGraph(4, false);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 3, 1);
        graph.addEdge(3, 0, 1);
        graph.addEdge(0, 2, 1);
        MstResult result = Prim.compute(graph);
        assert result.totalWeight() == 3;
        assertForest(graph, result);
    }

    private static void staleEdges() {
        WeightedGraph graph = new WeightedGraph(4, false);
        graph.addEdge(0, 1, 1);
        graph.addEdge(0, 2, 3);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 3, 10);
        graph.addEdge(1, 3, 20);
        MstResult result = Prim.compute(graph);
        assert result.totalWeight() == 12;
        assert result.edges().size() == 3;
        assertForest(graph, result);
    }

    private static void parallelEdges() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(0, 0, -100);
        graph.addEdge(1, 1, -100);
        graph.addEdge(0, 1, 9);
        graph.addEdge(1, 0, 2);
        graph.addEdge(0, 1, 2);
        graph.addEdge(1, 2, 5);
        MstResult result = Prim.compute(graph);
        assert result.totalWeight() == 7;
        assertForest(graph, result);
    }

    private static void extremeWeights() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(0, 1, Integer.MAX_VALUE);
        graph.addEdge(1, 2, Integer.MIN_VALUE);
        graph.addEdge(0, 2, 0);
        MstResult result = Prim.compute(graph);
        assert result.totalWeight() == (long) Integer.MIN_VALUE;
        assertForest(graph, result);

        WeightedGraph large = new WeightedGraph(3, false);
        large.addEdge(0, 1, Integer.MAX_VALUE);
        large.addEdge(1, 2, Integer.MAX_VALUE);
        assert Prim.compute(large).totalWeight() == 2L * Integer.MAX_VALUE;
    }

    private static void directedGraph() {
        try {
            Prim.compute(new WeightedGraph(2, true));
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected directed graph rejection");
    }

    private static void immutableResult() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(1, 0, 3);
        graph.addEdge(2, 1, 4);
        List<Edge> original = List.copyOf(graph.edges());
        MstResult result = Prim.compute(graph);
        assert graph.edges().equals(original);
        assert Prim.compute(graph).equals(result);
        assertForest(graph, result);
        graph.addEdge(0, 2, 1);
        assert result.totalWeight() == 7;
        assert Prim.compute(graph).totalWeight() == 4;
        try {
            result.edges().clear();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("Result edges must be immutable");
    }

    private static void randomComparison() {
        Random random = new Random(25);
        for (int sample = 0; sample < 100; sample++) {
            int count = 2 + random.nextInt(19);
            WeightedGraph graph = new WeightedGraph(count, false);
            boolean connected = sample % 2 == 0;
            if (connected) {
                for (int vertex = 1; vertex < count; vertex++) {
                    graph.addEdge(vertex - 1, vertex, random.nextInt(41) - 20);
                }
            }
            for (int from = 0; from < count; from++) {
                for (int to = from + 1; to < count; to++) {
                    if (!connected && (from < count / 2) != (to < count / 2)) {
                        continue;
                    }
                    if (random.nextInt(4) == 0) {
                        graph.addEdge(from, to, random.nextInt(41) - 20);
                    }
                }
            }
            MstResult result = Prim.compute(graph);
            assert result.connected() == connected;
            assertForest(graph, result);
        }
    }

    private static void assertForest(WeightedGraph graph, MstResult result) {
        long weight = 0;
        for (Edge edge : result.edges()) {
            assert graph.neighbors(edge.from()).contains(edge) : "Selected road must exist";
            weight += edge.weight();
        }
        MstResult reference = Kruskal.compute(graph);
        assert result.totalWeight() == weight;
        assert result.totalWeight() == reference.totalWeight();
        assert result.componentCount() == reference.componentCount();
        assert result.edges().size() == graph.vertexCount() - result.componentCount();
        assert componentCount(graph.vertexCount(), result.edges()) == result.componentCount();
    }

    private static int componentCount(int count, List<Edge> edges) {
        boolean[] visited = new boolean[count];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        int components = 0;
        for (int start = 0; start < count; start++) {
            if (visited[start]) {
                continue;
            }
            components++;
            visited[start] = true;
            queue.add(start);
            while (!queue.isEmpty()) {
                int vertex = queue.remove();
                for (Edge edge : edges) {
                    int next = edge.from() == vertex ? edge.to() : edge.to() == vertex ? edge.from() : -1;
                    if (next >= 0 && !visited[next]) {
                        visited[next] = true;
                        queue.add(next);
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
