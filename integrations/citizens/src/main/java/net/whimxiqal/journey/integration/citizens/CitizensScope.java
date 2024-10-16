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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.text.Component;
import net.whimxiqal.journey.Destination;
import net.whimxiqal.journey.JourneyPlayer;
import net.whimxiqal.journey.Scope;
import net.whimxiqal.journey.VirtualMap;
import net.whimxiqal.journey.bukkit.JourneyBukkitApi;

public class CitizensScope implements Scope {

  private static final String NPC_SCOPE = "journey.path.citizens";

  @Override
  public Component name() {
    return Component.text("NPCs (Citizens)");
  }

  @Override
  public VirtualMap<Destination> destinations(JourneyPlayer player) {
    Map<String, Destination> destinations = new HashMap<>();
    for (NPC npc : CitizensAPI.getNPCRegistry()) {
      if (!npc.isSpawned()) {
        continue;
      }
      destinations.put(String.valueOf(npc.getId()), Destination.cellBuilder(JourneyBukkitApi.get().toCell(npc.getEntity().getLocation()))
          .name(Component.text(npc.getFullName()))
          .build());
    }
    return VirtualMap.of(destinations);
  }

  @Override
  public Optional<String> permission() {
    return Optional.of(NPC_SCOPE);
  }

  @Override
  public boolean isStrict() {
    return true;
  }
}
