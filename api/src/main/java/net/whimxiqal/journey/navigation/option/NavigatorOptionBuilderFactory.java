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

package net.whimxiqal.journey.navigation.option;

import java.util.ServiceLoader;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
interface NavigatorOptionBuilderFactory {

  NavigatorOptionBuilderFactory INSTANCE = ServiceLoader.load(
          NavigatorOptionBuilderFactory.class,
          NavigatorOptionBuilderFactory.class.getClassLoader())
      .findFirst().orElseThrow();

  /**
   * Static constructor for a generic builder of a {@link NavigatorOption}.
   *
   * @param optionId the id the option
   * @param clazz    the class of the type of stored value
   * @param <X>      the type of stored value
   * @return the builder
   */
  <X> NavigatorOptionBuilder<X> builder(String optionId, Class<X> clazz);

  /**
   * Static constructor for an option wrapping a string value with a default parser already specified.
   *
   * @param optionId the id of the option
   * @return the builder
   */
  NavigatorOptionBuilder<String> stringValueBuilder(String optionId);

  /**
   * Static constructor for an option wrapping an integer value with a default parser and validator
   * already specified.
   *
   * @param optionId the id of the option
   * @param min      the minimum allowed value
   * @param max      the maximum allowed value
   * @return the builder
   */
  NavigatorOptionBuilder<Integer> integerValueBuilder(String optionId, int min, int max);

  /**
   * Static constructor for an option wrapping a double value with a default parser and validator
   * already specified.
   *
   * @param optionId the id of the option
   * @param min      the minimum allowed value
   * @param max      the maximum allowed value
   * @return the builder
   */
  NavigatorOptionBuilder<Double> doubleValueBuilder(String optionId, double min, double max);

}
