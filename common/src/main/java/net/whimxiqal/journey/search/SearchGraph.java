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

package net.whimxiqal.journey.search;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.Journey;
import net.whimxiqal.journey.Target;
import net.whimxiqal.journey.Tunnel;
import net.whimxiqal.journey.data.DataAccessException;
import net.whimxiqal.journey.navigation.Mode;
import net.whimxiqal.journey.search.graph.WeightedGraph;
import org.jetbrains.annotations.Nullable;

/**
 * An implementation of a weighted graph to be used for the overall search algorithm.
 */
public abstract class SearchGraph extends WeightedGraph<Tunnel, DestinationPathTrial> {

  protected final GraphGoalSearchSession<?> session;
  protected final Cell origin;
  protected final Tunnel originNode;

  public SearchGraph(GraphGoalSearchSession<?> session, Cell origin) {
    this.session = session;
    this.origin = origin;
    this.originNode = Tunnel.builder(origin, origin).cost(0).build();
  }

  /**
   * Add a path trial to the search graph that supposedly goes
   * from the origin of the entire search to a tunnel.
   * The endpoint of the path, then, would be the origin of the tunnel.
   *
   * @param end   the end of the path trial
   * @param modes the mode types used to traverse the path
   */
  public void addPathTrialOriginToTunnel(Tunnel end, Collection<Mode> modes, boolean persistentOrigin) {
    addPathTrial(session,
        origin, end.entrance(),
        originNode, end,
        modes, persistentOrigin);
  }

  /**
   * Add a path trial to the search graph that goes from a tunnel to
   * another tunnel.
   * The destination of the start tunnel is the origin of this path trial,
   * and the origin of the end tunnel is the destination of this path trial.
   *
   * @param start the starting tunnel
   * @param end   the ending tunnel
   * @param modes the mode types used to traverse the path
   */
  public void addPathTrialTunnelToTunnel(Tunnel start, Tunnel end, Collection<Mode> modes) {
    addPathTrial(session, start.exit(), end.entrance(),
        start, end,
        modes, true);
  }

  protected void addPathTrial(SearchSession session, Cell origin, Target destination,
                              Tunnel originNode,
                              Tunnel destinationNode,
                              Collection<Mode> modes, boolean saveOnComplete) {
    // First, try to access a cached path
    Set<ModeType> modeTypes = modes.stream().map(Mode::type).collect(Collectors.toSet());
    boolean added = false;
    try {
      // TODO temporarily disabled
//      if (Journey.get().proxy().dataManager()
//          .pathRecordManager()
//          .containsRecord(origin, destination, modeTypes)) {
//        addPathTrial(DestinationPathTrial.cached(session, origin, destination,
//                modes,
//                Journey.get().proxy().dataManager()
//                    .pathRecordManager()
//                    .getPath(origin, destination, modeTypes)),
//            originNode, destinationNode);
//        added = true;
//      }
    } catch (DataAccessException e) {
      e.printStackTrace();
    }
    if (!added) {
      addPathTrial(DestinationPathTrial.approximate(session, origin, destination, modes, saveOnComplete), originNode, destinationNode);
    }
  }

  private void addPathTrial(DestinationPathTrial trial, Tunnel start, Tunnel end) {
    addEdge(start, end, trial);
  }

  @Override
  protected double nodeWeight(Tunnel nodeData) {
    return nodeData.cost();
  }

  @Override
  protected double edgeLength(DestinationPathTrial edge) {
    return edge.getLength();
  }

  @Nullable
  abstract public ItineraryTrial calculate(boolean mustUseCache);
}
