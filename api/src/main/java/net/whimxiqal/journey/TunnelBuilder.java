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
 * A builder for a {@link Tunnel}.
 */
public interface TunnelBuilder extends Builder<Tunnel> {

  /**
   * Set the cost of traversing this Tunnel, where walking 1 block costs 1.
   * This is just used for optimizing the lowest cost routes, so estimate
   * the cost as closely to the effort of traversing this tunnel as possible.
   *
   * @param cost the cost
   * @return the builder, for chaining
   */
  TunnelBuilder cost(int cost);

  /**
   * Set the prompt that gets run once this tunnel is next in the path traversal.
   * This is often where a message gets sent to the user to do something specific,
   * like typing a command
   *
   * @param prompt the prompt
   * @return the builder, for chaining
   */
  TunnelBuilder prompt(Runnable prompt);

  /**
   * Set the permission for this tunnel. This should just be the permission
   * that physically gives a player the ability to traverse the tunnel.
   *
   * @param permission the permission
   * @return the builder, for chaining
   */
  TunnelBuilder permission(String permission);
}
