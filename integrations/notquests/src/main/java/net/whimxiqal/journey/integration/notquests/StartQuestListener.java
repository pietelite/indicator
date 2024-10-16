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

package net.whimxiqal.journey.integration.notquests;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.JourneyApi;
import net.whimxiqal.journey.bukkit.JourneyBukkitApi;
import net.whimxiqal.journey.navigation.NavigationApi;
import net.whimxiqal.journey.navigation.NavigatorDetails;
import net.whimxiqal.journey.navigation.NavigatorDetailsBuilder;
import net.whimxiqal.journey.search.SearchApi;
import net.whimxiqal.journey.search.SearchFlag;
import net.whimxiqal.journey.search.SearchFlags;
import net.whimxiqal.journey.search.SearchFlagsBuilder;
import net.whimxiqal.journey.search.SearchResult;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;
import rocks.gravili.notquests.paper.events.notquests.QuestFinishAcceptEvent;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.objectives.Objective;

public class StartQuestListener implements Listener {

  private static final String USE_JOURNEY_SETTING = "useJourney";
  private static final String FLAGS_SECTION = "journeyFlags";
  private static final String FLAG_SECTION_SEARCHING_KEY_FLY = "fly";
  private static final String FLAG_SECTION_SEARCHING_KEY_TIMEOUT = "timeout";
  private static final String FLAG_SECTION_NAVIGATION = "navigation";
  private static final String FLAG_SECTION_NAVIGATION_NAVIGATOR_TYPE = "navigator_type";
  private static final String FLAG_SECTION_NAVIGATION_OPTIONS = "options";

  private static SearchFlags readSearchFlags(@Nullable ConfigurationSection flagSection) {
    if (flagSection == null) {
      return SearchFlags.none();
    }
    ConfigurationSection searchSection = flagSection.getConfigurationSection("searching");
    if (searchSection == null) {
      return SearchFlags.none();
    }
    SearchFlagsBuilder flagsBuilder = SearchFlags.builder();
    for (String key : searchSection.getKeys(false)) {
      switch (key) {
        case FLAG_SECTION_SEARCHING_KEY_FLY -> flagsBuilder.add(SearchFlag.of(SearchFlag.Type.FLY, searchSection.getBoolean(FLAG_SECTION_SEARCHING_KEY_FLY)));
        case FLAG_SECTION_SEARCHING_KEY_TIMEOUT -> flagsBuilder.add(SearchFlag.of(SearchFlag.Type.TIMEOUT, searchSection.getInt(FLAG_SECTION_SEARCHING_KEY_TIMEOUT)));
        default -> throw new IllegalArgumentException("Unknown search flag '" + key + "'");
      }
    }
    return flagsBuilder.build();
  }

  private static @Nullable NavigatorDetails readNavigatorDetails(@Nullable ConfigurationSection flagSection, NavigationApi navigationApi) {
    if (flagSection == null) {
      return null;
    }
    ConfigurationSection navigationSection = flagSection.getConfigurationSection(FLAG_SECTION_NAVIGATION);
    if (navigationSection == null) {
      return null;
    }
    String navigatorType = navigationSection.getString(FLAG_SECTION_NAVIGATION_NAVIGATOR_TYPE);
    if (navigatorType == null) {
      return null;
    }
    NavigatorDetailsBuilder<?> details = navigationApi.navigatorDetailsBuilder(navigatorType);
    ConfigurationSection optionsSection = navigationSection.getConfigurationSection(FLAG_SECTION_NAVIGATION_OPTIONS);
    if (optionsSection == null) {
      return details.build();
    }
    for (Map.Entry<String, Object> entry : optionsSection.getValues(false).entrySet()) {
      details.setOption(entry.getKey(), entry.getValue());
    }
    return details.build();
  }

  @EventHandler
  public void onStartQuest(QuestFinishAcceptEvent event) {
    List<ActiveObjective> activeObjectives = event.getActiveQuest().getActiveObjectives();
    if (activeObjectives.isEmpty()) {
      return;
    }
    Objective firstObjective = activeObjectives.get(0).getObjective();
    Location location = firstObjective.getLocation();
    if (location == null) {
      return;  // there was no location associated with this objective
    }

    if (!firstObjective.getConfig().getBoolean(firstObjective.getInitialConfigPath() + '.' + USE_JOURNEY_SETTING, false)) {
      return;  // config doesn't specify to use journey
    }

    ConfigurationSection flagSection = firstObjective.getConfig().getConfigurationSection(firstObjective.getInitialConfigPath() + '.' + FLAGS_SECTION);
    Cell cell = JourneyBukkitApi.get().toCell(firstObjective.getLocation());

    SearchApi searchApi = JourneyApi.get().searching();
    NavigationApi navigationApi = JourneyApi.get().navigating();
    NavigatorDetails navigatorDetails = readNavigatorDetails(flagSection, navigationApi);
    UUID playerUuid = event.getQuestPlayer().getUniqueId();
    searchApi.runPlayerDestinationSearch(playerUuid, cell, readSearchFlags(flagSection))
        .thenAccept(searchResult -> {
          if (searchResult.status() != SearchResult.Status.SUCCESS) {
            return;
          }
          if (navigatorDetails == null) {
            navigationApi.navigatePlayer(playerUuid, searchResult.path());
          } else {
            navigationApi.navigatePlayer(playerUuid, searchResult.path(), navigatorDetails);
          }
        });
  }

}
