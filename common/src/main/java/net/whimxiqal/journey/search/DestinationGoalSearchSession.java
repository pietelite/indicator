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

import java.util.UUID;
import java.util.function.Predicate;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.Journey;
import net.whimxiqal.journey.JourneyAgent;
import net.whimxiqal.journey.JourneyPlayer;
import net.whimxiqal.journey.TargetFunction;
import net.whimxiqal.journey.Tunnel;

public class DestinationGoalSearchSession extends GraphGoalSearchSession<DestinationSearchGraph> {

  private final TargetFunction destination;
  private final Predicate<Cell> satisfactionPredicate;
  private final boolean stationaryDestination;

  public DestinationGoalSearchSession(UUID callerId, Caller callerType, JourneyAgent agent,
                                      Cell origin, TargetFunction destination,  Predicate<Cell> satisfactionPredicate,
                                      boolean stationaryOrigin, boolean stationaryDestination) {
    super(callerId, callerType, agent, origin, stationaryOrigin);
    this.destination = destination;
    this.satisfactionPredicate = satisfactionPredicate;
    this.stationaryDestination = stationaryDestination;
  }

  public DestinationGoalSearchSession(JourneyPlayer player, Cell origin,
                                      TargetFunction destination, Predicate<Cell> satisfactionPredicate,
                                      boolean stationaryOrigin, boolean stationaryDestination) {
    this(player.uuid(), Caller.PLAYER, player, origin, destination, satisfactionPredicate, stationaryOrigin, stationaryDestination);
  }

  @Override
  DestinationSearchGraph createSearchGraph() {
    return new DestinationSearchGraph(this, origin, destination, satisfactionPredicate);
  }

  @Override
  public void initialize() {
    super.initialize();
  }

  @Override
  protected void initSearchExtra() {
    if (origin.domain() == destination.domain()) {
      stateInfo.searchGraph.addPathTrialOriginToDestination(modes(), persistentOrigin && stationaryDestination);
    }

    for (Integer domain : stateInfo.allDomains) {
      // Path trials from tunnel -> destination
      for (Tunnel pathTrialOriginTunnel : stateInfo.tunnelsByDestinationDomain.get(domain)) {
        if (domain.equals(destination.domain())) {
          stateInfo.searchGraph.addPathTrialTunnelToDestination(pathTrialOriginTunnel, modes(), stationaryDestination);
        }
      }
    }
  }

  @Override
  public String toString() {
    return "[Destination Graph Goal Search] {session: " + uuid
        + ", origin: " + origin
        + ", destination: " + destination
        + ", caller: (" + callerType + ") " + callerId
        + ", state: " + state.get()
        + '}';
  }
}
