# Smart City Infrastructure and Route Planning

Java implementation and experimental analysis of graph algorithms for Assignment 3.

## Overview

The city is a weighted graph: vertices represent districts and edges represent roads or infrastructure connections. Edge weights represent construction costs for infrastructure planning or distances for route planning.

The assignment has three tasks:

1. Minimum spanning trees using Kruskal, Prim, and a disjoint set union structure.
2. Directed connectivity using topological ordering and strongly connected components.
3. Shortest paths using Dijkstra's algorithm.

## Build and run

Requirements: a **JDK 17 or newer** and **Bash**. There are no external Java dependencies; Maven and Gradle are not required.

From the repository root:

```bash
bash scripts/build.sh
bash scripts/run.sh
bash scripts/test.sh
```

The current demo prints a four-district undirected network. Algorithm demonstrations will be added with their implementations. Compiled classes are written to the ignored `build/` directory.

## Implementation

The graph representation and disjoint set union structure are implemented. The remaining algorithm implementations are in progress.

```text
src/
  Main.java
  graph/          Weighted graph and edge representation
  dsu/            Disjoint sets with path compression and union by size
  mst/            Kruskal and Prim (planned)
  connectivity/   Topological ordering and SCC (planned)
  shortestpath/   Dijkstra (planned)
tests/            Java assertion-based correctness tests
scripts/          Build, run, and test commands
results/
  tables/         Benchmark measurements (planned)
  plots/          Execution-time plots (planned)
```

`WeightedGraph` stores an adjacency list and a list of logical edges. Vertices use consecutive IDs from `0` to `V - 1`. The constructor selects directed or undirected behavior. For an undirected road, both endpoints have an outgoing adjacency entry, while `edges()` lists that road once so that Kruskal will not process it twice. A self-loop is stored once in its vertex's adjacency list. Parallel edges and duplicate weights are preserved.

`Edge` is an immutable Java record with integer endpoints and weight. The graph accepts any integer weight, since negative weights can be valid for MST algorithms; Dijkstra will enforce its own non-negative-weight requirement. Invalid vertex IDs are rejected before any change to the graph. Read methods expose unmodifiable live views, so callers can traverse connections without bypassing graph validation.

`DisjointSetUnion` maintains a partition of element IDs from `0` to `V - 1` using parent and component-size arrays. Initially each element is its own component. `find` first walks to the root and then redirects every visited element directly to that root (path compression). Both passes are iterative, so no recursion stack is needed. `union` attaches the smaller component's root to the larger component's root, updates its size, and decreases the component count. It returns `true` only when two different components were merged; repeated or self unions return `false`. This result will let Kruskal reject edges whose endpoints are already connected.

The DSU also provides `connected`, `componentSize`, and `componentCount`. Representatives identify components and may change after a union; their numeric IDs do not indicate component size or ordering. Negative element counts and out-of-range element IDs are rejected. An empty DSU has zero components, and a single element starts in a component of size one.

## Complexity Analysis

The adjacency list plus logical edge list uses **O(V + E)** space, including the reverse entries for undirected edges. Graph construction takes **O(V)** before edges are added. Adding an edge takes amortized **O(1)**; enumerating neighbors of vertex `v` takes **O(out-degree(v))**; enumerating all logical edges takes **O(E)**. Retrieving a read-only view or a vertex/edge count takes **O(1)**.

For DSU, let **V** be the number of elements:

| Operation | Time complexity |
| --- | --- |
| Construction | O(V) |
| `find`, `union` | O(α(V)) amortized per operation; O(log V) worst case for an individual operation |
| `connected`, `componentSize` | O(α(V)) amortized, since they call `find` |
| `componentCount` | O(1) |

Here **α(V)** is the inverse Ackermann function, which grows extremely slowly. Union by size alone bounds the tree height by O(log V): an element's depth can increase only when its component is attached to one at least as large, so that component's size at least doubles. Path compression flattens the visited path, making later searches cheaper. Together the optimizations give O(V + m α(V)) total time for initialization followed by **m** find/union operations. The two arrays use **O(V)** total space, with **O(1)** additional space per operation.

Kruskal, Prim, SCC, topological ordering, and Dijkstra analyses will be added alongside their implementations.

## Experimental Setup

Benchmarks will use a documented fixed random seed and these configurations:

| Graph | Vertices | Edges |
| --- | ---: | ---: |
| Small | 10 | 30 |
| Medium | 100 | 500 |
| Large | 1,000 | 5,000 |

Each applicable algorithm/configuration will have five measured repetitions using `System.nanoTime()`, with average execution times and defined operation counters. MST graphs will be connected and undirected. Directed experiments will document graph generation and include DAGs and guaranteed cycles for topological ordering. Detailed generation, warm-up, environment, and reproduction commands will be recorded when implemented.

## Results

No benchmarks have been run yet. Measured tables and plots will be added after the benchmark milestone.

## Comparison and Discussion

Performance comparisons and interpretation will follow the measured results. They will address Kruskal versus Prim, DSU optimizations, scaling of SCC and Dijkstra, topological ordering with and without cycles, and theoretical versus observed behavior.

## Testing

Run `bash scripts/test.sh`. It compiles the source and test classes, then runs each `*Test.java` entry point with Java assertions enabled (`-ea`). A compilation error or failed assertion stops the script with a nonzero exit status. The test runner uses only the JDK, keeping checks reproducible without downloading a testing framework.

Graph tests cover directed edges, symmetric undirected connections, isolated and single vertices, an empty graph, parallel edges, duplicate weights, self-loops, negative weights, invalid endpoints, and collection access. Algorithm correctness tests will be added alongside their implementations.

DSU tests cover separate components, transitive connectivity, repeated and self unions, component sizes and counts, union by size in both argument orders, repeated finds over a balanced merge sequence of 1,024 elements, empty/single-element sets, and invalid inputs. A fixed-seed test compares 150 unions on 24 elements against an independent reference that tracks components by relabeling every member; it checks every pair's connectivity and every component size after each merge.

## Conclusion

The graph representation and DSU provide the data structures needed for infrastructure planning. Conclusions about algorithm suitability and performance will be written after all implementations and experiments are complete.
