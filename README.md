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

The demo prints a four-district undirected network, then uses Kruskal to select roads with a minimum total construction cost of **17**. It also reports whether all districts are connected and the number of remaining components. Compiled classes are written to the ignored `build/` directory.

## Implementation

The graph representation, disjoint set union structure, and Kruskal's algorithm are implemented. The remaining algorithm implementations are in progress.

```text
src/
  Main.java
  graph/          Weighted graph and edge representation
  dsu/            Disjoint sets with path compression and union by size
  mst/            Kruskal and immutable MST results; Prim is planned
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

`DisjointSetUnion` maintains a partition of element IDs from `0` to `V - 1` using parent and component-size arrays. Initially each element is its own component. `find` first walks to the root and then redirects every visited element directly to that root (path compression). Both passes are iterative, so no recursion stack is needed. `union` attaches the smaller component's root to the larger component's root, updates its size, and decreases the component count. It returns `true` only when two different components were merged; repeated or self unions return `false`. Kruskal uses this result to reject edges whose endpoints are already connected.

The DSU also provides `connected`, `componentSize`, and `componentCount`. Representatives identify components and may change after a union; their numeric IDs do not indicate component size or ordering. Negative element counts and out-of-range element IDs are rejected. An empty DSU has zero components, and a single element starts in a component of size one.

`Kruskal.compute(graph)` copies the logical edge list, sorts it by ascending weight, and accepts an edge only when DSU can merge its endpoint components. This avoids cycles and naturally excludes self-loops. Once the graph has one component, the algorithm can stop. The input graph and its edge order are preserved. Directed graphs are rejected; parallel edges, duplicate weights, and negative weights are supported. Weight comparisons avoid integer subtraction, and the total is accumulated in a `long` so that sums of integer edge weights do not overflow an `int`.

`MstResult` contains an immutable snapshot of the selected edges, their total weight, and the remaining component count. For a connected graph, this is an MST with `V - 1` edges. For a disconnected graph, it is a minimum spanning forest with `V - C` edges, where `C` is the component count, and `connected()` returns `false`. A single vertex is connected with zero edges and cost zero. An empty graph has zero components, zero cost, and `connected() == false` by this implementation's convention. Different MSTs can contain different edges when weights tie; their minimum total weight is the quantity to compare.

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

Kruskal copies and sorts **E** logical edges, initializes DSU for **V** vertices, and makes at most **E** union attempts. Its time is **O(V + E log(E + 1) + E α(V))**, including DSU initialization, sorting, and edge processing. For a connected graph with at least two vertices, this is usually written as **O(E log E)**. The full expression also accounts for graphs with many isolated vertices or no edges. Increasing E adds sorting and union work; increasing V adds DSU initialization and storage. The copied edge list, sort workspace, DSU arrays, and selected edges require **O(V + E)** additional space beyond the input graph.

Prim, SCC, topological ordering, and Dijkstra analyses will be added alongside their implementations.

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

Kruskal tests cover the hand-calculated cost-17 example, disconnected graphs, isolated vertices, single/empty graphs, multiple valid MSTs with equal weights, parallel edges, self-loops, negative and zero weights, extreme integer weights, directed-input rejection, and immutable results. They check selected-edge membership, summed weight, connectivity, and the forest edge count using an independent breadth-first traversal. A fixed-seed check compares 60 graphs of up to five vertices with an exhaustive search over edge subsets, verifying that the returned forest has minimum possible weight. Repeated runs preserve the graph's original edge order, and previously returned results remain unchanged after graph updates.

## Conclusion

Kruskal now uses the graph representation and DSU to find a minimum-cost infrastructure network, reporting when the districts cannot all be connected. Conclusions comparing algorithm suitability and performance will be written after the remaining implementations and experiments are complete.
