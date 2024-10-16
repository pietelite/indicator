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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import net.kyori.adventure.text.Component;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.Destination;
import net.whimxiqal.journey.Journey;
import net.whimxiqal.journey.JourneyPlayer;
import net.whimxiqal.journey.Scope;
import net.whimxiqal.journey.VirtualMap;
import net.whimxiqal.journey.search.InternalScope;
import net.whimxiqal.journey.search.SearchSession;
import net.whimxiqal.journey.search.SurfaceGoalSearchSession;
import net.whimxiqal.journey.util.Permission;
import net.whimxiqal.journey.util.Validator;

public final class ScopeUtil {

  public final static long MAX_SIZE_TO_SHOW_OPTIONS = 128;

  private ScopeUtil() {
  }

  public static InternalScope root() {
    return new InternalScope(Scope.builder()
        .destinations(player -> {
          Map<String, Destination> destinations = new HashMap<>();
          Journey.get().playerManager().getDeathLocation(player.uuid()).ifPresent(location ->
              destinations.put("death", Destination.cellBuilder(location).permission(Permission.PATH_DEATH.path()).build()));
          return VirtualMap.of(destinations);
        })
        .build(),
        player -> {
          try {
            Optional<Cell> playerLocation = player.location();
            if (playerLocation.isPresent() && !Journey.get().locationManager().getAndTryUpdateIsAtSurface(player.uuid(), playerLocation.get())) {
              SearchSession surfaceSession = new SurfaceGoalSearchSession(player.uuid(), SearchSession.Caller.PLAYER, player, playerLocation.get());
              surfaceSession.setName(Component.text("Go to surface"));
              surfaceSession.addPermission(Permission.PATH_SURFACE.path());
              return VirtualMap.ofSingleton("surface", surfaceSession);
            }
          } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
          }
          return VirtualMap.empty();
        },
        player -> VirtualMap.of(Journey.get().scopeManager().scopes()));
  }

  public static Collection<String> options(JourneyPlayer player) {
    return options(null, root(), player, new Stack<>(), new HashSet<>(), Integer.MAX_VALUE);
  }

  private static Collection<String> options(String plugin, InternalScope scope, JourneyPlayer player, Stack<String> scopeHistory, Set<String> options, int minScopeIndexRequired) {
    VirtualMap<InternalScope> subScopes = scope.subScopes(player);
    VirtualMap<SearchSession> destinations = scope.sessions(player);
    if (scope.isStrict()) {
      minScopeIndexRequired = Math.min(scopeHistory.size() - 1, minScopeIndexRequired);
    }
    int totalSize = subScopes.size() + destinations.size();
    if (totalSize > MAX_SIZE_TO_SHOW_OPTIONS) {
      return options;
    }
    for (Map.Entry<String, ? extends SearchSession> entry : destinations.getAll().entrySet()) {
      if (Validator.isInvalidDataName(entry.getKey())) {
        if (plugin != null) {
          Journey.logger().warn("Destination from plugin "
              + plugin + " is using an invalid destination id: "
              + entry.getKey() + ". Please notify the plugin owner.");
        }
        continue;
      }
      if (restricted(entry.getValue().permissions(), player)) {
        continue;
      }
      // add with scoping (from most specific to least specific)
      LinkedList<String> scopeStrings = new LinkedList<>();
      StringBuilder currentScope = new StringBuilder();
      for (int i = scopeHistory.size() - 1; i >= 0; i--) {
        // is the destination name the same as the scope directly above it?
        if (i == scopeHistory.size() - 1 && scopeHistory.peek().equals(entry.getKey())) {
          // we just use the name once if a destination uses the same name as its scope
          continue;
        }
        // the scope string must include the scope at the index minScopeIndexRequired (which would have been added last iteration)
        if (i < minScopeIndexRequired) {
          scopeStrings.add(currentScope.toString());
        }
        currentScope.insert(0, scopeHistory.get(i) + ":");
      }
      // the entire scope history has been concatenated -- this will always be included
      scopeStrings.add(currentScope.toString());
      for (String scopeString : scopeStrings) {
        String newCandidate = scopeString + entry.getKey();
        if (options.contains(newCandidate)) {
          options.remove(newCandidate);  // if left in there, it would be ambiguous
        } else {
          options.add(newCandidate);
        }
      }
    }
    for (Map.Entry<String, ? extends InternalScope> subScope : subScopes.getAll().entrySet()) {
      String pluginName = plugin == null ? Journey.get().scopeManager().plugin(subScope.getKey()) : plugin;
      if (Validator.isInvalidDataName(subScope.getKey())) {
        Journey.logger().warn("Scope from plugin "
            + pluginName + " is using an invalid sub-scope id: "
            + subScope.getKey() + ". Please notify the plugin owner.");
        continue;
      }
      if (restricted(subScope.getValue().allPermissions(), player)) {
        continue;
      }
      scopeHistory.push(subScope.getKey());
      options.addAll(options(pluginName, subScope.getValue(), player, scopeHistory, options, minScopeIndexRequired));
      scopeHistory.pop();
    }
    return options;
  }

  public static ScopedSessionResult session(JourneyPlayer player, String scopedString) {
    InternalScope root = root();
    return session(root, player, "", scopedString);
  }

  private static ScopedSessionResult session(InternalScope scope, JourneyPlayer player, String scopeHistory, String scopedString) {
    String[] tokens = scopedString.split(":", 2);
    String item = tokens[0];  // = scopedString
    if (tokens.length == 1) {
      // this is the last item, so it must either be a direct call to a destination or an indirect call to a destination
      // that's part of some sub-scope.
      SearchSession sesh = scope.sessions(player).get(item);
      if (sesh != null) {
        // it's the name of a destination
        if (restricted(sesh.permissions(), player)) {
          return ScopedSessionResult.noPermission();
        } else {
          return ScopedSessionResult.exists(sesh, scopeHistory);
        }
      }
      // maybe this is the name of a sub-scope, indicating that a destination of the sub-scope with the same name is the goal
      VirtualMap<InternalScope> subScopes = scope.subScopes(player);
      InternalScope subScope = subScopes.get(item);
      if (subScope != null) {
        sesh = subScope.sessions(player).get(item);
        if (sesh != null) {
          // it's the name of a destination hidden under the name of the identically named sub-scope.
          if (restricted(sesh.permissions(), player)) {
            return ScopedSessionResult.noPermission();
          } else {
            return ScopedSessionResult.exists(sesh, scopeHistory);
          }
        }
      }
      // Not found in these items, maybe it's an item among our sub-scopes?
      // i.e. blah:foo:bar means "bar" can still show up on scope "blah"
      return locationFromSubScopes(subScopes.getAll(), player, scopeHistory, scopedString);
    } else {
      VirtualMap<InternalScope> subScopes = scope.subScopes(player);
      InternalScope subScope = subScopes.get(item);
      if (subScope == null) {
        return locationFromSubScopes(subScopes.getAll(), player, scopeHistory, scopedString);
      }
      if (restricted(subScope.allPermissions(), player)) {
        return ScopedSessionResult.noPermission();
      }
      return session(subScope, player, scopeHistory + item + ":", tokens[1]);
    }
  }

  private static ScopedSessionResult locationFromSubScopes(Map<String, ? extends InternalScope> subScopes, JourneyPlayer player, String scopeHistory, String scopedString) {
    // Item not found in this scope. Perhaps it's just not scoped and is present in a subscope?
    SearchSession session = null;
    String existingScope = null;  // scope that our item was found under
    for (Map.Entry<String, ? extends InternalScope> subScope : subScopes.entrySet()) {
      if (subScope.getValue().isStrict()) {
        continue;
      }
      if (restricted(subScope.getValue().allPermissions(), player)) {
        continue;
      }
      ScopedSessionResult result = session(subScope.getValue(), player, scopeHistory + subScope.getKey() + ":", scopedString);
      if (result.type() == ScopedSessionResult.Type.AMBIGUOUS) {
        // This has already been determined to be ambiguous. Propagate.
        return result;
      }
      if (result.type() == ScopedSessionResult.Type.EXISTS) {
        if (session != null) {
          // We've now found two sessions! Ambiguous!
          return ScopedSessionResult.ambiguous(existingScope, result.scope().get());
        }
        session = result.session().get();
        existingScope = result.scope().get();
      }
    }
    if (session == null) {
      // none found
      return ScopedSessionResult.none();
    }
    if (restricted(session.permissions(), player)) {
      return ScopedSessionResult.noPermission();
    }
    return ScopedSessionResult.exists(session, existingScope);
  }

  public static boolean restricted(List<String> permissions, JourneyPlayer player) {
    return !permissions.stream().allMatch(player::hasPermission);
  }

}
