package connectivity;

import java.util.List;

/** An immutable partition of vertices into strongly connected components. */
public record SccResult(List<List<Integer>> components) {
    public SccResult {
        components = components.stream().map(List::copyOf).toList();
    }

    public int componentCount() {
        return components.size();
    }
}
