import graph.Edge;
import graph.WeightedGraph;

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
    }
}
