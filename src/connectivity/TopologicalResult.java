package connectivity;

import java.util.List;

/** A complete topological order, or an empty order when a cycle prevents one. */
public record TopologicalResult(List<Integer> order, boolean acyclic) {
    public TopologicalResult {
        order = List.copyOf(order);
    }
}
