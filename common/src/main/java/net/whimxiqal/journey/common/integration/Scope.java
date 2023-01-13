/*
 * MIT License
 *
 * Copyright (c) Pieter Svenson
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

package net.whimxiqal.journey.common.integration;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.whimxiqal.journey.common.Journey;
import net.whimxiqal.journey.common.command.JourneyExecutor;
import net.whimxiqal.journey.common.navigation.Cell;
import net.whimxiqal.mantle.common.CommandSource;

public class Scope {

  private final String name;
  private final Map<String, Scope> subScopes;
  private final Map<String, Cell> items;

  public static Scope root(CommandSource src) {
    RootScopeBuilder builder = new RootScopeBuilder();
    builder.subScope(Scope.builder(JourneyExecutor.PERSONAL_WAYPOINT_SCOPE)
            .items(Journey.get().dataManager().personalWaypointManager().getAll(src.uuid()))
            .build())
        .subScope(Scope.builder(JourneyExecutor.PUBLIC_WAYPOINT_SCOPE)
            .items(Journey.get().dataManager().publicWaypointManager().getAll())
            .build());

    for (Integrator integrator : Journey.get().integrationManager().integrators()) {
      builder.subScope(integrator.scope(src));
    }
    return builder.build();
  }

  public static ScopeBuilder builder(String name) {
    return new ScopeBuilder(name);
  }

  public Scope(String name, Map<String, Scope> subScopes, Map<String, Cell> items) {
    this.name = name;
    this.subScopes = subScopes;
    this.items = items;
  }

  public String name() {
    return name;
  }

  public Collection<String> options() {
    if (name != null) {
      throw new RuntimeException(); // programmer error: must only be called on root
    }
    return options("", new HashSet<>());
  }

  private Collection<String> options(String currentScope, Set<String> allItems) {
    List<String> options = new LinkedList<>();
    for (String candidate : items.keySet()) {
      if (name == null) {
        throw new RuntimeException();  // programmer error: cannot set items if name is null (root)
      }
      if (!allItems.contains(candidate)) {
        allItems.add(candidate);
        options.add(candidate);
      }
      options.add(currentScope + candidate);
    }
    for (Scope scope : subScopes.values()) {
      options.addAll(scope.options(currentScope + scope.name + ":", allItems));
    }
    return options;
  }

  public ScopedLocationResult location(String scopedString) {
    if (name != null) {
      throw new RuntimeException(); // programmer error
    }
    return location("", scopedString);
  }

  private ScopedLocationResult location(String scopeHistory, String scopedString) {
    String[] tokens = scopedString.split(":", 2);
    if (tokens.length == 1) {
      String item = tokens[0];  // = scopedString
      if (items.containsKey(item)) {
        return ScopedLocationResult.exists(items.get(item), scopeHistory);
      }
      // Not found in these items, maybe it's an item among our subscopes?
      // i.e. blah:foo:bar means bar can still show up on scope "blah"
      return locationFromSubScopes(scopeHistory, item);
    } else {
      Scope subScope = subScopes.get(tokens[0]);
      if (subScope == null) {
        return ScopedLocationResult.noScope(tokens[0]);
      }
      return subScope.location(scopeHistory + subScope.name + ":", tokens[1]);
    }
  }

  private ScopedLocationResult locationFromSubScopes(String scopeHistory, String scopedString) {
// Item not found in this scope. Perhaps it's just not scoped and is present in subscope?
    Cell destination = null;
    String existingScope = null;  // scope that our item was found under
    for (Scope subScope : subScopes.values()) {
      ScopedLocationResult result = subScope.location(scopeHistory + subScope.name + ":", scopedString);
      if (result.type() == ScopedLocationResult.Type.AMBIGUOUS) {
        // This has already been determined to be ambiguous. Propagate.
        return result;
      }
      if (result.type() == ScopedLocationResult.Type.EXISTS) {
        if (destination != null) {
          // We've now found two cells! Ambiguous!
          return ScopedLocationResult.ambiguous(existingScope, result.scope().get());
        }
        destination = result.location().get();
        existingScope = result.scope().get();
      }
    }
    if (destination == null) {
      // none found
      return ScopedLocationResult.none();
    }
    return ScopedLocationResult.exists(destination, existingScope);
  }

}
