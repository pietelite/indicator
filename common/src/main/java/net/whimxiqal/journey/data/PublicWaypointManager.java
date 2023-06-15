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

package net.whimxiqal.journey.data;

import net.whimxiqal.journey.Cell;
import org.jetbrains.annotations.NotNull;

/**
 * A manager to handle public endpoints.
 */
public interface PublicWaypointManager extends PublicWaypointProvider {

  /**
   * Add a server endpoint.
   *
   * @param cell the cell to add
   * @param name the name of the location
   * @throws IllegalArgumentException for invalid inputs
   */
  void add(@NotNull Cell cell, @NotNull String name)
      throws IllegalArgumentException, DataAccessException;

  /**
   * Remove a cell. Name is irrelevant. Does nothing if cell isn't saved.
   *
   * @param cell the cell location
   */
  void remove(@NotNull Cell cell) throws DataAccessException;

  /**
   * Remove a cell from the database by name. Cell location is irrelevant.
   * Does nothing if the name doesn't exist
   *
   * @param name the name of the location
   */
  void remove(@NotNull String name) throws DataAccessException;

  /**
   * Rename a waypoint.
   *
   * @param name    the name
   * @param newName the new name
   */
  void renameWaypoint(String name, String newName) throws DataAccessException;

}
