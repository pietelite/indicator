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
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import net.whimxiqal.journey.Journey;
import net.whimxiqal.journey.Tunnel;
import net.whimxiqal.journey.navigation.Mode;
import net.whimxiqal.journey.navigation.ModeType;
import net.whimxiqal.journey.search.flag.Flags;
import net.whimxiqal.journey.util.SimpleTimer;

public class EverythingSearch extends SearchSession {

  private static final int LOG_PERIOD_MS = 1000;  // 5 seconds
  private final LinkedList<DestinationPathTrial> pathTrials = new LinkedList<>();
  private final SimpleTimer logTimer = new SimpleTimer();
  private final AtomicReference<Double> totalLengthToCalculate = new AtomicReference<>(0.0);
  private final AtomicReference<Double> lengthCalculated = new AtomicReference<>(0.0);
  private int pathTrialsCompleted = 0;

  public EverythingSearch(UUID caller, Caller callerType) {
    super(caller, callerType);
    flags.addFlag(Flags.TIMEOUT, -1);
    flags.addFlag(Flags.FLY, false);
    flags.addFlag(Flags.ANIMATE, 0);
    flags.addFlag(Flags.DIG, false);
    flags.addFlag(Flags.DOOR, true);
  }

  @Override
  public void asyncSearch() {
    state.set(ResultState.RUNNING);
    initSearch();
    runSearches();
  }

  private void initSearch() {
    Journey.logger().info("Performing preliminary caching to speed up search times...");
    super.timer.start();
    logTimer.start();

    final Set<Integer> allDomains = new HashSet<>();
    final Map<Integer, List<Tunnel>> tunnelsByOriginDomain = new HashMap<>();
    final Map<Integer, List<Tunnel>> tunnelsByDestinationDomain = new HashMap<>();

    for (Tunnel tunnel : tunnels()) {
      allDomains.add(tunnel.origin().domain());
      allDomains.add(tunnel.destination().domain());
    }

    // Prepare tunnel maps
    for (Integer domain : allDomains) {
      tunnelsByOriginDomain.put(domain, new LinkedList<>());
      tunnelsByDestinationDomain.put(domain, new LinkedList<>());
    }

    // Fill tunnel maps
    for (Tunnel tunnel : tunnels()) {
      tunnelsByOriginDomain.get(tunnel.origin().domain()).add(tunnel);
      tunnelsByDestinationDomain.get(tunnel.destination().domain()).add(tunnel);
    }

    // Collect path trials
    Set<ModeType> modeTypes = modes().stream().map(Mode::type).collect(Collectors.toSet());
    for (Integer domain : allDomains) {
      for (Tunnel pathTrialOriginTunnel : tunnelsByDestinationDomain.get(domain)) {
        for (Tunnel pathTrialDestinationTunnel : tunnelsByOriginDomain.get(domain)) {
          if (!Journey.get().dataManager()
              .pathRecordManager()
              .containsRecord(pathTrialOriginTunnel.destination(), pathTrialDestinationTunnel.origin(), modeTypes)) {
            DestinationPathTrial pathTrial = DestinationPathTrial.approximate(this, pathTrialOriginTunnel.destination(), pathTrialDestinationTunnel.origin(),
                modes(), true);
            pathTrials.add(pathTrial);
            totalLengthToCalculate.set(totalLengthToCalculate.get() + pathTrial.getLength());
          }
        }
      }
    }
  }

  private void runSearches() {
    if (pathTrials.isEmpty()) {
      Journey.logger().info("Caching paths: No paths to cache!");
      state.set(ResultState.STOPPED_SUCCESSFUL);
      complete(null);
      return;
    }

    Journey.logger().info("Caching paths: 0.00 % complete");
    for (DestinationPathTrial pathTrial : pathTrials) {
      double preCalculatedLength = pathTrial.getLength();

      if (pathTrial.isFromCache()) {
        // Already cached, let's not do this again unless we manually clear the cache
        onPathTrialComplete(preCalculatedLength);
      } else {
        Journey.get().workManager().schedule(pathTrial);
        pathTrial.future().thenAccept(result -> onPathTrialComplete(preCalculatedLength));
      }
    }
  }

  private void onPathTrialComplete(double preCalculatedLength) {
    synchronized (this) {
      double length = lengthCalculated.accumulateAndGet(preCalculatedLength, Double::sum);

      if (logTimer.elapsed() > LOG_PERIOD_MS) {
        logTimer.start(); // restart timer
        Journey.logger().info("Caching paths: " + String.format("%.2f", length / Math.max(totalLengthToCalculate.get(), 1) * 100) + " % complete");
      }

      pathTrialsCompleted++;
      if (pathTrialsCompleted == pathTrials.size()) {
        Journey.logger().info("Caching paths: complete!");
        state.set(ResultState.STOPPED_SUCCESSFUL);
        complete(null);
      }
    }
  }

  @Override
  public void initialize() {
    super.initialize();
    setPlayerModes();
    setTunnels(Journey.get().tunnelManager().tunnels(null));
  }

}
