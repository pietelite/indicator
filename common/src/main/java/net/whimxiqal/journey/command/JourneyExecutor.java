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

package net.whimxiqal.journey.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.CellTarget;
import net.whimxiqal.journey.InternalJourneyPlayer;
import net.whimxiqal.journey.Journey;
import net.whimxiqal.journey.data.PersonalWaypointManager;
import net.whimxiqal.journey.data.PublicWaypointManager;
import net.whimxiqal.journey.data.TunnelType;
import net.whimxiqal.journey.data.Waypoint;
import net.whimxiqal.journey.manager.SearchManager;
import net.whimxiqal.journey.message.Formatter;
import net.whimxiqal.journey.message.Messages;
import net.whimxiqal.journey.message.Pager;
import net.whimxiqal.journey.navigation.NavigatorDetails;
import net.whimxiqal.journey.scope.ScopeUtil;
import net.whimxiqal.journey.scope.ScopedSessionResult;
import net.whimxiqal.journey.search.DestinationGoalSearchSession;
import net.whimxiqal.journey.search.EverythingSearch;
import net.whimxiqal.journey.search.SearchSession;
import net.whimxiqal.journey.search.flag.FlagSet;
import net.whimxiqal.journey.search.flag.Flags;
import net.whimxiqal.journey.util.CommonLogger;
import net.whimxiqal.journey.util.Permission;
import net.whimxiqal.journey.util.Request;
import net.whimxiqal.journey.util.Validator;
import net.whimxiqal.mantle.common.CommandContext;
import net.whimxiqal.mantle.common.CommandExecutor;
import net.whimxiqal.mantle.common.CommandResult;
import net.whimxiqal.mantle.common.CommandSource;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.configurate.serialize.SerializationException;

public class JourneyExecutor implements CommandExecutor {

  @Override
  public ParseTreeVisitor<CommandResult> provide(CommandContext cmd) {
    CommandSource src = cmd.source();
    return new JourneyBaseVisitor<>() {
      final FlagSet flags = new FlagSet();

      @Override
      protected CommandResult aggregateResult(CommandResult aggregate, CommandResult nextResult) {
        if (aggregate != null) {
          return aggregate;
        }
        return nextResult;
      }

      @Override
      public CommandResult visitJourney(JourneyParser.JourneyContext ctx) {
        CommandResult result = visitChildren(ctx);
        if (result != null) {
          return result;
        }
        src.audience().sendMessage(Formatter.welcome());
        return CommandResult.success();
      }

      @Override
      public CommandResult visitJourneyto(JourneyParser.JourneytoContext ctx) {
        CommandResult result = visitChildren(ctx);
        if (result != null) {
          return result;
        }
        if (notAllowed(Permission.PATH_GUI, true)) {
          return CommandResult.failure();
        }
        boolean sent = Journey.get().proxy().platform().sendGui(InternalJourneyPlayer.from(src));
        if (!sent) {
          Messages.COMMAND_GUI_ERROR.sendTo(src.audience(), Formatter.ERROR);
          return CommandResult.failure();
        }
        return CommandResult.success();
      }

      @Override
      public CommandResult visitJourneytoTarget(JourneyParser.JourneytoTargetContext ctx) {
        CommandResult result = visitChildren(ctx);  // populate flags
        if (result != null && result.type() == CommandResult.Type.FAILURE) {
          return result;
        }
        String scopedText = String.join(":", cmd.identifiers().get(0));

        ScopedSessionResult scopedSessionResult = ScopeUtil.session(InternalJourneyPlayer.from(src), scopedText);
        switch (scopedSessionResult.type()) {
          case EXISTS:
            SearchSession session = scopedSessionResult.session().get();
            session.addFlags(flags);
            Journey.get().searchManager().launchIngameSearch(session);
            return CommandResult.success();
          case AMBIGUOUS:
            Messages.COMMAND_SCOPES_AMBIGUOUS.sendTo(src.audience(), Formatter.ERROR, scopedText,
                scopedSessionResult.ambiguousItem().get().scope1, scopedSessionResult.ambiguousItem().get().scope2);
            return CommandResult.failure();
          case NO_SCOPE:
            Messages.COMMAND_SCOPES_NO_SCOPE.sendTo(src.audience(), Formatter.ERROR,
                scopedSessionResult.missing().get());
          case NONE:
            Messages.COMMAND_SCOPES_NONE.sendTo(src.audience(), Formatter.ERROR, cmd.identifiers().get(0));
            return CommandResult.failure();
          case NO_PERMISSION:
            Messages.COMMAND_NO_PERMISSION_VALUE.sendTo(src.audience(), Formatter.ERROR, cmd.identifiers().get(0));
            return CommandResult.failure();
          default:
            return CommandResult.failure();  // should never happen
        }
      }

      @Override
      public CommandResult visitDebug(JourneyParser.DebugContext ctx) {
        Component message;
        if (Journey.logger().level() == CommonLogger.LogLevel.DEBUG) {
          message = Messages.COMMAND_ADMIN_DEBUG_MODE_DISABLED.resolve(Formatter.SUCCESS);
          Journey.logger().setLevel(CommonLogger.LogLevel.INFO);
        } else {
          message = Messages.COMMAND_ADMIN_DEBUG_MODE_ENABLED.resolve(Formatter.SUCCESS);
          Journey.logger().setLevel(CommonLogger.LogLevel.DEBUG);
        }
        src.audience().sendMessage(message);
        if (src.type() != CommandSource.Type.CONSOLE) {
          // Also send the message to the console, if the source wasn't already the console
          Journey.get().proxy().audienceProvider().console().sendMessage(message);
        }
        return CommandResult.success();
      }

      @Override
      public CommandResult visitCachePortals(JourneyParser.CachePortalsContext ctx) {
        if (ctx.clear != null) {
          Journey.get().netherManager().reset().thenRun(() -> Messages.COMMAND_ADMIN_PORTAL_CACHE_CLEAR.sendTo(src.audience(), Formatter.SUCCESS));
          return CommandResult.success();
        }
        return CommandResult.failure();
      }

      @Override
      public CommandResult visitCachePaths(JourneyParser.CachePathsContext ctx) {
        if (ctx.build != null) {
          Messages.COMMAND_ADMIN_PATH_CACHE_BUILD.sendTo(src.audience(), Formatter.SUCCESS);
          SearchSession.Caller callerType;
          UUID searcherUuid = null;
          switch (src.type()) {
            case PLAYER -> {
              callerType = SearchSession.Caller.PLAYER;
              searcherUuid = src.uuid();
            }
            case CONSOLE -> {
              callerType = SearchSession.Caller.CONSOLE;
              searcherUuid = SearchManager.CONSOLE_UUID;
            }
            default -> callerType = SearchSession.Caller.OTHER;
          }
          Journey.get().searchManager().launchIngameSearch(new EverythingSearch(searcherUuid, callerType));
          return CommandResult.success();
        } else if (ctx.clear != null) {
          Journey.get().proxy().schedulingManager().schedule(() -> {
            Journey.get().proxy().dataManager().pathRecordManager().truncate();
            Messages.COMMAND_ADMIN_PATH_CACHE_CLEAR.sendTo(src.audience(), Formatter.SUCCESS);
          }, true);
          return CommandResult.success();
        }
        return CommandResult.failure();
      }

      @Override
      public CommandResult visitListNetherPortals(JourneyParser.ListNetherPortalsContext ctx) {
        Optional<Integer> page = getPage(ctx.page);
        if (page.isEmpty()) {
          return CommandResult.failure();
        }
        Journey.get().proxy().schedulingManager().schedule(() -> Pager.of(Messages.COMMAND_ADMIN_NETHER_PORTAL_LIST_HEADER.resolve(Formatter.INFO),
                Journey.get().proxy().dataManager()
                    .netherPortalManager()
                    .getAllTunnels(TunnelType.NETHER),
                tunnel -> tunnel.entrance().print(),
                tunnel -> Formatter.cell(tunnel.exit()))
            .sendPage(src.audience(), page.get()), true);
        return CommandResult.success();
      }

      @Override
      public CommandResult visitSetwaypoint(JourneyParser.SetwaypointContext ctx) {
        if (src.type() != CommandSource.Type.PLAYER) {
          Messages.COMMAND_ONLY_PLAYERS.sendTo(src.audience(), Formatter.ERROR);
          return CommandResult.failure();
        }
        visitChildren(ctx);
        String name = cmd.identifiers().get(0);
        if (Validator.isInvalidDataName(name)) {
          Messages.COMMAND_INVALID_INPUT.sendTo(src.audience(), Formatter.ERROR, name);
          return CommandResult.failure();
        }

        Optional<Cell> location = Journey.get().proxy().platform().entityCellLocation(src.uuid());
        if (location.isEmpty()) {
          // This should never happen
          return CommandResult.failure();
        }
        PersonalWaypointManager personalWaypointManager = Journey.get().proxy().dataManager().personalWaypointManager();

        Journey.get().proxy().schedulingManager().schedule(() -> {
          Cell existingWaypoint = personalWaypointManager.getWaypoint(src.uuid(), name);
          if (existingWaypoint != null) {
            Messages.COMMAND_WAYPOINT_PERSONAL_ALREADY_EXISTS.sendTo(src.audience(), Formatter.ERROR, name);
            return;
          }

          personalWaypointManager.add(src.uuid(), location.get(), name);
          Journey.get().cachedDataProvider().personalWaypointCache().update(src.uuid(), true);
          Messages.COMMAND_WAYPOINT_PERSONAL_SET.sendTo(src.audience(), Formatter.SUCCESS, name, Formatter.cell(location.get()));
        }, true);
        return CommandResult.success();
      }

      @Override
      public CommandResult visitListwaypointsMine(JourneyParser.ListwaypointsMineContext ctx) {
        Optional<Integer> page = getPage(ctx.page);
        if (page.isEmpty()) {
          return CommandResult.failure();
        }

        Journey.get().proxy().schedulingManager().schedule(() -> {
          List<Waypoint> waypoints = new ArrayList<>(Journey.get().proxy().dataManager()
              .personalWaypointManager()
              .getAll(src.uuid(), false));

          if (waypoints.isEmpty()) {
            Messages.COMMAND_WAYPOINT_PERSONAL_LIST_EMPTY.sendTo(src.audience(), Formatter.WARN);
            return;
          }

          waypoints.sort(Comparator.naturalOrder());
          Pager.of(Messages.COMMAND_WAYPOINT_PERSONAL_LIST_HEADER.resolve(Formatter.INFO),
                  waypoints,
                  waypoint -> Component.text(waypoint.name()).color(Formatter.GOLD),
                  waypoint -> Formatter.cell(waypoint.location()))
              .sendPage(src.audience(), page.get());
        }, true);

        return CommandResult.success();
      }

      @Override
      public CommandResult visitListwaypointsPlayer(JourneyParser.ListwaypointsPlayerContext ctx) {
        Optional<Integer> page = getPage(ctx.page);
        if (page.isEmpty()) {
          return CommandResult.failure();
        }

        String playerName = cmd.identifiers().get(0);
        Optional<InternalJourneyPlayer> maybePlayer = Journey.get().proxy().platform().onlinePlayer(playerName);
        Journey.get().proxy().schedulingManager().schedule(() -> {
          Request.PlayerResponse player;
          if (maybePlayer.isPresent()) {
            player = new Request.PlayerResponse(maybePlayer.get().uuid(), maybePlayer.get().name());
          } else {
            player = Request.requestPlayerUuid(playerName);
            if (player == null) {
              Messages.COMMAND_PLAYER_REMOTE_CALL_ERROR.sendTo(src.audience(), Formatter.ERROR, playerName);
              return;
            }
          }
          List<Waypoint> waypoints = new ArrayList<>(Journey.get().proxy().dataManager()
              .personalWaypointManager()
              .getAll(player.uuid(), false));

          if (waypoints.isEmpty()) {
            Messages.COMMAND_WAYPOINT_PLAYER_LIST_EMPTY.sendTo(src.audience(), Formatter.WARN, player.name());
            return;
          }

          waypoints.sort(Comparator.naturalOrder());
          Pager.of(Messages.COMMAND_WAYPOINT_PLAYER_LIST_HEADER.resolve(Formatter.INFO, player.name()),
                  waypoints,
                  waypoint -> Component.text(waypoint.name()).color(Formatter.GOLD),
                  waypoint -> Formatter.cell(waypoint.location()))
              .sendPage(src.audience(), page.get());
        }, true);
        return CommandResult.success();
      }

      @Override
      public CommandResult visitWaypoint(JourneyParser.WaypointContext ctx) {
        CommandResult childrenResult = visitChildren(ctx);
        if (childrenResult != null) {
          return childrenResult;
        }
        // No other command specified, so journey to the waypoint
        if (notAllowed(Permission.PATH_PERSONAL, true)) {
          return CommandResult.failure();
        }
        personalWaypointSearch(cmd.identifiers().get(0));
        return CommandResult.success();
      }

      private void personalWaypointSearch(String name) {
        Optional<Cell> location = Journey.get().proxy().platform().entityCellLocation(src.uuid());
        if (location.isEmpty()) {
          // Should never happen
          Messages.COMMAND_SELF_LOCATION_NOT_FOUND.sendTo(src.audience(), Formatter.ERROR);
          return;
        }
        Journey.get().proxy().schedulingManager().schedule(() -> {
          Cell endLocation = Journey.get().proxy().dataManager().personalWaypointManager().getWaypoint(src.uuid(), name);

          if (endLocation == null) {
            Messages.COMMAND_WAYPOINT_PERSONAL_NOT_FOUND.sendTo(src.audience(), Formatter.ERROR, name);
            return;
          }

          // schedule back on main thread
          Journey.get().proxy().schedulingManager().schedule(() -> destinationSearch(location.get(), endLocation), false);
        }, true);
      }

      private void publicWaypointSearch(String name) {
        Optional<Cell> location = Journey.get().proxy().platform().entityCellLocation(src.uuid());
        if (location.isEmpty()) {
          // should never happen
          Messages.COMMAND_SELF_LOCATION_NOT_FOUND.sendTo(src.audience(), Formatter.ERROR);
          return;
        }
        Journey.get().proxy().schedulingManager().schedule(() -> {
          Cell endLocation = Journey.get().proxy().dataManager().publicWaypointManager().getWaypoint(name);

          if (endLocation == null) {
            Messages.COMMAND_WAYPOINT_SERVER_NOT_FOUND.sendTo(src.audience(), Formatter.ERROR, name);
            return;
          }

          // schedule back on main thread
          Journey.get().proxy().schedulingManager().schedule(() -> destinationSearch(location.get(), endLocation), false);
        }, true);
      }

      private void destinationSearch(Cell origin, Cell destination) {
        InternalJourneyPlayer player = InternalJourneyPlayer.from(src);
        DestinationGoalSearchSession session = new DestinationGoalSearchSession(player, origin,
            () -> new CellTarget(destination), false, true);
        session.addFlags(Journey.get().searchManager().getFlagPreferences(player.uuid(), false));
        session.addFlags(flags);

        Journey.get().searchManager().launchIngameSearch(session);
      }

      @Override
      public CommandResult visitUnsetWaypoint(JourneyParser.UnsetWaypointContext ctx) {
        PersonalWaypointManager waypointManager = Journey.get().proxy().dataManager().personalWaypointManager();
        String name = cmd.identifiers().get(0);
        Journey.get().proxy().schedulingManager().schedule(() -> {
          Cell waypoint = waypointManager.getWaypoint(src.uuid(), name);
          if (waypoint != null) {
            waypointManager.remove(src.uuid(), name);
            Journey.get().cachedDataProvider().personalWaypointCache().update(src.uuid(), true);
            Messages.COMMAND_WAYPOINT_PERSONAL_UNSET.sendTo(src.audience(), Formatter.SUCCESS, name, Formatter.cell(waypoint));
          } else {
            Messages.COMMAND_WAYPOINT_PERSONAL_NOT_FOUND.sendTo(src.audience(), Formatter.ERROR, name);
          }
        }, true);
        return CommandResult.success();
      }

      @Override
      public CommandResult visitRenameWaypoint(JourneyParser.RenameWaypointContext ctx) {
        if (src.type() != CommandSource.Type.PLAYER) {
          Messages.COMMAND_ONLY_PLAYERS.sendTo(src.audience(), Formatter.ERROR);
          return CommandResult.failure();
        }
        visitChildren(ctx);
        String name = cmd.identifiers().get(0);
        if (Validator.isInvalidDataName(name)) {
          Messages.COMMAND_INVALID_INPUT.sendTo(src.audience(), Formatter.ERROR, name);
          return CommandResult.failure();
        }

        String newName = cmd.identifiers().get(1);
        if (Validator.isInvalidDataName(newName)) {
          Messages.COMMAND_INVALID_INPUT.sendTo(src.audience(), Formatter.ERROR, newName);
          return CommandResult.failure();
        }

        PersonalWaypointManager personalWaypointManager = Journey.get().proxy().dataManager().personalWaypointManager();
        Journey.get().proxy().schedulingManager().schedule(() -> {
          Cell waypoint = personalWaypointManager.getWaypoint(src.uuid(), newName);
          if (waypoint != null) {
            Messages.COMMAND_WAYPOINT_PERSONAL_ALREADY_EXISTS.sendTo(src.audience(), Formatter.ERROR, newName);
            return;
          }
          waypoint = personalWaypointManager.getWaypoint(src.uuid(), name);
          if (waypoint == null) {
            Messages.COMMAND_WAYPOINT_PERSONAL_NOT_FOUND.sendTo(src.audience(), Formatter.ERROR, name);
            return;
          }

          personalWaypointManager.renameWaypoint(src.uuid(), name, newName);
          Journey.get().cachedDataProvider().personalWaypointCache().update(src.uuid(), true);
          Messages.COMMAND_WAYPOINT_PERSONAL_RENAME.sendTo(src.audience(), Formatter.SUCCESS, name, newName, Formatter.cell(waypoint));
        }, true);
        return CommandResult.success();
      }

      @Override
      public CommandResult visitPlayer(JourneyParser.PlayerContext ctx) {
        CommandResult childrenResult = visitChildren(ctx);
        if (childrenResult != null) {
          return childrenResult;
        }

        // No children commands run, so try go to the player themselves
        if (notAllowed(Permission.PATH_PLAYER_ENTITY, true)) {
          return CommandResult.failure();
        }
        Optional<InternalJourneyPlayer> maybePlayer = Journey.get().proxy().platform().onlinePlayer(ctx.user.getText());
        if (maybePlayer.isEmpty()) {
          Messages.COMMAND_PLAYER_NOT_FOUND.sendTo(src.audience(), Formatter.ERROR, ctx.user.getText());
          return CommandResult.failure();
        }
        if (maybePlayer.get().uuid().equals(src.uuid())) {
          Messages.COMMAND_PLAYER_SELF.sendTo(src.audience(), Formatter.ERROR);
          return CommandResult.failure();
        }
        Optional<Cell> location = Journey.get().proxy().platform().entityCellLocation(src.uuid());
        if (location.isEmpty()) {
          // Should never happen
          return CommandResult.failure();
        }
        Optional<Cell> otherLocation = Journey.get().proxy().platform().entityCellLocation(maybePlayer.get().uuid());
        if (otherLocation.isEmpty()) {
          // Should never happen
          return CommandResult.failure();
        }
        destinationSearch(location.get(), otherLocation.get());
        return CommandResult.success();
      }

      @Override
      public CommandResult visitPlayerWaypoint(JourneyParser.PlayerWaypointContext ctx) {
        // source is player
        visitChildren(ctx);
        String playerName = cmd.identifiers().get(0);
        String waypoint = cmd.identifiers().get(1);
        Optional<InternalJourneyPlayer> maybePlayer = Journey.get().proxy().platform().onlinePlayer(cmd.identifiers().get(0));
        if (maybePlayer.isPresent()) {
          visitPlayerWaypoint(playerName, maybePlayer.get().uuid(), waypoint);
          return CommandResult.success();
        }

        // Async call for the player uuid
        Journey.get().proxy().schedulingManager().schedule(() -> {
          Request.PlayerResponse response = Request.requestPlayerUuid(cmd.identifiers().get(0));
          if (response == null) {
            Messages.COMMAND_PLAYER_REMOTE_CALL_ERROR.sendTo(src.audience(), Formatter.ERROR, playerName);
          } else {
            visitPlayerWaypoint(response.name(), response.uuid(), waypoint);
          }
        }, true);
        return CommandResult.success();
      }

      private void visitPlayerWaypoint(String playerName, UUID dstPlayer, String waypoint) {
        Optional<Cell> location = Journey.get().proxy().platform().entityCellLocation(src.uuid());
        if (location.isEmpty()) {
          // should never happen
          return;
        }
        Journey.get().proxy().schedulingManager().schedule(() -> {
          PersonalWaypointManager manager = Journey.get().proxy().dataManager().personalWaypointManager();
          Cell waypointLocation = manager.getWaypoint(dstPlayer, waypoint);
          if (waypointLocation == null || (!manager.isPublic(dstPlayer, waypoint) && !dstPlayer.equals(src.uuid()))) {
            Messages.COMMAND_WAYPOINT_PLAYER_NOT_FOUND.sendTo(src.audience(), Formatter.ERROR, playerName, waypoint);
            return;
          }

          Cell destination = manager.getWaypoint(dstPlayer, waypoint);
          // schedule back on main thread
          Journey.get().proxy().schedulingManager().schedule(() -> destinationSearch(location.get(), destination), false);
        }, true);
      }

      @Override
      public CommandResult visitServerSetWaypoint(JourneyParser.ServerSetWaypointContext ctx) {
        if (src.type() != CommandSource.Type.PLAYER) {
          Messages.COMMAND_ONLY_PLAYERS.sendTo(src.audience(), Formatter.ERROR);
          return CommandResult.failure();
        }
        visitChildren(ctx);
        String name = cmd.identifiers().get(0);
        if (Validator.isInvalidDataName(name)) {
          Messages.COMMAND_INVALID_INPUT.sendTo(src.audience(), Formatter.ERROR, name);
          return CommandResult.failure();
        }

        Optional<Cell> location = Journey.get().proxy().platform().entityCellLocation(src.uuid());
        if (location.isEmpty()) {
          // should never happen
          return CommandResult.failure();
        }

        Journey.get().proxy().schedulingManager().schedule(() -> {
          PublicWaypointManager publicWaypointManager = Journey.get().proxy().dataManager().publicWaypointManager();
          Cell waypoint = publicWaypointManager.getWaypoint(name);
          if (waypoint != null) {
            Messages.COMMAND_WAYPOINT_SERVER_ALREADY_EXISTS.sendTo(src.audience(), Formatter.ERROR, name);
            return;
          }

          publicWaypointManager.add(location.get(), name);
          Journey.get().cachedDataProvider().publicWaypointCache().update(true);
          Messages.COMMAND_WAYPOINT_SERVER_SET.sendTo(src.audience(), Formatter.SUCCESS, name, Formatter.cell(location.get()));
        }, true);
        return CommandResult.success();
      }

      @Override
      public CommandResult visitServerListWaypoints(JourneyParser.ServerListWaypointsContext ctx) {
        Optional<Integer> page = getPage(ctx.page);
        if (page.isEmpty()) {
          return CommandResult.failure();
        }

        Journey.get().proxy().schedulingManager().schedule(() -> {
          List<Waypoint> waypoints = new ArrayList<>(Journey.get().proxy().dataManager().publicWaypointManager().getAll());

          if (waypoints.isEmpty()) {
            Messages.COMMAND_WAYPOINT_SERVER_LIST_EMPTY.sendTo(src.audience(), Formatter.WARN);
            return;
          }

          waypoints.sort(Comparator.naturalOrder());
          Pager.of(Messages.COMMAND_WAYPOINT_SERVER_LIST_HEADER.resolve(Formatter.INFO),
                  waypoints,
                  waypoint -> Component.text(waypoint.name()).color(Formatter.GOLD),
                  waypoint -> Formatter.cell(waypoint.location()))
              .sendPage(src.audience(), page.get());
        }, true);

        return CommandResult.success();
      }

      @Override
      public CommandResult visitServerWaypoint(JourneyParser.ServerWaypointContext ctx) {
        CommandResult childrenResult = visitChildren(ctx);
        if (childrenResult != null) {
          return childrenResult;
        }
        if (notAllowed(Permission.PATH_SERVER, true)) {
          return CommandResult.failure();
        }
        // No other command specified, so journey to the waypoint
        publicWaypointSearch(cmd.identifiers().get(0));
        return CommandResult.success();
      }

      @Override
      public CommandResult visitServerUnsetWaypoint(JourneyParser.ServerUnsetWaypointContext ctx) {
        PublicWaypointManager waypointManager = Journey.get().proxy().dataManager().publicWaypointManager();
        String name = cmd.identifiers().get(0);
        Cell waypoint = waypointManager.getWaypoint(name);
        if (waypoint != null) {
          waypointManager.remove(name);
          Journey.get().cachedDataProvider().publicWaypointCache().update(true);
          Messages.COMMAND_WAYPOINT_SERVER_UNSET.sendTo(src.audience(), Formatter.SUCCESS, name, Formatter.cell(waypoint));
          return CommandResult.success();
        } else {
          Messages.COMMAND_WAYPOINT_SERVER_NOT_FOUND.sendTo(src.audience(), Formatter.ERROR, name);
          return CommandResult.failure();
        }
      }

      @Override
      public CommandResult visitServerRenameWaypoint(JourneyParser.ServerRenameWaypointContext ctx) {
        visitChildren(ctx);
        String name = cmd.identifiers().get(0);
        if (Validator.isInvalidDataName(name)) {
          Messages.COMMAND_INVALID_INPUT.sendTo(src.audience(), Formatter.ERROR, name);
          return CommandResult.failure();
        }

        String newName = cmd.identifiers().get(1);
        if (Validator.isInvalidDataName(newName)) {
          Messages.COMMAND_INVALID_INPUT.sendTo(src.audience(), Formatter.ERROR, newName);
          return CommandResult.failure();
        }

        PublicWaypointManager publicWaypointManager = Journey.get().proxy().dataManager().publicWaypointManager();
        Journey.get().proxy().schedulingManager().schedule(() -> {
          Cell waypoint = publicWaypointManager.getWaypoint(newName);
          if (waypoint != null) {
            Messages.COMMAND_WAYPOINT_SERVER_ALREADY_EXISTS.sendTo(src.audience(), Formatter.ERROR, newName);
            return;
          }
          waypoint = publicWaypointManager.getWaypoint(name);
          if (waypoint == null) {
            Messages.COMMAND_WAYPOINT_SERVER_NOT_FOUND.sendTo(src.audience(), Formatter.ERROR, name);
            return;
          }

          publicWaypointManager.renameWaypoint(name, newName);
          Journey.get().cachedDataProvider().personalWaypointCache().update(src.uuid(), true);
          Messages.COMMAND_WAYPOINT_SERVER_RENAME.sendTo(src.audience(), Formatter.SUCCESS, name, newName, Formatter.cell(waypoint));
        }, true);
        return CommandResult.success();
      }

      @Override
      public CommandResult visitCancel(JourneyParser.CancelContext ctx) {
        UUID searcherUuid;
        switch (src.type()) {
          case PLAYER -> searcherUuid = src.uuid();
          case CONSOLE -> searcherUuid = SearchManager.CONSOLE_UUID;
          default -> {
            Messages.COMMAND_INTERNAL_ERROR.sendTo(src.audience(), Formatter.ERROR);
            throw new IllegalStateException("Found non-Player/Console source in visitCancel");
          }
        }
        boolean canceled = false;
        SearchSession session = Journey.get().searchManager().getSearch(searcherUuid);
        if (session != null) {
          session.stop(true);
          Messages.COMMAND_SEARCH_CANCEL_START.sendTo(src.audience(), Formatter.INFO);
          canceled = true;
        }
        int navigatorsStopped = Journey.get().navigatorManager().stopNavigators(searcherUuid);
        if (navigatorsStopped > 0) {
          Messages.COMMAND_NAVIGATION_CANCELED.sendTo(src.audience(), Formatter.INFO);
          canceled = true;
        }
        if (!canceled) {
          Messages.COMMAND_SEARCH_NOTHING_TO_CANCEL.sendTo(src.audience(), Formatter.ERROR);
          return CommandResult.failure();
        }
        return CommandResult.success();
      }

      @Override
      public CommandResult visitPublicWaypoint(JourneyParser.PublicWaypointContext ctx) {
        if (src.type() != CommandSource.Type.PLAYER) {
          Messages.COMMAND_ONLY_PLAYERS.sendTo(src.audience(), Formatter.ERROR);
          return CommandResult.failure();
        }

        String name = cmd.identifiers().get(0);

        PersonalWaypointManager personalWaypointManager = Journey.get().proxy().dataManager().personalWaypointManager();
        Journey.get().proxy().schedulingManager().schedule(() -> {
          Cell waypoint = personalWaypointManager.getWaypoint(src.uuid(), name);
          if (waypoint == null) {
            Messages.COMMAND_WAYPOINT_PERSONAL_NOT_FOUND.sendTo(src.audience(), Formatter.ERROR, name);
            return;
          }

          boolean setTrue = ctx.TRUE() != null;
          boolean setFalse = ctx.FALSE() != null;
          boolean isPublic = personalWaypointManager.isPublic(src.uuid(), name);
          // only one of setTrue and setFalse may be true
          if (setTrue) {
            if (isPublic) {
              Messages.COMMAND_WAYPOINT_PERSONAL_ALREADY_PUBLIC.sendTo(src.audience(), Formatter.WARN, name);
              return;
            }
            personalWaypointManager.setPublic(src.uuid(), name, true);
            Journey.get().cachedDataProvider().personalWaypointCache().update(src.uuid(), true);
            Messages.COMMAND_WAYPOINT_PERSONAL_SET_PUBLIC.sendTo(src.audience(), Formatter.SUCCESS, name);
          } else {
            // setFalse
            if (!setFalse) {
              Messages.COMMAND_INTERNAL_ERROR.sendTo(src.audience(), Formatter.ERROR);
              throw new IllegalStateException("Found ctx.TRUE() and ctx.FALSE() both null in visitPublicWaypoint");
            }
            if (!isPublic) {
              Messages.COMMAND_WAYPOINT_PERSONAL_ALREADY_PRIVATE.sendTo(src.audience(), Formatter.WARN, name);
              return;
            }
            personalWaypointManager.setPublic(src.uuid(), name, false);
            Journey.get().cachedDataProvider().personalWaypointCache().update(src.uuid(), true);
            Messages.COMMAND_WAYPOINT_PERSONAL_SET_PRIVATE.sendTo(src.audience(), Formatter.SUCCESS, name);
          }
        }, true);
        return CommandResult.success();
      }

      @Override
      public CommandResult visitAdmin(JourneyParser.AdminContext ctx) {
        if (ctx.reload != null) {
          if (notAllowed(Permission.ADMIN_RELOAD, false)) {
            return CommandResult.failure();
          }
          try {
            Journey.get().configManager().load();
          } catch (SerializationException e) {
            Messages.COMMAND_INTERNAL_ERROR.sendTo(src.audience(), Formatter.ERROR);
            e.printStackTrace();
            return CommandResult.failure();
          }
          Messages.COMMAND_ADMIN_CONFIG_RELOADED.sendTo(src.audience(), Formatter.SUCCESS);
          return CommandResult.success();
        }
        return super.visitAdmin(ctx);
      }

      @Override
      public CommandResult visitTimeoutFlag(JourneyParser.TimeoutFlagContext ctx) {
        int timeout;
        if (ctx.timeout == null) {
          return visitChildren(ctx);
        } else {
          try {
            timeout = Integer.parseUnsignedInt(ctx.timeout.getText());
          } catch (NumberFormatException e) {
            Messages.COMMAND_SEARCH_FLAG_PARSE_ERROR.sendTo(src.audience(), Formatter.ERROR, ctx.timeout.getText(), "-timeout");
            return CommandResult.failure();
          }
        }
        flags.addFlag(Flags.TIMEOUT, timeout);
        return super.visitTimeoutFlag(ctx);
      }

      @Override
      public CommandResult visitAnimateFlag(JourneyParser.AnimateFlagContext ctx) {
        int delay;
        if (ctx.delay == null) {
          delay = 50;  // 50 milliseconds default
        } else {
          try {
            delay = Integer.parseUnsignedInt(ctx.delay.getText());
          } catch (NumberFormatException e) {
            Messages.COMMAND_SEARCH_FLAG_PARSE_ERROR.sendTo(src.audience(), Formatter.ERROR, ctx.delay.getText(), "-animate");
            return CommandResult.failure();
          }
        }
        flags.addFlag(Flags.ANIMATE, delay);
        return super.visitAnimateFlag(ctx);
      }

      @Override
      public CommandResult visitFlyFlag(JourneyParser.FlyFlagContext ctx) {
        if (ctx.TRUE() != null) {
          flags.addFlag(Flags.FLY, true);
        } else if (ctx.FALSE() != null) {
          flags.addFlag(Flags.FLY, false);
        } else {
          flags.addFlag(Flags.FLY, !Flags.FLY.defaultValue());
        }
        return super.visitFlyFlag(ctx);
      }

      @Override
      public CommandResult visitDoorFlag(JourneyParser.DoorFlagContext ctx) {
        if (ctx.TRUE() != null) {
          flags.addFlag(Flags.DOOR, true);
        } else if (ctx.FALSE() != null) {
          flags.addFlag(Flags.DOOR, false);
        } else {
          flags.addFlag(Flags.DOOR, !Flags.DOOR.defaultValue());
        }
        return super.visitDoorFlag(ctx);
      }

      @Override
      public CommandResult visitDigFlag(JourneyParser.DigFlagContext ctx) {
        if (ctx.TRUE() != null) {
          flags.addFlag(Flags.DIG, true);
        } else if (ctx.FALSE() != null) {
          flags.addFlag(Flags.DIG, false);
        } else {
          flags.addFlag(Flags.DIG, !Flags.DIG.defaultValue());
        }
        return super.visitDigFlag(ctx);
      }

      @Override
      public CommandResult visitNavigatorFlag(JourneyParser.NavigatorFlagContext ctx) {
        NavigatorDetails navigatorDetails = Journey.get().navigatorManager().parseNavigatorFlagDefinition(src,
            cmd.identifiers().get("navigator", 0),
            ctx.options == null ? null : cmd.identifiers().get("navigator-options", 0));
        if (navigatorDetails == null) {
          return CommandResult.failure();
        }
        flags.addFlag(Flags.NAVIGATOR, navigatorDetails);
        return super.visitNavigatorFlag(ctx);
      }

      private boolean notAllowed(@Nullable Permission permission, boolean onlyPlayer) {
        if (onlyPlayer && src.type() != CommandSource.Type.PLAYER) {
          Messages.COMMAND_ONLY_PLAYERS.sendTo(src.audience(), Formatter.ERROR);
          return true;
        }
        if (permission != null && !src.hasPermission(permission.path())) {
          Messages.COMMAND_NO_PERMISSION.sendTo(src.audience(), Formatter.ERROR);
          return true;
        }
        return false;
      }

      private Optional<Integer> getPage(Token pageToken) {
        if (pageToken == null) {
          return Optional.of(1);
        } else {
          try {
            return Optional.of(Integer.parseUnsignedInt(pageToken.getText()));
          } catch (NumberFormatException e) {
            Messages.COMMAND_PAGING_INVALID_PAGE.sendTo(src.audience(), Formatter.ERROR, pageToken.getText());
            return Optional.empty();
          }
        }
      }
    };
  }
}
