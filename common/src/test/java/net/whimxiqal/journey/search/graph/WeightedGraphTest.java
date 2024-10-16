/*
 * MIT License
 *
 * Copyright (c) whimxiqal
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.whimxiqal.journey.search.graph;

import java.util.Collection;
import java.util.Iterator;
import net.whimxiqal.journey.JourneyTestHarness;
import net.whimxiqal.journey.tools.AlternatingList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class WeightedGraphTest extends JourneyTestHarness {

  @Test
  void findMinimumPath() {
    Graph graph = new Graph();
    Node A = new Node(0);
    Node B = new Node(0);
    Node C = new Node(0);
    Node D = new Node(0);
    Node E = new Node(0);
    Node F = new Node(0);
    graph.addEdge(A, B, new Edge(9));
    graph.addEdge(A, C, new Edge(4));
    graph.addEdge(C, B, new Edge(4));
    graph.addEdge(B, D, new Edge(12));
    graph.addEdge(B, E, new Edge(5));
    graph.addEdge(C, E, new Edge(13));
    graph.addEdge(D, F, new Edge(2));
    graph.addEdge(E, D, new Edge(3));
    graph.addEdge(E, F, new Edge(15));
    AlternatingList<Node, Edge, Object> result = graph.findMinimumPath(A, F::equals, e -> true);
    Assertions.assertNotNull(result);

    Collection<Node> nodes = result.getMajors();
    Assertions.assertEquals(6, nodes.size());

    Iterator<Node> nodesIt = nodes.iterator();
    Assertions.assertEquals(A, nodesIt.next());
    Assertions.assertEquals(C, nodesIt.next());
    Assertions.assertEquals(B, nodesIt.next());
    Assertions.assertEquals(E, nodesIt.next());
    Assertions.assertEquals(D, nodesIt.next());
    Assertions.assertEquals(F, nodesIt.next());

    Collection<Edge> edges = result.getMinors();
    Assertions.assertEquals(5, edges.size());

    Iterator<Edge> edgesIt = edges.iterator();
    Assertions.assertEquals(4, edgesIt.next().length);
    Assertions.assertEquals(4, edgesIt.next().length);
    Assertions.assertEquals(5, edgesIt.next().length);
    Assertions.assertEquals(3, edgesIt.next().length);
    Assertions.assertEquals(2, edgesIt.next().length);
  }

  private class Graph extends WeightedGraph<Node, Edge> {

    @Override
    protected double nodeWeight(Node nodeData) {
      return nodeData.weight;
    }

    @Override
    protected double edgeLength(Edge edge) {
      return edge.length;
    }
  }

  private static class Node {
    Node(double weight) {
      this.weight = weight;
    }
    private final double weight;
  }

  private static class Edge {
    Edge(double length) {
      this.length = length;
    }
    private final double length;
  }
}