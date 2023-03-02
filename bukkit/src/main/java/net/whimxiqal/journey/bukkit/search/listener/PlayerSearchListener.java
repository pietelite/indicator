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

package net.whimxiqal.journey.bukkit.search.listener;

import java.util.UUID;
import net.whimxiqal.journey.Journey;
import net.whimxiqal.journey.message.Formatter;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.navigation.Itinerary;
import net.whimxiqal.journey.navigation.journey.JourneySession;
import net.whimxiqal.journey.navigation.journey.PlayerJourneySession;
import net.whimxiqal.journey.search.PlayerDestinationGoalSearchSession;
import net.whimxiqal.journey.search.PlayerSessionState;
import net.whimxiqal.journey.search.PlayerSessionStateful;
import net.whimxiqal.journey.search.SearchSession;
import net.whimxiqal.journey.util.TimeUtil;
import net.whimxiqal.journey.bukkit.JourneyBukkit;
import net.whimxiqal.journey.bukkit.search.event.BukkitFoundSolutionEvent;
import net.whimxiqal.journey.bukkit.search.event.BukkitIgnoreCacheSearchEvent;
import net.whimxiqal.journey.bukkit.search.event.BukkitStartItinerarySearchEvent;
import net.whimxiqal.journey.bukkit.search.event.BukkitStopItinerarySearchEvent;
import net.whimxiqal.journey.bukkit.search.event.BukkitStopPathSearchEvent;
import net.whimxiqal.journey.bukkit.search.event.BukkitStartPathSearchEvent;
import net.whimxiqal.journey.bukkit.search.event.BukkitStopSearchEvent;
import net.whimxiqal.journey.bukkit.util.BukkitUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * A listener with a series of event handlers to perform general management operations
 * of running {@link PlayerDestinationGoalSearchSession}s.
 */
public class PlayerSearchListener implements Listener {

  private static final double STEVE_RUNNING_SPEED = 5.621;  // blocks per second
  public static final long VISITATION_TIMEOUT_MS = 10;  // Any visits with 10 ms

  private long lastVisitTime = 0;

  /**
   * Handle the event fired when a new solution is found.
   * Send the found {@link Itinerary} to the running {@link JourneySession}
   * as a prospective itinerary in case the player wants to use it.
   *
   * @param event the event
   */
  @EventHandler
  public void foundSolutionEvent(BukkitFoundSolutionEvent event) {
    SearchSession session = event.getSearchEvent().getSession();
    if (!(session instanceof PlayerSessionStateful playerSession)) {
      return;
    }
    // Need to update the session state
    PlayerSessionState playerSessionState = playerSession.sessionState();

    Player player = Bukkit.getPlayer(session.getCallerId());
    if (player == null) {
      return;
    }

    Journey.get().debugManager().broadcast(Formatter.debug("Found a solution to a search for player ___", player.getName()));

    Itinerary itinerary = event.getSearchEvent().getItinerary();
    if (playerSessionState.wasSolutionPresented()) {
      // TODO ignore for now, but perhaps send to the user to potentially "accept" a better path
      return;
    }

    if (playerSessionState.wasSolved()) {
      Bukkit.getScheduler().cancelTask(playerSessionState.getSuccessNotificationTaskId());
    }
    playerSessionState.setSolved(true);

    // Set up a success notification that will be cancelled if a better one is found in some amount of time
    playerSessionState.setSuccessNotificationTaskId(Bukkit.getScheduler()
        .runTaskLater(JourneyBukkit.get(),
            () -> {
              Journey.get().proxy().audienceProvider().player(player.getUniqueId()).sendMessage(Formatter.prefix()
                  .append(Component.text("Success! Please follow the path.")
                      .color(Formatter.SUCCESS)
                      .hoverEvent(HoverEvent.hoverEvent(HoverEvent.Action.SHOW_TEXT,
                          Component.text("Search Statistics")
                              .color(Formatter.THEME)
                              .decorate(TextDecoration.BOLD)
                              .append(Component.newline())
                              .append(Component.text("Walk Time: ")
                                  .color(Formatter.DULL)
                                  .append(Component.text(TimeUtil.toSimpleTime(Math.round(event.getSearchEvent().getItinerary().cost() / STEVE_RUNNING_SPEED)))
                                      .color(Formatter.ACCENT)))
                              .append(Component.newline())
                              .append(Component.text("Distance: ")
                                  .color(Formatter.DULL)
                                  .append(Component.text(Math.round(event.getSearchEvent().getItinerary().cost()) + " blocks")
                                      .color(Formatter.ACCENT)))
                              .append(Component.newline())
                              .append(Component.text("Search Time: ")
                                  .color(Formatter.DULL)
                                  .append(Component.text(TimeUtil.toSimpleTime(
                                          Math.round((double) event.getSearchEvent().getExecutionTime() / 1000)))
                                      .color(Formatter.ACCENT)))))));

              // Create a journey that is completed when the player reaches within 3 blocks of the endpoint
              PlayerJourneySession journey = new PlayerJourneySession(player.getUniqueId(), session, itinerary);
              journey.run();

              // Save the journey
              Journey.get().searchManager().putJourney(player.getUniqueId(), journey);

              playerSessionState.setSolutionPresented(true);
            },
            20 /* one second (20 ticks) */)
        .getTaskId());
  }

  /**
   * Handle the stop search event.
   * We need to remove the instance from memory storage and stop any running tasks.
   *
   * @param event the event
   */
  @EventHandler
  public void stopSearchEvent(BukkitStopSearchEvent event) {
    SearchSession session = event.getSearchEvent().getSession();
    // Send failure message if we finished unsuccessfully
    Player player = Bukkit.getPlayer(session.getCallerId());
    if (player == null) {
      return;
    }

    Journey.get().debugManager().broadcast(Formatter.debug("Stopping a search for player ___", player.getName()));
    Journey.get().debugManager().broadcast(Formatter.debug("Status: ___", session.getState()));

    switch (session.getState()) {
      case STOPPED_FAILED:
        Journey.get().proxy().audienceProvider().player(player.getUniqueId()).sendMessage(
            Formatter.error("Search failed!"));
        break;
      case STOPPED_ERROR:
        Journey.get().proxy().audienceProvider().player(player.getUniqueId()).sendMessage(
            Formatter.error("An internal error ocurred! Please notify an administrator"));
        break;
      case STOPPED_CANCELED:
        Journey.get().proxy().audienceProvider().player(player.getUniqueId()).sendMessage(
            Formatter.info("Search canceled."));
        break;
      case STOPPED_SUCCESSFUL:
        /* Don't say anything. They were already notified of the successful solutions. */
        break;
      default:
        Bukkit.getLogger().warning("A player search session stopped while in the "
            + session.getState() + " state");
    }
  }

  /**
   * Handle the event when an itinerary search begins for those that were caused by a player.
   *
   * @param event the event
   */
  @EventHandler
  public void startItinerarySearchEvent(BukkitStartItinerarySearchEvent event) {
    SearchSession session = event.getSearchEvent().getSession();
    Player player = Bukkit.getPlayer(session.getCallerId());
    if (player != null) {
      Journey.get().debugManager().broadcast(Formatter.debug("Started an itinerary search for player ___", player.getName()));
    }
  }

  /**
   * Handle the event when an itinerary search ends for those that were caused by a player.
   *
   * @param event the event
   */
  @EventHandler
  public void stopItinerarySearchEvent(BukkitStopItinerarySearchEvent event) {
    SearchSession session = event.getSearchEvent().getSession();
    Player player = Bukkit.getPlayer(session.getCallerId());
    if (player != null) {
      Journey.get().debugManager().broadcast(Formatter.debug("Stopped an itinerary search for player ___", player.getName()));
      Journey.get().debugManager().broadcast(Formatter.debug("Status: ___", event.getSearchEvent().getItineraryTrial().getState()));
    }
  }

  /**
   * Handle the event when a path search begins for those that were caused by a player.
   *
   * @param event the event
   */
  @EventHandler
  public void startPathSearchEvent(BukkitStartPathSearchEvent event) {
    SearchSession session = event.getSearchEvent().getSession();
    Player player = Bukkit.getPlayer(session.getCallerId());
    if (player != null) {
      Journey.get().debugManager().broadcast(Formatter.debug("Started a path search for player ___", player.getName()));
    }
  }

  /**
   * Handle the event when a path search ends for those that were caused by a player.
   *
   * @param event the event
   */
  @EventHandler
  public void stopPathSearchEvent(BukkitStopPathSearchEvent event) {
    SearchSession session = event.getSearchEvent().getSession();
    Player player = Bukkit.getPlayer(session.getCallerId());
    if (player != null) {
      Journey.get().debugManager().broadcast(Formatter.debug("Stopped a path search for player ___", player.getName()));
      Journey.get().debugManager().broadcast(Formatter.debug("Status: ___", event.getSearchEvent().getPathTrial().getState()));
    }
  }

  /**
   * Handle the event when a search stops considering cached paths for those that were caused by a player.
   *
   * @param event the event
   */
  @EventHandler
  public void ignoreCacheSearchEvent(BukkitIgnoreCacheSearchEvent event) {
    SearchSession session = event.getSearchEvent().getSession();
    Player player = Bukkit.getPlayer(session.getCallerId());
    if (player != null) {
      if (!session.getState().isSuccessful()) {
        Journey.get().proxy().audienceProvider().player(player.getUniqueId()).sendMessage(Formatter.warn("There probably isn't a solution, "
            + "but the search will continue, just in case!"));
      }
      Journey.get().debugManager().broadcast(Formatter.debug("Ignoring cache in search for ___", player.getName()));
      Journey.get().debugManager().broadcast(Formatter.debug("Status: ___", event.getSearchEvent().getSession().getState()));
    }
  }


  /**
   * Handler for when players move throughout the world.
   * This allows us to update the last known location so player journeys
   * know which particles to show.
   *
   * @param event the event
   */
  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    long now = System.currentTimeMillis();
    if (now < lastVisitTime + VISITATION_TIMEOUT_MS) {
      // ignore movements if they're too frequent
      return;
    }
    lastVisitTime = now;
    Cell cell = BukkitUtil.cell(event.getTo());
    UUID playerUuid = event.getPlayer().getUniqueId();
    Journey.get().searchManager().registerLocation(playerUuid, cell);
  }

  /**
   * Handle the player quit event.
   *
   * @param event the event
   */
  @EventHandler
  public void onQuit(PlayerQuitEvent event) {
    // Perform quit logic. Currently, nothing.
  }

}
