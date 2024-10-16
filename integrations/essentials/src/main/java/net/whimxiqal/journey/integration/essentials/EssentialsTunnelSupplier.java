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
import com.earth2me.essentials.api.IWarps;
import com.earth2me.essentials.commands.WarpNotFoundException;
import com.earth2me.essentials.spawn.IEssentialsSpawn;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.ess3.api.InvalidWorldException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.JourneyAgent;
import net.whimxiqal.journey.Tunnel;
import net.whimxiqal.journey.TunnelSupplier;
import net.whimxiqal.journey.bukkit.JourneyBukkitApi;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class EssentialsTunnelSupplier implements TunnelSupplier {

  public static final int TELEPORT_COST = 8;

  @Override
  @SuppressWarnings("deprecation")
  public Collection<Tunnel> tunnels(JourneyAgent agent) {
    Optional<Cell> location = agent.location();
    if (location.isEmpty()) {
      return Collections.emptyList();
    }
    List<Tunnel> tunnels = new LinkedList<>();
    IEssentials essentials = JourneyEssentials.essentials();

    // Warps
    IWarps warps = essentials.getWarps();
    warps.getList()
        .stream()
        .map(warp -> {
          try {
            return Tunnel.builder(location.get(), JourneyBukkitApi.get().toCell(warps.getWarp(warp)))
                .permission("essentials.warp")
                .prompt(() -> agent.audience().sendMessage(teleportMessage(warp, "/warp " + warp)))
                .cost(TELEPORT_COST)
                .build();
          } catch (WarpNotFoundException | InvalidWorldException e) {
            e.printStackTrace();
            // This should definitely never happen
            return null;
          }
        })
        .filter(Objects::nonNull)
        .forEach(tunnels::add);

    // Homes
    User user = essentials.getUser(agent.uuid());
    if (user.hasValidHomes()) {
      List<String> homes = user.getHomes();
      homes.stream()
          .map(home -> Tunnel.builder(location.get(), JourneyBukkitApi.get().toCell(user.getHome(home)))
              .permission("essentials.home")
              .prompt(() -> agent.audience().sendMessage(teleportMessage(home, "/home" + (homes.size() == 1 ? "" : " " + home))))
              .cost(TELEPORT_COST)
              .build())
          .forEach(tunnels::add);
    }

    if (Bukkit.getPluginManager().isPluginEnabled("EssentialsSpawn")) {
      Plugin plugin = Bukkit.getPluginManager().getPlugin("EssentialsSpawn");
      if (!(plugin instanceof IEssentialsSpawn)) {
        throw new RuntimeException("EssentialsSpawn class could not be found");
      }
      tunnels.add(Tunnel.builder(location.get(), JourneyBukkitApi.get().toCell(((IEssentialsSpawn) plugin).getSpawn(user.getGroup())))
          .permission("essentials.spawn")
          .prompt(() -> agent.audience().sendMessage(teleportMessage("spawn", "/spawn")))
          .cost(TELEPORT_COST)
          .build());
    }
    return tunnels;
  }

  private Component teleportMessage(String name, String command) {
    return Component.text("Teleport to ")
        .append(Component.text(name).color(NamedTextColor.AQUA))
        .append(Component.text(" using the command "))
        .append(Component.text(command).color(NamedTextColor.AQUA));
  }
}
