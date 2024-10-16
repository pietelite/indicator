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

package net.whimxiqal.journey.navigation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.Tunnel;
import net.whimxiqal.journey.chunk.BlockProvider;
import net.whimxiqal.journey.search.ModeType;
import org.jetbrains.annotations.NotNull;

/**
 * A series of steps that determine a possible path through a Minecraft world.
 */
public class Path implements Serializable {

  private final Cell origin;
  private final ArrayList<Step> steps;
  private final double cost;
  private final Runnable prompt;
  private final Predicate<Cell> completedWith;

  /**
   * General constructor.
   *
   * @param origin the origin of the whole path
   * @param steps  the steps required to get there
   * @param cost   the distance of the path
   */
  public Path(Cell origin, @NotNull List<Step> steps, double cost, Runnable prompt, Predicate<Cell> completedWith) {
    this.origin = origin;
    this.steps = new ArrayList<>(steps);
    this.cost = cost;
    this.prompt = prompt;
    this.completedWith = completedWith;
  }

  public Path(Cell origin, @NotNull List<Step> steps, double cost) {
    this.origin = origin;
    this.steps = new ArrayList<>(steps);
    this.cost = cost;
    this.prompt = () -> {};
    this.completedWith = cell -> cell.distanceToSquared(getDestination()) < 4;
  }

  /**
   * Create an invalid path.
   *
   * @return an invalid path (infinite length)
   */
  @NotNull
  public static Path invalid() {
    return new Path(null, new ArrayList<>(), Double.MAX_VALUE);
  }

  public static Path fromTunnel(Tunnel tunnel, Cell origin) {
    return new Path(origin, Collections.singletonList(new Step(tunnel.exit(), 0, ModeType.TUNNEL, tunnel::prompt)),
        tunnel.cost(), tunnel::prompt, cell -> tunnel.exit().equals(cell));
  }

  public static Path stationary(Cell location) {
    return new Path(location, Collections.singletonList(new Step(location, 0, ModeType.NONE)),
        0, () -> {}, cell -> true);
  }

  /**
   * Get the origin of the path.
   *
   * @return the origin location
   */
  @NotNull
  public final Cell getOrigin() {
    return origin;
  }

  /**
   * Get the destination of the path.
   *
   * @return the destination location
   */
  @NotNull
  public final Cell getDestination() {
    if (steps.isEmpty()) {
      throw new IllegalStateException("There is no destination for a path with no steps.");
    }
    return steps.get(steps.size() - 1).location();
  }

  /**
   * Get a copy of all the steps in this path.
   *
   * @return the steps
   */
  @NotNull
  public final ArrayList<Step> getSteps() {
    return new ArrayList<>(steps);
  }

  /**
   * Get the length of the path.
   *
   * @return the path length
   */
  public final double getCost() {
    return cost;
  }

  /**
   * Check if the existing at the given location constitutes the completion
   * of this path if traversed by some entity.
   * For example, if a player was walking along a path, they would be considered
   * to have reached the end if this returned true with their current location input.
   *
   * @param location the location to test
   * @return true if the path has been completed
   */
  public boolean completedWith(Cell location) {
    return completedWith.test(location);
  }

  /**
   * Verify if the path is still valid and traversable with the given mode collection.
   *
   * @param modes all modes
   * @return true if the path can be traversed, or false if it is impassable
   */
  public boolean test(Collection<Mode> modes, BlockProvider blockProvider) throws ExecutionException, InterruptedException {
    stepLoop:
    for (int i = 0; i < steps.size() - 1; i++) {
      for (Mode mode : modes) {
        for (Mode.Option option : mode.getDestinations(steps.get(i).location(), blockProvider)) {
          if (steps.get(i + 1).location().equals(option.location())) {
            continue stepLoop;  // we found a mode that gave us a fitting option. Continue to the next step.
          }
        }
      }
      // We've exhausted all modes (and each mode's options) for this step, so fail.
      return false;
    }
    return true;
  }

  /**
   * Get domain of this path (there is only one!).
   *
   * @return the domain
   */
  public int domain() {
    return getDestination().domain();
  }

  @Override
  public String toString() {
    return "Path{"
        + "origin=" + origin
        + ", # of steps=" + steps.size()
        + ", distance=" + cost
        + '}';
  }
}
