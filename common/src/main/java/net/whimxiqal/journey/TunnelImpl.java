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

import java.util.Optional;

public abstract class TunnelImpl implements Tunnel {

  private final Cell exit;
  private final int cost;
  private final Runnable prompt;
  private final String permission;

  public TunnelImpl(Cell exit, int cost, Runnable prompt, String permission) {
    this.exit = exit;
    this.cost = cost;
    this.prompt = prompt;
    this.permission = permission;
  }

  @Override
  public Cell exit() {
    return exit;
  }

  @Override
  public int cost() {
    return cost;
  }

  @Override
  public void prompt() {
    if (prompt == null) {
      Tunnel.super.prompt();
      return;
    }
    prompt.run();
  }

  @Override
  public Optional<String> permission() {
    return Optional.ofNullable(permission);
  }

  @Override
  public String toString() {
    return "TunnelImpl{cost="
        + cost + ", permission="
        + permission + '}';
  }

}
