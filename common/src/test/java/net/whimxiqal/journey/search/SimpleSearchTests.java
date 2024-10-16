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

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.CellTarget;
import net.whimxiqal.journey.Journey;
import net.whimxiqal.journey.JourneyTestHarness;
import net.whimxiqal.journey.manager.DistributedWorkManager;
import net.whimxiqal.journey.manager.TestSchedulingManager;
import net.whimxiqal.journey.navigation.Itinerary;
import net.whimxiqal.journey.platform.TestJourneyPlayer;
import net.whimxiqal.journey.platform.TestPlatformProxy;
import net.whimxiqal.journey.platform.WorldLoader;
import net.whimxiqal.journey.search.flag.FlagSet;
import net.whimxiqal.journey.search.flag.Flags;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SimpleSearchTests extends JourneyTestHarness {

  private UUID lastSessionUuid;
  private FlagSet flags;

  private SearchSession destinationSession(String origin, String destination) {
    Cell destinationCell = TestPlatformProxy.pois.get(destination);
    return new DestinationGoalSearchSession(new TestJourneyPlayer(PLAYER_UUID),
        TestPlatformProxy.pois.get(origin),
        new CellTarget(destinationCell),
        destinationCell::equals,
        false, false);
  }

  private SearchSession domainSession(String origin, int domainDestination) {
    return new DomainGoalSearchSession(new TestJourneyPlayer(PLAYER_UUID), TestPlatformProxy.pois.get(origin), domainDestination, false);
  }

  private void runDestinationSearch(String origin, String destination, ResultState expectedResult) throws InterruptedException, ExecutionException {
    runSearch(destinationSession(origin, destination), expectedResult);
  }

  private void runDomainSearch(String origin, String domainName, ResultState expectedResult) throws InterruptedException, ExecutionException {
    runSearch(domainSession(origin, Journey.get().domainManager().domainIndex(WorldLoader.getWorld(domainName).uuid)), expectedResult);
  }

  private void runSearch(SearchSession session, ResultState expectedResult) throws InterruptedException, ExecutionException {
    session.initialize();
    lastSessionUuid = session.uuid();
    Assertions.assertEquals(ResultState.IDLE, session.getState());
    session.addFlags(flags);
    Future<SearchSession.Result> future = TestSchedulingManager.runOnMainThread(() -> Journey.get().searchManager().launchIngameSearch(session));
    SearchSession.Result result = future.get();
    Assertions.assertEquals(expectedResult, result.state(), "Expected state of session search was incorrect");
    if (result.itinerary() != null) {
      SESSION_ITINERARIES.put(session.uuid, result.itinerary());
    }
  }

  @BeforeEach
  void beforeEach() {
    flags = new FlagSet();
    flags.addFlag(Flags.TIMEOUT, 5);
  }

  @Test
  void destinationSearchSameWorld() throws InterruptedException, ExecutionException {
    runDestinationSearch("1", "2", ResultState.STOPPED_SUCCESSFUL);
  }

  @Test
  void destinationSearchCaching() throws InterruptedException, ExecutionException {
    runDestinationSearch("1", "2", ResultState.STOPPED_SUCCESSFUL);
    Thread.sleep(100);  // wait for caching to complete on async thread
    runDestinationSearch("1", "2", ResultState.STOPPED_SUCCESSFUL);  // should use cached values
  }

  @Test
  void destinationSearchesMultipleWorlds() throws InterruptedException, ExecutionException {
    runDestinationSearch("1", "3", ResultState.STOPPED_SUCCESSFUL);
  }

  @Test
  void destinationSearchesPathLengths() throws InterruptedException, ExecutionException {
    runDestinationSearch("1", "3", ResultState.STOPPED_SUCCESSFUL);
    Itinerary oneToThree = SESSION_ITINERARIES.get(lastSessionUuid);

    runDestinationSearch("2", "3", ResultState.STOPPED_SUCCESSFUL);
    Itinerary twoToThree = SESSION_ITINERARIES.get(lastSessionUuid);

    // The distance from 1 -> a/A -> b/B -> 3 is shorter than the distance from 2 -> c/C -> b/B -> 3
    Assertions.assertTrue(oneToThree.cost() < twoToThree.cost());
  }

  @Test
  void destinationSearchesFailed() throws InterruptedException, ExecutionException {
    runDestinationSearch("3", "1", ResultState.STOPPED_FAILED);
    runDestinationSearch("1", "4", ResultState.STOPPED_FAILED);
  }

  @Test
  void domainSearchSuccess() throws InterruptedException, ExecutionException {
    runDomainSearch("1", "world2", ResultState.STOPPED_SUCCESSFUL);
    runDomainSearch("2", "world2", ResultState.STOPPED_SUCCESSFUL);
  }

  @Test
  void domainSearchFail() throws InterruptedException, ExecutionException {
    // 3 is completed walled off
    runDomainSearch("3", "world2", ResultState.STOPPED_FAILED);
  }

  @Test
  void domainSearchSameWorldError() throws InterruptedException, ExecutionException {
    // cannot go to the same world
    System.out.println("Starting test domainSearchSameWorldError()");
    runDomainSearch("1", "world1", ResultState.STOPPED_ERROR);
    runDomainSearch("2", "world1", ResultState.STOPPED_ERROR);
    runDomainSearch("3", "world1", ResultState.STOPPED_ERROR);
  }

  @Test
  void animation() throws InterruptedException, ExecutionException {
    flags.addFlag(Flags.ANIMATE, 5);
    TestPlatformProxy.animatedBlocks = 0;
    runDestinationSearch("1", "2", ResultState.STOPPED_SUCCESSFUL);
    Assertions.assertTrue(TestPlatformProxy.animatedBlocks > 0);
  }

  @Test
  void stressTest() throws InterruptedException, NoSuchFieldException, IllegalAccessException {
    // Set the work manager manually in main Journey class, so we can test with multiple threads and active path searches
    Field workManagerField = Journey.class.getDeclaredField("workManager");
    workManagerField.setAccessible(true);
    workManagerField.set(Journey.get(), new DistributedWorkManager(10));

    // Set cells-per-execution-cycle to something small to stress-test the DistributedWorkManager
    PathTrial.CELLS_PER_EXECUTION_CYCLE = 5;
    final int RUNS = 30;
    final int total = RUNS * 12;
    AtomicInteger finished = new AtomicInteger(0);
    AtomicInteger failed = new AtomicInteger(0);

    BiFunction<String, String, SearchSession> newSearch = (origin, destination) -> {
      Cell destinationCell = TestPlatformProxy.pois.get(destination);
      return new DestinationGoalSearchSession(new TestJourneyPlayer(UUID.randomUUID()),
          TestPlatformProxy.pois.get(origin),
          new CellTarget(destinationCell),
          destinationCell::equals,
          false, false);
    };

    BiConsumer<SearchSession, ResultState> runSearchAsync = (session, expected) -> {
      session.initialize();
      Assertions.assertEquals(ResultState.IDLE, session.getState());
      session.flags().addFlag(Flags.TIMEOUT, 20);
      session.search().thenAccept(result -> {
        finished.incrementAndGet();
        if (expected != result.state()) {
          System.err.println(session + ": Expected " + expected + ", got " + result.state());
          failed.incrementAndGet();
        }
      });
    };

    for (int i = 0; i < RUNS; i++) {
      TestSchedulingManager.runOnMainThread(() -> {
        runSearchAsync.accept(newSearch.apply("1", "2"), ResultState.STOPPED_SUCCESSFUL);
        runSearchAsync.accept(newSearch.apply("1", "3"), ResultState.STOPPED_SUCCESSFUL);
        runSearchAsync.accept(newSearch.apply("1", "4"), ResultState.STOPPED_FAILED);
        runSearchAsync.accept(newSearch.apply("2", "3"), ResultState.STOPPED_SUCCESSFUL);
        runSearchAsync.accept(newSearch.apply("2", "4"), ResultState.STOPPED_FAILED);
        runSearchAsync.accept(newSearch.apply("3", "4"), ResultState.STOPPED_FAILED);

        runSearchAsync.accept(newSearch.apply("2", "1"), ResultState.STOPPED_SUCCESSFUL);
        runSearchAsync.accept(newSearch.apply("3", "1"), ResultState.STOPPED_FAILED);
        runSearchAsync.accept(newSearch.apply("4", "1"), ResultState.STOPPED_FAILED);
        runSearchAsync.accept(newSearch.apply("3", "2"), ResultState.STOPPED_FAILED);
        runSearchAsync.accept(newSearch.apply("4", "2"), ResultState.STOPPED_FAILED);
        runSearchAsync.accept(newSearch.apply("4", "3"), ResultState.STOPPED_FAILED);
      });
    }

    long failureTime = System.currentTimeMillis() + (1000 * 30);  // 30 seconds until we consider it failure
    while (finished.get() != total) {
      Thread.sleep(100);
      if (System.currentTimeMillis() > failureTime) {
        Assertions.fail("The test took too long. Only " + finished.get() + " out of " + total + " finished.");
      }
    }
    Assertions.assertEquals(0, failed.get(), "There were unexpected search results");
  }

}
