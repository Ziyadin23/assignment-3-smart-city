package benchmark;

import connectivity.Kosaraju;
import connectivity.TopologicalSort;
import graph.Edge;
import graph.WeightedGraph;
import metrics.OperationCounts;
import metrics.OperationCounts.Operation;
import mst.Kruskal;
import mst.Prim;
import shortestpath.Dijkstra;
import shortestpath.ShortestPathResult;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

public final class BenchmarkRunner {
    public static final long BASE_SEED = 20261029L;
    public static final int WARMUP_ROUNDS = 100;
    public static final int REPETITIONS = 5;
    private static volatile Object blackhole;

    private BenchmarkRunner() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 1) throw new IllegalArgumentException("Usage: BenchmarkRunner [output-directory]");
        Path output = args.length == 1 ? Path.of(args[0]) : Path.of("results", "tables");
        List<Run> runs = new ArrayList<>();
        List<String> summaries = new ArrayList<>();
        int[][] sizes = {{10, 30}, {100, 500}, {1000, 5000}};
        for (int[] size : sizes) {
            int vertices = size[0];
            int edges = size[1];
            long seed = BASE_SEED + vertices;
            WeightedGraph undirected = GraphGenerator.connectedUndirected(vertices, edges, seed);
            WeightedGraph dag = GraphGenerator.dag(vertices, edges, seed);
            WeightedGraph cyclic = GraphGenerator.cyclicDirected(vertices, edges, seed);
            long expectedWeight = Kruskal.compute(undirected).totalWeight();
            ResultChecks.mst(undirected, Prim.compute(undirected), expectedWeight);

            measure(new CaseInfo("Kruskal", "connected_undirected", undirected, seed),
                    counts -> Kruskal.compute(undirected, counts),
                    result -> ResultChecks.mst(undirected, result, expectedWeight),
                    result -> "weight:" + result.totalWeight() + ";components:" + result.componentCount(), runs, summaries);
            measure(new CaseInfo("Prim", "connected_undirected", undirected, seed),
                    counts -> Prim.compute(undirected, counts),
                    result -> ResultChecks.mst(undirected, result, expectedWeight),
                    result -> "weight:" + result.totalWeight() + ";components:" + result.componentCount(), runs, summaries);

            for (boolean isDag : new boolean[] {true, false}) {
                WeightedGraph graph = isDag ? dag : cyclic;
                String type = isDag ? "dag" : "cyclic_directed";
                int[] expectedComponents = ResultChecks.sccReference(graph);
                measure(new CaseInfo("Kosaraju", type, graph, seed),
                        counts -> Kosaraju.compute(graph, counts),
                        result -> ResultChecks.scc(graph, result, expectedComponents),
                        result -> "components:" + result.componentCount(), runs, summaries);
                measure(new CaseInfo("TopologicalSort", type, graph, seed),
                        counts -> TopologicalSort.compute(graph, counts),
                        result -> ResultChecks.topological(graph, result, isDag),
                        result -> "acyclic:" + result.acyclic() + ";ordered:" + result.order().size(), runs, summaries);
            }

            long[] expectedDistances = ResultChecks.distanceReference(cyclic, 0);
            measure(new CaseInfo("Dijkstra", "cyclic_directed", cyclic, seed),
                    counts -> Dijkstra.compute(cyclic, 0, counts),
                    result -> ResultChecks.distances(result, expectedDistances),
                    BenchmarkRunner::distanceSignature, runs, summaries);
        }

        Files.createDirectories(output);
        String baseHeader = "algorithm,graph_type,vertices,edges,seed,graph_sha256";
        String counterHeader = Arrays.stream(Operation.values()).map(operation -> operation.name().toLowerCase(Locale.ROOT))
                .reduce((first, second) -> first + "," + second).orElseThrow();
        List<String> raw = new ArrayList<>();
        raw.add(baseHeader + ",repetition,elapsed_ns,result_signature," + counterHeader);
        for (Run run : runs) {
            raw.add(run.info().csv() + "," + run.repetition() + "," + run.elapsed() + ","
                    + run.signature() + "," + countersCsv(run.counts()));
        }
        Files.write(output.resolve("benchmark_runs.csv"), raw, StandardCharsets.UTF_8);
        summaries.add(0, baseHeader + ",repetitions,average_ns,min_ns,median_ns,max_ns,stddev_ns," + counterHeader);
        Files.write(output.resolve("benchmark_summary.csv"), summaries, StandardCharsets.UTF_8);
        Files.writeString(output.resolve("environment.properties"), environment(), StandardCharsets.UTF_8);
        System.out.printf("Validated %d measured runs across %d cases. Results: %s%n",
                runs.size(), summaries.size() - 1, output);
    }

    private static <T> void measure(CaseInfo info, Function<OperationCounts, T> algorithm,
                                    Consumer<T> validate, Function<T, String> signature,
                                    List<Run> runs, List<String> summaries) {
        for (int warmup = 0; warmup < WARMUP_ROUNDS; warmup++) blackhole = algorithm.apply(null);
        long[] times = new long[REPETITIONS];
        String[] signatures = new String[REPETITIONS];
        long[] firstCounts = null;
        String firstSignature = null;
        for (int repetition = 0; repetition < REPETITIONS; repetition++) {
            long start = System.nanoTime();
            T result = algorithm.apply(null);
            long elapsed = System.nanoTime() - start;
            validate.accept(result);
            blackhole = result;
            times[repetition] = elapsed;
            signatures[repetition] = signature.apply(result);
        }
        for (int repetition = 0; repetition < REPETITIONS; repetition++) {
            OperationCounts counts = new OperationCounts();
            T counted = algorithm.apply(counts);
            validate.accept(counted);
            String resultSignature = signatures[repetition];
            if (!resultSignature.equals(signature.apply(counted))) {
                throw new IllegalStateException("Instrumented result differs from timed result");
            }
            long[] snapshot = counts.snapshot();
            if (repetition == 0) {
                firstCounts = snapshot;
                firstSignature = resultSignature;
            } else if (!Arrays.equals(firstCounts, snapshot) || !firstSignature.equals(resultSignature)) {
                throw new IllegalStateException("Repeated operations or results are not reproducible");
            }
            runs.add(new Run(info, repetition + 1, times[repetition], resultSignature, snapshot));
        }
        summaries.add(summarize(info, times, firstCounts));
        System.out.printf("%s / %s: V=%d E=%d, mean %.2f ns%n", info.algorithm(), info.type(),
                info.graph().vertexCount(), info.graph().edgeCount(), Arrays.stream(times).average().orElseThrow());
    }

    private static String summarize(CaseInfo info, long[] times, long[] counts) {
        long[] sorted = times.clone();
        Arrays.sort(sorted);
        double mean = Arrays.stream(times).average().orElseThrow();
        double variance = 0;
        for (long time : times) variance += (time - mean) * (time - mean);
        return info.csv() + "," + times.length + "," + decimal(mean) + "," + sorted[0] + ","
                + sorted[sorted.length / 2] + "," + sorted[sorted.length - 1] + ","
                + decimal(Math.sqrt(variance / times.length)) + "," + countersCsv(counts);
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String countersCsv(long[] counts) {
        return Arrays.stream(counts).mapToObj(Long::toString).reduce((first, second) -> first + "," + second).orElseThrow();
    }

    private static String distanceSignature(ShortestPathResult result) {
        int reachable = 0;
        long checksum = 1;
        for (int vertex = 0; vertex < result.vertexCount(); vertex++) {
            if (result.reachable(vertex)) reachable++;
            checksum = 31 * checksum + result.distanceTo(vertex);
        }
        return "reachable:" + reachable + ";checksum:" + checksum;
    }

    static String fingerprint(WeightedGraph graph) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update((graph.vertexCount() + "," + graph.edgeCount() + "," + graph.isDirected() + "\n")
                    .getBytes(StandardCharsets.UTF_8));
            for (Edge edge : graph.edges()) {
                digest.update((edge.from() + "," + edge.to() + "," + edge.weight() + "\n").getBytes(StandardCharsets.UTF_8));
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required", exception);
        }
    }

    private static String environment() throws IOException {
        String cpu = "unavailable";
        Path cpuInfo = Path.of("/proc/cpuinfo");
        if (Files.isReadable(cpuInfo)) {
            try (var lines = Files.lines(cpuInfo)) {
                cpu = lines.filter(line -> line.startsWith("model name")).findFirst()
                        .map(line -> line.substring(line.indexOf(':') + 1).trim()).orElse(cpu);
            }
        }
        return "recorded_at_utc=" + Instant.now() + "\n"
                + "java_version=" + System.getProperty("java.version") + "\n"
                + "vm_name=" + System.getProperty("java.vm.name") + "\n"
                + "jvm_arguments=" + String.join(" ", ManagementFactory.getRuntimeMXBean().getInputArguments()) + "\n"
                + "os_name=" + System.getProperty("os.name") + "\n"
                + "os_version=" + System.getProperty("os.version") + "\n"
                + "os_arch=" + System.getProperty("os.arch") + "\n"
                + "available_processors=" + Runtime.getRuntime().availableProcessors() + "\n"
                + "cpu_model=" + cpu + "\n"
                + "base_seed=" + BASE_SEED + "\n"
                + "warmup_rounds_per_case=" + WARMUP_ROUNDS + "\n"
                + "measured_repetitions=" + REPETITIONS + "\n"
                + "counter_collection=separate_untimed_invocation_per_repetition\n"
                + "path_reconstruction_timed=false\n";
    }

    private record CaseInfo(String algorithm, String type, WeightedGraph graph, long seed, String fingerprint) {
        CaseInfo(String algorithm, String type, WeightedGraph graph, long seed) {
            this(algorithm, type, graph, seed, BenchmarkRunner.fingerprint(graph));
        }

        String csv() {
            return algorithm + "," + type + "," + graph.vertexCount() + "," + graph.edgeCount() + "," + seed + "," + fingerprint;
        }
    }

    private record Run(CaseInfo info, int repetition, long elapsed, String signature, long[] counts) {
    }
}
