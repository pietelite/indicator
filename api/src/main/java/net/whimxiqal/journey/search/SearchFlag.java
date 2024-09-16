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

/**
 * An optional parameter to change the behavior of a path search.
 *
 * @param <T> the type of value associated with the flag
 */
public class SearchFlag<T> {

  protected final SearchFlag.Type type;
  protected final T value;

  private SearchFlag(Type type, T value) {
    this.type = type;
    this.value = value;
    if (!type.clazz.isInstance(value)) {
      throw new IllegalArgumentException("Flag with type " + type.name()
          + " was given incompatible value type: " + value.getClass().getSimpleName());
    }
  }

  /**
   * A static constructor a {@link SearchFlag}.
   *
   * @param type  the type, to determine which behavior of the search to alter
   * @param value the value, to determine how the search behavior is altered
   * @param <T>   the type of the value
   * @return the flag
   */
  public static <T> SearchFlag<T> of(Type type, T value) {
    return new SearchFlag<>(type, value);
  }

  /**
   * The type of flag, to determine which behavior to alter.
   *
   * @return the type of flag
   */
  public SearchFlag.Type type() {
    return type;
  }

  /**
   * The value of the flag, to determine how the behavior is altered.
   *
   * @return the value of the flag
   */
  public T value() {
    return value;
  }

  /**
   * The types of flags allowed to alter the behavior of a search.
   */
  public enum Type {

    /**
     * How long a search takes before it is stops and fails.
     */
    TIMEOUT(Integer.class),

    /**
     * Whether flight should be considered a possible mode of transportation for players
     * with the ability to fly.
     * This flag does not affect players who don't have the ability to fly anyway.
     */
    FLY(Boolean.class);

    private final Class<?> clazz;

    Type(Class<?> clazz) {
      this.clazz = clazz;
    }
  }
}
