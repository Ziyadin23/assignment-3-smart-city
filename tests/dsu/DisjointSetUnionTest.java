package dsu;

import java.util.Arrays;
import java.util.Random;

public final class DisjointSetUnionTest {
    private static int passed;

    public static void main(String[] args) {
        if (!DisjointSetUnionTest.class.desiredAssertionStatus()) {
            throw new IllegalStateException("Tests require assertions: run with -ea");
        }

        run("elements start in separate singleton components", DisjointSetUnionTest::separateComponents);
        run("unions establish transitive connectivity", DisjointSetUnionTest::transitiveConnectivity);
        run("repeated and self unions leave counts unchanged", DisjointSetUnionTest::repeatedUnions);
        run("component sizes are correct for every member", DisjointSetUnionTest::componentSizes);
        run("larger components keep their root regardless of argument order", DisjointSetUnionTest::unionBySize);
        run("repeated finds preserve a merged partition", DisjointSetUnionTest::repeatedFinds);
        run("single element remains one component", DisjointSetUnionTest::singleElement);
        run("empty disjoint set has zero components", DisjointSetUnionTest::emptySet);
        run("invalid inputs are rejected without merging components", DisjointSetUnionTest::invalidInputs);
        run("random unions match an independent label-based reference", DisjointSetUnionTest::referenceComparison);
        System.out.println("DisjointSetUnionTest: " + passed + " tests passed");
    }

    private static void separateComponents() {
        DisjointSetUnion sets = new DisjointSetUnion(5);
        assert sets.componentCount() == 5;
        for (int element = 0; element < 5; element++) {
            assert sets.find(element) == element;
            assert sets.componentSize(element) == 1;
            for (int other = 0; other < 5; other++) {
                assert sets.connected(element, other) == (element == other);
            }
        }
    }

    private static void transitiveConnectivity() {
        DisjointSetUnion sets = new DisjointSetUnion(5);
        assert sets.union(0, 1);
        assert sets.union(1, 2);
        assert sets.connected(0, 2);
        assert sets.find(0) == sets.find(2);
        assert !sets.connected(0, 3);
        assert !sets.connected(3, 4);
        assert sets.componentCount() == 3;
    }

    private static void repeatedUnions() {
        DisjointSetUnion sets = new DisjointSetUnion(3);
        assert sets.union(0, 1);
        assert !sets.union(0, 1);
        assert !sets.union(1, 0);
        assert !sets.union(1, 1);
        assert !sets.union(2, 2);
        assert sets.componentCount() == 2;
        assert sets.componentSize(0) == 2;
        assert sets.componentSize(2) == 1;
    }

    private static void componentSizes() {
        DisjointSetUnion sets = new DisjointSetUnion(6);
        sets.union(0, 1);
        sets.union(1, 2);
        sets.union(3, 4);
        assert sets.componentSize(0) == 3;
        assert sets.componentSize(2) == 3;
        assert sets.componentSize(4) == 2;
        assert sets.componentSize(5) == 1;
        assert sets.union(2, 4);
        for (int element = 0; element < 5; element++) {
            assert sets.componentSize(element) == 5;
        }
        assert sets.componentSize(5) == 1;
        assert sets.componentCount() == 2;
    }

    private static void unionBySize() {
        for (boolean smallerFirst : new boolean[] {true, false}) {
            DisjointSetUnion sets = new DisjointSetUnion(6);
            sets.union(0, 1);
            sets.union(2, 3);
            sets.union(4, 5);
            sets.union(2, 4);
            int largerRoot = sets.find(5);
            assert smallerFirst ? sets.union(0, 2) : sets.union(2, 0);
            assert sets.find(0) == largerRoot;
            assert sets.find(1) == largerRoot;
            assert sets.componentSize(1) == 6;
            assert sets.componentCount() == 1;
        }
    }

    private static void repeatedFinds() {
        int count = 1024;
        DisjointSetUnion sets = new DisjointSetUnion(count);
        for (int width = 1; width < count; width *= 2) {
            for (int start = 0; start < count; start += width * 2) {
                assert sets.union(start, start + width);
            }
        }
        int root = sets.find(count - 1);
        for (int pass = 0; pass < 3; pass++) {
            for (int element = 0; element < count; element++) {
                assert sets.find(element) == root;
                assert sets.componentSize(element) == count;
            }
        }
        assert sets.componentCount() == 1;
        assert !sets.union(0, count - 1);
    }

    private static void singleElement() {
        DisjointSetUnion sets = new DisjointSetUnion(1);
        assert sets.find(0) == 0;
        assert sets.connected(0, 0);
        assert !sets.union(0, 0);
        assert sets.componentSize(0) == 1;
        assert sets.componentCount() == 1;
    }

    private static void emptySet() {
        DisjointSetUnion sets = new DisjointSetUnion(0);
        assert sets.componentCount() == 0;
        expectInvalid(() -> sets.find(0));
        expectInvalid(() -> sets.union(0, 0));
        expectInvalid(() -> sets.componentSize(0));
    }

    private static void invalidInputs() {
        expectInvalid(() -> new DisjointSetUnion(-1));
        DisjointSetUnion sets = new DisjointSetUnion(3);
        sets.union(0, 1);
        expectInvalid(() -> sets.find(-1));
        expectInvalid(() -> sets.find(3));
        expectInvalid(() -> sets.union(-1, 2));
        expectInvalid(() -> sets.union(0, 3));
        expectInvalid(() -> sets.union(3, 0));
        expectInvalid(() -> sets.union(2, -1));
        expectInvalid(() -> sets.connected(0, 3));
        expectInvalid(() -> sets.componentSize(-1));
        assert sets.componentCount() == 2;
        assert sets.componentSize(0) == 2;
        assert !sets.connected(0, 2);
    }

    private static void referenceComparison() {
        int count = 24;
        int[] labels = new int[count];
        for (int element = 0; element < count; element++) {
            labels[element] = element;
        }
        DisjointSetUnion sets = new DisjointSetUnion(count);
        Random random = new Random(42);
        for (int step = 0; step < 150; step++) {
            int first = random.nextInt(count);
            int second = random.nextInt(count);
            int firstLabel = labels[first];
            int secondLabel = labels[second];
            assert sets.union(first, second) == (firstLabel != secondLabel);
            for (int element = 0; element < count; element++) {
                if (labels[element] == secondLabel) {
                    labels[element] = firstLabel;
                }
            }
            assert sets.componentCount() == Arrays.stream(labels).distinct().count();
            for (int element = 0; element < count; element++) {
                int expectedSize = 0;
                for (int other = 0; other < count; other++) {
                    boolean sameComponent = labels[element] == labels[other];
                    assert sets.connected(element, other) == sameComponent;
                    if (sameComponent) {
                        expectedSize++;
                    }
                }
                assert sets.componentSize(element) == expectedSize;
            }
        }
    }

    private static void run(String name, Runnable test) {
        test.run();
        passed++;
        System.out.println("PASS: " + name);
    }

    private static void expectInvalid(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }
}
