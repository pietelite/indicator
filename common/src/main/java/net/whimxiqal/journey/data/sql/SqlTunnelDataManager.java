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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import net.whimxiqal.journey.BoxTargetTunnel;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.CellBox;
import net.whimxiqal.journey.Journey;
import net.whimxiqal.journey.Tunnel;
import net.whimxiqal.journey.data.DataAccessException;
import net.whimxiqal.journey.data.TunnelDataManager;
import net.whimxiqal.journey.data.TunnelType;
import net.whimxiqal.journey.navigation.NetherTunnel;
import net.whimxiqal.journey.util.UUIDUtil;

public class SqlTunnelDataManager extends SqlManager implements TunnelDataManager {

  public static final String NETHER_TUNNEL_TABLE_NAME = "journey_tunnels";
  public static final String ENTRANCE_DOMAIN_ID_COLUMN_NAME = "entrance_domain_id";
  public static final String ENTRANCE_0_X_COLUMN_NAME = "entrance_0_x";
  public static final String ENTRANCE_0_Y_COLUMN_NAME = "entrance_0_y";
  public static final String ENTRANCE_0_Z_COLUMN_NAME = "entrance_0_z";
  public static final String ENTRANCE_1_X_COLUMN_NAME = "entrance_1_x";
  public static final String ENTRANCE_1_Y_COLUMN_NAME = "entrance_1_y";
  public static final String ENTRANCE_1_Z_COLUMN_NAME = "entrance_1_z";
  public static final String EXIT_DOMAIN_ID_COLUMN_NAME = "exit_domain_id";
  public static final String EXIT_X_ID_COLUMN_NAME = "exit_x";
  public static final String EXIT_Y_ID_COLUMN_NAME = "exit_y";
  public static final String EXIT_Z_ID_COLUMN_NAME = "exit_z";
  public static final String TUNNEL_TYPE_COLUMN_NAME = "tunnel_type";

  public SqlTunnelDataManager(SqlConnectionController connectionController) {
    super(connectionController);
  }

  @Override
  public void addTunnel(CellBox entrance, Cell exit, TunnelType type) {
    try (Connection connection = getConnectionController().establishConnection()) {
      PreparedStatement statement = connection.prepareStatement(String.format(
          "INSERT INTO %s (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);",
          NETHER_TUNNEL_TABLE_NAME,
          ENTRANCE_DOMAIN_ID_COLUMN_NAME,
          ENTRANCE_0_X_COLUMN_NAME,
          ENTRANCE_0_Y_COLUMN_NAME,
          ENTRANCE_0_Z_COLUMN_NAME,
          ENTRANCE_1_X_COLUMN_NAME,
          ENTRANCE_1_Y_COLUMN_NAME,
          ENTRANCE_1_Z_COLUMN_NAME,
          EXIT_DOMAIN_ID_COLUMN_NAME,
          EXIT_X_ID_COLUMN_NAME,
          EXIT_Y_ID_COLUMN_NAME,
          EXIT_Z_ID_COLUMN_NAME,
          TUNNEL_TYPE_COLUMN_NAME));

      int parameterIndex = 1;
      statement.setBytes(parameterIndex++, UUIDUtil.uuidToBytes(Journey.get().domainManager().domainId(entrance.domain())));
      statement.setInt(parameterIndex++, entrance.min().blockX());
      statement.setInt(parameterIndex++, entrance.min().blockY());
      statement.setInt(parameterIndex++, entrance.min().blockZ());
      statement.setInt(parameterIndex++, entrance.max().blockX());
      statement.setInt(parameterIndex++, entrance.max().blockY());
      statement.setInt(parameterIndex++, entrance.max().blockZ());
      statement.setBytes(parameterIndex++, UUIDUtil.uuidToBytes(Journey.get().domainManager().domainId(exit.domain())));
      statement.setInt(parameterIndex++, exit.blockX());
      statement.setInt(parameterIndex++, exit.blockY());
      statement.setInt(parameterIndex++, exit.blockZ());
      statement.setInt(parameterIndex++, type.id());

      statement.execute();
    } catch (SQLException e) {
      e.printStackTrace();
      throw new DataAccessException();
    }
  }

  @Override
  public Collection<BoxTargetTunnel> getAllTunnels(TunnelType type) {
    try (Connection connection = getConnectionController().establishConnection()) {
      PreparedStatement statement = connection.prepareStatement(String.format(
          "SELECT * FROM %s WHERE %s = ?;",
          NETHER_TUNNEL_TABLE_NAME,
          TUNNEL_TYPE_COLUMN_NAME));

      statement.setInt(1, type.id());

      return extractTunnels(statement.executeQuery());
    } catch (SQLException e) {
      e.printStackTrace();
      throw new DataAccessException();
    }
  }

  @Override
  public void removeTunnelsWithOrigin(CellBox entrance, TunnelType exit) {
    try (Connection connection = getConnectionController().establishConnection()) {
      PreparedStatement statement = connection.prepareStatement(String.format(
          "DELETE FROM %s WHERE %s = ? AND %s = ? AND %s = ? AND %s = ? AND %s = ? AND %s = ? AND %s = ? AND %s = ?;",
          NETHER_TUNNEL_TABLE_NAME,
          ENTRANCE_DOMAIN_ID_COLUMN_NAME,
          ENTRANCE_0_X_COLUMN_NAME,
          ENTRANCE_0_Y_COLUMN_NAME,
          ENTRANCE_0_Z_COLUMN_NAME,
          ENTRANCE_1_X_COLUMN_NAME,
          ENTRANCE_1_Y_COLUMN_NAME,
          ENTRANCE_1_Z_COLUMN_NAME,
          TUNNEL_TYPE_COLUMN_NAME));

      int parameterIndex = 1;
      statement.setBytes(parameterIndex++, UUIDUtil.uuidToBytes(Journey.get().domainManager().domainId(entrance.domain())));
      statement.setInt(parameterIndex++, entrance.min().blockX());
      statement.setInt(parameterIndex++, entrance.min().blockY());
      statement.setInt(parameterIndex++, entrance.min().blockZ());
      statement.setInt(parameterIndex++, entrance.max().blockX());
      statement.setInt(parameterIndex++, entrance.max().blockY());
      statement.setInt(parameterIndex++, entrance.max().blockZ());
      statement.setInt(parameterIndex++, exit.id());

      statement.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
      throw new DataAccessException();
    }
  }

  private Collection<BoxTargetTunnel> extractTunnels(ResultSet resultSet) throws SQLException {
    List<BoxTargetTunnel> tunnels = new LinkedList<>();
    while (resultSet.next()) {
      TunnelType type = TunnelType.MAP.get(resultSet.getInt(TUNNEL_TYPE_COLUMN_NAME));
      if (type == null) {
        throw new IllegalStateException("A tunnel with an invalid type was found in the database: " + resultSet.getInt(TUNNEL_TYPE_COLUMN_NAME));
      }
      switch (type) {
        case NETHER -> {
          int domain = Journey.get().domainManager().domainIndex(UUIDUtil.bytesToUuid(resultSet.getBytes(ENTRANCE_DOMAIN_ID_COLUMN_NAME)));
          tunnels.add(new NetherTunnel(
              new CellBox(
                  new Cell(resultSet.getInt(ENTRANCE_0_X_COLUMN_NAME),
                      resultSet.getInt(ENTRANCE_0_Y_COLUMN_NAME),
                      resultSet.getInt(ENTRANCE_0_Z_COLUMN_NAME),
                      domain),
                  new Cell(resultSet.getInt(ENTRANCE_1_X_COLUMN_NAME),
                      resultSet.getInt(ENTRANCE_1_Y_COLUMN_NAME),
                      resultSet.getInt(ENTRANCE_1_Z_COLUMN_NAME),
                      domain)),
              new Cell(resultSet.getInt(EXIT_X_ID_COLUMN_NAME),
                  resultSet.getInt(EXIT_Y_ID_COLUMN_NAME),
                  resultSet.getInt(EXIT_Z_ID_COLUMN_NAME),
                  Journey.get().domainManager().domainIndex(UUIDUtil.bytesToUuid(resultSet.getBytes(EXIT_DOMAIN_ID_COLUMN_NAME))))));
        }
        default -> throw new RuntimeException("Unhandled portal type");
      }
    }
    return tunnels;
  }

  @Override
  public void removeTunnels(CellBox entrance, Cell exit, TunnelType type) {
    try (Connection connection = getConnectionController().establishConnection()) {
      PreparedStatement statement = connection.prepareStatement(String.format(
          "DELETE FROM %s WHERE %s = ? AND %s = ? AND %s = ? AND %s = ? AND %s = ? AND %s = ? AND %s = ? AND %s = ? AND %s = ?;",
          NETHER_TUNNEL_TABLE_NAME,
          ENTRANCE_DOMAIN_ID_COLUMN_NAME,
          ENTRANCE_0_X_COLUMN_NAME,
          ENTRANCE_0_Y_COLUMN_NAME,
          ENTRANCE_0_Z_COLUMN_NAME,
          ENTRANCE_1_X_COLUMN_NAME,
          ENTRANCE_1_Y_COLUMN_NAME,
          ENTRANCE_1_Z_COLUMN_NAME,
          EXIT_DOMAIN_ID_COLUMN_NAME,
          EXIT_X_ID_COLUMN_NAME,
          EXIT_Y_ID_COLUMN_NAME,
          EXIT_Z_ID_COLUMN_NAME,
          TUNNEL_TYPE_COLUMN_NAME));

      int parameterIndex = 1;
      statement.setBytes(parameterIndex++, UUIDUtil.uuidToBytes(Journey.get().domainManager().domainId(entrance.domain())));
      statement.setInt(parameterIndex++, entrance.min().blockX());
      statement.setInt(parameterIndex++, entrance.min().blockY());
      statement.setInt(parameterIndex++, entrance.min().blockZ());
      statement.setInt(parameterIndex++, entrance.max().blockX());
      statement.setInt(parameterIndex++, entrance.max().blockY());
      statement.setInt(parameterIndex++, entrance.max().blockZ());
      statement.setBytes(parameterIndex++, UUIDUtil.uuidToBytes(Journey.get().domainManager().domainId(exit.domain())));
      statement.setInt(parameterIndex++, exit.blockX());
      statement.setInt(parameterIndex++, exit.blockY());
      statement.setInt(parameterIndex++, exit.blockZ());
      statement.setInt(parameterIndex++, type.id());

      statement.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
      throw new DataAccessException();
    }
  }

  @Override
  public void removeTunnels(TunnelType type) {
    try (Connection connection = getConnectionController().establishConnection()) {
      PreparedStatement statement = connection.prepareStatement(String.format(
          "DELETE FROM %s WHERE %s = ?;",
          NETHER_TUNNEL_TABLE_NAME,
          TUNNEL_TYPE_COLUMN_NAME));

      statement.setInt(1, type.id());

      statement.executeUpdate();
    } catch (SQLException e) {
      e.printStackTrace();
      throw new DataAccessException();
    }
  }
}
