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

import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.math.Vector;

public class NavigationStep {

  private final int domain;
  private final Vector startVector;
  private final Vector path;

  private final double totalLength;
  private final Cell destination;

  public NavigationStep(Cell origin, Cell destination) {
    if (origin.domain() != destination.domain()) {
      throw new IllegalArgumentException("Origin and destination must have the same domain");
    }
    this.domain = origin.domain();
    this.startVector = new Vector(origin.blockX(), origin.blockY(), origin.blockZ());
    this.path = new Vector(destination.blockX() - origin.blockX(),
        destination.blockY() - origin.blockY(),
        destination.blockZ() - origin.blockZ());
    this.totalLength = path.magnitude();
    this.destination = destination;
  }

  double length() {
    return totalLength;
  }

  public int domain() {
    return domain;
  }

  public Vector startVector() {
    return startVector;
  }

  public Vector path() {
    return path;
  }

  public Cell destination() {
    return destination;
  }

  @Override
  public String toString() {
    return "NavigationStep{" +
        "startVector=" + startVector +
        ", path=" + path +
        ", destination=" + destination +
        '}';
  }
}
