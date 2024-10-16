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

package net.whimxiqal.journey;

import java.io.IOException;
import net.whimxiqal.journey.chunk.CentralChunkCache;
import net.whimxiqal.journey.config.ConfigManager;
import net.whimxiqal.journey.config.Settings;
import net.whimxiqal.journey.data.DataVersion;
import net.whimxiqal.journey.data.cache.CachedDataProvider;
import net.whimxiqal.journey.manager.AnimationManager;
import net.whimxiqal.journey.manager.DistributedWorkManager;
import net.whimxiqal.journey.manager.DomainManager;
import net.whimxiqal.journey.manager.LocationManager;
import net.whimxiqal.journey.message.MessageManager;
import net.whimxiqal.journey.navigation.NavigationManager;
import net.whimxiqal.journey.manager.NetherManager;
import net.whimxiqal.journey.manager.PlayerManager;
import net.whimxiqal.journey.manager.SearchManager;
import net.whimxiqal.journey.manager.TunnelManager;
import net.whimxiqal.journey.scope.ScopeManager;
import net.whimxiqal.journey.stats.StatsManager;
import net.whimxiqal.journey.util.BStatsUtil;
import net.whimxiqal.journey.util.CommonLogger;

public final class Journey {

  public static final String NAME = "Journey";
  private static Journey instance;
  private final ConfigManager configManager = new ConfigManager();
  private final PlayerManager playerManager = new PlayerManager();
  private final NetherManager netherManager = new NetherManager();
  private final SearchManager searchManager = new SearchManager();
  private final NavigationManager navigationManager = new NavigationManager();
  private final LocationManager locationManager = new LocationManager();
  private final ScopeManager scopeManager = new ScopeManager();
  private final TunnelManager tunnelManager = new TunnelManager();
  private final StatsManager statsManager = new StatsManager();
  private final DomainManager domainManager = new DomainManager();
  private final CentralChunkCache centralChunkCache = new CentralChunkCache();
  private final AnimationManager animationManager = new AnimationManager();
  private final CachedDataProvider cachedDataProvider = new CachedDataProvider();
  private final MessageManager messageManager = new MessageManager();
  private DistributedWorkManager workManager;
  private Proxy proxy;

  public static CommonLogger logger() {
    return instance.proxy.logger();
  }

  public static void create() {
    if (instance != null) {
      throw new IllegalStateException("Journey was already created");
    }
    instance = new Journey();
  }

  public static Journey get() {
    if (instance == null) {
      throw new IllegalStateException("Journey is either uninitialized or has been shutdown");
    }
    return instance;
  }

  public static void remove() {
    if (instance == null) {
      throw new IllegalStateException("Journey may only be removed after it was created");
    }
    instance = null;
  }

  public Proxy proxy() {
    return proxy;
  }

  public void registerProxy(Proxy proxy) {
    if (this.proxy != null) {
      throw new IllegalStateException("The proxy was already registered.");
    }
    this.proxy = proxy;
  }

  public boolean init() {

    // load settings first
    try {
      configManager.initialize(proxy.configPath(), proxy.messagesConfigPath());
    } catch (IOException e) {
      logger().error("There was an error trying to read the config");
      e.printStackTrace();
      return false;
    }

    messageManager.initialize();
    proxy.initialize();
    netherManager.initialize();
    navigationManager.initialize();
    locationManager.initialize();
    scopeManager.initialize();
    statsManager.initialize();
    BStatsUtil.register(proxy.platform().bStatsChartConsumer());
    centralChunkCache.initialize();
    animationManager.initialize();
    cachedDataProvider.initialize();

    if (proxy.dataManager().version() != DataVersion.latest()) {
      logger().error("There is an error in your database configuration. Please delete all Journey database files and tables and restart");
      return false;
    }

    workManager = new DistributedWorkManager(Settings.MAX_SEARCHES.getValue());
    return true;
  }

  public void shutdown() {
    logger().setImmediateSubmit(true);
    // shutdown cache first so any executing searches can continue with the completed chunks requests
    centralChunkCache.shutdown();

    // shutdown search manager and wait for all ongoing searches to cancel and complete
    searchManager.shutdown();
    navigationManager.shutdown();
    locationManager.shutdown();

    statsManager.shutdown();
    animationManager.shutdown();
    cachedDataProvider.shutdown();
    proxy.shutdown();
  }

  public ConfigManager configManager() {
    return configManager;
  }

  public PlayerManager playerManager() {
    assertSynchronous();
    return playerManager;
  }

  public NetherManager netherManager() {
    assertSynchronous();
    return netherManager;
  }

  public SearchManager searchManager() {
    assertSynchronous();
    return searchManager;
  }

  public NavigationManager navigatorManager() {
    assertSynchronous();
    return navigationManager;
  }

  public LocationManager locationManager() {
    assertSynchronous();
    return locationManager;
  }

  public ScopeManager scopeManager() {
    assertSynchronous();
    return scopeManager;
  }

  public TunnelManager tunnelManager() {
    assertSynchronous();
    return tunnelManager;
  }

  public StatsManager statsManager() {
    return statsManager;
  }

  public DomainManager domainManager() {
    return domainManager;
  }

  public CentralChunkCache centralChunkCache() {
    return centralChunkCache;
  }

  public AnimationManager animationManager() {
    return animationManager;
  }
  public CachedDataProvider cachedDataProvider() {
    return cachedDataProvider;
  }

  public MessageManager messageManager() {
    return messageManager;
  }

  public DistributedWorkManager workManager() {
    return workManager;
  }

  private void assertSynchronous() {
    if (!proxy.schedulingManager().isMainThread()) {
      Journey.logger().warn("This may only be called on the main server thread, but was called on thread: " + Thread.currentThread().getName());
    }
  }

}
