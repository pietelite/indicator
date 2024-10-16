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

package net.whimxiqal.journey.schematic;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.platform.PlatformsRegisteredEvent;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.InternalJourneyPlayer;
import net.whimxiqal.journey.Journey;
import net.whimxiqal.journey.Proxy;
import net.whimxiqal.journey.data.TestDataManager;
import net.whimxiqal.journey.manager.TestSchedulingManager;
import net.whimxiqal.journey.navigation.Mode;
import net.whimxiqal.journey.navigation.PlatformProxy;
import net.whimxiqal.journey.navigation.mode.JumpMode;
import net.whimxiqal.journey.navigation.mode.WalkMode;
import net.whimxiqal.journey.platform.TestPlatformProxy;
import net.whimxiqal.journey.search.DestinationPathTrial;
import net.whimxiqal.journey.search.ResultState;
import net.whimxiqal.journey.search.SearchSession;
import net.whimxiqal.journey.util.CommonLogger;
import net.whimxiqal.journey.util.TestLogger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockSettings;
import org.mockito.Mockito;
import org.openjdk.jmh.annotations.AuxCounters;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

public class SchematicSearchTests {

  public static final InternalJourneyPlayer PLAYER = new SchematicPlayer();
  /**
   * The id of the SearchParams to benchmark with, or null if nothing should benchmark.
   * Example: "basic_uphill"
   */
  private static final String BENCHMARKING_SEARCH_PARAM_ID = null;
  private static final boolean DEBUG = true;
  private static final SchematicLoader TEST_LOADER = new SchematicLoader();
  private static final Map<String, SearchParams> searchParams = new HashMap<>();
  private static boolean mockedBlockRegistry = false;

  static void tryMockAllBlockTypes() {
    if (mockedBlockRegistry) {
      return;
    }
    MockSettings settings = Mockito.withSettings()
        .stubOnly()
        .defaultAnswer(Answers.RETURNS_SMART_NULLS);
    for (String id : AllBlockTypes.IDS) {
      BlockType blockType = Mockito.mock(BlockType.class, settings);
      BlockState blockState = Mockito.mock(BlockState.class, settings);
      BaseBlock baseBlock = Mockito.mock(BaseBlock.class, settings);

      Mockito.when(blockType.getId()).thenReturn(id);
      Mockito.when(blockType.getDefaultState()).thenReturn(blockState);
      Mockito.when(blockState.getBlockType()).thenReturn(blockType);
      Mockito.when(blockState.toBaseBlock()).thenReturn(baseBlock);
      Mockito.when(blockState.toBaseBlock(Mockito.any())).thenReturn(baseBlock);
      Mockito.when(baseBlock.getBlockType()).thenReturn(blockType);
      Mockito.when(baseBlock.toBaseBlock()).thenReturn(baseBlock);
      Mockito.when(baseBlock.toImmutableState()).thenReturn(blockState);

      BlockType.REGISTRY.register(id, blockType);
    }
    mockedBlockRegistry = true;
  }

  @BeforeAll
  static void setUp() throws IOException {
    // Set up for tests
    setUp(SchematicSearchTests.TEST_LOADER);
  }

  static void setUp(SchematicLoader loader) throws IOException {
    // ADD DIFFERENT SEARCH PATHS HERE
    searchParams.put("basic_uphill", new SearchParams("overworld1.schem",
        new Cell(-15, 64, -56, 0),
        new Cell(-117, 93, -203, 0),
        List.of(new WalkMode(), new JumpMode())));
    /// END ADD SEARCH PATHS

    tryMockAllBlockTypes();
    WorldEdit.getInstance().getPlatformManager().register(new JourneyPlatform());
    WorldEdit.getInstance().getEventBus().post(new PlatformsRegisteredEvent());

    Journey.create();
    PlatformProxy schematicPlatformProxy = Mockito.mock(PlatformProxy.class, Mockito.RETURNS_DEFAULTS);
    Mockito.when(schematicPlatformProxy.toChunk(Mockito.any(), Mockito.anyBoolean())).then((invocation) ->
        CompletableFuture.completedFuture(new SchematicChunk(invocation.getArgument(0), loader.get())));
    Mockito.when(schematicPlatformProxy.toBlock(Mockito.any())).then((invocation) -> {
      Cell cell = invocation.getArgument(0);
      return new SchematicBlock(cell, loader.get().getBlock(BlockVector3.at(cell.blockX(), cell.blockY(), cell.blockZ())).getBlockType());
    });
    Mockito.when(schematicPlatformProxy.onlinePlayer(Mockito.<UUID>any())).then((invocation) -> {
      UUID uuid = invocation.getArgument(0);
      if (uuid.equals(SchematicSearchTests.PLAYER.uuid())) {
        return Optional.of(SchematicSearchTests.PLAYER);
      } else {
        return Optional.empty();
      }
    });
    Mockito.when(schematicPlatformProxy.bStatsChartConsumer()).thenReturn(chart -> {});
    Proxy proxy = Mockito.mock(Proxy.class);
    Mockito.when(proxy.logger()).thenReturn(new TestLogger());
    Mockito.when(proxy.schedulingManager()).thenReturn(new TestSchedulingManager());
    Mockito.when(proxy.dataManager()).thenReturn(new TestDataManager());
    Mockito.when(proxy.platform()).thenReturn(schematicPlatformProxy);
    Mockito.when(proxy.configPath()).thenReturn(File.createTempFile("journey-config", "yml").toPath());
    Mockito.when(proxy.messagesConfigPath()).thenReturn(File.createTempFile("journey-messages", "yml").toPath());
    Journey.get().registerProxy(proxy);

    if (DEBUG) {
      Journey.logger().setLevel(CommonLogger.LogLevel.DEBUG);
    }

    proxy.schedulingManager().initialize();  // initialize early so that we can schedule on main thread
    TestSchedulingManager.runOnMainThread(() -> {
      if (!Journey.get().init()) {
        Assertions.fail("Journey initialization failed");
      }

      Journey.get().tunnelManager().register(player -> TestPlatformProxy.tunnels);
    });
  }

  @AfterAll
  static void tearDown() {
    Journey.get().shutdown();
    Journey.remove();
  }

  /**
   * Runs each search for correctness.
   */
  @Test
  void runSearches() {
    for (SearchParams params : searchParams.values()) {
      Assertions.assertTrue(TEST_LOADER.load(params.schematicFile()), "Schematic load failed");

      SearchSession session = new DummySearchSession();
      DestinationPathTrial pathTrial = new DestinationPathTrial(session,
          params.start(),
          params.end(),
          params.end()::equals,
          params.modes(),
          0, null, ResultState.IDLE, false, false);

      while (!pathTrial.run()) {
        // do nothing, just wait utnil path trial is complete
      }

      Journey.logger().flush();
      Assertions.assertEquals(ResultState.STOPPED_SUCCESSFUL, pathTrial.getState());
    }
  }

  /**
   * Run the benchmark, but only if an ID for some params are specified in
   * {@link SchematicSearchTests#BENCHMARKING_SEARCH_PARAM_ID}. If it's null,
   * then this test will be skipped, thus skipping any benchmarking.
   *
   * @throws RunnerException from {@link Runner#run()}
   */
  @Test
  void runBenchmark() throws RunnerException {
    if (BENCHMARKING_SEARCH_PARAM_ID == null) {
      return;
    }
    Options opt = new OptionsBuilder()
        // Specify which benchmarks to run.
        // You can be more specific if you'd like to run only one benchmark per test.
        .include(this.getClass().getName() + ".*")
        // Set the following options as needed
        .mode(org.openjdk.jmh.annotations.Mode.AverageTime)
        .timeUnit(TimeUnit.MILLISECONDS)
        .warmupTime(TimeValue.seconds(1))
        .warmupIterations(1)
        .measurementTime(TimeValue.seconds(5))
        .measurementIterations(1)
        .threads(1)
        .forks(1)
        .shouldFailOnError(true)
        .shouldDoGC(true)
//        .param("weight", DoubleStream.iterate(1, cur -> cur + 0.1).limit(40).mapToObj(String::valueOf).toArray(String[]::new))
        .build();

    new Runner(opt).run();
  }

  @Benchmark
  public void runBenchmark(BenchmarkState state) {
    SearchParams params = searchParams.get(BENCHMARKING_SEARCH_PARAM_ID);
    if (params == null) {
      Assertions.fail("No search params have the given id: " + BENCHMARKING_SEARCH_PARAM_ID);
    }
    SearchSession session = new DummySearchSession();
    DestinationPathTrial pathTrial = new DestinationPathTrial(session,
        params.start(),
        params.end(),
        params.end()::equals,
        List.of(new WalkMode(), new JumpMode()),
        0, null, ResultState.IDLE, false, false);

    while (!pathTrial.run()) {
      // do nothing, just wait until path trial is complete
    }

    Journey.logger().flush();
    Assertions.assertEquals(ResultState.STOPPED_SUCCESSFUL, pathTrial.getState());

    state.pathLength = pathTrial.getPath().getCost();
    state.totalVisited = pathTrial.getTotalVisitedCells();
    state.cycles = pathTrial.getCycles();
  }

  private record SearchParams(String schematicFile, Cell start, Cell end, List<Mode> modes) {
  }

  @AuxCounters(AuxCounters.Type.EVENTS)
  @State(Scope.Thread)
  public static class BenchmarkState {
    private final SchematicLoader benchmarkLoader = new SchematicLoader();

    /// Aux Counters Start (must be public and primitives)
    public double pathLength;
    public int totalVisited;
    public int cycles;
    /// Aux Counters End

    /// Parameters (set to private)
//    @Param("1")
//    private double weight = 1;
    /// Parameters End

    @Setup(Level.Iteration)
    public void setUp() throws IOException {
      SchematicSearchTests.setUp(benchmarkLoader);
      SearchParams params = searchParams.get(BENCHMARKING_SEARCH_PARAM_ID);
      if (params == null) {
        Assertions.fail("No search params have the given id: " + BENCHMARKING_SEARCH_PARAM_ID);
      }
      Assertions.assertTrue(benchmarkLoader.load(params.schematicFile()), "Schematic load failed");
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
      SchematicSearchTests.tearDown();
    }

  }
}
