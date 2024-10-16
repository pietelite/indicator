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

import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.bukkit.util.BukkitUtil;
import org.bukkit.Location;
import org.bukkit.World;

public class JourneyBukkitApiImpl implements JourneyBukkitApi {
  @Override
  public int toDomain(World world) {
    return BukkitUtil.getDomain(world).id();
  }

  @Override
  public World toWorld(int domain) {
    return BukkitUtil.getWorld(domain);
  }

  @Override
  public Cell toCell(Location location) {
    return BukkitUtil.toCell(location);
  }

  @Override
  public Location toLocation(Cell cell) {
    return BukkitUtil.toLocation(cell);
  }
}
