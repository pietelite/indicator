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

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;
import org.jetbrains.annotations.Nullable;

/**
 * A supplier of a {@link Map} which has {@link String} keys.
 * The size of the map may be very large, so the size of the map supplied may be given
 * so the map is only loaded in circumstances where the size is small enough to warrant it.
 *
 * @param <T> the value type stored in the map
 */
public interface VirtualMap<T> {

  /**
   * Static constructor with a supplier of a map.
   *
   * @param supplier the supplier
   * @param size     the size of the map, if it were to be supplied
   * @param <X>      the map value type
   * @return the map supplier
   */
  static <X> VirtualMap<X> of(Supplier<Map<String, ? extends X>> supplier, int size) {
    return VirtualMapFactory.INSTANCE.of(supplier, size);
  }

  /**
   * Static constructor with a loaded map.
   *
   * @param map the map
   * @param <X> the map value type
   * @return the map supplier
   */
  static <X> VirtualMap<X> of(Map<String, ? extends X> map) {
    return VirtualMapFactory.INSTANCE.of(map);
  }

  /**
   * Static constructor for easily supplying a single item.
   *
   * @param id   the id of the single item
   * @param item the item
   * @param <X>  the type of the item
   * @return the map supplier
   */
  static <X> VirtualMap<X> ofSingleton(String id, X item) {
    return VirtualMapFactory.INSTANCE.ofSingleton(id, item);
  }

  /**
   * Static constructor for an empty map.
   *
   * @param <X> the map value type
   * @return the map supplier
   */
  static <X> VirtualMap<X> empty() {
    return VirtualMapFactory.INSTANCE.empty();
  }

  /**
   * Get all entries, where the key is the identifier.
   *
   * @return all entries
   */
  Map<String, ? extends T> getAll();

  /**
   * The size of the map that this object supplies.
   *
   * <p>Sometimes, the size of map of values is too large to load or render to the user.
   * Here, the size may be calculated before the map is actually loaded, if such a function
   * makes sense for the type of data supplied.
   *
   * @return the size of the map that would be supplied
   */
  default int size() {
    return getAll().size();
  }

  /**
   * Get a value by its id. This can be overridden for cases where the entire map need not
   * be supplied in order to search for a value, such as components backed by a database.
   *
   * @param id the identifier
   * @return the value
   */
  @Nullable
  default T get(String id) {
    return getAll().get(id);
  }

}
