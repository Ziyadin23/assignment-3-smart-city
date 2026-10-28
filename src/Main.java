import connectivity.Kosaraju;
import connectivity.SccResult;
import connectivity.TopologicalResult;
import connectivity.TopologicalSort;
import graph.Edge;
import graph.WeightedGraph;
import mst.Kruskal;
import mst.MstResult;
import mst.Prim;
import shortestpath.Dijkstra;
import shortestpath.ShortestPathResult;

public class Main {
    public static void main(String[] args) {
        WeightedGraph city = new WeightedGraph(4, false);
        city.addEdge(0, 1, 7);
        city.addEdge(1, 2, 4);
        city.addEdge(2, 3, 6);
        city.addEdge(0, 3, 12);

        System.out.printf("City network: %d districts, %d roads%n",
                city.vertexCount(), city.edgeCount());
        for (Edge road : city.edges()) {
            System.out.printf("District %d <-> District %d: weight %d%n",
                    road.from(), road.to(), road.weight());
        }

        MstResult network = Kruskal.compute(city);
        System.out.println("\nKruskal infrastructure plan:");
        for (Edge road : network.edges()) {
            System.out.printf("Build road %d <-> %d: cost %d%n",
                    road.from(), road.to(), road.weight());
        }
        System.out.printf("Total cost: %d%nConnected: %s%nComponents: %d%n",
                network.totalWeight(), network.connected(), network.componentCount());

        MstResult primNetwork = Prim.compute(city);
        System.out.printf("%nPrim total cost: %d%nMST weights agree: %s%n",
                primNetwork.totalWeight(), network.totalWeight() == primNetwork.totalWeight());

        WeightedGraph dependencies = new WeightedGraph(5, true);
        dependencies.addEdge(0, 2, 1);
        dependencies.addEdge(1, 2, 1);
        dependencies.addEdge(2, 3, 1);
        dependencies.addEdge(3, 4, 1);
        TopologicalResult ordering = TopologicalSort.compute(dependencies);
        System.out.printf("%nService dependency order: %s%n", ordering.order());
        dependencies.addEdge(4, 2, 1);
        System.out.printf("Cycle detected after adding dependency 4 -> 2: %s%n",
                !TopologicalSort.compute(dependencies).acyclic());
        SccResult groups = Kosaraju.compute(dependencies);
        System.out.printf("Strongly connected components (%d): %s%n",
                groups.componentCount(), groups.components());

        WeightedGraph routes = new WeightedGraph(5, true);
        routes.addEdge(0, 1, 7);
        routes.addEdge(0, 2, 2);
        routes.addEdge(2, 1, 1);
        routes.addEdge(1, 3, 3);
        routes.addEdge(2, 3, 8);
        ShortestPathResult paths = Dijkstra.compute(routes, 0);
        System.out.println("\nShortest routes from district 0:");
        for (int vertex = 0; vertex < paths.vertexCount(); vertex++) {
            if (paths.reachable(vertex)) {
                System.out.printf("District %d: distance %d, path %s%n",
                        vertex, paths.distanceTo(vertex), paths.pathTo(vertex));
            } else {
                System.out.printf("District %d: unreachable%n", vertex);
            }
        }
    }
}
