package dsu;

/** Disjoint sets for fixed element IDs from 0 to elementCount - 1. */
public final class DisjointSetUnion {
    private final int[] parent;
    private final int[] size;
    private int componentCount;

    public DisjointSetUnion(int elementCount) {
        if (elementCount < 0) {
            throw new IllegalArgumentException("Element count must be non-negative");
        }
        parent = new int[elementCount];
        size = new int[elementCount];
        componentCount = elementCount;
        for (int element = 0; element < elementCount; element++) {
            parent[element] = element;
            size[element] = 1;
        }
    }

    /** Returns the representative, compressing every visited node's path to it. */
    public int find(int element) {
        validateElement(element);
        int root = element;
        while (root != parent[root]) {
            root = parent[root];
        }
        while (element != root) {
            int next = parent[element];
            parent[element] = root;
            element = next;
        }
        return root;
    }

    /** Merges two components; returns false if the elements already share one. */
    public boolean union(int first, int second) {
        validateElement(first);
        validateElement(second);
        int firstRoot = find(first);
        int secondRoot = find(second);
        if (firstRoot == secondRoot) {
            return false;
        }
        if (size[firstRoot] < size[secondRoot]) {
            int temporary = firstRoot;
            firstRoot = secondRoot;
            secondRoot = temporary;
        }
        parent[secondRoot] = firstRoot;
        size[firstRoot] += size[secondRoot];
        componentCount--;
        return true;
    }

    public boolean connected(int first, int second) {
        return find(first) == find(second);
    }

    public int componentSize(int element) {
        return size[find(element)];
    }

    public int componentCount() {
        return componentCount;
    }

    private void validateElement(int element) {
        if (element < 0 || element >= parent.length) {
            throw new IllegalArgumentException("Element out of range: " + element);
        }
    }
}
