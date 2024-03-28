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

package net.whimxiqal.journey.config;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.whimxiqal.journey.Journey;
import net.whimxiqal.journey.config.serializer.ColorSerializer;
import net.whimxiqal.journey.config.serializer.ComponentSerializer;
import net.whimxiqal.journey.config.struct.ConfigFillPhase;
import net.whimxiqal.journey.config.struct.ConfigItemsRule;
import net.whimxiqal.journey.config.struct.ConfigItemType;
import net.whimxiqal.journey.config.struct.ConfigStaticButton;
import net.whimxiqal.journey.Color;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

/**
 * An interface for determining the operation of a configuration manager.
 */
public class ConfigManager {

  private ConfigurationLoader<CommentedConfigurationNode> configurationLoader;
  private ConfigurationLoader<CommentedConfigurationNode> messageConfigurationLoader;

  public void initialize(Path path, Path messagesPath) throws IOException {
    configurationLoader = YamlConfigurationLoader.builder()
        .defaultOptions(options -> options.serializers(builder ->
            builder.register(Color.class, new ColorSerializer())
                .register(Component.class, new ComponentSerializer())
                .register(ConfigItemType.class, new ConfigItemType.Deserializer())
                .register(ConfigStaticButton.class, new ConfigStaticButton.Deserializer())
                .register(ConfigFillPhase.class, new ConfigFillPhase.Deserializer())
                .register(ConfigItemsRule.class, new ConfigItemsRule.Deserializer())
        )).indent(2)
        .nodeStyle(NodeStyle.BLOCK)
        .path(path)
        .build();
    File file = path.toFile();
    if (!file.exists()) {
      URL resourceUrl = getClass().getClassLoader().getResource("config.yml");
      if (resourceUrl == null) {
        throw new RuntimeException("Couldn't get config.yml resource");
      }
      Files.copy(resourceUrl.openConnection().getInputStream(), path);
    }
    load();

    // messages
    messageConfigurationLoader = YamlConfigurationLoader.builder()
        .defaultOptions(options -> options.serializers(builder ->
            builder.register(Component.class, new ComponentSerializer())))
        .indent(2)
        .nodeStyle(NodeStyle.BLOCK)
        .path(messagesPath)
        .build();
    File messagesFile = messagesPath.toFile();
    if (!messagesFile.exists()) {
      URL resourceUrl = getClass().getClassLoader().getResource("messages.yml");
      if (resourceUrl == null) {
        throw new RuntimeException("Couldn't get messages.yml resource");
      }
      Files.copy(resourceUrl.openConnection().getInputStream(), messagesPath);
    }
  }

  /**
   * Load all config values from the config file to memory.
   */
  public void load() throws SerializationException {
    CommentedConfigurationNode root;
    try {
      root = configurationLoader.load();
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
    for (Map.Entry<String, Setting<?>> entry : Settings.ALL_SETTINGS.entrySet()) {
      entry.getValue().load(root);
    }
  }

  public Optional<String> loadMessage(String key) {
    CommentedConfigurationNode root;
    try {
      root = messageConfigurationLoader.load();
    } catch (IOException e) {
      Journey.logger().error("Could not load message " + key + ": " + e.getMessage());
      return Optional.empty();
    }
    List<String> path = Arrays.stream(key.split("\\.")).toList();
    CommentedConfigurationNode node = root.node(path);
    Journey.logger().info("loading node for key " + key + ". node: " + path);
    if (node.virtual()) {
      Journey.logger().info("Node is virtual");
      return Optional.empty();
    }
    Journey.logger().info("Node is not virtual");
    return Optional.ofNullable(node.getString());
  }

}
