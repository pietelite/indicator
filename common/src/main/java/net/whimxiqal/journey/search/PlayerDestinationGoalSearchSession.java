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

package net.whimxiqal.journey.search;

import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.Journey;
import net.whimxiqal.journey.search.flag.FlagSet;
import net.whimxiqal.journey.search.flag.Flags;

/**
 * A search session designed to be used for players finding their way to a specific destination.
 */
public class PlayerDestinationGoalSearchSession extends DestinationGoalSearchSession implements PlayerSessionStateful {

  private final PlayerSessionState sessionState;

  public PlayerDestinationGoalSearchSession(UUID player, Cell origin, Cell destination, boolean persistentDestination) {
    super(player, Caller.PLAYER, origin, destination, false, persistentDestination);
    sessionState = new PlayerSessionState(player);
  }

  public PlayerSessionState sessionState() {
    return sessionState;
  }

  @Override
  public Audience audience() {
    return Journey.get().proxy().audienceProvider().player(getCallerId());
  }

  @Override
  public void initialize() {
    int stepDelay = flags.getValueFor(Flags.ANIMATE);
    if (stepDelay > 0) {
      sessionState.animationManager().setAnimating(true);
      setAlgorithmStepDelay(stepDelay);
    } else {
      sessionState.animationManager().setAnimating(false);
    }
    Journey.get().proxy().platform().prepareSearchSession(this, getCallerId(), flags, true);
    Journey.get().proxy().platform().prepareDestinationSearchSession(this, getCallerId(), flags, destination);
    Journey.get().netherManager().makeTunnels().forEach(this::registerTunnel);
    Journey.get().proxy().platform().onlinePlayer(getCallerId()).ifPresent(jPlayer ->
        Journey.get().tunnelManager().tunnels(jPlayer).forEach(this::registerTunnel));
  }
}
