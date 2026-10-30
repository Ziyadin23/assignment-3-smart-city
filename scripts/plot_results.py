#!/usr/bin/env python3
"""Plot recorded benchmark means and observed ranges without rerunning Java."""
import argparse
import csv
from pathlib import Path
import math

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.ticker import ScalarFormatter

plt.rcParams["svg.hashsalt"] = "assignment3-benchmarks"

ROOT = Path(__file__).resolve().parent.parent
GROUPS = [
    ("Minimum spanning trees", [("Kruskal", "connected_undirected", "Kruskal"),
                                ("Prim", "connected_undirected", "Prim")]),
    ("Directed connectivity", [("Kosaraju", "dag", "Kosaraju / DAG"),
                               ("Kosaraju", "cyclic_directed", "Kosaraju / cyclic"),
                               ("TopologicalSort", "dag", "Topological / DAG"),
                               ("TopologicalSort", "cyclic_directed", "Topological / cyclic")]),
    ("Shortest paths", [("Dijkstra", "cyclic_directed", "Dijkstra / cyclic")]),
]


def read_summary(path):
    with path.open(newline="", encoding="utf-8") as source:
        rows = list(csv.DictReader(source))
    expected = {(a, g, v, e) for _, series in GROUPS for a, g, _ in series
                for v, e in [(10, 30), (100, 500), (1000, 5000)]}
    data = {}
    for row in rows:
        key = (row["algorithm"], row["graph_type"], int(row["vertices"]), int(row["edges"]))
        low, mean, high = (float(row[k]) for k in ("min_ns", "average_ns", "max_ns"))
        if (key in data or key not in expected or int(row["repetitions"]) != 5
                or not all(math.isfinite(x) for x in (low, mean, high))
                or not 0 < low <= mean <= high):
            raise ValueError(f"Invalid or duplicate benchmark row: {key}")
        data[key] = (low / 1000, mean / 1000, high / 1000)
    if data.keys() != expected:
        raise ValueError("Expected all 21 algorithm/input/size combinations")
    return data


def plot(data, output, dimension):
    fig, axes = plt.subplots(1, 3, figsize=(15, 5.5), layout="constrained")
    for ax, (title, series) in zip(axes, GROUPS):
        for algorithm, graph, label in series:
            points = sorted((key[dimension], value) for key, value in data.items()
                            if key[:2] == (algorithm, graph))
            x = [p[0] for p in points]
            low, mean, high = zip(*(p[1] for p in points))
            ax.errorbar(x, mean, yerr=([m-l for m, l in zip(mean, low)],
                                     [h-m for h, m in zip(high, mean)]),
                        marker="o", capsize=4, linewidth=1.5, label=label)
        ax.set(xscale="log", yscale="log", title=title,
               xlabel="Vertices (V)" if dimension == 2 else "Edges (E)",
               ylabel="Execution time (µs)")
        ax.set_xticks([10, 100, 1000] if dimension == 2 else [30, 500, 5000])
        ax.xaxis.set_major_formatter(ScalarFormatter())
        ax.minorticks_off()
        ax.grid(True, which="major", alpha=0.25)
        ax.legend(fontsize=8, loc="best")
    axis_name = "vertices" if dimension == 2 else "edges"
    fig.suptitle(f"Execution time vs {axis_name}: mean and observed min–max, five runs\n"
                 "Logarithmic axes; each panel uses its own time scale", fontsize=13)
    for suffix in ("png", "svg"):
        fig.savefig(output / f"execution_time_vs_{axis_name}.{suffix}", dpi=160,
                    metadata={"Date": None} if suffix == "svg" else {})
        if suffix == "svg":
            path = output / f"execution_time_vs_{axis_name}.svg"
            path.write_text("\n".join(line.rstrip() for line in path.read_text(encoding="utf-8").splitlines()) + "\n",
                            encoding="utf-8")
    plt.close(fig)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, default=ROOT / "results/tables/benchmark_summary.csv")
    parser.add_argument("--output", type=Path, default=ROOT / "results/plots")
    args = parser.parse_args()
    data = read_summary(args.input)
    args.output.mkdir(parents=True, exist_ok=True)
    for dimension in (2, 3):
        plot(data, args.output, dimension)
    print(f"Plotted {len(data)} benchmark cases in {args.output}")


if __name__ == "__main__":
    main()
