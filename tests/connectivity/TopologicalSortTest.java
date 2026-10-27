package connectivity;

import graph.Edge;
import graph.WeightedGraph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class TopologicalSortTest {
    private static int passed;

    public static void main(String[] args) {
        if (!TopologicalSortTest.class.desiredAssertionStatus()) {
            throw new IllegalStateException("Tests require assertions: run with -ea");
        }
        run("dependencies precede their destinations", TopologicalSortTest::dependencyGraph);
        run("disconnected DAGs include every isolated vertex", TopologicalSortTest::disconnectedDag);
        run("empty and edgeless graphs have valid orders", TopologicalSortTest::boundaryGraphs);
        run("directed cycles prevent ordering", TopologicalSortTest::directedCycle);
        run("a processable prefix is not returned as a valid order", TopologicalSortTest::cycleWithPrefix);
        run("self-loops are cycles", TopologicalSortTest::selfLoop);
        run("duplicate edges are counted and weights are ignored", TopologicalSortTest::duplicateEdges);
        run("undirected graphs are rejected", TopologicalSortTest::undirectedGraph);
        run("input and returned orders are preserved", TopologicalSortTest::immutableResult);
        run("large dependency chains are ordered", TopologicalSortTest::longChain);
        run("random DAGs and injected cycles are distinguished", TopologicalSortTest::randomGraphs);
        System.out.println("TopologicalSortTest: " + passed + " tests passed");
    }

    private static void dependencyGraph() {
        WeightedGraph graph = new WeightedGraph(6, true);
        graph.addEdge(4, 1, 1);
        graph.addEdge(5, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 0, 1);
        graph.addEdge(1, 3, 1);
        assertOrder(graph, TopologicalSort.compute(graph));
    }

    private static void disconnectedDag() {
        WeightedGraph graph = new WeightedGraph(8, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(3, 4, 1);
        graph.addEdge(4, 5, 1);
        assertOrder(graph, TopologicalSort.compute(graph));
    }

    private static void boundaryGraphs() {
        for (int count : new int[] {0, 1, 5}) {
            WeightedGraph graph = new WeightedGraph(count, true);
            assertOrder(graph, TopologicalSort.compute(graph));
        }
    }

    private static void directedCycle() {
        WeightedGraph graph = new WeightedGraph(3, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        graph.addEdge(2, 0, 1);
        assertCycle(graph);
    }

    private static void cycleWithPrefix() {
        WeightedGraph graph = new WeightedGraph(6, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(2, 3, 1);
        graph.addEdge(3, 4, 1);
        graph.addEdge(4, 2, 1);
        assertCycle(graph);
    }

    private static void selfLoop() {
        WeightedGraph graph = new WeightedGraph(2, true);
        graph.addEdge(1, 1, 1);
        assertCycle(graph);
    }

    private static void duplicateEdges() {
        WeightedGraph graph = new WeightedGraph(3, true);
        graph.addEdge(0, 1, -7);
        graph.addEdge(0, 1, 12);
        graph.addEdge(1, 2, 0);
        assertOrder(graph, TopologicalSort.compute(graph));
    }

    private static void undirectedGraph() {
        try {
            TopologicalSort.compute(new WeightedGraph(1, false));
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected undirected graph rejection");
    }

    private static void immutableResult() {
        WeightedGraph graph = new WeightedGraph(3, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        List<Edge> original = List.copyOf(graph.edges());
        TopologicalResult result = TopologicalSort.compute(graph);
        assertOrder(graph, result);
        assert graph.edges().equals(original);
        assert TopologicalSort.compute(graph).equals(result);
        graph.addEdge(2, 0, 1);
        assertCycle(graph);
        assert result.acyclic();
        assert result.order().size() == 3;
        try {
            result.order().clear();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("Returned order must be immutable");
    }

    private static void longChain() {
        WeightedGraph graph = new WeightedGraph(1000, true);
        for (int vertex = 1; vertex < graph.vertexCount(); vertex++) {
            graph.addEdge(vertex, vertex - 1, 1);
        }
        assertOrder(graph, TopologicalSort.compute(graph));
    }

    private static void randomGraphs() {
        Random random = new Random(26);
        for (int sample = 0; sample < 80; sample++) {
            int count = 3 + random.nextInt(30);
            List<Integer> vertices = new ArrayList<>();
            for (int vertex = 0; vertex < count; vertex++) {
                vertices.add(vertex);
            }
            Collections.shuffle(vertices, random);
            WeightedGraph graph = new WeightedGraph(count, true);
            for (int first = 0; first < count; first++) {
                for (int second = first + 1; second < count; second++) {
                    if (random.nextInt(6) == 0) {
                        graph.addEdge(vertices.get(first), vertices.get(second), 1);
                    }
                }
            }
            assertOrder(graph, TopologicalSort.compute(graph));
            graph.addEdge(vertices.get(0), vertices.get(1), 1);
            graph.addEdge(vertices.get(1), vertices.get(0), 1);
            assertCycle(graph);
        }
    }

    private static void assertOrder(WeightedGraph graph, TopologicalResult result) {
        assert result.acyclic();
        assert result.order().size() == graph.vertexCount();
        int[] positions = new int[graph.vertexCount()];
        Arrays.fill(positions, -1);
        for (int index = 0; index < result.order().size(); index++) {
            int vertex = result.order().get(index);
            assert vertex >= 0 && vertex < graph.vertexCount();
            assert positions[vertex] == -1 : "Duplicate vertex";
            positions[vertex] = index;
        }
        for (Edge edge : graph.edges()) {
            assert positions[edge.from()] < positions[edge.to()] : "Dependency ordered backwards";
        }
    }

    private static void assertCycle(WeightedGraph graph) {
        TopologicalResult result = TopologicalSort.compute(graph);
        assert !result.acyclic();
        assert result.order().isEmpty() : "A partial order is not a complete topological ordering";
    }

    private static void run(String name, Runnable test) {
        test.run();
        passed++;
        System.out.println("PASS: " + name);
    }
}
