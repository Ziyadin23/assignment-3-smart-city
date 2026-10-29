package benchmark;

import connectivity.SccResult;
import connectivity.TopologicalResult;
import dsu.DisjointSetUnion;
import graph.Edge;
import graph.WeightedGraph;
import mst.MstResult;
import shortestpath.ShortestPathResult;

import java.util.ArrayDeque;
import java.util.Arrays;

/** Correctness checks performed outside measured intervals. */
final class ResultChecks {
    private ResultChecks() {
    }

    static void mst(WeightedGraph graph, MstResult result, long expectedWeight) {
        require(result.connected() && result.componentCount() == 1, "MST connectivity mismatch");
        require(result.edges().size() == graph.vertexCount() - 1, "MST edge count mismatch");
        DisjointSetUnion sets = new DisjointSetUnion(graph.vertexCount());
        long weight = 0;
        for (Edge edge : result.edges()) {
            require(graph.neighbors(edge.from()).contains(edge), "Selected road is absent");
            require(sets.union(edge.from(), edge.to()), "Selected roads form a cycle");
            weight += edge.weight();
        }
        require(sets.componentCount() == 1 && weight == result.totalWeight()
                && weight == expectedWeight, "MST weight mismatch");
    }

    static void topological(WeightedGraph graph, TopologicalResult result, boolean expectedDag) {
        require(result.acyclic() == expectedDag, "Cycle detection mismatch");
        if (!expectedDag) {
            require(result.order().isEmpty(), "Cyclic result must not expose a partial order");
            return;
        }
        require(result.order().size() == graph.vertexCount(), "Missing ordered vertices");
        int[] positions = new int[graph.vertexCount()];
        Arrays.fill(positions, -1);
        for (int index = 0; index < result.order().size(); index++) {
            int vertex = result.order().get(index);
            require(vertex >= 0 && vertex < positions.length && positions[vertex] == -1,
                    "Invalid or duplicate ordered vertex");
            positions[vertex] = index;
        }
        for (Edge edge : graph.edges()) {
            require(positions[edge.from()] < positions[edge.to()], "Invalid topological order");
        }
    }

    static int[] sccReference(WeightedGraph graph) {
        int count = graph.vertexCount();
        boolean[][] reachable = new boolean[count][count];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int source = 0; source < count; source++) {
            reachable[source][source] = true;
            queue.add(source);
            while (!queue.isEmpty()) {
                int vertex = queue.remove();
                for (Edge edge : graph.neighbors(vertex)) {
                    if (!reachable[source][edge.to()]) {
                        reachable[source][edge.to()] = true;
                        queue.add(edge.to());
                    }
                }
            }
        }
        int[] labels = new int[count];
        Arrays.fill(labels, -1);
        int nextLabel = 0;
        for (int source = 0; source < count; source++) {
            if (labels[source] != -1) continue;
            for (int vertex = 0; vertex < count; vertex++) {
                if (reachable[source][vertex] && reachable[vertex][source]) labels[vertex] = nextLabel;
            }
            nextLabel++;
        }
        return labels;
    }

    static void scc(WeightedGraph graph, SccResult result, int[] expected) {
        int[] actual = new int[graph.vertexCount()];
        Arrays.fill(actual, -1);
        int[] expectedToActual = new int[graph.vertexCount()];
        int[] actualToExpected = new int[graph.vertexCount()];
        Arrays.fill(expectedToActual, -1);
        Arrays.fill(actualToExpected, -1);
        for (int index = 0; index < result.components().size(); index++) {
            require(!result.components().get(index).isEmpty(), "Empty SCC");
            for (int vertex : result.components().get(index)) {
                require(vertex >= 0 && vertex < actual.length && actual[vertex] == -1, "Duplicate/invalid SCC vertex");
                actual[vertex] = index;
                int label = expected[vertex];
                require(expectedToActual[label] == -1 || expectedToActual[label] == index, "SCC incorrectly split");
                require(actualToExpected[index] == -1 || actualToExpected[index] == label, "SCC incorrectly merged");
                expectedToActual[label] = index;
                actualToExpected[index] = label;
            }
        }
        for (int label : actual) require(label != -1, "Missing SCC vertex");
    }

    static long[] distanceReference(WeightedGraph graph, int source) {
        long[] distances = new long[graph.vertexCount()];
        Arrays.fill(distances, ShortestPathResult.UNREACHABLE);
        distances[source] = 0;
        for (int pass = 1; pass < graph.vertexCount(); pass++) {
            boolean changed = false;
            for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
                if (distances[vertex] == ShortestPathResult.UNREACHABLE) continue;
                for (Edge edge : graph.neighbors(vertex)) {
                    long candidate = distances[vertex] + edge.weight();
                    if (candidate < distances[edge.to()]) {
                        distances[edge.to()] = candidate;
                        changed = true;
                    }
                }
            }
            if (!changed) break;
        }
        return distances;
    }

    static void distances(ShortestPathResult result, long[] expected) {
        require(result.vertexCount() == expected.length && result.source() == 0, "Distance result metadata mismatch");
        for (int vertex = 0; vertex < expected.length; vertex++) {
            require(result.distanceTo(vertex) == expected[vertex], "Incorrect shortest distance");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
