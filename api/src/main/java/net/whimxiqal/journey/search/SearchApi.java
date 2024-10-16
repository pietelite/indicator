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
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.JourneyAgent;

/**
 * A sub-API to be used for managing searching within Journey.
 */
public interface SearchApi {

  /**
   * Execute a search from a given origin to a given destination for a given {@link JourneyAgent}.
   * Flags may be specified to alter the behavior of the search.
   *
   * <p>The resulting completion stage will always be completed on the main thread
   *
   * @param agent       the agent
   * @param origin      the origin of the search
   * @param destination the destination of the search
   * @param flags       the flags to alter the behavior of the search
   * @return a completion stage with the result of the search, for callbacks
   */
  CompletionStage<SearchResult> runDestinationSearch(JourneyAgent agent,
                                                     Cell origin, Cell destination,
                                                     SearchFlags flags);

  /**
   * Execute a search for a player to a given destination.
   * The result may optionally be displayed to the player in the same fashion
   * it is presented in Journey by standard commands.
   * <b>This may be called asynchronously.</b>
   *
   * @param playerUuid  the uuid of the player
   * @param destination the destination
   * @param display     true if the player should be shown the resulting path
   * @param flags       optional flags to adjust the behavior of the search
   * @return a future of the result of the search
   */
  @Deprecated(since = "1.2.0")
  Future<SearchResult> runPlayerDestinationSearch(UUID playerUuid, Cell destination,
                                                  boolean display, SearchFlag<?>... flags);

  /**
   * Execute a search from a given origin to a given destination for a player with the given UUID.
   * The origin of the search will be the player's current location.
   * Flags may be specified to alter the behavior of the search.
   *
   * <p>The resulting completion stage will always be completed on the main thread
   *
   * @param playerUuid  the uuid of the player
   * @param destination the destination of the search
   * @param flags       the flags to alter the behavior of the search
   * @return a completion stage with the result of the search, for callbacks
   */
  CompletionStage<SearchResult> runPlayerDestinationSearch(UUID playerUuid, Cell destination,
                                                           SearchFlags flags);

}
