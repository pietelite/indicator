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
 * A connection from one location to another, indicating the ability for a player to travel through
 * unnatural methods like by typing commands to teleport.
 */
public interface Tunnel extends Permissible {

  /**
   * A standard tunnel builder.
   *
   * @param entrance the entrance
   * @param exit     the exit
   * @return the builder
   */
  static TunnelBuilder builder(Cell entrance, Cell exit) {
    return TunnelBuilderFactory.INSTANCE.builder(entrance, exit);
  }

  /**
   * A builder that has a cubiod region for an entrance.
   *
   * @param entrance1 point 1 of the cubiod entrance
   * @param entrance2 point 2 of the cubiod entrance
   * @param exit      the exit
   * @return the builder
   */
  static TunnelBuilder boxEntranceBuilder(Cell entrance1, Cell entrance2, Cell exit) {
    return TunnelBuilderFactory.INSTANCE.boxEntranceBuilder(entrance1, entrance2, exit);
  }

  /**
   * The starting location, or "entrance" to the tunnel.
   *
   * @return the entrance
   */
  Target entrance();

  /**
   * The final location, or "exit" to the tunnel.
   *
   * @return the exit
   */
  Cell exit();

  /**
   * The cost of traveling along the tunnel. A cost of 1 indicates the cost to walk a single block.
   * This may not be negative.
   *
   * @return the cost
   */
  int cost();

  /**
   * The prompt to signal that the tunnel should be traversed.
   * This is useful when the player needs guidance on how to enter the tunnel,
   * like which command they need to type.
   */
  default void prompt() {
    // nothing
  }

}
