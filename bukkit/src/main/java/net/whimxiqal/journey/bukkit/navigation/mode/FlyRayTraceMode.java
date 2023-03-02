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

package net.whimxiqal.journey.bukkit.navigation.mode;

import java.util.List;
import java.util.Set;
import net.whimxiqal.journey.Journey;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.navigation.ModeType;
import net.whimxiqal.journey.search.SearchSession;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

public class FlyRayTraceMode extends RayTraceMode {
  public FlyRayTraceMode(SearchSession session, Set<Material> forcePassable, Cell destination) {
    super(session, forcePassable, destination, 0.6, 1.8, 0.6, FluidCollisionMode.NEVER);
  }

  @Override
  public @NotNull ModeType type() {
    return ModeType.FLY;
  }

  @Override
  protected boolean check(Cell origin) {
    return Journey.get().proxy().platform().isAtSurface(origin);
  }

  protected void finish(Cell origin, Cell destination, List<Option> options) {
    options.add(Option.between(origin, destination, 1));
  }
}
