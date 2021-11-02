package io.github.vipcxj.jasync.asm;

import org.jgrapht.Graph;
import org.jgrapht.alg.connectivity.GabowStrongConnectivityInspector;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.util.SupplierUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class GraphTest {

    private List<Graph<Integer, DefaultEdge>> graphs;
    private List<Vertex[]> vertexesList;
    private List<Vertex[]> largeVertexesList;

    private static Vertex[] toNodes(Graph<Integer, DefaultEdge> graph) {
        Set<Integer> vertexSet = graph.vertexSet();
        TestVertex[] vertexes = new TestVertex[vertexSet.size()];
        for (Integer v : vertexSet) {
            TestVertex vertex = new TestVertex(v);
            vertexes[v] = vertex;
        }
        for (TestVertex vertex : vertexes) {
            for (DefaultEdge edge : graph.outgoingEdgesOf(vertex.getValue())) {
                Integer target = graph.getEdgeTarget(edge);
                vertex.getSuccessors().add(vertexes[target]);
            }
        }
        return vertexes;
    }

    private static List<Graph<Integer, DefaultEdge>> generateGraph(int n, int m, int num) {
        List<Graph<Integer, DefaultEdge>> graphs = new ArrayList<>();
        GnmRandomGraphGenerator<Integer, DefaultEdge> generator = new GnmRandomGraphGenerator<>(n, m, System.currentTimeMillis());
        for (int i = 0; i < num; ++i) {
            Supplier<Integer> vSupplier = new Supplier<Integer>() {
                private int id = 0;

                @Override
                public Integer get() {
                    return id++;
                }
            };
            Graph<Integer, DefaultEdge> graph = new SimpleDirectedGraph<>(vSupplier, SupplierUtil.createDefaultEdgeSupplier(), false);
            generator.generateGraph(graph);
            graphs.add(graph);
        }
        return graphs;
    }

    @BeforeEach
    public void prepare() {
        graphs = generateGraph(500, 1000, 30);
        vertexesList = graphs.stream().map(GraphTest::toNodes).collect(Collectors.toList());
        List<Graph<Integer, DefaultEdge>> largeGraphs = generateGraph(100000, 200000, 10);
        largeVertexesList = largeGraphs.stream().map(GraphTest::toNodes).collect(Collectors.toList());
    }

    @Test
    public void testTarjan() {
        Iterator<Graph<Integer, DefaultEdge>> graphIterator = graphs.iterator();
        Iterator<Vertex[]> vertexesIterator = vertexesList.iterator();
        int i = 0;
        TimeUtils times = new TimeUtils();
        while (graphIterator.hasNext()) {
            Graph<Integer, DefaultEdge> graph = graphIterator.next();
            Vertex[] vertices = vertexesIterator.next();
            GabowStrongConnectivityInspector<Integer, DefaultEdge> cyclesFinder = new GabowStrongConnectivityInspector<>(graph);
            System.out.println("Graph " + ++i + ":");
            times.start();
            Set<Set<Integer>> testCycles = new HashSet<>(CodePiece.tarjan(vertices, 0, vertices.length));
            times.stop();
            System.out.println("Test cycles calculated. Used " + times.escaped(TimeUnit.MICROSECONDS) + " μs.");
            times.start();
            Set<Set<Integer>> expectCycles = new HashSet<>(cyclesFinder.stronglyConnectedSets());
            times.stop();
            System.out.println("Expected cycles calculated. Used " + times.escaped(TimeUnit.MICROSECONDS) + " μs.");
            Assertions.assertEquals(expectCycles, testCycles);
        }
        i = 0;
        for (Vertex[] vertices : largeVertexesList) {
            times.start();
            CodePiece.tarjan(vertices, 0, vertices.length);
            times.stop();
            System.out.println("Large Graph " + ++i + ": used " + times.escaped(TimeUnit.MILLISECONDS) + " ms.");
        }
    }

    static class TestVertex implements Vertex {
        private final int value;
        private final Set<Vertex> successors;

        TestVertex(int value) {
            this.value = value;
            this.successors = new HashSet<>();
        }

        @Override
        public int getValue() {
            return value;
        }

        public Set<Vertex> getSuccessors() {
            return successors;
        }

        @Override
        public Successors createSuccessors() {
            return new SuccessorsImpl(successors.iterator());
        }
    }

    static class SuccessorsImpl implements Successors {
        private final Iterator<Vertex> iterator;
        private Vertex current;

        SuccessorsImpl(Iterator<Vertex> iterator) {
            this.iterator = iterator;
            this.current = iterator.hasNext() ? iterator.next() : null;
        }

        @Override
        public void next() {
            this.current = iterator.hasNext() ? iterator.next() : null;
        }

        @Override
        public Vertex current() {
            return current;
        }
    }
}
