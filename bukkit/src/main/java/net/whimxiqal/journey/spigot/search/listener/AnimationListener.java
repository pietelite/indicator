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

package net.whimxiqal.journey.spigot.search.listener;

import net.whimxiqal.journey.common.search.AnimationManager;
import net.whimxiqal.journey.common.search.PlayerSessionStateful;
import net.whimxiqal.journey.common.search.event.StepSearchEvent;
import net.whimxiqal.journey.common.search.event.StopPathSearchEvent;
import net.whimxiqal.journey.common.search.event.StopSearchEvent;
import net.whimxiqal.journey.spigot.search.event.SpigotModeFailureEvent;
import net.whimxiqal.journey.spigot.search.event.SpigotModeSuccessEvent;
import net.whimxiqal.journey.spigot.search.event.SpigotSearchEvent;
import net.whimxiqal.journey.spigot.search.event.SpigotStepSearchEvent;
import net.whimxiqal.journey.spigot.search.event.SpigotStopPathSearchEvent;
import net.whimxiqal.journey.spigot.search.event.SpigotStopSearchEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class AnimationListener implements Listener {

  /**
   * A handler animating the case where the algorithm finds that a certain block is accessible
   * during algorithm execution.
   *
   * @param event the event
   */
  @EventHandler
  public void successModeEvent(SpigotModeSuccessEvent event) {
    AnimationManager manager = getAnimationManager(event);
    if (manager != null) {
      manager.showResult(event.getSearchEvent().getCell(), true, event.getSearchEvent().getModeType());
    }
  }

  /**
   * A handler animating the case where the algorithm finds that a certain block is inaccessible
   * during algorithm execution.
   *
   * @param event the event
   */
  @EventHandler
  public void failModeEvent(SpigotModeFailureEvent event) {
    AnimationManager manager = getAnimationManager(event);
    if (manager != null) {
      manager.showResult(event.getSearchEvent().getCell(), false, event.getSearchEvent().getModeType());
    }
  }

  /**
   * Handle the {@link StepSearchEvent}
   * by showing blocks to the player that called for the search.
   * This is part of the algorithm-animation process.
   *
   * @param event the event
   */
  @EventHandler
  public void stepSearchEvent(SpigotStepSearchEvent event) {
    AnimationManager manager = getAnimationManager(event);
    if (manager != null) {
      manager.showStep(event.getSearchEvent().getStep().location());
    }
  }

  /**
   * Handle the {@link StopPathSearchEvent}
   * by cleaning up the pieces of the animation for that specific path and calling
   * {@link AnimationManager#undoAnimation()}.
   *
   * @param event the event
   */
  @EventHandler
  public void stopPathSearchEvent(SpigotStopPathSearchEvent event) {
    AnimationManager manager = getAnimationManager(event);
    if (manager != null) {
      manager.undoAnimation();
    }
  }

  /**
   * Handle the {@link StopSearchEvent}
   * by cleaning up whatever is needed to be cleaned up by the running of the
   * animation.
   *
   * <p>{@link #stopPathSearchEvent} is the one that normally cleans up the animations
   * but if the animation is cancelled before it finishes, then this is the last resort
   * to fix the user's environment.
   *
   * @param event the event
   */
  @EventHandler
  public void stopSearchEvent(SpigotStopSearchEvent event) {
    AnimationManager manager = getAnimationManager(event);
    if (manager != null) {
      manager.undoAnimation();
    }
  }

  private AnimationManager getAnimationManager(SpigotSearchEvent<?> event) {
    if (event.getSearchEvent().getSession() instanceof PlayerSessionStateful) {
      AnimationManager animationManager = ((PlayerSessionStateful) event.getSearchEvent().getSession()).sessionState().animationManager();
      if (animationManager.isAnimating()) {
        return animationManager;
      }
    }
    return null;
  }

}
