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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.Journey;
import net.whimxiqal.journey.JourneyAgent;
import net.whimxiqal.journey.Target;
import net.whimxiqal.journey.Tunnel;
import org.jetbrains.annotations.Nullable;

/**
 * An implementation of the {@link SearchSession} that uses a "reverse"
 * and predictive method to calculated ideal Itineraries.
 *
 * <p>First, all possible paths will be collected into a series of {@link PathTrial}s.
 */
public abstract class GraphGoalSearchSession<G extends SearchGraph> extends SearchSession {

  protected final Cell origin;
  protected final boolean persistentOrigin;
  protected final State stateInfo = new State();

  public GraphGoalSearchSession(UUID callerId, Caller callerType, JourneyAgent agent,
                                Cell origin, boolean persistentOrigin) {
    super(callerId, callerType, agent);
    this.origin = origin;
    this.persistentOrigin = persistentOrigin;
  }

  @Override
  public void asyncSearch() {
    state.getAndUpdate(current -> ResultState.RUNNING);
    if (!initSearch()) {
      state.getAndUpdate(current -> ResultState.STOPPED_FAILED);
      complete(null);
    }
    runSearchUnit();
  }

  private boolean initSearch() {
    synchronized (stateInfo) {
      super.timer.start();

      for (Tunnel tunnel : tunnels()) {
        stateInfo.allDomains.add(tunnel.entrance().domain());
        stateInfo.allDomains.add(tunnel.exit().domain());
      }

      // Prepare tunnel maps
      for (Integer domain : stateInfo.allDomains) {
        stateInfo.tunnelsByOriginDomain.put(domain, new LinkedList<>());
        stateInfo.tunnelsByDestinationDomain.put(domain, new LinkedList<>());
      }

      // Fill tunnel maps
      for (Tunnel tunnel : tunnels()) {
        stateInfo.tunnelsByOriginDomain.get(tunnel.entrance().domain()).add(tunnel);
        stateInfo.tunnelsByDestinationDomain.get(tunnel.exit().domain()).add(tunnel);
      }

      stateInfo.searchGraph = createSearchGraph();

      // Collect path trials
      for (Integer domain : stateInfo.allDomains) {
        for (Tunnel pathTrialOriginTunnel : stateInfo.tunnelsByDestinationDomain.get(domain)) {
          for (Tunnel pathTrialDestinationTunnel : stateInfo.tunnelsByOriginDomain.get(domain)) {
            stateInfo.searchGraph.addPathTrialTunnelToTunnel(
                pathTrialOriginTunnel,
                pathTrialDestinationTunnel,
                modes());
            if (domain.equals(origin.domain())) {
              stateInfo.searchGraph.addPathTrialOriginToTunnel(pathTrialDestinationTunnel, modes(), persistentOrigin);
            }
          }
        }
      }

      initSearchExtra();
    }
    return true;
  }

  @Nullable
  abstract G createSearchGraph();

  protected void initSearchExtra(Target target) {
    // do nothing by default
  }

  /**
   * Run one cycle, which calculates a possible itinerary with known path trials, then tries to validate
   * the itinerary trial by searching for paths in the path trials.
   * If the cycle fails, it may update the path graph and try to calculate a new itinerary.
   * It is synchronized on stateInfo because each call may be on a different thread, so we just want to verify that
   * each one is only executing at a time.
   */
  private void runSearchUnit() {
    if (evaluateState()) {
      // Could have canceled at this point
      complete(null);
      return;
    }

    synchronized (stateInfo) {
      // Create itinerary
      ItineraryTrial itineraryTrial = stateInfo.searchGraph.calculate(stateInfo.cachingStatus == CachingStatus.ALWAYS_USE);

      if (evaluateState()) {
        // even if itinerary trial exists, we were too late :(
        complete(null);
        return;
      }

      if (itineraryTrial == null) {
        if (stateInfo.cachingStatus == CachingStatus.ALWAYS_USE) {
          // We were forcing ourselves to use the cache, and we failed. Loosen the restriction and retry
          stateInfo.cachingStatus = CachingStatus.USE_IF_POSSIBLE;
          Journey.get().proxy().schedulingManager().schedule(this::runSearchUnit, true);
          return;
        }
        // The graph search did not find any possible itineraries, so there is no solution
        state.updateAndGet(ResultState::stoppedResult);
        complete(null);
        return;
      }

      // Solve itinerary (We have an overall solution with individual paths that aren't verified/calculated)
      itineraryTrial.attempt(stateInfo.cachingStatus != CachingStatus.NEVER_USE).thenAccept(result -> {
        synchronized (stateInfo) {
          if (result.state() == ResultState.STOPPED_ERROR) {
            state.set(ResultState.STOPPED_ERROR);
          } else if (result.itinerary() != null) {
            state.set(ResultState.STOPPED_SUCCESSFUL);
            complete(result.itinerary());
            return;
          } else if (!result.changedProblem()) {
            // This result did not change the problem.
            // If run again, then, we would get the same solution to the graph.
            if (stateInfo.cachingStatus == CachingStatus.ALWAYS_USE) {
              // Turn off the use of the cache. Maybe we can find better solution by re-solving some paths.
              stateInfo.cachingStatus = CachingStatus.USE_IF_POSSIBLE;
              // continue...
            } else if (stateInfo.cachingStatus == CachingStatus.USE_IF_POSSIBLE) {
              Journey.logger().debug(this + ": Itinerary failed, disabling cache");
              stateInfo.cachingStatus = CachingStatus.NEVER_USE;
            } else {
              // The problem hasn't changed, and we aren't using the cache,
              // so no better solutions are possible.
              state.getAndUpdate(current -> current.stoppingResult(false));
            }
          }
        }

        if (evaluateState()) {
          complete(null);
          return;
        }

        Journey.get().proxy().schedulingManager().schedule(this::runSearchUnit, true);
      });
    }
  }

  @Override
  public String toString() {
    return "[Graph Goal Search] {session: " + uuid
        + ", origin: " + origin
        + ", caller: (" + callerType + ") " + callerId
        + ", state: " + state.get()
        + '}';
  }

  enum CachingStatus {
    ALWAYS_USE,
    USE_IF_POSSIBLE,
    NEVER_USE,
  }

  protected class State {
    final Set<Integer> allDomains = new HashSet<>();
    final Map<Integer, List<Tunnel>> tunnelsByOriginDomain = new HashMap<>();
    final Map<Integer, List<Tunnel>> tunnelsByDestinationDomain = new HashMap<>();
    G searchGraph = null;
    GraphGoalSearchSession.CachingStatus cachingStatus = GraphGoalSearchSession.CachingStatus.ALWAYS_USE;
  }
}
