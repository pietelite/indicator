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

package net.whimxiqal.journey.data.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.Journey;
import net.whimxiqal.journey.data.DataAccessException;
import net.whimxiqal.journey.data.PathRecordManager;
import net.whimxiqal.journey.search.ModeType;
import net.whimxiqal.journey.navigation.Path;
import net.whimxiqal.journey.navigation.Step;
import net.whimxiqal.journey.search.DestinationPathTrial;
import net.whimxiqal.journey.util.UUIDUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A generic path record manager for SQL storage.
 */
public class SqlPathRecordManager
    extends SqlManager
    implements PathRecordManager {

  /**
   * General constructor.
   *
   * @param connectionController the connection controller
   */
  public SqlPathRecordManager(SqlConnectionController connectionController) {
    super(connectionController);
  }

  @Override
  public void report(DestinationPathTrial trial,
                     Set<ModeType> modeTypes,
                     long executionTime)
      throws DataAccessException {
    Path path = trial.getPath();
    if (path == null) {
      throw new IllegalArgumentException("The path of he input path trial was not valid."
          + " The input path trial must be successful and have a valid path.");
    }
    if (path.getSteps().isEmpty()) {
      // This is not something we need to report
      return;
    }

    // Delete any previous record if it has the same origin/destination/world and is slower
    // TODO [caching]
    List<PathTrialRecord> oldRecords = null; // getRecords(trial.getOrigin(), trial.getDestination());
    for (PathTrialRecord oldRecord : oldRecords) {
      try (Connection connection = getConnectionController().establishConnection()) {
        if (oldRecord.pathCost() <= path.getCost()) {
          continue;
        }
        if (modeTypes.containsAll(oldRecord.modes().stream().map(PathTrialModeRecord::modeType).toList())) {
          // this path distance is better and can do it in the same or fewer modes, so delete the current one
          connection.prepareStatement("DELETE FROM " + SqlManager.CACHED_PATHS_TABLE
                  + " WHERE "
                  + "id = " + oldRecord.id())
              .execute();
        }
      } catch (SQLException e) {
        e.printStackTrace();
        throw new DataAccessException();
      }
    }

    long pathReportId = -1;
    try (Connection connection = getConnectionController().establishConnection()) {
      PreparedStatement statement = connection.prepareStatement(String.format(
              "INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s) "
                  + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);",
              SqlManager.CACHED_PATHS_TABLE,
              "created",
              "duration",
              "path_length",
              "origin_x",
              "origin_y",
              "origin_z",
              "destination_x",
              "destination_y",
              "destination_z",
              "domain_id"),
          Statement.RETURN_GENERATED_KEYS);

      statement.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
      statement.setInt(2, (int) executionTime);
      statement.setDouble(3, trial.getLength());
      statement.setInt(4, trial.getOrigin().blockX());
      statement.setInt(5, trial.getOrigin().blockY());
      statement.setInt(6, trial.getOrigin().blockZ());
      // TODO [caching]
//      statement.setInt(7, trial.getDestination().blockX());
//      statement.setInt(8, trial.getDestination().blockY());
//      statement.setInt(9, trial.getDestination().blockZ());
      statement.setBytes(10, UUIDUtil.uuidToBytes(Journey.get().domainManager().domainId(trial.getDomain())));

      statement.execute();

      try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
        if (generatedKeys.next()) {
          pathReportId = generatedKeys.getLong(1);
        }
      }

    } catch (SQLException e) {
      e.printStackTrace();
      throw new DataAccessException();
    }

    if (pathReportId < 0) {
      throw new DataAccessException("No id found from the inserted path record");
    }
    ArrayList<Step> steps = path.getSteps();
    for (int i = 0; i < steps.size(); i++) {
      Step step = steps.get(i);
      try (Connection connection = getConnectionController().establishConnection()) {
        PreparedStatement statement = connection.prepareStatement(String.format(
            "INSERT INTO %s (%s, %s, %s, %s, %s, %s) "
                + "VALUES (?, ?, ?, ?, ?, ?);",
            SqlManager.CACHED_PATH_CELLS_TABLE,
            "path_id",
            "x", "y", "z",
            "path_index",
            "mode_type"));

        statement.setLong(1, pathReportId);
        statement.setInt(2, step.location().blockX());
        statement.setInt(3, step.location().blockY());
        statement.setInt(4, step.location().blockZ());
        statement.setInt(5, i);
        statement.setInt(6, step.mode().id());
        statement.execute();
      } catch (SQLException e) {
        e.printStackTrace();
        throw new DataAccessException();
      }
    }

    for (ModeType modeType : modeTypes) {
      try (Connection connection = getConnectionController().establishConnection()) {
        PreparedStatement statement = connection.prepareStatement(String.format(
            "INSERT INTO %s (%s, %s) VALUES (?, ?);",
            SqlManager.CACHED_PATH_MODES_TABLE,
            "path_id",
            "mode_type"));

        statement.setLong(1, pathReportId);
        statement.setInt(2, modeType.id());

        statement.execute();
      } catch (SQLException e) {
        e.printStackTrace();
        throw new DataAccessException();
      }
    }

  }

  @Override
  public void truncate() {
    try (Connection connection = getConnectionController().establishConnection()) {
      PreparedStatement statement = connection.prepareStatement(String.format(
          "DELETE FROM %s;",
          SqlManager.CACHED_PATHS_TABLE));
      statement.execute();

      statement = connection.prepareStatement(String.format(
          "DELETE FROM %s;",
          SqlManager.CACHED_PATH_CELLS_TABLE));
      statement.execute();

      statement = connection.prepareStatement(String.format(
          "DELETE FROM %s;",
          SqlManager.CACHED_PATH_MODES_TABLE));
      statement.execute();
    } catch (SQLException e) {
      e.printStackTrace();
      throw new DataAccessException();
    }
  }

  @Override
  public int totalRecordCellCount() {
    try (Connection connection = getConnectionController().establishConnection()) {
      PreparedStatement statement = connection.prepareStatement(String.format(
          "SELECT COUNT(*) FROM %s;",
          SqlManager.CACHED_PATH_CELLS_TABLE));
      ResultSet result = statement.executeQuery();
      if (result.next()) {
        return result.getInt(1);
      } else {
        return 0;
      }
    } catch (SQLException e) {
      e.printStackTrace();
      throw new DataAccessException();
    }
  }

  /**
   * Get all records that have this origin and destination,
   * but do not populate the internal cells, but <b>do</b> populate
   * the modes.
   *
   * @param origin      the origin location
   * @param destination the destination location
   * @return a list of all records
   */
  private List<PathTrialRecord> getRecordsWithoutCells(Cell origin, Cell destination) {
    try (Connection connection = getConnectionController().establishConnection()) {
      ResultSet recordResult = connection.prepareStatement("SELECT * FROM "
              + SqlManager.CACHED_PATHS_TABLE
              + " WHERE "
              + "origin_x = " + origin.blockX() + " AND "
              + "origin_y = " + origin.blockY() + " AND "
              + "origin_z = " + origin.blockZ() + " AND "
              + "destination_x = " + destination.blockX() + " AND "
              + "destination_y = " + destination.blockY() + " AND "
              + "destination_z = " + destination.blockZ() + " AND "
              + "domain_id = '" + origin.domain() + "'")
          .executeQuery();
      List<PathTrialRecord> records = new LinkedList<>();
      while (recordResult.next()) {
        PathTrialRecord record = extractRecord(recordResult);
        ResultSet modeResult = connection.prepareStatement("SELECT * FROM "
            + SqlManager.CACHED_PATH_MODES_TABLE
            + " WHERE "
            + "path_id = " + record.id()).executeQuery();
        while (modeResult.next()) {
          record.modes().add(new PathTrialModeRecord(record,
              Objects.requireNonNull(ModeType.get(modeResult.getInt("mode_type")))));
        }
        records.add(record);
      }

      return records;
    } catch (SQLException e) {
      e.printStackTrace();
      return Collections.emptyList();
    }
  }

  @Override
  public @NotNull List<PathTrialRecord> getRecords(Cell origin, Cell destination) {
    try (Connection connection = getConnectionController().establishConnection()) {
      List<PathTrialRecord> emptyRecords = getRecordsWithoutCells(origin, destination);

      // Add the subcomponents (modes and cells) to the previously empty records
      for (PathTrialRecord emptyRecord : emptyRecords) {
        ResultSet cellResult = connection.prepareStatement("SELECT * FROM "
            + SqlManager.CACHED_PATH_CELLS_TABLE
            + " WHERE "
            + "path_id = " + emptyRecord.id()).executeQuery();
        while (cellResult.next()) {
          emptyRecord.cells().add(extractCell(emptyRecord, cellResult));
        }
      }
      return emptyRecords;
    } catch (SQLException e) {
      e.printStackTrace();
      return Collections.emptyList();
    }
  }

  @Nullable
  private PathTrialRecord findRecordWithModes(Collection<PathTrialRecord> records, Set<ModeType> modeTypes) {
    for (PathTrialRecord record : records) {
      if (record.modes()
          .stream()
          .map(PathTrialModeRecord::modeType)
          .allMatch(modeTypes::contains)) {
        return record;
      }
    }
    return null;
  }

  @Override
  public PathTrialRecord getRecord(Cell origin, Cell destination, Set<ModeType> modeTypes) {
    return findRecordWithModes(getRecords(origin, destination), modeTypes);
  }

  @Override
  public Path getPath(Cell origin, Cell destination, Set<ModeType> modeTypeGroup) {
    try (Connection connection = getConnectionController().establishConnection()) {
      PathTrialRecord record = findRecordWithModes(getRecordsWithoutCells(origin, destination),
          modeTypeGroup);

      ResultSet cellResult = connection.prepareStatement("SELECT * FROM "
          + SqlManager.CACHED_PATH_CELLS_TABLE
          + " WHERE "
          + "path_id = " + record.id()).executeQuery();
      while (cellResult.next()) {
        record.cells().add(extractCell(record, cellResult));
      }
      if (record.cells().isEmpty()) {
        throw new DataAccessException("Tried to get a path (id:" + record.id() + "), but found no path cells");
      }

      record.cells().sort(Comparator.comparing(PathTrialCellRecord::index));

      LinkedList<Step> steps = new LinkedList<>();

      // Add the first one because we don't move to get here
      steps.add(new Step(record.cells().get(0).toCell(), 0, record.cells().get(0).modeType()));
      for (int i = 1; i < record.cells().size(); i++) {
        Cell cell = record.cells().get(i).toCell();
        steps.add(new Step(cell,
            cell.distanceTo(steps.getLast().location()),
            record.cells().get(i).modeType()));
      }

      return new Path(steps.getFirst().location(), steps, record.pathCost());
    } catch (SQLException e) {
      e.printStackTrace();
      return null;
    }
  }

  @Override
  public boolean containsRecord(Cell origin, Cell destination, Set<ModeType> modeTypeGroup) {
    return findRecordWithModes(getRecordsWithoutCells(origin, destination), modeTypeGroup) != null;
  }

  private PathTrialRecord extractRecord(final ResultSet resultSet) throws SQLException {
    return new PathTrialRecord(
        resultSet.getLong("id"),
        resultSet.getDate("created"),
        resultSet.getLong("duration"),
        resultSet.getLong("path_length"),
        resultSet.getInt("origin_x"),
        resultSet.getInt("origin_y"),
        resultSet.getInt("origin_z"),
        resultSet.getInt("destination_x"),
        resultSet.getInt("destination_y"),
        resultSet.getInt("destination_z"),
        Journey.get().domainManager().domainIndex(UUIDUtil.bytesToUuid(resultSet.getBytes("domain_id"))),
        new LinkedList<>(),
        new LinkedList<>()
    );
  }

  private PathTrialCellRecord extractCell(final PathTrialRecord record,
                                          final ResultSet resultSet) throws SQLException {
    return new PathTrialCellRecord(
        record,
        resultSet.getInt("x"),
        resultSet.getInt("y"),
        resultSet.getInt("z"),
        resultSet.getInt("path_index"),
        Objects.requireNonNull(ModeType.get(resultSet.getInt("mode_type")))
    );
  }

}
