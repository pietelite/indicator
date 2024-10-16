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

package net.whimxiqal.journey.scope;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.whimxiqal.journey.Destination;
import net.whimxiqal.journey.Journey;
import net.whimxiqal.journey.JourneyPlayer;
import net.whimxiqal.journey.Scope;
import net.whimxiqal.journey.VirtualMap;
import net.whimxiqal.journey.data.Waypoint;
import net.whimxiqal.journey.message.Formatter;
import net.whimxiqal.journey.message.Messages;
import net.whimxiqal.journey.navigation.PlatformProxy;
import net.whimxiqal.journey.search.DomainGoalSearchSession;
import net.whimxiqal.journey.search.InternalScope;
import net.whimxiqal.journey.search.SearchSession;
import net.whimxiqal.journey.util.Permission;
import net.whimxiqal.journey.util.Validator;

public class ScopeManager {

  private final Map<String, InternalScope> scopes = new HashMap<>();
  private final Map<String, String> plugins = new HashMap<>();

  public void registerDefault() {
    register(Journey.NAME, "personal", Scope.builder()
        .name(Messages.GUI_SCOPE_PERSONAL_TITLE.resolve(Formatter.DULL, null, false))
        .destinations(player -> VirtualMap.of(
            () -> Journey.get().cachedDataProvider().personalWaypointCache()
                .getAll(player.uuid(), false)
                .stream()
                .collect(Collectors.toMap(Waypoint::name, waypoint -> Destination.of(waypoint.location()))),
            Journey.get().cachedDataProvider().personalWaypointCache().getCount(player.uuid(), false)))
        .permission(Permission.PATH_PERSONAL.path())
        .build());
    register(Journey.NAME, "server", Scope.builder()
        .name(Messages.GUI_SCOPE_SERVER_TITLE.resolve(Formatter.DULL, null, false))
        .destinations(player -> VirtualMap.of(
            () -> Journey.get().cachedDataProvider().publicWaypointCache().getAll()
                .stream()
                .collect(Collectors.toMap(Waypoint::name, waypoint -> Destination.of(waypoint.location()))),
            Journey.get().cachedDataProvider().publicWaypointCache().getCount()))
        .permission(Permission.PATH_SERVER.path())
        .build());
    register(Journey.NAME, "player", Scope.builder()
        .name(Messages.GUI_SCOPE_PLAYERS_TITLE.resolve(Formatter.DULL))
        .description(Messages.GUI_SCOPE_PLAYERS_DESCRIPTION.resolve(Formatter.DULL, null, false))
        .subScopes(player -> VirtualMap.of(Journey.get().proxy().platform()
            .onlinePlayers()
            .stream()
            .filter(p -> !p.uuid().equals(player.uuid()))
            .collect(Collectors.<JourneyPlayer, String, Scope>toMap(JourneyPlayer::name, p -> Scope.builder()
                .name(Component.text(p.name()))
                .description(Messages.GUI_SCOPE_PLAYERS_TO_ENTITY_DESCRIPTION.resolve(Formatter.DULL))
                .destinations(() -> p.location()
                    .map(location -> VirtualMap.ofSingleton(p.name(), Destination.cellBuilder(location).permission(Permission.PATH_PLAYER_ENTITY.path()).build()))
                    .orElse(VirtualMap.empty()))
                .subScopes(() -> VirtualMap.ofSingleton("waypoints", Scope.builder()
                    .name(Messages.GUI_SCOPE_PLAYERS_WAYPOINTS_TITLE.resolve(NamedTextColor.WHITE, Formatter.ACCENT, false, p.name()))
                    .description(Messages.GUI_SCOPE_PLAYERS_WAYPOINTS_DESCRIPTION.resolve(Formatter.DULL))
                    .permission(Permission.PATH_PLAYER_WAYPOINTS.path())
                    .destinations(VirtualMap.of(
                        () -> Journey.get().cachedDataProvider().personalWaypointCache()
                            .getAll(p.uuid(), true)
                            .stream()
                            .collect(Collectors.toMap(Waypoint::name, waypoint -> Destination.of(waypoint.location()))),
                        Journey.get().cachedDataProvider().personalWaypointCache().getCount(p.uuid(), true)))
                    .build()))
                .strict()  // to access any player destinations, you must at least scope to the player
                .build()))))
        .build());

    register(Journey.NAME, "world", new InternalScope(Scope.builder()
        .name(Messages.GUI_SCOPE_WORLDS_TITLE.resolve(Formatter.DULL, null, false))
        .build(),
        // destinations are "minecraft" type worlds
        p1 -> {
          Map<String, PlatformProxy.DomainInfo> minecraftDomains = Journey.get().proxy().platform().domainResourceKeys().get("minecraft");
          if (minecraftDomains == null) {
            return VirtualMap.empty();
          } else {
            return p1.location().map(cell -> VirtualMap.of(minecraftDomains.entrySet().stream().collect(Collectors.toMap(entry2 -> entry2.getValue().name(),
                entry2 -> {
                  SearchSession session = new DomainGoalSearchSession(
                      p1.uuid(), SearchSession.Caller.PLAYER,
                      p1, cell, entry2.getValue().id(), false);
                  session.addPermission(Permission.PATH_WORLD.path());
                  return session;
                })))).orElse(VirtualMap.empty());
          }
        },
        // other type worlds are put under separate sub-scopes
        p1 -> VirtualMap.of(Journey.get().proxy().platform().domainResourceKeys()
            .entrySet()
            .stream()
            .filter(entry2 -> !entry2.getKey().equals("minecraft"))
            .collect(Collectors.toMap(Map.Entry::getKey, entry ->
                new InternalScope(Scope.builder()
                    .name(Component.text(entry.getKey()))
                    .build(),
                    p2 -> p2.location().map(cell -> VirtualMap.of(entry.getValue().entrySet().stream()
                        .filter(entry2 -> entry2.getValue().id() != cell.domain())  // can't request to go to their current domain
                        .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry2 -> {
                              SearchSession session = new DomainGoalSearchSession(
                                  p2.uuid(), SearchSession.Caller.PLAYER,
                                  p2, cell, entry2.getValue().id(), false);
                              session.setName(Component.text(entry2.getValue().name()));
                              session.addPermission(Permission.PATH_WORLD.path());
                              return session;
                            })))).orElseGet(VirtualMap::empty),
                    p2 -> VirtualMap.empty()))))));
  }

  public void register(String plugin, String id, Scope scope) {
    register(plugin, id, new InternalScope(scope));
  }

  private void register(String plugin, String id, InternalScope scope) {
    if (Validator.isInvalidDataName(id)) {
      throw new IllegalArgumentException("Scope id '" + id + "' is invalid");
    }
    if (scopes.containsKey(id)) {
      throw new IllegalArgumentException("A scope with id " + id + " already exists");
    }
    scopes.put(id, scope);
    plugins.put(id, plugin);
  }

  public Map<String, InternalScope> scopes() {
    return scopes;
  }

  public String plugin(String scopeId) {
    return plugins.get(scopeId);
  }

  public void initialize() {
    registerDefault();
  }
}
