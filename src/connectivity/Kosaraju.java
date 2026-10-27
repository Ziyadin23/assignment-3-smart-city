package connectivity;

import graph.Edge;
import graph.WeightedGraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Kosaraju {
    private Kosaraju() {
    }

    /** Finds SCCs using finishing order followed by DFS on reversed edges. */
    public static SccResult compute(WeightedGraph graph) {
        if (!graph.isDirected()) {
            throw new IllegalArgumentException("Kosaraju requires a directed graph");
        }

        List<Integer> order = finishingOrder(graph);
        List<List<Integer>> reversed = new ArrayList<>(graph.vertexCount());
        for (int vertex = 0; vertex < graph.vertexCount(); vertex++) {
            reversed.add(new ArrayList<>());
        }
        for (Edge edge : graph.edges()) {
            reversed.get(edge.to()).add(edge.from());
        }

        boolean[] visited = new boolean[graph.vertexCount()];
        ArrayDeque<Integer> stack = new ArrayDeque<>();
        List<List<Integer>> components = new ArrayList<>();
        for (int index = order.size() - 1; index >= 0; index--) {
            int start = order.get(index);
            if (visited[start]) {
                continue;
            }
            List<Integer> component = new ArrayList<>();
            visited[start] = true;
            stack.push(start);
            while (!stack.isEmpty()) {
                int vertex = stack.pop();
                component.add(vertex);
                for (int next : reversed.get(vertex)) {
                    if (!visited[next]) {
                        visited[next] = true;
                        stack.push(next);
                    }
                }
            }
            components.add(component);
        }
        return new SccResult(components);
    }

    private static List<Integer> finishingOrder(WeightedGraph graph) {
        boolean[] visited = new boolean[graph.vertexCount()];
        ArrayDeque<Frame> stack = new ArrayDeque<>();
        List<Integer> order = new ArrayList<>();
        for (int start = 0; start < graph.vertexCount(); start++) {
            if (visited[start]) {
                continue;
            }
            visited[start] = true;
            stack.push(new Frame(start, graph.neighbors(start).iterator()));
            while (!stack.isEmpty()) {
                Frame frame = stack.peek();
                if (frame.edges().hasNext()) {
                    int next = frame.edges().next().to();
                    if (!visited[next]) {
                        visited[next] = true;
                        stack.push(new Frame(next, graph.neighbors(next).iterator()));
                    }
                } else {
                    order.add(frame.vertex());
                    stack.pop();
                }
            }
        }
        return order;
    }

    private record Frame(int vertex, Iterator<Edge> edges) {
    }
}
