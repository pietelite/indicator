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

package net.whimxiqal.journey.platform;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.InternalJourneyPlayer;
import net.whimxiqal.journey.Journey;
import net.whimxiqal.journey.JourneyAgent;
import net.whimxiqal.journey.JourneyPlayer;
import net.whimxiqal.journey.Tunnel;
import net.whimxiqal.journey.chunk.ChunkId;
import net.whimxiqal.journey.math.Vector;
import net.whimxiqal.journey.navigation.PlatformProxy;
import net.whimxiqal.journey.proxy.JourneyBlock;
import net.whimxiqal.journey.proxy.JourneyChunk;
import net.whimxiqal.journey.proxy.TestJourneyBlock;
import net.whimxiqal.journey.proxy.TestJourneyChunk;
import net.whimxiqal.journey.search.SearchSession;
import net.whimxiqal.journey.search.flag.FlagSet;
import net.whimxiqal.journey.Color;
import org.bstats.charts.CustomChart;

public class TestPlatformProxy implements PlatformProxy {

  public static Map<Integer, TestWorld> worlds = new HashMap<>();  // domain -> world
  public static Map<String, Cell> pois = new HashMap<>();
  public static List<Tunnel> tunnels = new LinkedList<>();
  public static List<InternalJourneyPlayer> onlinePlayers = new LinkedList<>();
  public static int animatedBlocks = 0;

  @Override
  public CompletableFuture<JourneyChunk> toChunk(ChunkId chunkId, boolean generate) {
    return CompletableFuture.completedFuture(new TestJourneyChunk(chunkId));
  }

  @Override
  public JourneyBlock toBlock(Cell cell) {
    return new TestJourneyBlock(cell);
  }

  @Override
  public void spawnParticle(UUID playerUuid, String particle, Color color, int domain, double x, double y, double z) {
    // ignore
  }

  @Override
  public List<InternalJourneyPlayer> onlinePlayers() {
    return onlinePlayers;
  }

  @Override
  public Optional<InternalJourneyPlayer> onlinePlayer(UUID uuid) {
    return onlinePlayers.stream().filter(player -> player.uuid().equals(uuid)).findFirst();
  }

  @Override
  public Optional<InternalJourneyPlayer> onlinePlayer(String name) {
    return onlinePlayers.stream().filter(player -> player.name().equals(name)).findFirst();
  }

  @Override
  public Optional<Cell> entityCellLocation(UUID entityUuid) {
    return Optional.of(new Cell(0, 0, 0, WorldLoader.domain(0)));  // just say everything is at the origin
  }

  @Override
  public Optional<Vector> entityVector(UUID entityUuid) {
    return Optional.empty();
  }

  @Override
  public void sendAnimationBlock(UUID player, Cell location) {
    animatedBlocks += 1;
  }

  @Override
  public void resetAnimationBlocks(UUID player, Collection<Cell> locations) {
    animatedBlocks -= locations.size();
    Journey.logger().info("Reset: " + locations.size());
  }

  @Override
  public String domainName(int domain) {
    return worlds.get(domain).name;
  }

  @Override
  public boolean sendGui(JourneyPlayer player) {
    return false;
  }

  @Override
  public Consumer<CustomChart> bStatsChartConsumer() {
    return chart -> {/* nothing */};
  }

  @Override
  public Map<String, Map<String, DomainInfo>> domainResourceKeys() {
    return Collections.singletonMap("whimxiqal", TestPlatformProxy.worlds.values().stream().collect(Collectors.toMap(k -> k.name, k -> new DomainInfo(k.name, Journey.get().domainManager().domainIndex(k.uuid)))));
  }

  @Override
  public List<String> particleTypes() {
    return List.of("glow");  // that's it
  }

  @Override
  public boolean isValidParticleType(String particleType) {
    return particleType.equals("glow");
  }
}
