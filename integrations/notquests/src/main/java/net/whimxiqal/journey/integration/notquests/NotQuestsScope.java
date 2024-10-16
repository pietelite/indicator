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

import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.whimxiqal.journey.Destination;
import net.whimxiqal.journey.DestinationBuilder;
import net.whimxiqal.journey.JourneyPlayer;
import net.whimxiqal.journey.Scope;
import net.whimxiqal.journey.ScopeBuilder;
import net.whimxiqal.journey.VirtualMap;
import net.whimxiqal.journey.bukkit.JourneyBukkitApi;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.npc.NQNPC;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.ActiveQuest;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

public class NotQuestsScope implements Scope {
  @Override
  public Component name() {
    return Component.text("NotQuests");
  }

  @Override
  public VirtualMap<Scope> subScopes(JourneyPlayer player) {
    NotQuests notQuests = JourneyNotQuests.notQuests();
    Map<String, Scope> subScopes = new HashMap<>();

    QuestPlayer qPlayer = notQuests.getQuestPlayerManager().getActiveQuestPlayer(player.uuid());
    if (qPlayer == null) {
      return VirtualMap.of(subScopes);
    }
    for (ActiveQuest quest : qPlayer.getActiveQuests()) {
      ScopeBuilder questScope = Scope.builder();
      questScope.name(Component.text(quest.getQuest().getDisplayNameOrIdentifier()));

      Map<String, Scope> questSubScopes = new HashMap<>();
      for (ActiveObjective objective : quest.getActiveObjectives()) {
        ScopeBuilder objectiveScope = Scope.builder();
        objectiveScope.name(Component.text(String.valueOf(objective.getObjective().getDisplayNameOrIdentifier())));
        objectiveScope.description(Component.text("Objective " + objective.getObjectiveID()));

        Map<String, Destination> objectiveDestinations = new HashMap<>();
        Location objectiveLocation = objective.getObjective().getLocation();
        if (objectiveLocation != null) {
          DestinationBuilder objectiveDestination = Destination.cellBuilder(JourneyBukkitApi.get().toCell(objectiveLocation));
          objectiveDestination.name(Component.text("Location"));
          objectiveDestinations.put("location", objectiveDestination.build());
        }
        NQNPC npc = objective.getObjective().getCompletionNPC();
        if (npc != null) {
          Entity entity = npc.getEntity();
          if (entity != null) {
            DestinationBuilder entityDestination = Destination.movingBuilder(() -> {
              Entity _entity = Bukkit.getEntity(entity.getUniqueId());
              if (_entity == null) {
                return null;
              }
              return JourneyBukkitApi.get().toCell(_entity.getLocation());
            });
            String name = npc.getName();
            if (name == null) {
              name = npc.getIdentifyingString();
            }
            entityDestination.name(Component.text(name));
            entityDestination.description(Component.text(npc.getNPCType()));
            objectiveDestinations.put(npc.getIdentifyingString(), entityDestination.build());
          }
        }
        objectiveScope.destinations(VirtualMap.of(objectiveDestinations));
        questSubScopes.put(String.valueOf(objective.getObjectiveID()), objectiveScope.build());
      }
      questScope.subScopes(VirtualMap.of(questSubScopes));
      questScope.strict();  // strict because the quest should always be mentioned to refer to any of its objectives and destinations
      subScopes.put(quest.getQuestIdentifier(), questScope.build());
    }

    return VirtualMap.of(subScopes);
  }

}
