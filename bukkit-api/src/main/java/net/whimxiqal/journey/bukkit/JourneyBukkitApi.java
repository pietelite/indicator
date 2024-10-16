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

package net.whimxiqal.journey.bukkit;

import java.util.ServiceLoader;
import net.whimxiqal.journey.Cell;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.ApiStatus;

/**
 * The interface for Bukkit-specific external-facing endpoints for Journey.
 */
public interface JourneyBukkitApi {

  /**
   * Get the Bukkit API instance.
   *
   * @return the Journey Bukkit API
   */
  static JourneyBukkitApi get() {
    return JourneyBukkitApi.Provider.INSTANCE;
  }

  /**
   * Convert the Bukkit {@link World} to a domain identifier, which is used to identify
   * worlds in Journey.
   *
   * @param world the world
   * @return the domain id
   */
  int toDomain(World world);

  /**
   * Convert a domain identifier to a Bukkit {@link World}.
   *
   * @param domain the domain id
   * @return the world
   */
  World toWorld(int domain);

  /**
   * Convert a Bukkit {@link Location} to a {@link Cell}, which is just Journey's version of a location.
   *
   * @param location the location
   * @return the cell
   */
  Cell toCell(Location location);

  /**
   * Convert a {@link Cell} to a Bukkit {@link Location}.
   *
   * @param cell the cell
   * @return the location
   */
  Location toLocation(Cell cell);

  @ApiStatus.Internal
  final class Provider {

    private static final JourneyBukkitApi INSTANCE = ServiceLoader.load(
            JourneyBukkitApi.class,
            JourneyBukkitApi.class.getClassLoader())
        .findFirst().orElseThrow();

    private Provider() {
    }
  }

}
