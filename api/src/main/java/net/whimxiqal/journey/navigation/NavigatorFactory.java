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

import java.util.Map;
import net.whimxiqal.journey.Permissible;
import net.whimxiqal.journey.navigation.option.NavigatorOption;

/**
 * A factory for and source of information about a specific type of {@link Navigator}.
 */
public interface NavigatorFactory extends NavigatorSupplier, Permissible {

  /**
   * Static constructor for a builder of a {@link NavigatorFactory}.
   *
   * @param plugin        the creating plugin
   * @param navigatorType the type of navigator
   * @return the builder
   */
  static NavigatorFactoryBuilder builder(String plugin, String navigatorType) {
    return NavigatorFactoryBuilderFactory.INSTANCE.builder(plugin, navigatorType);
  }

  /**
   * The plugin responsible for this factory and its resultant navigators.
   *
   * @return the plugin
   */
  String plugin();

  /**
   * The type (id) of the navigator.
   *
   * @return the navigator type
   */
  String navigatorType();

  /**
   * The options for this factory's resultant navigators.
   * A user of one of this factory's navigators may use these options
   * and new option values to manipulate the behavior of the navigator.
   *
   * @return the options allowed on this factory's navigators
   */
  Map<String, NavigatorOption<?>> options();

}
