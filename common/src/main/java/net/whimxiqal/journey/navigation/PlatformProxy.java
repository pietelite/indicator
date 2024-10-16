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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.InternalJourneyPlayer;
import net.whimxiqal.journey.JourneyAgent;
import net.whimxiqal.journey.JourneyPlayer;
import net.whimxiqal.journey.chunk.BlockProvider;
import net.whimxiqal.journey.chunk.ChunkCacheBlockProvider;
import net.whimxiqal.journey.chunk.ChunkId;
import net.whimxiqal.journey.math.Vector;
import net.whimxiqal.journey.proxy.JourneyBlock;
import net.whimxiqal.journey.proxy.JourneyChunk;
import net.whimxiqal.journey.search.SearchSession;
import net.whimxiqal.journey.search.flag.FlagSet;
import net.whimxiqal.journey.Color;
import org.bstats.charts.CustomChart;

/**
 * An interface to the specific platform/engine that is running Minecraft.
 * All calls to this proxy must be made synchronously on the main server thread.
 */
public interface PlatformProxy extends BlockProvider {

  /**
   * Convert chunk id to a {@link JourneyChunk}.
   * <b>May be called async!</b>
   * The returned future is always completed on the main server thread,
   * so any callbacks thereafter are also synchronous on the main thread.
   * The generate argument may be false if the caller does not want the server to generate the chunk
   * if it hasn't already. If this is the case, the returned {@link JourneyChunk} will reflect the inaccessibility
   * of the underlying chunk, given it is un-generated and unloaded.
   * The returned future cannot be completed with null.
   *
   * @param chunkId the chunk id
   * @param generate true to generate the chunk if it doesn't exist
   * @return the journey chunk future, to be completed on the main server thread
   */
  CompletableFuture<JourneyChunk> toChunk(ChunkId chunkId, boolean generate);

  /**
   * Convert a cell to a {@link JourneyBlock} with real-world data.
   * <b>Must be called on the main server thread!</b>
   * If you need a block asynchronously, use a {@link ChunkCacheBlockProvider}
   * or the {@link net.whimxiqal.journey.chunk.CentralChunkCache}.
   *
   * @param cell the cell with the location information for the block
   * @return the block
   */
  @Override
  JourneyBlock toBlock(Cell cell);

  void spawnParticle(UUID playerUuid, String particle, Color color, int domain, double x, double y, double z);

  List<InternalJourneyPlayer> onlinePlayers();

  Optional<InternalJourneyPlayer> onlinePlayer(UUID uuid);

  Optional<InternalJourneyPlayer> onlinePlayer(String name);

  Optional<Cell> entityCellLocation(UUID entityUuid);

  Optional<Vector> entityVector(UUID entityUuid);

  void sendAnimationBlock(UUID player, Cell location);

  void resetAnimationBlocks(UUID player, Collection<Cell> locations);

  String domainName(int domainId);

  boolean sendGui(JourneyPlayer source);

  Consumer<CustomChart> bStatsChartConsumer();

  record DomainInfo(String name, int id) {}

  Map<String, Map<String, DomainInfo>> domainResourceKeys();

  List<String> particleTypes();

  boolean isValidParticleType(String particleType);
}
