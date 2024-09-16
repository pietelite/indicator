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

package net.whimxiqal.journey.config.struct;

import java.lang.reflect.Type;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.whimxiqal.journey.config.serializer.TypeDeserializer;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;

public record ConfigItemsRule(Predicate<String> ruleMatcher, List<ConfigItemType> items) {

  public ConfigItemsRule(String regex, List<ConfigItemType> items) {
    this(Pattern.compile(regex).asMatchPredicate(), items);
  }

  public static class Deserializer extends TypeDeserializer<ConfigItemsRule> {

    @Override
    public ConfigItemsRule deserialize(Type type, ConfigurationNode node) throws SerializationException {
      String ruleRegex = node.node("rule").getString();
      if (ruleRegex == null) {
        throw new SerializationException("Invalid rule list, rule regex is null");
      }
      List<ConfigItemType> items = node.node("items").getList(ConfigItemType.class);
      if (items == null || items.isEmpty()) {
        throw new SerializationException("Invalid rule list, there are no items");
      }
      Pattern rule;
      try {
        rule = Pattern.compile(ruleRegex);
      } catch (PatternSyntaxException e) {
        throw new SerializationException(e);
      }
      return new ConfigItemsRule(rule.asMatchPredicate(), items);
    }
  }

}
