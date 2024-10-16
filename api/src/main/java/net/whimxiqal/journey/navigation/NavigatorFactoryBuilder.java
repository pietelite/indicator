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

package net.whimxiqal.journey.navigation;

import net.whimxiqal.journey.Builder;
import net.whimxiqal.journey.navigation.option.NavigatorOption;

/**
 * A builder for a {@link NavigatorFactory}.
 */
public interface NavigatorFactoryBuilder extends Builder<NavigatorFactory> {
  /**
   * Set the permission required to create a navigator.
   *
   * @param permission the permission
   * @return the builder, for chaining
   */
  NavigatorFactoryBuilder permission(String permission);

  /**
   * Add an allowed option for this factory's navigators.
   *
   * @param option the option
   * @param <T>    the type of value for this option
   * @return the builder, for chaining
   */
  <T> NavigatorFactoryBuilder option(NavigatorOption<T> option);

  /**
   * Set the supplier function for new navigators.
   *
   * @param supplier the supplier
   * @return the builder, for chaining
   */
  NavigatorFactoryBuilder supplier(NavigatorSupplier supplier);
}
