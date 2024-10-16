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

import java.util.List;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.ai.event.CancelReason;
import net.citizensnpcs.api.ai.tree.BehaviorGoalAdapter;
import net.citizensnpcs.api.ai.tree.BehaviorStatus;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.Gravity;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.bukkit.JourneyBukkitApi;
import net.whimxiqal.journey.search.SearchStep;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

public class GuideBehavior extends BehaviorGoalAdapter {
  private static final int NAVIGATION_TARGET_MIN_DISTANCE_SQUARED = 25;
  private static final int NAVIGATION_TARGET_MAX_DISTANCE_SQUARED = 64;
  private static final int AGENT_DISTANCE_PROGRESS_THRESHOLD_SQUARED = 64;
  private static final int COMPLETION_THRESHOLD_SQUARED = 9;
  private final NpcNavigator navigator;
  private final List<? extends SearchStep> path;
  private boolean done;
  private boolean idle;
  private int currentPathIndex;

  public GuideBehavior(NpcNavigator navigator, List<? extends SearchStep> path, int currentPathIndex) {
    this.navigator = navigator;
    this.path = path;
    this.done = false;
    this.idle = false;
    this.currentPathIndex = currentPathIndex;
  }

  private NPC getNpc() {
    return CitizensAPI.getNPCRegistry().getById(navigator.npcId());
  }

  private Entity getAgent() {
    return Bukkit.getEntity(navigator.agent().uuid());
  }

  @Override
  public void reset() {
    // do nothing
  }

  @Override
  public BehaviorStatus run() {
    if (navigator.stopped()) {
      // This should never happen, the navigator.stop() despawns this NPC
      return BehaviorStatus.RESET_AND_REMOVE;
    }
    NPC npc = getNpc();
    if (npc == null) {
      // This should never happen, the NPC must exist for its behavior to run
      JourneyCitizens.logger().severe("[GuideBehavior] NPC [" + navigator.npcId() + "] could not be found");
      return BehaviorStatus.RESET_AND_REMOVE;
    }
    Entity agent = getAgent();
    if (agent == null) {
      // do nothing and wait
      return BehaviorStatus.RUNNING;
    }

    if (done) {
      performIdleBehavior(npc, agent);
      if (!JourneyBukkitApi.get().toCell(npc.getEntity().getLocation()).equals(path.get(path.size() - 1).location())) {
        // somehow we got off the destination, re-teleport there
        npc.teleport(destination(), PlayerTeleportEvent.TeleportCause.PLUGIN);
      }
      return BehaviorStatus.RUNNING;
    }

    if (atDestination(npc.getEntity())) {
      // wasn't done before, but now we are at the destination
      done = true;
      npc.teleport(destination(), PlayerTeleportEvent.TeleportCause.PLUGIN);
      npc.data().set(NPC.Metadata.FLYABLE, false);
      Gravity gravityTrait = npc.getOrAddTrait(Gravity.class);
      gravityTrait.setHasGravity(true);
      gravityTrait.run();
      if (npc.getNavigator().isNavigating()) {
        npc.getNavigator().cancelNavigation(CancelReason.PLUGIN);
      }
      performIdleBehavior(npc, agent);
      return BehaviorStatus.RUNNING;
    }

    Location npcLocation = npc.getEntity().getLocation();
    Cell npcCell = JourneyBukkitApi.get().toCell(npcLocation);
    if (agentTooFarWay(npc.getEntity(), agent)) {
      // Agent is too far away from NPC, or we've reached the destination. Turn, face agent, and wait
      if (npc.getNavigator().isNavigating()) {
        npc.getNavigator().cancelNavigation(CancelReason.PLUGIN);
      }
      npc.getEntity().setVelocity(new Vector(0, 0, 0));
      performIdleBehavior(npc, agent);
      idle = true;
      return BehaviorStatus.RUNNING;
    }

    // agent is not too far away
    if (idle) {
      // un-idle
      idle = false;
      undoIdleBehavior(npc, agent);
    }

    if (npc.getNavigator().isNavigating()) {
      // just continue whatever navigation we were doing
      return BehaviorStatus.SUCCESS;
    }

    // get the next best target to navigate to
    SearchStep target = path.get(currentPathIndex);
    while (npcCell.distanceToSquared(target.location()) <= NAVIGATION_TARGET_MIN_DISTANCE_SQUARED && currentPathIndex < path.size() - 1) {
      currentPathIndex++;
      target = path.get(currentPathIndex);
    }

    Location targetLocation = JourneyBukkitApi.get().toLocation(target.location()).toCenterLocation();
    if (npcCell.distanceToSquared(target.location()) > NAVIGATION_TARGET_MAX_DISTANCE_SQUARED) {
      // oops, this is too far. Just teleport.
      npc.teleport(targetLocation, PlayerTeleportEvent.TeleportCause.PLUGIN);
      return BehaviorStatus.SUCCESS;
    }

    npc.getNavigator().setTarget(targetLocation);
    return BehaviorStatus.SUCCESS;
  }

  @Override
  public boolean shouldExecute() {
    NPC npc = getNpc();
    if (npc == null) {
      throw new IllegalStateException("NPC could not be found");
    }
    Entity agent = getAgent();
    return agent == null // we want to sit and wait for them to come back
        || done
        || !npc.getNavigator().isNavigating()  // we want to set the navigator
        || agentTooFarWay(npc.getEntity(), agent)  // we want to sit and wait for the user to get close again
        || atDestination(npc.getEntity());
  }

  private boolean agentTooFarWay(Entity npc, Entity agent) {
    return npc.getLocation().distanceSquared(agent.getLocation()) >= AGENT_DISTANCE_PROGRESS_THRESHOLD_SQUARED;
  }

  private boolean atDestination(Entity npc) {
    if (currentPathIndex < path.size() - 1) {
      return false;
    }

    return npc.getLocation().distanceSquared(destination()) <= COMPLETION_THRESHOLD_SQUARED;
  }

  private Location destination() {
    return JourneyBukkitApi.get().toLocation(path.getLast().location()).toCenterLocation();
  }

  private void performIdleBehavior(NPC npc, Entity agent) {
    npc.faceLocation(agent.getLocation());
  }

  private void undoIdleBehavior(NPC npc, Entity agent) {
    // no op for now
  }

}
