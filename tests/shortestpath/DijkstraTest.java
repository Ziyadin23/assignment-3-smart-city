package shortestpath;

import graph.Edge;
import graph.WeightedGraph;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public final class DijkstraTest {
    private static int passed;

    public static void main(String[] args) {
        if (!DijkstraTest.class.desiredAssertionStatus()) {
            throw new IllegalStateException("Tests require assertions: run with -ea");
        }
        run("known directed graph has correct distances and paths", DijkstraTest::knownGraph);
        run("unreachable destinations have no path", DijkstraTest::unreachableVertices);
        run("source and single-vertex paths have zero distance", DijkstraTest::sourcePaths);
        run("undirected roads work in both directions", DijkstraTest::undirectedGraph);
        run("zero-weight cycles do not create predecessor cycles", DijkstraTest::zeroCycles);
        run("equal-distance alternatives return a valid shortest path", DijkstraTest::equalAlternatives);
        run("parallel edges and outdated queue entries are handled", DijkstraTest::staleEntries);
        run("negative edges are rejected even outside source reachability", DijkstraTest::negativeWeights);
        run("invalid sources and empty graph sources are rejected", DijkstraTest::invalidSources);
        run("path distances can exceed integer range", DijkstraTest::largeDistances);
        run("input and result snapshots remain unchanged", DijkstraTest::immutableResult);
        run("invalid result queries are rejected", DijkstraTest::invalidQueries);
        run("long paths are reconstructed iteratively", DijkstraTest::longChain);
        run("random distances match independent repeated relaxation", DijkstraTest::referenceComparison);
        System.out.println("DijkstraTest: " + passed + " tests passed");
    }

    private static void knownGraph() {
        WeightedGraph graph = new WeightedGraph(6, true);
        graph.addEdge(0, 1, 7);
        graph.addEdge(0, 2, 9);
        graph.addEdge(0, 5, 14);
        graph.addEdge(1, 2, 10);
        graph.addEdge(1, 3, 15);
        graph.addEdge(2, 3, 11);
        graph.addEdge(2, 5, 2);
        graph.addEdge(3, 4, 6);
        graph.addEdge(5, 4, 9);
        ShortestPathResult result = Dijkstra.compute(graph, 0);
        long[] expected = {0, 7, 9, 20, 20, 11};
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            assert result.distanceTo(vertex) == expected[vertex];
            assertPath(graph, result, vertex);
        }
        assert result.pathTo(4).equals(List.of(0, 2, 5, 4));
    }

    private static void unreachableVertices() {
        WeightedGraph graph = new WeightedGraph(4, true);
        graph.addEdge(0, 1, 3);
        graph.addEdge(2, 3, 1);
        ShortestPathResult result = Dijkstra.compute(graph, 0);
        assert result.distanceTo(1) == 3;
        for (int vertex : new int[] {2, 3}) {
            assert !result.reachable(vertex);
            assert result.distanceTo(vertex) == ShortestPathResult.UNREACHABLE;
            assert result.pathTo(vertex).isEmpty();
        }
    }

    private static void sourcePaths() {
        WeightedGraph single = new WeightedGraph(1, true);
        single.addEdge(0, 0, 0);
        ShortestPathResult result = Dijkstra.compute(single, 0);
        assert result.source() == 0;
        assert result.vertexCount() == 1;
        assert result.distanceTo(0) == 0;
        assert result.pathTo(0).equals(List.of(0));
        WeightedGraph isolated = new WeightedGraph(3, true);
        ShortestPathResult other = Dijkstra.compute(isolated, 2);
        assert other.source() == 2;
        assert other.distanceTo(2) == 0;
        assert other.pathTo(2).equals(List.of(2));
        assert !other.reachable(0);
    }

    private static void undirectedGraph() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(1, 0, 2);
        graph.addEdge(2, 1, 3);
        ShortestPathResult forward = Dijkstra.compute(graph, 0);
        ShortestPathResult backward = Dijkstra.compute(graph, 2);
        assert forward.distanceTo(2) == 5;
        assert forward.pathTo(2).equals(List.of(0, 1, 2));
        assert backward.distanceTo(0) == 5;
        assert backward.pathTo(0).equals(List.of(2, 1, 0));
        assertPath(graph, forward, 2);
        assertPath(graph, backward, 0);
    }

    private static void zeroCycles() {
        WeightedGraph graph = new WeightedGraph(4, true);
        graph.addEdge(0, 1, 0);
        graph.addEdge(1, 2, 0);
        graph.addEdge(2, 1, 0);
        graph.addEdge(2, 0, 0);
        graph.addEdge(2, 2, 0);
        graph.addEdge(2, 3, 2);
        ShortestPathResult result = Dijkstra.compute(graph, 0);
        assert result.distanceTo(2) == 0;
        assert result.distanceTo(3) == 2;
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            assertPath(graph, result, vertex);
        }
    }

    private static void equalAlternatives() {
        WeightedGraph graph = new WeightedGraph(4, true);
        graph.addEdge(0, 1, 2);
        graph.addEdge(0, 2, 2);
        graph.addEdge(1, 3, 3);
        graph.addEdge(2, 3, 3);
        ShortestPathResult result = Dijkstra.compute(graph, 0);
        assert result.distanceTo(3) == 5;
        assertPath(graph, result, 3);
    }

    private static void staleEntries() {
        WeightedGraph graph = new WeightedGraph(4, true);
        graph.addEdge(0, 1, 10);
        graph.addEdge(0, 1, 3);
        graph.addEdge(0, 2, 1);
        graph.addEdge(2, 1, 1);
        graph.addEdge(1, 3, 1);
        graph.addEdge(0, 3, 99);
        ShortestPathResult result = Dijkstra.compute(graph, 0);
        assert result.distanceTo(1) == 2;
        assert result.distanceTo(3) == 3;
        assert result.pathTo(3).equals(List.of(0, 2, 1, 3));
    }

    private static void negativeWeights() {
        WeightedGraph reachable = new WeightedGraph(2, true);
        reachable.addEdge(0, 1, -1);
        expectInvalid(() -> Dijkstra.compute(reachable, 0));
        WeightedGraph disconnected = new WeightedGraph(3, true);
        disconnected.addEdge(1, 2, -1);
        expectInvalid(() -> Dijkstra.compute(disconnected, 0));
        WeightedGraph undirected = new WeightedGraph(2, false);
        undirected.addEdge(0, 1, -2);
        expectInvalid(() -> Dijkstra.compute(undirected, 0));
    }

    private static void invalidSources() {
        WeightedGraph graph = new WeightedGraph(2, true);
        expectInvalid(() -> Dijkstra.compute(graph, -1));
        expectInvalid(() -> Dijkstra.compute(graph, 2));
        expectInvalid(() -> Dijkstra.compute(new WeightedGraph(0, true), 0));
    }

    private static void largeDistances() {
        WeightedGraph graph = new WeightedGraph(3, true);
        graph.addEdge(0, 1, Integer.MAX_VALUE);
        graph.addEdge(1, 2, Integer.MAX_VALUE);
        ShortestPathResult result = Dijkstra.compute(graph, 0);
        assert result.distanceTo(2) == 2L * Integer.MAX_VALUE;
        assertPath(graph, result, 2);
        graph.addEdge(0, 2, 5);
        assert Dijkstra.compute(graph, 0).distanceTo(2) == 5;
    }

    private static void immutableResult() {
        WeightedGraph graph = new WeightedGraph(3, true);
        graph.addEdge(0, 1, 2);
        graph.addEdge(1, 2, 3);
        List<Edge> original = List.copyOf(graph.edges());
        ShortestPathResult result = Dijkstra.compute(graph, 0);
        assert graph.edges().equals(original);
        graph.addEdge(0, 2, 1);
        assert Dijkstra.compute(graph, 0).distanceTo(2) == 1;
        assert result.distanceTo(2) == 5;
        assert result.pathTo(2).equals(List.of(0, 1, 2));
        long[] distances = {0, 4};
        int[] predecessors = {-1, 0};
        ShortestPathResult snapshot = new ShortestPathResult(0, distances, predecessors);
        distances[1] = 99;
        predecessors[1] = -1;
        assert snapshot.distanceTo(1) == 4;
        assert snapshot.pathTo(1).equals(List.of(0, 1));
        try {
            result.pathTo(2).clear();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("Returned paths must be immutable");
    }

    private static void invalidQueries() {
        ShortestPathResult result = Dijkstra.compute(new WeightedGraph(2, true), 0);
        expectInvalid(() -> result.distanceTo(-1));
        expectInvalid(() -> result.distanceTo(2));
        expectInvalid(() -> result.reachable(-1));
        expectInvalid(() -> result.reachable(2));
        expectInvalid(() -> result.pathTo(-1));
        expectInvalid(() -> result.pathTo(2));
    }

    private static void longChain() {
        WeightedGraph graph = new WeightedGraph(10_000, true);
        for (int vertex = 1; vertex < graph.vertexCount(); vertex++) {
            graph.addEdge(vertex - 1, vertex, 1);
        }
        ShortestPathResult result = Dijkstra.compute(graph, 0);
        assert result.distanceTo(9999) == 9999;
        List<Integer> path = result.pathTo(9999);
        assert path.size() == 10_000;
        for (int index = 0; index < path.size(); index++) {
            assert path.get(index) == index;
        }
    }

    private static void referenceComparison() {
        Random random = new Random(28);
        for (int sample = 0; sample < 80; sample++) {
            int count = 1 + random.nextInt(8);
            WeightedGraph graph = new WeightedGraph(count, sample % 2 == 0);
            for (int from = 0; from < count; from++) {
                for (int to = 0; to < count; to++) {
                    if (random.nextInt(5) == 0) {
                        graph.addEdge(from, to, random.nextInt(21));
                    }
                }
            }
            for (int source = 0; source < count; source++) {
                long[] expected = referenceDistances(graph, source);
                ShortestPathResult result = Dijkstra.compute(graph, source);
                for (int vertex = 0; vertex < count; vertex++) {
                    assert result.distanceTo(vertex) == expected[vertex];
                    assertPath(graph, result, vertex);
                }
            }
        }
    }

    private static long[] referenceDistances(WeightedGraph graph, int source) {
        long[] distances = new long[graph.vertexCount()];
        Arrays.fill(distances, ShortestPathResult.UNREACHABLE);
        distances[source] = 0;
        for (int pass = 1; pass < graph.vertexCount(); pass++) {
            for (int from = 0; from < graph.vertexCount(); from++) {
                if (distances[from] == ShortestPathResult.UNREACHABLE) {
                    continue;
                }
                for (Edge edge : graph.neighbors(from)) {
                    distances[edge.to()] = Math.min(distances[edge.to()], distances[from] + edge.weight());
                }
            }
        }
        return distances;
    }

    private static void assertPath(WeightedGraph graph, ShortestPathResult result, int target) {
        List<Integer> path = result.pathTo(target);
        if (!result.reachable(target)) {
            assert path.isEmpty();
            assert result.distanceTo(target) == ShortestPathResult.UNREACHABLE;
            return;
        }
        assert path.get(0) == result.source();
        assert path.get(path.size() - 1) == target;
        boolean[] visited = new boolean[graph.vertexCount()];
        long weight = 0;
        for (int index = 0; index < path.size(); index++) {
            int vertex = path.get(index);
            assert !visited[vertex] : "Path contains a cycle";
            visited[vertex] = true;
            if (index > 0) {
                int previous = path.get(index - 1);
                int cost = graph.neighbors(previous).stream().filter(edge -> edge.to() == vertex)
                        .mapToInt(Edge::weight).min().orElseThrow();
                weight += cost;
            }
        }
        assert weight == result.distanceTo(target) : "Path weight does not match distance";
    }

    private static void expectInvalid(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }

    private static void run(String name, Runnable test) {
        test.run();
        passed++;
        System.out.println("PASS: " + name);
    }
}
