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

package net.whimxiqal.journey.integration.essentials;

import com.earth2me.essentials.IEssentials;
import com.earth2me.essentials.User;
import com.earth2me.essentials.commands.WarpNotFoundException;
import com.earth2me.essentials.spawn.IEssentialsSpawn;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.ess3.api.InvalidWorldException;
import net.kyori.adventure.text.Component;
import net.whimxiqal.journey.Destination;
import net.whimxiqal.journey.JourneyPlayer;
import net.whimxiqal.journey.Scope;
import net.whimxiqal.journey.VirtualMap;
import net.whimxiqal.journey.bukkit.JourneyBukkitApi;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class EssentialsScope implements Scope {
  @Override
  public Component name() {
    return Component.text("Essentials");
  }

  @Override
  public VirtualMap<Scope> subScopes(JourneyPlayer player) {
    // Generate scopes for
    // - Homes, but only if the player has multiple homes
    // - Warps
    IEssentials essentials = JourneyEssentials.essentials();
    Map<String, Scope> subScopes = new HashMap<>();
    User user = essentials.getUser(player.uuid());
    if (user.hasValidHomes()) {
      List<String> homes = user.getHomes();
      if (homes.size() > 1) {
        subScopes.put("homes", Scope.builder()
            .name(Component.text("Homes"))
            .description(Component.text("Your homes saved in Essentials"))
            .destinations(VirtualMap.of(user.getHomes()
                .stream()
                .collect(Collectors.toMap(name -> name, name ->
                    Destination.of(JourneyBukkitApi.get().toCell(user.getHome(name)))))))
            .permission("journey.path.essentials.home")
            .build());
      }
    }
    subScopes.put("warps", Scope.builder()
        .name(Component.text("Warps"))
        .destinations(p -> {
          Map<String, Destination> destinations = new HashMap<>();
          essentials.getWarps().getList()
              .forEach(warp -> {
                try {
                  destinations.put(warp, Destination.of(JourneyBukkitApi.get().toCell(essentials.getWarps().getWarp(warp))));
                } catch (WarpNotFoundException | InvalidWorldException e) {
                  JourneyEssentials.logger().warning("Could not find warp " + warp + " for player " + player);
                }
              });
          return VirtualMap.of(destinations);
        })
        .strict()
        .permission("journey.path.essentials.warp")
        .build());
    return VirtualMap.of(subScopes);
  }

  @Override
  public VirtualMap<Destination> destinations(JourneyPlayer player) {
    // Generate destinations for
    // - Home, but only if the player has just one home
    IEssentials essentials = JourneyEssentials.essentials();
    Map<String, Destination> destinations = new HashMap<>();
    User user = essentials.getUser(player.uuid());
    if (user.hasValidHomes()) {
      List<String> homes = user.getHomes();
      if (homes.size() == 1) {
        destinations.put("home", Destination.cellBuilder(JourneyBukkitApi.get().toCell(user.getHome(homes.get(0))))
            .permission("journey.path.essentials.home")
            .build());
      }
    }

    // Essentials Spawn
    if (Bukkit.getPluginManager().isPluginEnabled("EssentialsSpawn")) {
      Plugin plugin = Bukkit.getPluginManager().getPlugin("EssentialsSpawn");
      if (!(plugin instanceof IEssentialsSpawn)) {
        throw new RuntimeException("Essentials class could not be found");
      }
      destinations.put("spawn", Destination.cellBuilder(JourneyBukkitApi.get().toCell(((IEssentialsSpawn) plugin).getSpawn(user.getGroup())))
          .permission("journey.path.essentials.spawn")
          .build());
    }
    return VirtualMap.of(destinations);
  }

}
