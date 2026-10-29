package metrics;

/** Per-invocation counters, collected separately from execution-time measurements. */
public final class OperationCounts {
    public enum Operation {
        EDGE_EXAMINATIONS,
        KEY_COMPARISONS,
        FIND_CALLS,
        UNION_CALLS,
        PARENT_TRAVERSALS,
        PARENT_WRITES,
        FRONTIER_ADDS,
        FRONTIER_REMOVES,
        RELAXATION_ATTEMPTS,
        SUCCESSFUL_RELAXATIONS
    }

    private final long[] values = new long[Operation.values().length];

    public void increment(Operation operation) {
        values[operation.ordinal()]++;
    }

    public long get(Operation operation) {
        return values[operation.ordinal()];
    }

    public long[] snapshot() {
        return values.clone();
    }
}
