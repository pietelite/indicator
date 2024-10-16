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

package net.whimxiqal.journey.manager;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.CellBox;
import net.whimxiqal.journey.Journey;
import net.whimxiqal.journey.data.TunnelType;
import net.whimxiqal.journey.navigation.NetherTunnel;

/**
 * A manager for all nether portals.
 */
public final class NetherManager {

  private final Map<CellBox, Cell> portalConnections = new ConcurrentHashMap<>();

  public void initialize() {
    // Calls to the db directly
    Journey.get().proxy().dataManager()
        .netherPortalManager()
        .getAllTunnels(TunnelType.NETHER)
        .forEach(tunnel -> portalConnections.put(tunnel.box(), tunnel.exit()));
    Journey.get().tunnelManager().register(player -> Journey.get().netherManager().makeTunnels());
  }

  /**
   * Create tunnels specifically representing all nether portals in the world.
   *
   * @return all nether tunnels
   */
  public Collection<NetherTunnel> makeTunnels() {
    List<NetherTunnel> linksUnverified = portalConnections.entrySet().stream()
        .map(entry -> new NetherTunnel(entry.getKey(), entry.getValue())).toList();
    List<NetherTunnel> linksVerified = new LinkedList<>();
    List<NetherTunnel> tunnelsToRemove = new LinkedList<>();
    for (NetherTunnel tunnel : linksUnverified) {
      if (tunnel.verify()) {
        linksVerified.add(tunnel);
      } else {
        // put new nether tunnel in list to send to async thread
        tunnelsToRemove.add(new NetherTunnel(tunnel.box(), tunnel.exit()));
      }
    }
    if (!tunnelsToRemove.isEmpty()) {
      Journey.get().proxy().schedulingManager().schedule(() -> {
        for (NetherTunnel tunnel : tunnelsToRemove) {
          portalConnections.remove(tunnel.box(), tunnel.exit());
          Journey.get().proxy().dataManager().netherPortalManager().removeTunnels(tunnel.box(), tunnel.exit(), TunnelType.NETHER);
        }
      }, true);
    }
    return linksVerified;
  }

  public void lookForPortal(Cell origin, Supplier<Cell> destination) {
    Optional<PortalGroup> originGroup = locateAll(origin,
        8,
        origin.blockY() - 8,
        origin.blockY() + 8)
        .stream()
        .min(Comparator.comparingDouble(group ->
            group.tunnelLocation().distanceToSquared(origin)));
    if (originGroup.isEmpty()) {
      return;  // We can't find the origin portal
    }
    lookForPortal(destination, originGroup.get(), 0);
  }

  private void lookForPortal(Supplier<Cell> resultantLocation, PortalGroup originGroup, int count) {
    if (count > 5) {
      // only try five times. 5 seconds is enough
      Journey.logger().debug("[Nether Manager] Tried to look for nether portal starting at "
          + originGroup.blocks().stream().findFirst().get()
          + " but found none");
      return;
    }
    Journey.get().proxy().schedulingManager().schedule(() -> {
      Optional<PortalGroup> destinationGroup = locateAll(resultantLocation.get(),
          16,
          resultantLocation.get().blockY() - 16,
          resultantLocation.get().blockY() + 16)
          .stream()
          .findFirst();
      if (destinationGroup.isEmpty() || destinationGroup.get().blocks().isEmpty()) {
        return;  // We can't find the destination portal
      }

      if (originGroup.tunnelLocation().domain() == destinationGroup.get().tunnelLocation().domain()) {
        // If they're in the same world, we have the same portal! We haven't actually teleported yet. Try again
        lookForPortal(resultantLocation, originGroup, count + 1);
        return;
      }

      // Schedule update on async so db call happens off main thread
      Journey.get().proxy().schedulingManager().schedule(() -> {
        // Check if we have any portals with this origin and destination already. If so, and the one found here is
        //  different, we have to remove the old one(s)

        // TODO pieter: we have to somehow store portals based on the entire portal region
        //  We can do this probably by mapping from min-block to both max-block and destination min/max block of portal
        //  So then we only really need to check the min/max of the candidate originGroup, which is nice
        //  Also, I don't think a concurrent hashmap for the portalConnections is sufficient handling for multi-threading.
        //  The portal connections can change within a single execution of this scheduled task, so we should probably
        //  lock the whole map while we are doing these checks. Idk though, more analysis here is needed
        CellBox tunnelEntrance = originGroup.getBox();
        Cell tunnelExit = destinationGroup.get().tunnelLocation();
        Cell existingExit = portalConnections.get(tunnelEntrance);
        if (existingExit != null) {
          if (existingExit.equals(tunnelExit)) {
            // We already have the correct portal tunnel, no need to continue
            return;
          }
          // we have the wrong portal tunnel
          portalConnections.remove(tunnelEntrance);
          Journey.get().proxy().dataManager()
              .netherPortalManager()
              .removeTunnelsWithOrigin(tunnelEntrance, TunnelType.NETHER);
          Journey.logger().debug("[Nether Manager] Removed nether portal tunnel: " + tunnelEntrance + " -> " + existingExit);
        }

        // Add the portal
        Cell previous = portalConnections.put(tunnelEntrance, tunnelExit);
        Journey.get().proxy().dataManager().netherPortalManager().addTunnel(tunnelEntrance,
            destinationGroup.get().tunnelLocation(),
            TunnelType.NETHER);
        if (previous == null) {
          Journey.logger().debug("[Nether Manager] Added nether tunnel: " + tunnelEntrance + " -> " + tunnelExit);
        }
      }, true);
    }, false, 20);
  }

  /**
   * Clear all stored nether portals, from both db and cache.
   *
   * @return completion stage, to be used to run async logic after the reset is completed
   */
  public CompletionStage<Void> reset() {
    CompletableFuture<Void> future = new CompletableFuture<>();
    Journey.get().proxy().schedulingManager().schedule(() -> {
      portalConnections.clear();
      Journey.get().proxy().dataManager().netherPortalManager().removeTunnels(TunnelType.NETHER);
      future.complete(null);
    }, true);
    return future;
  }

  /**
   * Get the number of portal connections that are registered.
   *
   * @return the portal connection count
   */
  public int size() {
    return portalConnections.size();
  }


  /**
   * * Gets the nearest Nether portal within the specified radius in relation to the given cell.
   *
   * @param origin    Center of the search radius
   * @param radius    The search radius
   * @param minHeight Minimum height of search
   * @param maxHeight Maximum height of search
   * @return Returns cell in the bottom center of the nearest nether portal if found. Otherwise, returns null.
   */
  private Collection<PortalGroup> locateAll(Cell origin,
                                            int radius,
                                            int minHeight,
                                            int maxHeight) {
    Set<PortalGroup> portals = new HashSet<>();  // All PortalGroups found
    Set<Cell> stored = new HashSet<>();  // All Portal blocks found in the PortalGroups
    int domain = origin.domain();

    int startY = Math.max(origin.blockY() - radius, minHeight);
    int endY = Math.min(origin.blockY() + radius, maxHeight);

    for (int x = origin.blockX() - radius; x <= origin.blockX() + radius; x++) {
      for (int y = startY; y <= endY; y += 2) {
        for (int z = origin.blockZ() - radius; z <= origin.blockZ() + radius; z++) {
          if ((x + z) % 2 == 0) {
            continue;  // Check only in checkerboard pattern
          }
          // Location being iterated over.
          Cell cell = new Cell(x, y, z, domain);
          // Don't do anything if the Portal block is already stored.
          if (stored.contains(cell)) {
            continue;
          }

          PortalGroup pg = getPortalBlocks(cell);
          // Do nothing if there are no Portal blocks
          if (pg == null) {
            continue;
          }
          // If the PortalGroup was added, store the Portal blocks in the Collection
          if (portals.add(pg)) {
            stored.addAll(pg.blocks());
          }
        }
      }
    }
    return portals;
  }

  /**
   * Gets the Portal blocks that is part of the Nether portal.
   * Will return null if there are no portals or the portal that has been found
   * has too few portal blocks (<6).
   *
   * @param cell - Location to start the getting the portal blocks.
   * @return A PortalGroup of all the found Portal blocks. Otherwise, returns null.
   */
  private PortalGroup getPortalBlocks(Cell cell) {
    if (!Journey.get().proxy().platform().toBlock(cell).isNetherPortal()) {
      return null;
    }

    PortalGroup group = portalBlock(new PortalGroup(cell.domain()), cell);
    return group.size() > 5 ? group : null;
  }

  private PortalGroup portalBlock(PortalGroup group, Cell cell) {
    for (int i = -1; i <= 1; i++) {
      for (int j = -1; j <= 1; j++) {
        for (int k = -1; k <= 1; k++) {
          Cell offset = new Cell(cell.blockX() + i, cell.blockY() + j, cell.blockZ() + k, cell.domain());
          if (Journey.get().proxy().platform().toBlock(offset).isNetherPortal() && group.add(offset)) {
            portalBlock(group, offset);
          }
        }
      }
    }
    return group;
  }

  /**
   * Represents a Nether portal. This actually consists of some number of blocks
   */
  public static class PortalGroup {
    private final Set<Cell> portal = new HashSet<>();
    private final int domain;
    private Cell teleportTo;
    private int bottom = Integer.MAX_VALUE;
    private int minX = Integer.MAX_VALUE;
    private int minY = Integer.MAX_VALUE;
    private int minZ = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE;
    private int maxY = Integer.MIN_VALUE;
    private int maxZ = Integer.MIN_VALUE;
    private CellBox box;

    /**
     * A group of Portal block for a Nether portal.
     *
     * @param domain The world the Portal blocks resides in.
     */
    public PortalGroup(int domain) {
      this.domain = domain;
    }

    /**
     * Adds the Location to the PortalGroup.
     *
     * @param cell Vector to add
     * @return if the Location was added. Otherwise, false.
     */
    public boolean add(Cell cell) {
      // Check to see if the block is a Portal block.
      if (!Journey.get().proxy().platform().toBlock(cell).isNetherPortal()) {
        return false;
      }

      boolean added = portal.add(cell);
      // If the cell was added, do more actions.
      if (added) {
        int y = cell.blockY();
        if (y < bottom) {
          // The bottom of the Nether portal
          bottom = cell.blockY();
        }
        // Reset the teleport cell since a new block was added.
        teleportTo = null;
        box = null;

        minX = Math.min(minX, cell.blockX());
        minY = Math.min(minY, cell.blockY());
        minZ = Math.min(minZ, cell.blockZ());
        maxX = Math.max(maxX, cell.blockX());
        maxY = Math.max(maxY, cell.blockY());
        maxZ = Math.max(maxZ, cell.blockZ());
      }
      return added;
    }

    /**
     * Get the number of blocks in the portal.
     *
     * @return the size
     */
    public int size() {
      return portal.size();
    }

    /**
     * Get all the blocks in the portal.
     *
     * @return all portal blocks
     */
    public Collection<Cell> blocks() {
      return Collections.unmodifiableCollection(portal);
    }

    /**
     * Gets the place to teleport an entity to the Nether portal.
     *
     * @return The cell at the bottom center of the Nether portal.
     */
    public Cell tunnelLocation() {
      if (teleportTo != null) {
        return teleportTo;
      }

      if (portal.size() == 0) {
        return null;
      }

      teleportTo = new Cell((maxX + minX) / 2, bottom, (maxZ + minZ) / 2, domain);
      return teleportTo;
    }

    @Override
    public int hashCode() {
      return portal.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof PortalGroup pg) {
        return portal.equals(pg.portal);
      }
      return portal.equals(o);
    }

    public CellBox getBox() {
      if (box != null) {
        return box;
      }

      // find the largest contiguous box of nether portal blocks within cell set
      Cell minCell = null;
      int maxBoxX = Integer.MIN_VALUE;
      int maxBoxY = Integer.MIN_VALUE;
      int maxBoxZ = Integer.MIN_VALUE;
      for (int x = minX; x <= maxX; x++) {
        for (int y = minY; y <= maxY; y++) {
          for (int z = minZ; z <= maxZ; z++) {
            Cell cell = new Cell(x, y, z, domain);
            if (Journey.get().proxy().platform().toBlock(cell).isNetherPortal()) {
              minCell = cell;
              break;
            }
          }
          if (minCell != null) {
            break;
          }
        }
        if (minCell != null) {
          break;
        }
      }

      if (minCell == null) {
        throw new IllegalStateException("Portal group has no nether portal blocks");
      }

      Cell maxCell = minCell;
      boolean done = false;
      for (int x = minCell.blockX(); x <= maxX; x++) {
        for (int y = minCell.blockY(); y <= maxY; y++) {
          for (int z = minCell.blockZ(); z <= maxZ; z++) {
            maxCell = new Cell(x, y, z, domain);
            if (!Journey.get().proxy().platform().toBlock(maxCell).isNetherPortal()) {
              done = true;
              break;
            }
          }
          if (done) {
            break;
          }
        }
        if (done) {
          break;
        }
      }
      return new CellBox(minCell, maxCell);
    }

  }
}