package connectivity;

import graph.Edge;
import graph.WeightedGraph;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class KosarajuTest {
    private static int passed;

    public static void main(String[] args) {
        if (!KosarajuTest.class.desiredAssertionStatus()) {
            throw new IllegalStateException("Tests require assertions: run with -ea");
        }
        run("known network has four strongly connected groups", KosarajuTest::knownGraph);
        run("a directed cycle is one strongly connected component", KosarajuTest::oneComponent);
        run("a DAG has one component per vertex", KosarajuTest::dag);
        run("isolated vertices are included exactly once", KosarajuTest::isolatedVertices);
        run("empty and single-vertex graphs are supported", KosarajuTest::boundaryGraphs);
        run("self-loops, duplicate edges, and weights do not change groups", KosarajuTest::duplicateEdges);
        run("a one-way bridge does not merge two SCCs", KosarajuTest::oneWayBridge);
        run("reversing every edge preserves the SCC partition", KosarajuTest::transpose);
        run("input and nested result collections are preserved", KosarajuTest::immutableResult);
        run("undirected graphs are rejected", KosarajuTest::undirectedGraph);
        run("deep chains and large cycles use explicit stacks", KosarajuTest::longChain);
        run("random partitions match independent mutual reachability", KosarajuTest::randomGraphs);
        System.out.println("KosarajuTest: " + passed + " tests passed");
    }

    private static WeightedGraph knownNetwork() {
        WeightedGraph graph = new WeightedGraph(8, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 0, 1);
        graph.addEdge(2, 3, 1);
        graph.addEdge(3, 4, 1);
        graph.addEdge(4, 3, 1);
        graph.addEdge(4, 5, 1);
        graph.addEdge(5, 6, 1);
        graph.addEdge(6, 7, 1);
        graph.addEdge(7, 6, 1);
        return graph;
    }

    private static void knownGraph() {
        WeightedGraph graph = knownNetwork();
        SccResult result = Kosaraju.compute(graph);
        assert groups(result).equals(Set.of(Set.of(0, 1, 2), Set.of(3, 4), Set.of(5), Set.of(6, 7)));
        assertPartition(graph, result);
    }

    private static void oneComponent() {
        WeightedGraph graph = new WeightedGraph(6, true);
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            graph.addEdge(vertex, (vertex + 1) % graph.vertexCount(), 1);
        }
        SccResult result = Kosaraju.compute(graph);
        assert result.componentCount() == 1;
        assertPartition(graph, result);
    }

    private static void dag() {
        WeightedGraph graph = new WeightedGraph(5, true);
        graph.addEdge(0, 2, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 3, 1);
        graph.addEdge(3, 4, 1);
        SccResult result = Kosaraju.compute(graph);
        assert result.componentCount() == 5;
        assertPartition(graph, result);
    }

    private static void isolatedVertices() {
        WeightedGraph graph = new WeightedGraph(4, true);
        SccResult result = Kosaraju.compute(graph);
        assert groups(result).equals(Set.of(Set.of(0), Set.of(1), Set.of(2), Set.of(3)));
        assertPartition(graph, result);
    }

    private static void boundaryGraphs() {
        for (int count : new int[] {0, 1}) {
            WeightedGraph graph = new WeightedGraph(count, true);
            SccResult result = Kosaraju.compute(graph);
            assert result.componentCount() == count;
            assertPartition(graph, result);
        }
    }

    private static void duplicateEdges() {
        WeightedGraph graph = new WeightedGraph(4, true);
        graph.addEdge(0, 1, -10);
        graph.addEdge(0, 1, 20);
        graph.addEdge(1, 0, 0);
        graph.addEdge(1, 1, -3);
        graph.addEdge(2, 2, 7);
        SccResult result = Kosaraju.compute(graph);
        assert groups(result).equals(Set.of(Set.of(0, 1), Set.of(2), Set.of(3)));
        assertPartition(graph, result);
    }

    private static void oneWayBridge() {
        WeightedGraph graph = new WeightedGraph(4, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 0, 1);
        graph.addEdge(2, 3, 1);
        graph.addEdge(3, 2, 1);
        graph.addEdge(1, 2, 1);
        SccResult result = Kosaraju.compute(graph);
        assert groups(result).equals(Set.of(Set.of(0, 1), Set.of(2, 3)));
        assertPartition(graph, result);
    }

    private static void transpose() {
        WeightedGraph graph = knownNetwork();
        WeightedGraph reversed = new WeightedGraph(graph.vertexCount(), true);
        for (Edge edge : graph.edges()) {
            reversed.addEdge(edge.to(), edge.from(), edge.weight());
        }
        SccResult result = Kosaraju.compute(reversed);
        assert groups(result).equals(groups(Kosaraju.compute(graph)));
        assertPartition(reversed, result);
    }

    private static void immutableResult() {
        WeightedGraph graph = knownNetwork();
        List<Edge> original = List.copyOf(graph.edges());
        SccResult result = Kosaraju.compute(graph);
        assert graph.edges().equals(original);
        assert groups(Kosaraju.compute(graph)).equals(groups(result));
        expectImmutable(() -> result.components().clear());
        expectImmutable(() -> result.components().get(0).clear());
        graph.addEdge(7, 0, 1);
        assert Kosaraju.compute(graph).componentCount() == 1;
        assert result.componentCount() == 4;
        assert groups(result).equals(Set.of(Set.of(0, 1, 2), Set.of(3, 4), Set.of(5), Set.of(6, 7)));
    }

    private static void undirectedGraph() {
        try {
            Kosaraju.compute(new WeightedGraph(1, false));
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected undirected graph rejection");
    }

    private static void longChain() {
        WeightedGraph graph = new WeightedGraph(10_000, true);
        for (int vertex = 1; vertex < graph.vertexCount(); vertex++) {
            graph.addEdge(vertex - 1, vertex, 1);
        }
        SccResult chain = Kosaraju.compute(graph);
        assert chain.componentCount() == graph.vertexCount();
        assertPartition(graph, chain);
        graph.addEdge(graph.vertexCount() - 1, 0, 1);
        SccResult cycle = Kosaraju.compute(graph);
        assert cycle.componentCount() == 1;
        assert cycle.components().get(0).size() == graph.vertexCount();
        assertPartition(graph, cycle);
    }

    private static void randomGraphs() {
        Random random = new Random(27);
        for (int sample = 0; sample < 80; sample++) {
            int count = 1 + random.nextInt(9);
            WeightedGraph graph = new WeightedGraph(count, true);
            boolean[][] reachable = new boolean[count][count];
            for (int from = 0; from < count; from++) {
                reachable[from][from] = true;
                for (int to = 0; to < count; to++) {
                    if (random.nextInt(5) == 0) {
                        graph.addEdge(from, to, random.nextInt(11) - 5);
                        reachable[from][to] = true;
                    }
                }
            }
            for (int via = 0; via < count; via++) {
                for (int from = 0; from < count; from++) {
                    for (int to = 0; to < count; to++) {
                        reachable[from][to] |= reachable[from][via] && reachable[via][to];
                    }
                }
            }
            SccResult result = Kosaraju.compute(graph);
            int[] membership = assertPartition(graph, result);
            for (int first = 0; first < count; first++) {
                for (int second = 0; second < count; second++) {
                    assert (membership[first] == membership[second])
                            == (reachable[first][second] && reachable[second][first]);
                }
            }
            boolean acyclic = result.components().stream().allMatch(component -> component.size() == 1)
                    && graph.edges().stream().noneMatch(edge -> edge.from() == edge.to());
            assert TopologicalSort.compute(graph).acyclic() == acyclic;
        }
    }

    private static int[] assertPartition(WeightedGraph graph, SccResult result) {
        int[] membership = new int[graph.vertexCount()];
        Arrays.fill(membership, -1);
        for (int index = 0; index < result.components().size(); index++) {
            List<Integer> component = result.components().get(index);
            assert !component.isEmpty();
            for (int vertex : component) {
                assert vertex >= 0 && vertex < graph.vertexCount();
                assert membership[vertex] == -1 : "Vertex belongs to multiple components";
                membership[vertex] = index;
            }
        }
        for (int component : membership) {
            assert component >= 0 : "Missing vertex";
        }
        assert result.componentCount() == result.components().size();
        return membership;
    }

    private static Set<Set<Integer>> groups(SccResult result) {
        Set<Set<Integer>> groups = new HashSet<>();
        for (List<Integer> component : result.components()) {
            groups.add(Set.copyOf(component));
        }
        return groups;
    }

    private static void expectImmutable(Runnable action) {
        try {
            action.run();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("Result collections must be immutable");
    }

    private static void run(String name, Runnable test) {
        test.run();
        passed++;
        System.out.println("PASS: " + name);
    }
}
