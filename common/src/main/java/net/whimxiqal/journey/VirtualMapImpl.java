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

import java.util.Map;
import java.util.function.Supplier;

class VirtualMapImpl<T> implements VirtualMap<T> {

  private final Supplier<Map<String, ? extends T>> supplier;
  private final int size;
  Map<String, ? extends T> value;

  VirtualMapImpl(Supplier<Map<String, ? extends T>> supplier, int size) {
    if (size < 0) {
      throw new IllegalArgumentException("A size may not be less than 0");
    }
    this.supplier = supplier;
    this.size = size;
  }

  VirtualMapImpl(Map<String, ? extends T> map) {
    this.supplier = null;
    value = map;
    this.size = value.size();
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public Map<String, ? extends T> getAll() {
    if (value == null) {
      value = supplier.get();
    }
    return value;
  }
}
