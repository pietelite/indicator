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

import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.Tunnel;
import net.whimxiqal.journey.tools.AlternatingList;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link SearchGraph} for finding a path to a specific domain
 */
public class DomainSearchGraph extends SearchGraph {

  private final int destinationDomain;

  public DomainSearchGraph(GraphGoalSearchSession<DomainSearchGraph> session, Cell origin, int domain) {
    super(session, origin);
    this.destinationDomain = domain;
  }

  @Nullable
  @Override
  public ItineraryTrial calculate(boolean mustUseCache) {
    AlternatingList<Tunnel, DestinationPathTrial, Object> graphPath = findMinimumPath(originNode,
        node -> node.exit().domain() == destinationDomain,
        trial -> !(mustUseCache && trial.isFromCache()));
    if (graphPath == null) {
      return null;
    } else {
      return new ItineraryTrial(session, origin, graphPath, session.flags);
    }
  }

}
