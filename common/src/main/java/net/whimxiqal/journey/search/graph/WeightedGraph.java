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

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Predicate;
import net.whimxiqal.journey.Journey;
import net.whimxiqal.journey.tools.AlternatingList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An implementation of a weighted uni-directional graph.
 * That means each edge (and node, in this case) has a weight (or length)
 * assigned to it, which together determine the size of any path found as a solution
 * to the graph when traversing from one node to another.
 * The solution of any solved path is one which minimized the distance between the nodes.
 *
 * <p>The {@link #findMinimumPath} method uses Dijkstra's algorithm.
 *
 * @param <N> the graph node type
 * @param <E> the graph edge type
 */
public abstract class WeightedGraph<N, E> {

  private final Set<Node> nodes = new HashSet<>();
  private final Table edgeTable = new Table();
  private final HashMap<N, Node> dataToNodes = new HashMap<>();

  /**
   * Add an edge to the graph.
   *
   * @param origin      the origin node of the edge
   * @param destination the destination node of the edge
   * @param edge        the edge itself
   */
  public void addEdge(@NotNull N origin, @NotNull N destination, @NotNull E edge) {
    Node originNode = makeOrGetNode(origin);
    Node destinationNode = makeOrGetNode(destination);
    this.nodes.add(originNode);
    this.nodes.add(destinationNode);
    this.edgeTable.put(originNode, destinationNode, edge);
  }

  private Node makeOrGetNode(N data) {
    Node existingNode = dataToNodes.get(data);
    if (existingNode != null) {
      return existingNode;
    }
    existingNode = new Node(data);
    dataToNodes.put(data, existingNode);
    return existingNode;
  }

  @Nullable
  protected final AlternatingList<N, E, Object> findMinimumPath(N origin, Predicate<N> done, Predicate<E> edgeFilter) {
    Node originNode = makeOrGetNode(origin);

    PriorityQueue<Node> toVisit = new PriorityQueue<>(Comparator.comparingDouble(n -> n.distance));
    Set<Node> visited = new HashSet<>();

    originNode.setDistance(0);
    originNode.setPrevious(null);
    toVisit.add(originNode);

    Node current;
    while (!toVisit.isEmpty()) {
      current = toVisit.poll();
      visited.add(current);

      if (done.test(current.data)) {
        // We've reached destination. Package solution.
        AlternatingList.Builder<N, E, Object> pathBuilder = AlternatingList.builder(current.data);
        while (!current.equals(originNode)) {
          pathBuilder.addFirst(current.getPrevious().data,
              Objects.requireNonNull(this.edgeTable.getEdge(current.getPrevious(), current)));
          current = current.getPrevious();
        }

        resetNodes();
        return pathBuilder.build();
      }
      // Not yet done
      Map<Node, E> edges = edgeTable.edgesFrom(current);
      if (edges == null) {
        continue;
      }
      for (Map.Entry<Node, E> outlet : edgeTable.edgesFrom(current).entrySet()) {
        // outlet.getKey() is destination
        // outlet.getValue() is edge from 'current' to destination
        if (!edgeFilter.test(outlet.getValue())) {
          // manually filtered out this edge
          continue;
        }
        if (visited.contains(outlet.getKey())) {
          // we've already gotten to this node (in a faster way)
          continue;
        }
        // the outlet constructs with max double distance, aka, infinite distance
        if (outlet.getKey().getDistance() > current.getDistance() + edgeLength(outlet.getValue())) {
          // A better path for this node would be to come from current.
          toVisit.remove(outlet.getKey());  // Remove from waiting queue in case it was already queued before
          outlet.getKey().setDistance(current.getDistance()
              + edgeLength(outlet.getValue())
              + nodeWeight(outlet.getKey().getData()));
          outlet.getKey().setPrevious(current);
          toVisit.add(outlet.getKey());
        }
      }
    }

    resetNodes();
    return null;  // Could not find it

  }

  private void resetNodes() {
    this.nodes.forEach(node -> {
      node.setDistance(Double.MAX_VALUE);
      node.setPrevious(null);
    });
  }

  protected abstract double nodeWeight(N nodeData);

  protected abstract double edgeLength(E edge);

  /**
   * A node of this {@link WeightedGraph}.
   * This node acts as a wrapper around some important data which this
   * graph is wrapping to organize a path from the internal data piece,
   * intermittently using edges to get there.
   */
  private class Node {

    private final N data;
    private double distance = Double.MAX_VALUE;
    private Node previous = null;

    /**
     * General constructor.
     *
     * @param data the content of the node
     */
    public Node(N data) {
      this.data = data;
    }

    /**
     * Get the data wrapped into this node.
     *
     * @return the data
     */
    public N getData() {
      return data;
    }

    /**
     * Get the current total distance of this node from the origin.
     *
     * @return the distance
     */
    public double getDistance() {
      return distance;
    }

    /**
     * Set the current total distance of this node from the origin.
     *
     * @param distance the distance
     */
    public void setDistance(double distance) {
      this.distance = distance;
    }

    /**
     * Get the node previous to this one in the current operation
     * trying to solve the graph.
     *
     * @return the previous node
     */
    public Node getPrevious() {
      return previous;
    }

    /**
     * Set the node previous to this one in the current operation
     * trying to solve the graph.
     *
     * @param previous set the previous node
     */
    public void setPrevious(Node previous) {
      this.previous = previous;
    }

    @Override
    public String toString() {
      return String.format("Node: {data: %s, distance: %s, weight: %f}",
          data.hashCode(),
          distance > Double.MAX_VALUE * .9
              ? "inf"
              : distance, nodeWeight(data));
    }
  }

  private class Table {
    private final Map<Node, Map<Node, E>> edgeMap = new HashMap<>();

    public void put(Node start, Node end, E edge) {
      edgeMap.computeIfAbsent(start, (k) -> new HashMap<>()).put(end, edge);
    }

    public Map<Node, E> edgesFrom(Node start) {
      return edgeMap.get(start);
    }

    public E getEdge(Node start, Node end) {
      if (edgeMap.containsKey(start)) {
        return edgeMap.get(start).get(end);
      } else {
        return null;
      }
    }
  }

}
