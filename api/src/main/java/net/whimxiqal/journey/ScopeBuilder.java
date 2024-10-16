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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;

/**
 * A {@link Builder} for a {@link Scope}.
 */
public class ScopeBuilder implements Builder<Scope> {

  private Component name = Component.empty();
  private List<Component> description = Collections.emptyList();
  private Function<JourneyPlayer, VirtualMap<Scope>> subScopes;
  private Function<JourneyPlayer, VirtualMap<Destination>> destinations;
  private String permission = null;
  private boolean strict = false;

  ScopeBuilder() {
  }

  /**
   * Set the name.
   *
   * @param name the name
   * @return the builder, for chaining
   */
  public ScopeBuilder name(Component name) {
    this.name = name;
    return this;
  }

  /**
   * Set the description.
   *
   * @param description the description
   * @return the builder, for chaining
   */
  public ScopeBuilder description(Component... description) {
    this.description = Arrays.asList(description);
    return this;
  }

  /**
   * Set the supplier of sub-scopes, where the sub-scopes are based on a player.
   *
   * @param subScopes the sub-scopes
   * @return the builder, for chaining
   */
  public ScopeBuilder subScopes(Function<JourneyPlayer, VirtualMap<Scope>> subScopes) {
    this.subScopes = subScopes;
    return this;
  }

  /**
   * Set the supplier of sub-scopes, where the sub-scopes are not based on a player.
   *
   * @param subScopes the sub-scopes
   * @return the builder, for chaining
   */
  public ScopeBuilder subScopes(Supplier<VirtualMap<Scope>> subScopes) {
    this.subScopes = unusedPlayer -> subScopes.get();
    return this;
  }

  /**
   * Set the supplier of sub-scopes, where the sub-scopes are already computed.
   *
   * @param subScopes the sub-scopes
   * @return the builder, for chaining
   */
  public ScopeBuilder subScopes(VirtualMap<Scope> subScopes) {
    this.subScopes = unusedPlayer -> subScopes;
    return this;
  }

  /**
   * Set the supplier of destinations, where the destinations are based on a player.
   *
   * @param destinations the destinations
   * @return the builder, for chaining
   */
  public ScopeBuilder destinations(Function<JourneyPlayer, VirtualMap<Destination>> destinations) {
    this.destinations = destinations;
    return this;
  }

  /**
   * Set the supplier of destinations, where the destinations are not based on a player.
   *
   * @param destinations the destinations
   * @return the builder, for chaining
   */
  public ScopeBuilder destinations(Supplier<VirtualMap<Destination>> destinations) {
    this.destinations = unusedPlayer -> destinations.get();
    return this;
  }

  /**
   * Set the supplier of destinations, where the destinations are already computed.
   *
   * @param destinations the destinations
   * @return the builder, for chaining
   */
  public ScopeBuilder destinations(VirtualMap<Destination> destinations) {
    this.destinations = unusedPlayer -> destinations;
    return this;
  }

  /**
   * Set the permission.
   * A user will need both the given permission and also the permission "journey.path.your-permission"
   * in order to see and use the resultant destination.
   * If the permission itself starts with "journey.path", then only the given permission is required.
   *
   * @param permission the permission
   * @return the builder, for chaining
   */
  public ScopeBuilder permission(String permission) {
    this.permission = permission;
    return this;
  }

  /**
   * Set whether this scope is strict or not.
   * A strict scope will require that a user specify any targets within this scope
   * explicitly by naming the previous scope ids.
   * Otherwise, targets within this scope may be specified even after omitting some scope ids.
   *
   * @return the builder
   */
  public ScopeBuilder strict() {
    this.strict = true;
    return this;
  }

  @Override
  public Scope build() {
    return new ScopeImpl(name, description, subScopes, destinations, permission, strict);
  }
}
