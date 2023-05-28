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
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.Tunnel;

public abstract class DestinationGoalSearchSession extends GraphGoalSearchSession<DestinationSearchGraph> {

  protected final Cell destination;
  private final boolean persistentDestination;

  public DestinationGoalSearchSession(UUID callerId, Caller callerType,
                                      Cell origin, Cell destination,
                                      boolean persistentOrigin, boolean persistentDestination) {
    super(callerId, callerType, origin, persistentOrigin);
    this.destination = destination;
    this.persistentDestination = persistentDestination;
  }

  @Override
  DestinationSearchGraph createSearchGraph() {
    return new DestinationSearchGraph(this, origin, destination);
  }

  @Override
  protected void initSearchExtra() {
    if (origin.domain() == destination.domain()) {
      stateInfo.searchGraph.addPathTrialOriginToDestination(this.modes, persistentOrigin && persistentDestination);
    }

    for (Integer domain : stateInfo.allDomains) {
      // Path trials from tunnel -> destination
      for (Tunnel pathTrialOriginTunnel : stateInfo.tunnelsByDestinationDomain.get(domain)) {
        if (domain.equals(destination.domain())) {
          stateInfo.searchGraph.addPathTrialTunnelToDestination(pathTrialOriginTunnel, this.modes, persistentDestination);
        }
      }
    }
  }
}
