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

package net.whimxiqal.journey;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import net.whimxiqal.journey.navigation.Itinerary;
import net.whimxiqal.journey.search.DestinationGoalSearchSession;
import net.whimxiqal.journey.search.SearchApi;
import net.whimxiqal.journey.search.SearchFlag;
import net.whimxiqal.journey.search.SearchFlags;
import net.whimxiqal.journey.search.SearchResult;
import net.whimxiqal.journey.search.SearchResultImpl;
import net.whimxiqal.journey.search.SearchSession;
import net.whimxiqal.journey.search.flag.FlagSet;
import net.whimxiqal.journey.search.flag.Flags;

public class SearchApiImpl implements SearchApi {

  public CompletableFuture<SearchResult> runDestinationSearch(JourneyAgent agent, Cell origin, Cell destination, SearchFlag<?>... flags) {
    CompletableFuture<SearchResult> future = new CompletableFuture<>();
    if (Journey.get().proxy().schedulingManager().isMainThread()) {
      runDestinationSearchInternal(agent, origin, destination, future, flags);
    } else {
      Journey.get().proxy().schedulingManager().schedule(() -> runDestinationSearchInternal(agent, origin, destination, future, flags), false);
    }
    return future;
  }

  private void runDestinationSearchInternal(JourneyAgent agent, Cell origin, Cell destination, CompletableFuture<SearchResult> future, SearchFlag<?>[] flags) {
    if (!Journey.get().proxy().schedulingManager().isMainThread()) {
      throw new IllegalThreadStateException();
    }
    DestinationGoalSearchSession session = new DestinationGoalSearchSession(null, SearchSession.Caller.PLUGIN, agent,
        origin, () -> new CellTarget(destination), false, false);
    session.addFlags(FlagSet.from(flags));
    session.initialize();  // sets the modes and tunnels (must be run on main thread)
    session.search().thenAccept(result -> {
      switch (result.state()) {
        case STOPPED_SUCCESSFUL -> {
          Itinerary itinerary = result.itinerary();
          if (itinerary == null) {
            Journey.logger().error("Found null itinerary from result that returned success");
            future.complete(new SearchResultImpl(SearchResult.Status.ERROR, null));
            return;
          }
          future.complete(new SearchResultImpl(SearchResult.Status.SUCCESS, Collections.unmodifiableList(itinerary.steps())));
        }
        case STOPPED_CANCELED -> future.complete(new SearchResultImpl(SearchResult.Status.CANCELED, null));
        case STOPPED_FAILED -> future.complete(new SearchResultImpl(SearchResult.Status.FAILED, null));
        case STOPPED_ERROR -> future.complete(new SearchResultImpl(SearchResult.Status.ERROR, null));
        default -> {
          Journey.logger().error("Session completed with invalid final state: " + result.state());
          future.complete(new SearchResultImpl(SearchResult.Status.ERROR, null));
        }
      }
    });
  }

  @Override
  public CompletableFuture<SearchResult> runPlayerDestinationSearch(UUID playerUuid, Cell destination, boolean display, SearchFlag<?>... flags) {
    CompletableFuture<SearchResult> future = new CompletableFuture<>();
    if (Journey.get().proxy().schedulingManager().isMainThread()) {
      runPlayerDestinationSearchInternal(playerUuid, destination, display, future, flags);
    } else {
      Journey.get().proxy().schedulingManager().schedule(() -> runPlayerDestinationSearchInternal(playerUuid, destination, display, future, flags), false);
    }
    return future;
  }

  @Override
  public CompletionStage<SearchResult> runDestinationSearch(JourneyAgent agent, Cell origin, Cell destination, SearchFlags flags) {
    return runDestinationSearch(agent, origin, destination, flags.get().toArray(new SearchFlag[0]));
  }

  @Override
  public CompletionStage<SearchResult> runPlayerDestinationSearch(UUID playerUuid, Cell destination, SearchFlags searchFlags) {
    return runPlayerDestinationSearch(playerUuid, destination, false, searchFlags.get().toArray(new SearchFlag[0]));
  }

  private void runPlayerDestinationSearchInternal(UUID playerUuid, Cell destination, boolean display, CompletableFuture<SearchResult> future, SearchFlag<?>... flags) {
    if (!Journey.get().proxy().schedulingManager().isMainThread()) {
      throw new IllegalThreadStateException();
    }
    Optional<InternalJourneyPlayer> player = Journey.get().proxy().platform().onlinePlayer(playerUuid);
    if (player.isEmpty()) {
      future.complete(new SearchResultImpl(SearchResult.Status.ERROR, null));
      return;
    }
    Optional<Cell> playerLocation = player.get().location();
    if (playerLocation.isEmpty()) {
      future.complete(new SearchResultImpl(SearchResult.Status.ERROR, null));
      return;
    }

    DestinationGoalSearchSession session = new DestinationGoalSearchSession(null, SearchSession.Caller.PLUGIN, player.get(),
        playerLocation.get(), new CellTarget(destination), destination::equals, false, false);
    session.addFlags(FlagSet.from(flags));
    session.initialize();  // sets the modes and tunnels (must be run on main thread)
    session.search().thenAccept(result -> {
      switch (result.state()) {
        case STOPPED_SUCCESSFUL -> {
          Itinerary itinerary = result.itinerary();
          if (itinerary == null) {
            Journey.logger().error("Found null itinerary from result that returned success");
            future.complete(new SearchResultImpl(SearchResult.Status.ERROR, null));
            return;
          }

          SearchResult searchResult = new SearchResultImpl(SearchResult.Status.SUCCESS, itinerary.steps()
              .stream()
              .toList());
          if (display) {
            Journey.get().proxy().schedulingManager().schedule(() -> {
              Journey.get().navigatorManager().stopNavigators(session.agent().uuid());
              Journey.get().navigatorManager().startNavigating(session.agent(), itinerary.steps(), session.flags().getValueFor(Flags.NAVIGATOR));
              future.complete(searchResult);
            }, false);
          } else {
            future.complete(searchResult);
          }
        }
        case STOPPED_CANCELED -> future.complete(new SearchResultImpl(SearchResult.Status.CANCELED, null));
        case STOPPED_FAILED -> future.complete(new SearchResultImpl(SearchResult.Status.FAILED, null));
        case STOPPED_ERROR -> future.complete(new SearchResultImpl(SearchResult.Status.ERROR, null));
        default -> {
          Journey.logger().error("Session completed with invalid final state: " + result.state());
          future.complete(new SearchResultImpl(SearchResult.Status.ERROR, null));
        }
      }
    });
  }

}
