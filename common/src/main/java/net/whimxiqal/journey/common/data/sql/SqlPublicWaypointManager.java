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

package net.whimxiqal.journey.common.data.sql;

import java.util.Map;
import net.whimxiqal.journey.common.data.DataAccessException;
import net.whimxiqal.journey.common.data.PublicWaypointManager;
import net.whimxiqal.journey.common.navigation.Cell;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A public endpoint manager implemented for SQL.
 */
public class SqlPublicWaypointManager
    extends SqlWaypointManager
    implements PublicWaypointManager {

  /**
   * General constructor.
   *
   * @param connectionController the connection controller
   */
  public SqlPublicWaypointManager(SqlConnectionController connectionController) {
    super(connectionController);
  }

  @Override
  public void add(@NotNull Cell cell, @NotNull String name)
      throws IllegalArgumentException, DataAccessException {
    addWaypoint(null, cell, name);
  }

  @Override
  public void remove(@NotNull Cell cell) throws DataAccessException {
    removeWaypoint(null, cell);
  }

  @Override
  public void remove(@NotNull String name) throws DataAccessException {
    removeWaypoint(null, name);
  }

  @Override
  public void renameWaypoint(String name, String newName) throws DataAccessException {
    super.renameWaypoint(null, name, newName);
  }

  @Override
  public @Nullable String getName(@NotNull Cell cell) throws DataAccessException {
    return getWaypointName(null, cell);
  }

  @Override
  public @Nullable Cell getWaypoint(@NotNull String name) throws DataAccessException {
    return getWaypoint(null, name);
  }

  @Override
  public Map<String, Cell> getAll() throws DataAccessException {
    return getWaypoints(null);
  }
}
