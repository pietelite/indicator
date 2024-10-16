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

/**
 * A named place.
 */
public interface Destination extends Describable, Permissible, Targetter {

  /**
   * Builder for a destination defined by a single cell.
   *
   * @param location the cell
   * @return the builder
   */
  static DestinationBuilder cellBuilder(Cell location) {
    return DestinationBuilderFactory.INSTANCE.cellDestinationBuilder(location);
  }

  /**
   * Builder for a destination defined by a cell whose location changes.
   *
   * @param location the variable cell
   * @return the builder
   */
  static DestinationBuilder movingBuilder(CellSupplier location) {
    return DestinationBuilderFactory.INSTANCE.movingDestinationBuilder(location);
  }

  /**
   * Builder for a destination defined by a cuboid region.
   *
   * @param point1 One corner of the region
   * @param point2 Another corner of the region
   * @return the builder
   */
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

}
