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

import java.util.List;
import net.kyori.adventure.text.Component;

/**
 * A builder for a {@link Destination}.
 */
public interface DestinationBuilder extends Builder<Destination> {

  /**
   * Set the name.
   *
   * @param name the name
   * @return the builder, for chaining
   */
  DestinationBuilder name(Component name);

  /**
   * Set the description.
   *
   * @param description the description
   * @return the builder, for chaining
   */
  DestinationBuilder description(Component... description);

  /**
   * Set the description.
   *
   * @param description the description
   * @return the builder, for chaining
   */
  DestinationBuilder description(List<Component> description);

  /**
   * Set the permission.
   * A user will need both the given permission and also the permission "journey.path.your-permission"
   * in order to see and use the resultant destination.
   * If the permission itself starts with "journey.path", then only the given permission is required.
   *
   * @param permission the permission
   * @return the builder, for chaining
   */
  DestinationBuilder permission(String permission);

}
