package metrics;

import connectivity.Kosaraju;
import connectivity.TopologicalSort;
import dsu.DisjointSetUnion;
import graph.WeightedGraph;
import mst.Kruskal;
import mst.Prim;
import shortestpath.Dijkstra;
import shortestpath.ShortestPathResult;

import static metrics.OperationCounts.Operation.*;

public final class OperationCountsTest {
    private static int passed;

    public static void main(String[] args) {
        if (!OperationCountsTest.class.desiredAssertionStatus()) throw new IllegalStateException("Tests require -ea");
        run("counter instances and snapshots are independent", OperationCountsTest::snapshots);
        run("DSU counts finds, unions, parent traversals, and writes", OperationCountsTest::dsu);
        run("Kruskal counts examined edges, comparisons, and DSU work", OperationCountsTest::kruskal);
        run("Prim counts adjacency and heap operations", OperationCountsTest::prim);
        run("topological counters distinguish DAG and cycle processing", OperationCountsTest::topological);
        run("SCC counts both DFS passes and transpose construction", OperationCountsTest::scc);
        run("Dijkstra counts validation, relaxation, and stale-state removals", OperationCountsTest::dijkstra);
        System.out.println("OperationCountsTest: " + passed + " tests passed");
    }

    private static void snapshots() {
        OperationCounts first = new OperationCounts();
        first.increment(EDGE_EXAMINATIONS);
        long[] snapshot = first.snapshot();
        snapshot[EDGE_EXAMINATIONS.ordinal()] = 99;
        assert first.get(EDGE_EXAMINATIONS) == 1;
        assert new OperationCounts().get(EDGE_EXAMINATIONS) == 0;
    }

    private static void dsu() {
        OperationCounts counts = new OperationCounts();
        DisjointSetUnion sets = new DisjointSetUnion(3, counts);
        assert sets.union(0, 1);
        assert sets.union(1, 2);
        assert sets.find(2) == sets.find(0);
        assert counts.get(UNION_CALLS) == 2;
        assert counts.get(FIND_CALLS) == 6;
        assert counts.get(PARENT_TRAVERSALS) == 4;
        assert counts.get(PARENT_WRITES) == 4;
        assert sets.componentCount() == 1;
    }

    private static WeightedGraph triangle() {
        WeightedGraph graph = new WeightedGraph(3, false);
        graph.addEdge(0, 1, 3);
        graph.addEdge(1, 2, 1);
        graph.addEdge(0, 2, 2);
        return graph;
    }

    private static void kruskal() {
        WeightedGraph graph = triangle();
        OperationCounts counts = new OperationCounts();
        assert Kruskal.compute(graph, counts).equals(Kruskal.compute(graph));
        assert counts.get(EDGE_EXAMINATIONS) == 2;
        assert counts.get(UNION_CALLS) == 2;
        assert counts.get(FIND_CALLS) == 4;
        assert counts.get(KEY_COMPARISONS) > 0;
        assert counts.get(FRONTIER_ADDS) == 0;
    }

    private static void prim() {
        WeightedGraph graph = triangle();
        OperationCounts counts = new OperationCounts();
        assert Prim.compute(graph, counts).equals(Prim.compute(graph));
        assert counts.get(EDGE_EXAMINATIONS) == 6;
        assert counts.get(FRONTIER_ADDS) == 3;
        assert counts.get(FRONTIER_REMOVES) == 3;
        assert counts.get(KEY_COMPARISONS) > 0;
        assert counts.get(FIND_CALLS) == 0;
    }

    private static void topological() {
        WeightedGraph graph = new WeightedGraph(3, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 2, 1);
        OperationCounts counts = new OperationCounts();
        assert TopologicalSort.compute(graph, counts).equals(TopologicalSort.compute(graph));
        assert counts.get(EDGE_EXAMINATIONS) == 4;
        assert counts.get(FRONTIER_ADDS) == 3 && counts.get(FRONTIER_REMOVES) == 3;
        graph.addEdge(2, 0, 1);
        OperationCounts cyclic = new OperationCounts();
        assert !TopologicalSort.compute(graph, cyclic).acyclic();
        assert cyclic.get(EDGE_EXAMINATIONS) == 3;
        assert cyclic.get(FRONTIER_ADDS) == 0;
    }

    private static void scc() {
        WeightedGraph graph = new WeightedGraph(4, true);
        graph.addEdge(0, 1, 1);
        graph.addEdge(1, 0, 1);
        graph.addEdge(1, 2, 1);
        OperationCounts counts = new OperationCounts();
        assert Kosaraju.compute(graph, counts).equals(Kosaraju.compute(graph));
        assert counts.get(EDGE_EXAMINATIONS) == 9;
        assert counts.get(FRONTIER_ADDS) == 8 && counts.get(FRONTIER_REMOVES) == 8;
    }

    private static void dijkstra() {
        WeightedGraph graph = new WeightedGraph(4, true);
        graph.addEdge(0, 1, 5);
        graph.addEdge(0, 2, 1);
        graph.addEdge(2, 1, 1);
        graph.addEdge(1, 3, 1);
        OperationCounts counts = new OperationCounts();
        ShortestPathResult counted = Dijkstra.compute(graph, 0, counts);
        ShortestPathResult ordinary = Dijkstra.compute(graph, 0);
        for (int vertex = 0; vertex < 4; vertex++) {
            assert counted.distanceTo(vertex) == ordinary.distanceTo(vertex);
            assert counted.pathTo(vertex).equals(ordinary.pathTo(vertex));
        }
        assert counts.get(EDGE_EXAMINATIONS) == 8;
        assert counts.get(RELAXATION_ATTEMPTS) == 4;
        assert counts.get(SUCCESSFUL_RELAXATIONS) == 4;
        assert counts.get(FRONTIER_ADDS) == 5 && counts.get(FRONTIER_REMOVES) == 5;
        assert counts.get(KEY_COMPARISONS) > 0;
    }

    private static void run(String name, Runnable test) {
        test.run();
        passed++;
        System.out.println("PASS: " + name);
    }
}
