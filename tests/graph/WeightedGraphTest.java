package graph;

import java.util.List;

public final class WeightedGraphTest {
    private static int passed;

    public static void main(String[] args) {
        if (!WeightedGraphTest.class.desiredAssertionStatus()) {
            throw new IllegalStateException("Tests require assertions: run with -ea");
        }

        run("directed edges remain one-way", WeightedGraphTest::directedEdges);
        run("undirected roads are reachable from both ends", WeightedGraphTest::undirectedEdges);
        run("single vertex has no roads", WeightedGraphTest::singleVertex);
        run("empty graph is representable", WeightedGraphTest::emptyGraph);
        run("duplicate weights and parallel edges are preserved", WeightedGraphTest::parallelEdges);
        run("self-loop is counted once", WeightedGraphTest::selfLoop);
        run("graph preserves negative weights for algorithms that allow them", WeightedGraphTest::negativeWeight);
        run("invalid endpoints do not partially insert an edge", WeightedGraphTest::invalidVertices);
        run("negative vertex count is rejected", WeightedGraphTest::invalidSize);
        run("callers cannot modify graph collections", WeightedGraphTest::readOnlyCollections);
        run("read-only views reflect later graph additions", WeightedGraphTest::liveViews);
        System.out.println("WeightedGraphTest: " + passed + " tests passed");
    }

    private static void directedEdges() {
        WeightedGraph graph = new WeightedGraph(3, true);
        graph.addEdge(0, 1, 7);
        graph.addEdge(1, 2, 4);

        assert graph.isDirected();
        assert graph.vertexCount() == 3;
        assert graph.edgeCount() == 2;
        assert graph.neighbors(0).equals(List.of(new Edge(0, 1, 7)));
        assert graph.neighbors(1).equals(List.of(new Edge(1, 2, 4)));
        assert graph.neighbors(2).isEmpty();
    }

    private static void undirectedEdges() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(0, 2, 9);

        assert !graph.isDirected();
        assert graph.edgeCount() == 1;
        assert graph.edges().equals(List.of(new Edge(0, 2, 9)));
        assert graph.neighbors(0).equals(List.of(new Edge(0, 2, 9)));
        assert graph.neighbors(2).equals(List.of(new Edge(2, 0, 9)));
        assert graph.neighbors(1).isEmpty();
    }

    private static void singleVertex() {
        WeightedGraph graph = new WeightedGraph(1, false);
        assert graph.vertexCount() == 1;
        assert graph.edgeCount() == 0;
        assert graph.neighbors(0).isEmpty();
    }

    private static void emptyGraph() {
        WeightedGraph graph = new WeightedGraph(0, true);
        assert graph.vertexCount() == 0;
        assert graph.edges().isEmpty();
        expectException(IllegalArgumentException.class, () -> graph.neighbors(0));
    }

    private static void parallelEdges() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(0, 1, 5);
        graph.addEdge(0, 1, 5);
        graph.addEdge(1, 2, 5);

        assert graph.edgeCount() == 3;
        assert graph.neighbors(0).size() == 2;
        assert graph.neighbors(1).size() == 3;
        assert graph.edges().stream().allMatch(edge -> edge.weight() == 5);
    }

    private static void selfLoop() {
        WeightedGraph graph = new WeightedGraph(1, false);
        graph.addEdge(0, 0, 3);
        assert graph.edgeCount() == 1;
        assert graph.neighbors(0).equals(List.of(new Edge(0, 0, 3)));
    }

    private static void negativeWeight() {
        WeightedGraph graph = new WeightedGraph(2, true);
        graph.addEdge(0, 1, -2);
        assert graph.edges().get(0).weight() == -2;
        assert graph.neighbors(0).get(0).weight() == -2;
    }

    private static void invalidVertices() {
        WeightedGraph graph = new WeightedGraph(2, false);
        expectException(IllegalArgumentException.class, () -> graph.addEdge(0, 2, 5));
        expectException(IllegalArgumentException.class, () -> graph.addEdge(-1, 1, 5));
        expectException(IllegalArgumentException.class, () -> graph.addEdge(2, 0, 5));
        expectException(IllegalArgumentException.class, () -> graph.addEdge(0, -1, 5));
        expectException(IllegalArgumentException.class, () -> graph.neighbors(-1));
        expectException(IllegalArgumentException.class, () -> graph.neighbors(2));
        assert graph.edgeCount() == 0;
        assert graph.neighbors(0).isEmpty();
        assert graph.neighbors(1).isEmpty();
    }

    private static void invalidSize() {
        expectException(IllegalArgumentException.class, () -> new WeightedGraph(-1, false));
    }

    private static void readOnlyCollections() {
        WeightedGraph graph = new WeightedGraph(2, false);
        graph.addEdge(0, 1, 8);
        expectException(UnsupportedOperationException.class, () -> graph.edges().clear());
        expectException(UnsupportedOperationException.class,
                () -> graph.neighbors(0).add(new Edge(0, 1, 1)));
        assert graph.edgeCount() == 1;
        assert graph.neighbors(0).equals(List.of(new Edge(0, 1, 8)));
    }

    private static void liveViews() {
        WeightedGraph graph = new WeightedGraph(2, true);
        List<Edge> edges = graph.edges();
        List<Edge> neighbors = graph.neighbors(0);
        graph.addEdge(0, 1, 6);
        assert edges.equals(List.of(new Edge(0, 1, 6)));
        assert neighbors.equals(List.of(new Edge(0, 1, 6)));
    }

    private static void run(String name, Runnable test) {
        test.run();
        passed++;
        System.out.println("PASS: " + name);
    }

    private static void expectException(Class<? extends RuntimeException> expected, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            if (expected.isInstance(exception)) {
                return;
            }
            throw new AssertionError("Expected " + expected.getSimpleName(), exception);
        }
        throw new AssertionError("Expected " + expected.getSimpleName());
    }
}
