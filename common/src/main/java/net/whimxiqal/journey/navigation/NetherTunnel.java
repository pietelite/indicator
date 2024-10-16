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

package net.whimxiqal.journey.navigation;

import net.whimxiqal.journey.BoxTargetTunnel;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.CellBox;
import net.whimxiqal.journey.Journey;
import net.whimxiqal.journey.Target;
import net.whimxiqal.journey.BoxTarget;
import net.whimxiqal.journey.Tunnel;
import org.jetbrains.annotations.NotNull;

/**
 * A tunnel that represents a Minecraft Nether Portal.
 */
public final class NetherTunnel extends BoxTargetTunnel {

  public static final int COST = 8;
  private final CellBox entrance;
  private final Cell exit;

  /**
   * General constructor.
   *
   * @param entrance the origin of the portal
   * @param exit     the destination of the portal
   */
  public NetherTunnel(@NotNull final CellBox entrance, @NotNull final Cell exit) {
    super(entrance, exit, COST, () -> {}, null);
    this.entrance = entrance;
    this.exit = exit;
  }

  public boolean verify() {
    for (int x = entrance.min().blockX(); x <= entrance.max().blockX(); x++) {
      for (int y = entrance.min().blockY(); y <= entrance.max().blockY(); y++) {
        for (int z = entrance.min().blockZ(); z <= entrance.max().blockZ(); z++) {
          if (!Journey.get().proxy().platform().toBlock(new Cell(x, y, z, entrance.domain())).isNetherPortal()) {
            return false;
          }
        }
      }
    }
    return true;
  }

  @Override
  public String toString() {
    return "NetherTunnel: "
        + entrance + " -> "
        + exit;
  }

}
