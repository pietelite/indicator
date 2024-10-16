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

package net.whimxiqal.journey.bukkit.util;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.Journey;
import net.whimxiqal.journey.bukkit.JourneyBukkit;
import net.whimxiqal.journey.navigation.PlatformProxy;
import net.whimxiqal.journey.search.flag.FlagSet;
import net.whimxiqal.journey.search.flag.Flags;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Snow;
import org.bukkit.util.Vector;

/**
 * A utility class to handle general odd Spigot Minecraft-related operations.
 */
public final class BukkitUtil {

  /**
   * Return true if the given block can be possibly passed through vertically,
   * like falling through or flying upwards through.
   *
   * @param block the block
   * @return true if it can be passed
   */
  public static boolean isVerticallyPassable(BlockData block) {
    return isVerticallyPassable(block, Collections.emptySet());
  }

  /**
   * Return true if you can pass vertically through this block.
   *
   * @param block the block
   * @return false if you cannot pass through this block vertically
   */
  public static boolean isVerticallyPassable(BlockData block, Set<Material> forcePassable) {
    if (isPassable(block, forcePassable)) {
      return true;
    }
    return MaterialGroups.isVerticallySpecialPassable(block.getMaterial());
  }

  /**
   * Determine if a block can be laterally passed through, as in,
   * can the entity move in a lateral direction and go through that
   * block location.
   *
   * @param block the block
   * @return true if passable
   */
  public static boolean isLaterallyPassable(BlockData block) {
    if (isPassable(block)) {
      return true;
    }
    return MaterialGroups.isLaterallySpecialPassable(block.getMaterial());
  }

  /**
   * Return true if you can pass laterally through this block.
   *
   * @param block the block
   * @return false if you cannot pass through this block laterally
   */
  public static boolean isLaterallyPassable(BlockData block, FlagSet flagSet) {
    if (isPassable(block)) {
      return true;
    }
    if (block.getMaterial() == Material.IRON_DOOR && flagSet.getValueFor(Flags.DOOR)) {
      return true;
    }
    return MaterialGroups.isLaterallySpecialPassable(block.getMaterial());
  }

  /**
   * Determine if this block is generally passable.
   *
   * @param block the block
   * @return true if passable
   */
  public static boolean isPassable(BlockData block) {
    return isPassable(block, Collections.emptySet());
  }

  /**
   * Return true if you can pass through this block in any direction.
   *
   * @param block the block
   * @return false if you cannot pass through at all
   */
  public static boolean isPassable(BlockData block, Set<Material> forcePassable) {
    if (forcePassable.contains(block.getMaterial())) {
      return true;
    }
    if (block instanceof Snow) {
      if (((Snow) block).getLayers() == 1) {
        return true;
      }
    }
    return MaterialGroups.isPassable(block.getMaterial());
  }

  /**
   * Can a player be supported by this block below him.
   *
   * @param block the block
   * @return false if a player cannot stand on top of the block
   */
  public static boolean canStandOn(BlockData block) {
    return !MaterialGroups.isPassable(block.getMaterial())
        || MaterialGroups.TALL_SOLIDS.contains(block.getMaterial());
  }

  /**
   * Can a player be supported by this block within the same block as him.
   *
   * @param block the block
   * @return false if a player cannot stand within this block and be supported
   */
  public static boolean canStandIn(BlockData block) {
    return isLaterallyPassable(block)
        && !isVerticallyPassable(block);
  }

  public static PlatformProxy.DomainInfo getDomain(World world) {
    return new PlatformProxy.DomainInfo(world.getName(), Journey.get().domainManager().domainIndex(getWorldId(world)));
  }

  public static UUID getWorldId(World world) {
    return world.getUID();
  }

  public static Cell toCell(Location location) {
    return new Cell(location.getBlockX(), location.getBlockY(), location.getBlockZ(), getDomain(Objects.requireNonNull(location.getWorld())).id());
  }

  public static World getWorld(Cell cell) {
    return getWorld(Journey.get().domainManager().domainId(cell.domain()));
  }

  public static World getWorld(UUID domainId) {
    World world = Bukkit.getWorld(domainId);
    if (world == null) {
      throw new IllegalArgumentException("There is no world with id " + domainId);
    }
    return world;
  }

  public static World getWorld(int domain) {
    return getWorld(Journey.get().domainManager().domainId(domain));
  }

  /**
   * Get the Bukkit block data at the given location.
   * Must be called on the main server thread.
   *
   * @param cell cell
   * @return the block data at the cell location
   */
  public static BlockData getBlock(Cell cell) {
    if (!Bukkit.isPrimaryThread()) {
      throw new IllegalThreadStateException("Calls to getBlock must be made on the main thread");
    }
    return getWorld(Journey.get().domainManager().domainId(cell.domain())).getBlockAt(cell.blockX(), cell.blockY(), cell.blockZ()).getBlockData();
  }

  public static Location toLocation(Cell cell) {
    return new Location(getWorld(cell), cell.blockX(), cell.blockY(), cell.blockZ());
  }

  public static net.whimxiqal.journey.math.Vector toLocalVector(Vector vector) {
    return new net.whimxiqal.journey.math.Vector(vector.getX(), vector.getY(), vector.getZ());
  }

  public static <T> T waitUntil(Future<T> future) {
    if (Bukkit.isPrimaryThread()) {
      throw new RuntimeException("This was called on the main server thread");  // programmer error
    }
    try {
      return future.get();
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static void runSync(Runnable runnable) {
    if (Bukkit.isPrimaryThread()) {
      runnable.run();
      return;
    }
    CompletableFuture<Void> future = new CompletableFuture<>();
    Bukkit.getScheduler().runTask(JourneyBukkit.get(), () -> {
      try {
        runnable.run();
      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        future.complete(null);
      }
    });
    waitUntil(future);
  }

  public static <T> T supplySync(Supplier<T> supplier) {
    AtomicReference<T> ref = new AtomicReference<>();
    runSync(() -> ref.set(supplier.get()));
    return ref.get();
  }

}
