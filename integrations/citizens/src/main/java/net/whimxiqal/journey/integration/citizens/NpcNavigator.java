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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.event.DespawnReason;
import net.citizensnpcs.api.npc.NPC;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.whimxiqal.journey.JourneyAgent;
import net.whimxiqal.journey.bukkit.JourneyBukkitApi;
import net.whimxiqal.journey.integration.citizens.config.ConfigSettings;
import net.whimxiqal.journey.navigation.NavigationProgress;
import net.whimxiqal.journey.navigation.Navigator;
import net.whimxiqal.journey.navigation.option.NavigatorOptionValues;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.EntityType;

public class NpcNavigator implements Navigator {

  public static final Set<EntityType> ALLOWED_GUIDE_ENTITY_TYPES = new HashSet<>(Arrays.asList(
      EntityType.WITHER_SKELETON,
      EntityType.STRAY,
      EntityType.HUSK,
      EntityType.ZOMBIE_VILLAGER,
      EntityType.SKELETON_HORSE,
      EntityType.ZOMBIE_HORSE,
      EntityType.DONKEY,
      EntityType.MULE,
      EntityType.EVOKER,
      EntityType.VEX,
      EntityType.VINDICATOR,
      EntityType.ILLUSIONER,
      EntityType.CREEPER,
      EntityType.SKELETON,
      EntityType.SPIDER,
      EntityType.ZOMBIE,
      EntityType.SLIME,
      EntityType.GHAST,
      EntityType.ZOMBIFIED_PIGLIN,
      EntityType.ENDERMAN,
      EntityType.CAVE_SPIDER,
      EntityType.SILVERFISH,
      EntityType.BLAZE,
      EntityType.MAGMA_CUBE,
      EntityType.WITHER,
      EntityType.BAT,
      EntityType.WITCH,
      EntityType.ENDERMITE,
      EntityType.PIG,
      EntityType.SHEEP,
      EntityType.COW,
      EntityType.CHICKEN,
      EntityType.WOLF,
      EntityType.MOOSHROOM,
      EntityType.SNOW_GOLEM,
      EntityType.OCELOT,
      EntityType.IRON_GOLEM,
      EntityType.HORSE,
      EntityType.RABBIT,
      EntityType.POLAR_BEAR,
      EntityType.LLAMA,
      EntityType.PARROT,
      EntityType.VILLAGER,
      EntityType.TURTLE,
      EntityType.PHANTOM,
      EntityType.DROWNED,
      EntityType.CAT,
      EntityType.PANDA,
      EntityType.PILLAGER,
      EntityType.RAVAGER,
      EntityType.TRADER_LLAMA,
      EntityType.WANDERING_TRADER,
      EntityType.FOX,
      EntityType.BEE,
      EntityType.PLAYER
  ));
  private final static int BLOCKS_AHEAD_NPC_STARTS = 1;
  private final JourneyAgent agent;
  private final NavigationProgress progress;
  private final NavigatorOptionValues optionValues;
  private int npcId;
  private boolean stopped = false;

  public NpcNavigator(JourneyAgent agent, NavigationProgress progress, NavigatorOptionValues optionValues) {
    this.agent = agent;
    this.progress = progress;
    this.optionValues = optionValues;
  }

  public static void spawnEntitySpawnParticles(Location location) {
    if (location == null) {
      return;
    }
    Particle particle = ConfigSettings.SPAWN_PARTICLE.load();
    if (particle == null) {
      return;
    }
    particle.builder()
        .location(location)
        .offset(0.5, 0.5, 0.5)
        .color(particle == Particle.DUST ? ConfigSettings.SPAWN_PARTICLE_COLOR.load() : null, 3)
        .count(15)
        .spawn();
  }

  @Override
  public Collection<String> pluginDependencies() {
    return Collections.singleton("Citizens");
  }

  @Override
  public boolean start() {
    JourneyBukkitApi journeyBukkitApi = JourneyBukkitApi.get();
    if (progress.steps().isEmpty()) {
      return true;
    }

    // Get where the NPC spawns
    int spawnLocationIndex = 0;
    for (int i = 0; i < BLOCKS_AHEAD_NPC_STARTS; i++) {
      if (spawnLocationIndex + 1 >= progress.steps().size()) {
        break;
      }
      if (progress.steps().get(spawnLocationIndex).location().domain() != progress.steps().get(spawnLocationIndex + 1).location().domain()) {
        break;
      }
      spawnLocationIndex++;
    }

    EntityType preferredEntityType = optionValues.value(NpcNavigatorOptions.ENTITY_TYPE);
    NPC npc;
    try {
      npc = JourneyCitizens.get().guideStore().issueGuide(preferredEntityType, optionValues.value(NpcNavigatorOptions.NAME));
    } catch (IOException e) {
      JourneyCitizens.logger().severe(e.getMessage());
      return false;
    }
    if (npc == null) {
      agent.audience().sendMessage(Component.text("Could not spawn an NPC right now, try again later").color(NamedTextColor.RED));
      return false;
    }

    Location spawnLocation = journeyBukkitApi.toLocation(progress.steps().get(spawnLocationIndex).location());
    npc.spawn(spawnLocation);
    spawnEntitySpawnParticles(spawnLocation);

    npcId = npc.getId();
    npc.getDefaultGoalController().addGoal(new GuideBehavior(this, progress.steps(), spawnLocationIndex), 1000);

    return true;
  }

  @Override
  public boolean shouldStop() {
    NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
    if (npc == null) {
      return true;
    }
    return !npc.isSpawned();
  }

  @Override
  public void stop() {
    stopped = true;
    JourneyCitizens.get().guideStore().releaseGuide(npcId);
    NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
    if (npc == null) {
      return;
    }
    spawnEntitySpawnParticles(npc.getStoredLocation());
    npc.despawn(DespawnReason.PLUGIN);
  }

  public boolean stopped() {
    return stopped;
  }

  public int npcId() {
    return npcId;
  }

  public JourneyAgent agent() {
    return agent;
  }
}
