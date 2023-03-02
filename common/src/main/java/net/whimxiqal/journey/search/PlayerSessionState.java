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

/**
 * An object to help hold the current state of a running
 * {@link SearchSession}.
 */
public class PlayerSessionState {

  /**
   * True if the algorithm has found a path.
   */
  private boolean solved = false;
  private int successNotificationTaskId = 0;
  /**
   * True if the player has been presented with a solution (itinerary -> journey).
   */
  private boolean solutionPresented = false;
  private final AnimationManager animationManager;
  public AnimationManager animationManager() {
    return animationManager;
  }

  public PlayerSessionState(UUID playerUuid) {
    this.animationManager = new AnimationManager(playerUuid);
  }

  /**
   * Set whether the search has been solved already or not.
   *
   * @param solved true if it's solved
   */
  public void setSolved(boolean solved) {
    this.solved = solved;
  }

  /**
   * Get whether the session was already solved.
   *
   * @return true if solved
   */
  public boolean wasSolved() {
    return solved;
  }

  /**
   * Get the success notification task id for the purpose
   * of possibly using it to canceling the task.
   *
   * @return the task id
   */
  public int getSuccessNotificationTaskId() {
    return successNotificationTaskId;
  }

  /**
   * Set the success notification task id for the purpose
   * of possibly canceling the task at a later time.
   *
   * @param successNotificationTaskId the id
   */
  public void setSuccessNotificationTaskId(int successNotificationTaskId) {
    this.successNotificationTaskId = successNotificationTaskId;
  }

  /**
   * Set whether a solution has been presented to the player already.
   *
   * @param solutionPresented true if presented, false if not yet presented
   */
  public void setSolutionPresented(boolean solutionPresented) {
    this.solutionPresented = solutionPresented;
  }

  /**
   * Get whether a solution has been presented to the player already.
   *
   * @return true if presented, false if not yet presented
   */
  public boolean wasSolutionPresented() {
    return solutionPresented;
  }

}
