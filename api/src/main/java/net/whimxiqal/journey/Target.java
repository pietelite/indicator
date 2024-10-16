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

package net.whimxiqal.journey;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

/**
 * A function to determine the target.
 */
public interface Target {

  /**
   * Given a starting location, the target location that should be approached.
   *
   * @param origin the origin
   * @return an appropriate destination
   */
  @Nullable
  Cell get(Cell origin);

  /**
   * Determine whether a given location is "at" the destination.
   *
   * @param location the location to check
   * @return true if destination is reached by being at the given location
   */
  boolean isSatisfiedBy(Cell location);

  /**
   * The domain that this targeting is operating in.
   *
   * @return the domain
   */
  int domain();

  Component print();

}
