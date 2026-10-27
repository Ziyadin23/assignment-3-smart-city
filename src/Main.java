import graph.Edge;
import graph.WeightedGraph;
import mst.Kruskal;
import mst.MstResult;
import mst.Prim;

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
    }
}
