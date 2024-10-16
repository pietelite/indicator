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

import java.util.ServiceLoader;
import net.whimxiqal.journey.navigation.NavigationApi;
import net.whimxiqal.journey.search.SearchApi;
import org.jetbrains.annotations.ApiStatus;

/**
 * The central interface for all external-facing endpoints for Journey.
 *
 * <p><b>Note: Methods annotated with {@link Synchronous} must be accessed on the main server thread.</b>
 */
public interface JourneyApi {

  /**
   * Get the API instance.
   *
   * @return the Journey API
   */
  static JourneyApi get() {
    return Provider.INSTANCE;
  }

  /**
   * Register a {@link Scope} to Journey. {@link Scope}s will provide players with the information about
   * other plugin's custom destinations.
   *
   * @param id    the id of the scope. Each character must either be alphanumeric, a hyphen, or a space.
   * @param scope the scope
   */
  @Synchronous
  void registerScope(String plugin, String id, Scope scope);

  /**
   * Register a supplier of {@link Tunnel}s, indicating unique methods of travel through which
   * players may get to locations unnaturally.
   *
   * @param tunnelSupplier the supplier of tunnels for a given player
   */
  @Synchronous
  void registerTunnels(String plugin, TunnelSupplier tunnelSupplier);

  /**
   * Get the sub-API for searching/pathfinding.
   *
   * @return the searching API
   */
  SearchApi searching();

  /**
   * Get the sub-API for navigation.
   *
   * @return the navigation API
   */
  NavigationApi navigating();

  @ApiStatus.Internal
  final class Provider {

    private static final JourneyApi INSTANCE = ServiceLoader.load(
            JourneyApi.class,
            JourneyApi.class.getClassLoader())
        .findFirst().orElseThrow();

    private Provider() {
    }
  }

}
