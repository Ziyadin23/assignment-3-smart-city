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

The demo prints a four-district undirected network, then uses Kruskal to select roads with a minimum total construction cost of **17**. It reports connectivity and component count, runs Prim on the same network, and checks that both MST weights agree. It also orders directed service dependencies, detects a cycle after adding a back edge, and reports three strongly connected groups: `{0}`, `{1}`, and `{2, 3, 4}`. Group/member order is not significant. Compiled classes are written to the ignored `build/` directory.

## Implementation

The graph representation, disjoint set union structure, Kruskal, Prim, topological ordering, and Kosaraju SCC are implemented. Dijkstra's shortest-path implementation remains in progress.

```text
src/
  Main.java
  graph/          Weighted graph and edge representation
  dsu/            Disjoint sets with path compression and union by size
  mst/            Kruskal, Prim, and immutable MST results
  connectivity/   Kahn topological ordering and Kosaraju SCC
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

`Prim.compute(graph)` grows a tree from an unvisited vertex using a binary-heap priority queue of frontier edges. Each visit marks its vertex and adds outgoing edges to unvisited neighbors. The cheapest queued edge extends the tree unless its destination has already been visited; such stale entries are discarded. When the queue empties, the algorithm restarts at another unvisited vertex, so disconnected graphs include every component and isolated vertex. It returns the same `MstResult` type and follows the same input and empty-graph conventions as Kruskal. Selected undirected edges may use the reverse orientation of the original logical edge.

`TopologicalSort.compute(graph)` implements Kahn's algorithm for directed graphs. It counts incoming edges, queues all zero-indegree vertices, and removes each queued vertex while decrementing its outgoing neighbors' indegrees. If every vertex is processed, `TopologicalResult` contains a complete immutable order with `acyclic() == true`. Otherwise the graph has a cycle and the result has `acyclic() == false` and an empty order, so a processable prefix cannot be mistaken for a complete answer. Disconnected DAGs include all isolated vertices. Empty directed graphs have a valid empty order, duplicate edges are counted individually, and edge weights do not affect ordering. Undirected input is rejected.

`Kosaraju.compute(graph)` finds every strongly connected component of a directed graph. The first depth-first pass records each vertex after all its outgoing edges have been examined. The algorithm then builds a transpose by reversing every edge and starts traversals there in decreasing finishing order. Each second-pass traversal collects exactly one SCC. Both passes use explicit stacks, including saved edge iterators for first-pass finishing order, avoiding dependence on the Java call-stack depth.

`SccResult` holds an immutable outer list of components and an immutable list of members for each component. Every vertex belongs to exactly one component, including isolated vertices; an empty directed graph has no components. Edge weights do not affect reachability, and duplicate edges or self-loops are supported. Undirected input is rejected. Component order and member order are traversal details rather than a ranking of districts. Vertices share an SCC only when each can reach the other; a one-way road between two SCCs does not merge them.

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

This implementation uses **lazy edge-based Prim**, without decrease-key: at most E logical edges enter the queue, and all adjacency entries are examined once. Its time is **O(V + E log(E + 1))** and additional space is **O(V + E)**, including the visited array, selected edges, and a queue that can hold O(E) entries. For connected simple graphs this is commonly written as O(E log V), since E is at most quadratic in V. The E-based bound also describes graphs with parallel edges. Larger E increases heap work and memory; larger V increases visited-array initialization and the scan for new components.

Topological ordering takes **O(V + E)** time and **O(V)** additional space with adjacency lists and an array-backed FIFO queue. Counting indegrees and scanning outgoing edges each take O(E); initialization, queue operations, and result construction take O(V). Both DAG and cyclic inputs have the same worst-case bound; a cycle may leave part of the graph unprocessed, but all incoming edges are still counted. Increasing either vertices or edges increases the work linearly.

Kosaraju takes **O(V + E)** time and **O(V + E)** additional space with adjacency lists. Each DFS pass visits every vertex and examines its outgoing edges once, and building the transpose also takes O(V + E). The transpose stores V adjacency lists and E reversed edges. Visited arrays, finishing order, explicit DFS stacks, and the output partition use O(V) space. More edges increase traversal work and transpose storage linearly; more vertices increase all vertex-indexed structures and the number of DFS starts in disconnected graphs.

Dijkstra's complexity analysis will be added alongside its implementation.

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

Kruskal orders all logical edges globally and uses DSU to avoid cycles. Prim grows one tree at a time using the adjacency lists and a heap, discarding frontier edges that have become obsolete. Both implementations produce the same minimum total weight and component count; selected edges can differ when weights tie. Both support disconnected networks by returning minimum spanning forests. The lazy Prim implementation can store O(E) queue entries, so a smaller heap or faster execution should not be assumed without measurements.

Timing comparisons and interpretation will follow the measured results. They will address Kruskal versus Prim, DSU optimizations, scaling of SCC and Dijkstra, topological ordering with and without cycles, and theoretical versus observed behavior.

## Testing

Run `bash scripts/test.sh`. It compiles the source and test classes, then runs each `*Test.java` entry point with Java assertions enabled (`-ea`). A compilation error or failed assertion stops the script with a nonzero exit status. The test runner uses only the JDK, keeping checks reproducible without downloading a testing framework.

Graph tests cover directed edges, symmetric undirected connections, isolated and single vertices, an empty graph, parallel edges, duplicate weights, self-loops, negative weights, invalid endpoints, and collection access. Algorithm correctness tests will be added alongside their implementations.

DSU tests cover separate components, transitive connectivity, repeated and self unions, component sizes and counts, union by size in both argument orders, repeated finds over a balanced merge sequence of 1,024 elements, empty/single-element sets, and invalid inputs. A fixed-seed test compares 150 unions on 24 elements against an independent reference that tracks components by relabeling every member; it checks every pair's connectivity and every component size after each merge.

Kruskal tests cover the hand-calculated cost-17 example, disconnected graphs, isolated vertices, single/empty graphs, multiple valid MSTs with equal weights, parallel edges, self-loops, negative and zero weights, extreme integer weights, directed-input rejection, and immutable results. They check selected-edge membership, summed weight, connectivity, and the forest edge count using an independent breadth-first traversal. A fixed-seed check compares 60 graphs of up to five vertices with an exhaustive search over edge subsets, verifying that the returned forest has minimum possible weight. Repeated runs preserve the graph's original edge order, and previously returned results remain unchanged after graph updates.

Prim tests cover the same MST conventions, stale heap entries, reverse edge orientations, negative/extreme weights, parallel roads, self-loops, immutable results, and directed-input rejection. A fixed-seed comparison checks 100 graphs: 50 guaranteed connected and 50 guaranteed disconnected. Each result must agree with Kruskal's weight and component count, use real input roads, and form a spanning forest under an independent breadth-first connectivity check.

Topological tests check vertex uniqueness, full coverage, and source-before-destination positions for every edge. Cases include disconnected DAGs, isolated vertices, empty/single-vertex graphs, a 1,000-vertex dependency chain, directed cycles, cycles alongside processable vertices, self-loops, duplicate edges, and immutable results. A fixed-seed test creates 80 DAGs using a shuffled hidden vertex order, verifies each returned order, then injects a two-vertex cycle and checks rejection. Tests accept any valid ordering rather than requiring one arbitrary tie order.

SCC tests cover a known four-component graph, one large SCC, DAGs, isolated vertices, empty/single-vertex graphs, one-way bridges, self-loops, duplicate edges, transpose invariance, and immutable nested results. Every partition is checked for complete coverage and exactly one membership per vertex. A 10,000-vertex chain and its cycle-closing variant exercise both iterative passes without recursion. For 80 fixed-seed random directed graphs, an independent transitive-closure matrix checks that two vertices share a component exactly when they are mutually reachable. The same cases cross-check topological cycle detection: a DAG has only singleton SCCs and no self-loops.

## Conclusion

Kruskal and Prim find minimum-cost infrastructure networks and report when districts cannot all be connected. Their MST weights agree on the correctness tests. Topological ordering schedules directed dependencies and detects cycles that prevent a valid schedule. Kosaraju identifies mutually reachable groups even when cycles exist. Conclusions comparing performance will be written after the remaining implementation and experiments are complete.
