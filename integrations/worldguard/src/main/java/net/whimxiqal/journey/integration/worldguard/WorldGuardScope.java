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

package net.whimxiqal.journey.integration.worldguard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.Destination;
import net.whimxiqal.journey.JourneyPlayer;
import net.whimxiqal.journey.Scope;
import net.whimxiqal.journey.VirtualMap;
import net.whimxiqal.journey.bukkit.JourneyBukkitApi;
import org.bukkit.NamespacedKey;

public class WorldGuardScope implements Scope {
  @Override
  public Component name() {
    return Component.text("WorldGuard");
  }

  @Override
  public VirtualMap<Scope> subScopes(JourneyPlayer player) {
    return VirtualMap.of(() -> {
      Map<String, Scope> scopes = new HashMap<>();

      Map<String, Map<String, Map<String, Destination>>> destinations = new HashMap<>();
      for (Map.Entry<Integer, List<CachedRegions.CachedRegion>> entry : JourneyWorldGuard.instance().cachedRegions().get().entrySet()) {
        NamespacedKey key = JourneyBukkitApi.get().toWorld(entry.getKey()).getKey();
        destinations.computeIfAbsent(key.getNamespace(), k -> new HashMap<>()).put(key.getKey(), entry.getValue().stream()
            .collect(Collectors.toMap(CachedRegions.CachedRegion::name, r -> Destination.boxBuilder(
                    new Cell(r.min().getBlockX(), r.min().getBlockY(), r.min().getBlockZ(), entry.getKey()),
                    new Cell(r.max().getBlockX(), r.max().getBlockY(), r.max().getBlockZ(), entry.getKey()))
                .build())));
      }

      scopes.put("regions", Scope.builder()
          .subScopes(VirtualMap.of(destinations.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e1 ->  // keyed by namespace key
              Scope.builder()
                  .subScopes(VirtualMap.of(e1.getValue().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e2 ->  // keyed by namespace value
                      Scope.builder()
                          .destinations(VirtualMap.of(e2.getValue().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))))
                          .build()))))
                  .build()))))
          .build());
      return scopes;
    }, JourneyWorldGuard.instance().cachedRegions().get().size());
  }

  @Override
  public boolean isStrict() {
    return true;
  }

  @Override
  public Optional<String> permission() {
    return Optional.of("worldguard.region.list");
  }
}
