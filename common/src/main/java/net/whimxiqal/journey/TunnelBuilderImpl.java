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
public class TunnelBuilderImpl implements TunnelBuilder {

  private final Target entrance;
  private final Cell exit;
  private int cost = 0;
  private Runnable prompt;
  private String permission;

  TunnelBuilderImpl(Target entrance, Cell exit) {
    this.entrance = entrance;
    this.exit = exit;
  }

  @Override
  public TunnelBuilder cost(int cost) {
    this.cost = cost;
    return this;
  }

  @Override
  public TunnelBuilder prompt(Runnable prompt) {
    this.prompt = prompt;
    return this;
  }

  @Override
  public TunnelBuilder permission(String permission) {
    this.permission = permission;
    return this;
  }

  @Override
  public Tunnel build() {
    return new TunnelImpl(entrance, exit, cost, prompt, permission);
  }
}
