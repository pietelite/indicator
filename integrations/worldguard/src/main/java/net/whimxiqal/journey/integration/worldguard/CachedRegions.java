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

package net.whimxiqal.journey.integration.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.JourneyApi;
import net.whimxiqal.journey.bukkit.JourneyBukkitApi;
import org.bukkit.Bukkit;
import org.bukkit.World;

public class CachedRegions {

  public record CachedRegion(String name, BlockVector3 min, BlockVector3 max) {}

  private static final long CACHE_PERIOD = 20 * 5;  // period in ticks
  private final Map<Integer, List<CachedRegion>> cached = new HashMap<>();

  public void initialize() {
    Bukkit.getScheduler().scheduleSyncRepeatingTask(JourneyWorldGuard.instance(), this::load, CACHE_PERIOD, CACHE_PERIOD);
  }

  public void load() {
    cached.clear();
    for (World world : Bukkit.getWorlds()) {
      RegionManager manager = WorldGuard.getInstance().getPlatform()
          .getRegionContainer()
          .get(BukkitAdapter.adapt(world));
      if (manager == null) {
        continue;
      }
      List<CachedRegion> container = new ArrayList<>(manager.getRegions().size());
      cached.put(JourneyBukkitApi.get().toDomain(world), container);
      manager.getRegions().values().forEach(protectedRegion -> {
        if (!(protectedRegion instanceof ProtectedCuboidRegion)) {
          return;
        }
        container.add(new CachedRegion(protectedRegion.getId(),
            protectedRegion.getMinimumPoint(),
            protectedRegion.getMaximumPoint()));
      });
    }
  }

  public Map<Integer, List<CachedRegion>> get() {
    return Collections.unmodifiableMap(cached);
  }

}
