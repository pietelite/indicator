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

import java.util.ServiceLoader;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * A named place.
 */
public interface Destination extends Describable, Permissible, TargetFunction, TargetSatisfiable {

  static DestinationBuilder cellBuilder(Cell location) {
    return DestinationBuilderFactory.INSTANCE.cellDestinationBuilder(location);
  }

  static DestinationBuilder cellBuilder(CellSupplier location) {
    return DestinationBuilderFactory.INSTANCE.cellDestinationBuilder(location);
  }

  static DestinationBuilder boxBuilder(Cell point1, Cell point2) {
    return DestinationBuilderFactory.INSTANCE.boxDestinationBuilder(point1, point2);
  }

  /**
   * Static constructor using just a location.
   *
   * @param location the location
   * @return the unnamed destination
   */
  static Destination of(Cell location) {
    return DestinationBuilderFactory.INSTANCE.cellDestinationBuilder(location).build();
  }


  @Nullable
  Cell target(Cell origin);


  boolean isSatisfiedBy(Cell location);

  /**
   * Whether the destination is stationary most of the time.
   *
   * @return stationary
   */
  boolean stationary();

}
