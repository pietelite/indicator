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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.whimxiqal.journey.Journey;
import net.whimxiqal.journey.data.TunnelType;
import net.whimxiqal.journey.message.Formatter;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.navigation.NetherTunnel;

/**
 * A manager for all nether portals.
 */
public final class NetherManager {

  private final Map<Cell, Cell> portalConnections = new ConcurrentHashMap<>();

  public void load() {
    Journey.get().dataManager()
        .netherPortalManager()
        .getAllTunnels(TunnelType.NETHER)
        .forEach(tunnel -> portalConnections.put(tunnel.origin(), tunnel.destination()));
  }

  /**
   * Create tunnels specifically representing all nether portals in the world.
   *
   * @return all nether tunnels
   */
  public Collection<NetherTunnel> makeTunnels() {
    List<NetherTunnel> linksUnverified = portalConnections.entrySet().stream()
        .map(entry -> new NetherTunnel(entry.getKey(), entry.getValue()))
        .collect(Collectors.toList());
    List<NetherTunnel> linksVerified = new LinkedList<>();
    for (NetherTunnel tunnel : linksUnverified) {
      if (tunnel.verify()) {
        linksVerified.add(tunnel);
      } else {
        portalConnections.remove(tunnel.origin(), tunnel.destination());
        Journey.get().dataManager().netherPortalManager().removeTunnels(tunnel.origin(), tunnel.destination(), TunnelType.NETHER);
      }
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
    if (!originGroup.isPresent()) {
      return;  // We can't find the origin portal
    }
    lookForPortal(destination, originGroup.get(), 0);
  }

  private void lookForPortal(Supplier<Cell> resultantLocation, PortalGroup originGroup, int count) {
    if (count > 5) {
      // only try five times. 5 seconds is enough
      return;
    }
    Journey.get().proxy().schedulingManager().schedule(() -> {
      Optional<PortalGroup> destinationGroup = locateAll(resultantLocation.get(),
          16,
          resultantLocation.get().blockY() - 16,
          resultantLocation.get().blockY() + 16)
          .stream()
          .findFirst();
      if (!destinationGroup.isPresent() || destinationGroup.get().blocks().isEmpty()) {
        return;  // We can't find the destination portal
      }

      if (originGroup.tunnelLocation().domain() == destinationGroup.get().tunnelLocation().domain()) {
        // If they're in the same world, we have the same portal! We haven't actually teleported yet. Try again
        lookForPortal(resultantLocation, originGroup, count + 1);
        return;
      }

      List<Cell> linkedOrigins = new LinkedList<>();
      for (Cell originCell : originGroup.blocks()) {
        for (Cell destinationCell : destinationGroup.get().blocks()) {
          if (!portalConnections.containsKey(originCell)) {
            continue;
          }
          linkedOrigins.add(originCell);
          if (portalConnections.get(originCell).equals(destinationCell)) {
            return;  // We already have this portal link set up
          }
        }
      }
      /* We don't have this portal link set up yet */

      // Remove any connections with this origin (the portal link changed)
      linkedOrigins.forEach(old -> {
        if (portalConnections.containsKey(old)) {
          Journey.get().debugManager().broadcast(Formatter.debug("Removed nether tunnel:"));
          Journey.get().debugManager().broadcast(Formatter.debug(old + " -> "));
          Journey.get().debugManager().broadcast(Formatter.debug(portalConnections.get(old).toString()));

          portalConnections.remove(old);
          Journey.get().dataManager()
              .netherPortalManager()
              .getTunnelsWithOrigin(old, TunnelType.NETHER)
              .forEach(tunnel -> Journey.get().dataManager()
                  .netherPortalManager()
                  .removeTunnels(old, tunnel.destination(), TunnelType.NETHER));
        }
      });

      // Add the portal
      Cell previous = portalConnections.put(originGroup.tunnelLocation(), destinationGroup.get().tunnelLocation());
      Journey.get().dataManager().netherPortalManager().addTunnel(originGroup.tunnelLocation(),
          destinationGroup.get().tunnelLocation(),
          NetherTunnel.COST,
          TunnelType.NETHER);
      if (previous == null) {
        Journey.get().debugManager().broadcast(Formatter.debug("Added nether tunnel:"
            + originGroup.tunnelLocation() + " -> "
            + destinationGroup.get().tunnelLocation().toString()));
      }
    }, false, 20);
  }

  public void reset() {
    portalConnections.clear();
    Journey.get().dataManager().netherPortalManager().removeTunnels(TunnelType.NETHER);
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
  public Collection<PortalGroup> locateAll(Cell origin,
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
   * Locate all portals near the given cell within the given radius.
   *
   * @param cell   the central cell
   * @param radius the radius around the cell
   * @return the portals
   */
  public Collection<PortalGroup> locateAll(Cell cell, int radius) {
    return locateAll(cell, radius, cell.blockY() - radius, cell.blockY() + radius);
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
    if (!Journey.get().proxy().platform().isNetherPortal(cell)) {
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
          if (Journey.get().proxy().platform().isNetherPortal(offset) && group.add(offset)) {
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
    private final HashMap<Integer, Set<Cell>> blockY = new HashMap<>();
    private final int domain;
    private Cell teleportTo;
    private int bottom = Integer.MAX_VALUE;

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
      if (!Journey.get().proxy().platform().isNetherPortal(cell)) {
        return false;
      }

      boolean added = portal.add(cell);
      // If the cell was added, do more actions.
      if (added) {
        int y = cell.blockY();
        if (y < bottom) {
          //The bottom of the Nether portal
          bottom = cell.blockY();
        }
        //Put the Location in a Map sorted by Y value.
        Set<Cell> set = blockY.computeIfAbsent(y, k -> new HashSet<>());
        set.add(cell);
        //Reset the teleport cell and distance squared since a new block was added.
        teleportTo = null;
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

      Set<Cell> bottomY = blockY.get(bottom);
      int minX = Collections.min(bottomY, Comparator.comparingInt(Cell::blockX)).blockX();
      int maxX = Collections.max(bottomY, Comparator.comparingInt(Cell::blockX)).blockX();
      int minZ = Collections.min(bottomY, Comparator.comparingInt(Cell::blockZ)).blockZ();
      int maxZ = Collections.max(bottomY, Comparator.comparingInt(Cell::blockZ)).blockZ();

      return new Cell((maxX + minX) / 2, bottom, (maxZ + minZ) / 2, domain);
    }

    @Override
    public int hashCode() {
      return portal.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof PortalGroup) {
        PortalGroup pg = (PortalGroup) o;
        return portal.equals(pg.portal);
      }
      return portal.equals(o);
    }

  }
}