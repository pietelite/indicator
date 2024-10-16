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

package net.whimxiqal.journey.integration.citizens;

import java.util.logging.Logger;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.trait.TraitInfo;
import net.whimxiqal.journey.JourneyApi;
import net.whimxiqal.journey.integration.citizens.config.ConfigSettings;
import net.whimxiqal.journey.navigation.NavigatorFactory;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class JourneyCitizens extends JavaPlugin {
  public static final String NAME = "JourneyCitizens";
  private static JourneyCitizens instance;
  private static Logger logger;
  private final GuideStore guideStore = new GuideStore();

  public static Logger logger() {
    return logger;
  }

  public static JourneyCitizens get() {
    return instance;
  }

  @Override
  public void onEnable() {
    instance = this;
    JourneyCitizens.logger = getLogger();
    if (!getDataFolder().toPath().resolve("config.yml").toFile().exists()) {
      saveResource("config.yml", false);
    }
    ConfigSettings.testLoadAll();

    JourneyApi api = JourneyApi.get();
    api.registerScope(getName(), "npc", new CitizensScope());
    api.navigating().registerNavigator(NavigatorFactory.builder(NAME, "npc")
        .permission("journey.navigator.npc")
        .supplier(NpcNavigator::new)
        .option(NpcNavigatorOptions.ENTITY_TYPE)
        .option(NpcNavigatorOptions.NAME)
        .build());

    // guide store initializes in CitizensEventListener

    Bukkit.getPluginManager().registerEvents(new CitizensEventListener(), this);
    CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(GuideTrait.class));
  }

  @Override
  public void onDisable() {
    guideStore.shutdown();
  }

  public GuideStore guideStore() {
    return guideStore;
  }
}
