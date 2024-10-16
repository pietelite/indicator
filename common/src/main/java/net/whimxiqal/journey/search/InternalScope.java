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

package net.whimxiqal.journey.search;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.Destination;
import net.whimxiqal.journey.Journey;
import net.whimxiqal.journey.JourneyPlayer;
import net.whimxiqal.journey.Scope;
import net.whimxiqal.journey.VirtualMap;
import net.whimxiqal.journey.util.Permission;

/**
 * Wrapper around public-facing {@link Scope}s which stores {@link SearchSession}s as the base component
 * (rather than {@link net.whimxiqal.journey.Destination}s).
 */
public class InternalScope {

  private final Scope scope;
  /**
   * Sessions in addition to those created by the {@link Scope}.
   */
  private final Function<JourneyPlayer, VirtualMap<SearchSession>> sessions;
  /**
   * Internal sub-scopes in addition to the sub-scopes contained in the {@link Scope}.
   */
  private final Function<JourneyPlayer, VirtualMap<InternalScope>> internalSubScopes;

  public InternalScope(Scope scope) {
    this(scope, player -> VirtualMap.empty(), player -> VirtualMap.empty());
  }

  public InternalScope(Scope scope, Function<JourneyPlayer, VirtualMap<SearchSession>> sessions, Function<JourneyPlayer, VirtualMap<InternalScope>> internalScopes) {
    this.scope = scope;
    this.sessions = player -> VirtualMap.of(() -> {
      Optional<Cell> playerLocation = player.location();
      if (playerLocation.isEmpty()) {
        return Collections.emptyMap();
      }
      Map<String, SearchSession> sessionMap = new HashMap<>(sessions.apply(player).getAll());
      for (Map.Entry<String, ? extends Destination> destination : scope.destinations(player).getAll().entrySet()) {
        SearchSession session;
        session = new DestinationGoalSearchSession(player, playerLocation.get(),
            destination.getValue(), false, false);
        session.addFlags(Journey.get().searchManager().getFlagPreferences(player.uuid(), false));
        session.setName(destination.getValue().name());
        session.setDescription(destination.getValue().description());
        destination.getValue().permission().ifPresent(perm -> Permission.journeyPathExtend(perm).forEach(session::addPermission));
        sessionMap.put(destination.getKey(), session);
      }
      return sessionMap;
    }, sessions.apply(player).size() + scope.destinations(player).size());
    this.internalSubScopes = player -> VirtualMap.of(() -> {
      Map<String, InternalScope> scopesMap = new HashMap<>(internalScopes.apply(player).getAll());
      scope.subScopes(player).getAll().forEach((name, subScope) -> scopesMap.put(name, new InternalScope(subScope)));
      return scopesMap;
    }, internalScopes.apply(player).size() + scope.subScopes(player).size());
  }

  public VirtualMap<InternalScope> subScopes(JourneyPlayer player) {
    return internalSubScopes.apply(player);
  }

  public VirtualMap<SearchSession> sessions(JourneyPlayer player) {
    return sessions.apply(player);
  }

  public boolean isStrict() {
    return scope.isStrict();
  }

  public Scope wrappedScope() {
    return scope;
  }

  public List<String> allPermissions() {
    return wrappedScope().permission().map(Permission::journeyPathExtend).orElse(Collections.emptyList());
  }

  public boolean hasAnyDescendantSessions(JourneyPlayer player) {
    if (sessions(player).size() > 0) {
      return true;
    }
    return subScopes(player).getAll().entrySet()
        .stream()
        .anyMatch(entry -> entry.getValue().hasAnyDescendantSessions(player));
  }

}
