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

package net.whimxiqal.journey.integration.betonquest;

import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.Destination;
import net.whimxiqal.journey.JourneyPlayer;
import net.whimxiqal.journey.Scope;
import net.whimxiqal.journey.ScopeBuilder;
import net.whimxiqal.journey.VirtualMap;
import net.whimxiqal.journey.bukkit.JourneyBukkitApi;
import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.api.bukkit.config.custom.multi.MultiConfiguration;
import org.betonquest.betonquest.api.config.quest.QuestPackage;
import org.betonquest.betonquest.api.profiles.OnlineProfile;
import org.betonquest.betonquest.config.Config;
import org.betonquest.betonquest.database.PlayerData;
import org.betonquest.betonquest.utils.PlayerConverter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class BetonQuestScope implements Scope {
  @Override
  public Component name() {
    return Component.text("BetonQuest");
  }

  @Override
  public VirtualMap<Scope> subScopes(JourneyPlayer player) {
    Map<String, Scope> betonQuestScopes = new HashMap<>();
    Scope compassScope = Scope.builder()
        .name(Component.text("Compass Destinations"))
        .permission("journey.path.betonquest.compass")
        .subScopes(() -> {
          Player bukkitPlayer = Bukkit.getPlayer(player.uuid());
          if (bukkitPlayer == null) {
            JourneyBetonQuest.logger.severe("Could not find player " + player);
            return VirtualMap.empty();
          }

          Map<String, Scope> packageScopes = new HashMap<>();
          OnlineProfile profile = PlayerConverter.getID(bukkitPlayer);
          PlayerData playerData = BetonQuest.getInstance().getPlayerData(profile);
          for (final QuestPackage pack : Config.getPackages().values()) {
            final String packName = pack.getQuestPath();
            MultiConfiguration config = pack.getConfig();

            // loop all compass locations
            final ConfigurationSection section = pack.getConfig().getConfigurationSection("compass");
            if (section == null) {
              continue;
            }
            ScopeBuilder packageScope = Scope.builder().name(Component.text(packName));
            for (final String key : section.getKeys(false)) {
              final String location = config.getString("compass." + key + ".location");

              // Get name
              String name;
              if (section.isConfigurationSection(key + ".name")) {
                name = config.getString("compass." + key + ".name." + playerData.getLanguage());
                if (name == null) {
                  name = config.getString("compass." + key + ".name." + Config.getLanguage());
                }
                if (name == null) {
                  name = config.getString("compass." + key + ".name.en");
                }
              } else {
                name = config.getString("compass." + key + ".name");
              }
              if (name == null) {
                JourneyBetonQuest.logger.warning("Name not defined in a compass pointer in " + packName + " package: " + key);
                continue;
              }

              if (location == null) {
                JourneyBetonQuest.logger.warning("Location not defined in a compass pointer in " + packName + " package: " + key);
                continue;
              }
              // check if the player has special compass tag
              if (!playerData.hasTag(packName + ".compass-" + key)) {
                continue;
              }
              // if the tag is present, continue
              final String[] parts = location.split(";");
              if (parts.length != 4) {
                JourneyBetonQuest.logger.warning("Could not parse location in a compass pointer in " + packName + " package: " + key);
                continue;
              }
              final World world = Bukkit.getWorld(parts[3]);
              if (world == null) {
                JourneyBetonQuest.logger.warning("World does not exist in a compass pointer in " + packName + " package: " + key);
                continue;
              }
              final int locX;
              final int locY;
              final int locZ;
              try {
                locX = Integer.parseInt(parts[0]);
                locY = Integer.parseInt(parts[1]);
                locZ = Integer.parseInt(parts[2]);
              } catch (final NumberFormatException e) {
                JourneyBetonQuest.logger.warning("Could not parse location coordinates in a compass pointer in " + packName + " package: " + key + ": " + e.getMessage());
                continue;
              }
              packageScope.destinations(VirtualMap.ofSingleton(key, Destination.cellBuilder(new Cell(locX, locY, locZ, JourneyBukkitApi.get().toDomain(world)))
                  .name(Component.text(name))
                  .build()));
            }

            packageScopes.put(packName, packageScope.build());
          }
          return VirtualMap.of(packageScopes);
        }).build();
    betonQuestScopes.put("compass", compassScope);
    return VirtualMap.of(betonQuestScopes);
  }

  @Override
  public boolean isStrict() {
    return true;
  }

}
