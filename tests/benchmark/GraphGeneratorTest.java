package benchmark;

import connectivity.Kosaraju;
import connectivity.SccResult;
import connectivity.TopologicalResult;
import connectivity.TopologicalSort;
import graph.Edge;
import graph.WeightedGraph;
import shortestpath.Dijkstra;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class GraphGeneratorTest {
    private static int passed;

    public static void main(String[] args) {
        if (!GraphGeneratorTest.class.desiredAssertionStatus()) throw new IllegalStateException("Tests require -ea");
        run("all required configurations have exact unique edges and valid weights", GraphGeneratorTest::configurations);
        run("fixed seeds reproduce edges and fingerprints", GraphGeneratorTest::reproducibility);
        run("spanning-tree generation reaches every district", GraphGeneratorTest::connectedGraphs);
        run("DAG generation permits a valid topological order", GraphGeneratorTest::dags);
        run("cyclic generation always includes its guaranteed cycle", GraphGeneratorTest::cycles);
        run("boundary and saturated graphs terminate with exact sizes", GraphGeneratorTest::boundaries);
        run("invalid and impossible edge counts are rejected", GraphGeneratorTest::invalidInputs);
        run("independent result validators accept correct results and reject wrong ones", GraphGeneratorTest::validators);
        System.out.println("GraphGeneratorTest: " + passed + " tests passed");
    }

    private static void configurations() {
        for (int[] size : new int[][] {{10, 30}, {100, 500}, {1000, 5000}}) {
            for (WeightedGraph graph : List.of(GraphGenerator.connectedUndirected(size[0], size[1], 29),
                    GraphGenerator.dag(size[0], size[1], 29), GraphGenerator.cyclicDirected(size[0], size[1], 29))) {
                assert graph.vertexCount() == size[0];
                assert graph.edgeCount() == size[1];
                Set<String> edges = new HashSet<>();
                for (Edge edge : graph.edges()) {
                    assert edge.from() != edge.to();
                    assert edge.weight() >= 1 && edge.weight() <= 100;
                    String pair = graph.isDirected() ? edge.from() + ":" + edge.to()
                            : Math.min(edge.from(), edge.to()) + ":" + Math.max(edge.from(), edge.to());
                    assert edges.add(pair) : "Duplicate endpoint pair";
                }
            }
        }
    }

    private static void reproducibility() {
        List<WeightedGraph> first = List.of(GraphGenerator.connectedUndirected(10, 30, 29),
                GraphGenerator.dag(10, 30, 29), GraphGenerator.cyclicDirected(10, 30, 29));
        List<WeightedGraph> second = List.of(GraphGenerator.connectedUndirected(10, 30, 29),
                GraphGenerator.dag(10, 30, 29), GraphGenerator.cyclicDirected(10, 30, 29));
        for (int index = 0; index < first.size(); index++) {
            assert first.get(index).edges().equals(second.get(index).edges());
            assert BenchmarkRunner.fingerprint(first.get(index)).equals(BenchmarkRunner.fingerprint(second.get(index)));
        }
        assert !BenchmarkRunner.fingerprint(first.get(0)).equals(
                BenchmarkRunner.fingerprint(GraphGenerator.connectedUndirected(10, 30, 30)));
    }

    private static void connectedGraphs() {
        for (long seed = 0; seed < 20; seed++) {
            WeightedGraph graph = GraphGenerator.connectedUndirected(20, 19, seed);
            boolean[] seen = new boolean[20];
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            seen[0] = true;
            queue.add(0);
            while (!queue.isEmpty()) {
                for (Edge edge : graph.neighbors(queue.remove())) {
                    if (!seen[edge.to()]) {
                        seen[edge.to()] = true;
                        queue.add(edge.to());
                    }
                }
            }
            for (boolean reachable : seen) assert reachable;
        }
    }

    private static void dags() {
        for (long seed = 0; seed < 20; seed++) {
            WeightedGraph graph = GraphGenerator.dag(20, 70, seed);
            ResultChecks.topological(graph, TopologicalSort.compute(graph), true);
        }
    }

    private static void cycles() {
        for (long seed = 0; seed < 20; seed++) {
            WeightedGraph graph = GraphGenerator.cyclicDirected(10, 30, seed);
            assert graph.neighbors(0).stream().anyMatch(edge -> edge.to() == 1);
            assert graph.neighbors(1).stream().anyMatch(edge -> edge.to() == 2);
            assert graph.neighbors(2).stream().anyMatch(edge -> edge.to() == 0);
            ResultChecks.topological(graph, TopologicalSort.compute(graph), false);
        }
    }

    private static void boundaries() {
        assert GraphGenerator.connectedUndirected(1, 0, 1).edgeCount() == 0;
        assert GraphGenerator.dag(0, 0, 1).vertexCount() == 0;
        assert GraphGenerator.dag(1, 0, 1).edgeCount() == 0;
        assert GraphGenerator.connectedUndirected(4, 6, 1).edgeCount() == 6;
        assert GraphGenerator.dag(4, 6, 1).edgeCount() == 6;
        assert GraphGenerator.cyclicDirected(3, 3, 1).edgeCount() == 3;
        assert GraphGenerator.cyclicDirected(4, 12, 1).edgeCount() == 12;
    }

    private static void invalidInputs() {
        expect(IllegalArgumentException.class, () -> GraphGenerator.connectedUndirected(0, 0, 1));
        expect(IllegalArgumentException.class, () -> GraphGenerator.connectedUndirected(10, 8, 1));
        expect(IllegalArgumentException.class, () -> GraphGenerator.connectedUndirected(3, 4, 1));
        expect(IllegalArgumentException.class, () -> GraphGenerator.dag(-1, 0, 1));
        expect(IllegalArgumentException.class, () -> GraphGenerator.dag(3, -1, 1));
        expect(IllegalArgumentException.class, () -> GraphGenerator.dag(3, 4, 1));
        expect(IllegalArgumentException.class, () -> GraphGenerator.cyclicDirected(2, 2, 1));
        expect(IllegalArgumentException.class, () -> GraphGenerator.cyclicDirected(3, 2, 1));
        expect(IllegalArgumentException.class, () -> GraphGenerator.cyclicDirected(3, 7, 1));
    }

    private static void validators() {
        WeightedGraph graph = new WeightedGraph(4, true);
        graph.addEdge(0, 1, 2);
        graph.addEdge(1, 0, 2);
        graph.addEdge(1, 2, 3);
        int[] expected = ResultChecks.sccReference(graph);
        assert expected[0] == expected[1] && expected[1] != expected[2] && expected[2] != expected[3];
        ResultChecks.scc(graph, Kosaraju.compute(graph), expected);
        expect(IllegalStateException.class, () -> ResultChecks.scc(graph,
                new SccResult(List.of(List.of(0, 1, 2), List.of(3))), expected));
        expect(IllegalStateException.class, () -> ResultChecks.scc(graph,
                new SccResult(List.of(List.of(0), List.of(1), List.of(2), List.of(3))), expected));
        long[] distances = ResultChecks.distanceReference(graph, 0);
        assert distances[0] == 0 && distances[1] == 2 && distances[2] == 5;
        ResultChecks.distances(Dijkstra.compute(graph, 0), distances);
        distances[2] = 99;
        expect(IllegalStateException.class, () -> ResultChecks.distances(Dijkstra.compute(graph, 0), distances));
        WeightedGraph dag = GraphGenerator.dag(5, 6, 1);
        expect(IllegalStateException.class, () -> ResultChecks.topological(dag,
                new TopologicalResult(List.of(0, 0, 1, 2, 3), true), true));
    }

    private static void expect(Class<? extends RuntimeException> type, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            if (type.isInstance(exception)) return;
            throw exception;
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }

    private static void run(String name, Runnable test) {
        test.run();
        passed++;
        System.out.println("PASS: " + name);
    }
}
