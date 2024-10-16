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

package net.whimxiqal.journey.integration.notquests;

import java.util.List;
import java.util.logging.Logger;
import net.whimxiqal.journey.JourneyApi;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.shadow.cloud.arguments.CommandArgument;
import rocks.gravili.notquests.paper.shadow.cloud.arguments.parser.ArgumentParseResult;
import rocks.gravili.notquests.paper.structs.objectives.Objective;
import rocks.gravili.notquests.paper.structs.objectives.ObjectiveHolder;

public final class JourneyNotQuests extends JavaPlugin {

  private static Logger logger;

  public static Logger logger() {
    return logger;
  }

  public static NotQuests notQuests() {
    NotQuests plugin = NotQuests.getInstance();
    if (plugin == null) {
      throw new RuntimeException("NotQuests class could not be found");
    }
    return plugin;
  }

  @Override
  public void onEnable() {
    JourneyNotQuests.logger = getLogger();

    JourneyApi.get().registerScope(getName(), "notquests", new NotQuestsScope());
    Bukkit.getPluginManager().registerEvents(new StartQuestListener(), this);

    // edit command structure to include a "useJourney" argument for objectives
    notQuests().getCommandManager()
        .getPaperCommandManager()
        .command(notQuests().getCommandManager()
            .getAdminEditObjectivesBuilder()
            .literal("useJourney")
            .argument(CommandArgument.<CommandSender, Boolean>ofType(Boolean.class, "value")
                .asOptionalWithDefault("true")
                .withSuggestionsProvider((context, s) -> List.of("true", "false"))
                .withParser((commandContext, inputQueue) -> {
                  if (inputQueue.isEmpty()) {
                    // no input counts as true
                    return ArgumentParseResult.success(true);
                  }
                  String value = inputQueue.remove();
                  if (value.equalsIgnoreCase("true")) {
                    return ArgumentParseResult.success(true);
                  } else if (value.equalsIgnoreCase("false")) {
                    return ArgumentParseResult.success(false);
                  } else {
                    return ArgumentParseResult.failure(new IllegalArgumentException("Invalid expression: " + value + ". Expected 'true' or 'false'"));
                  }
                })
                .build())
            .handler(context -> {
              NotQuests notQuests = notQuests();
              ObjectiveHolder objectiveHolder = notQuests.getCommandManager().getObjectiveHolderFromContextAndLevel(context, 0);
              Objective objective = notQuests.getCommandManager().getObjectiveFromContextAndLevel(context, 0);
              String path = objectiveHolder.getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".useJourney";
              final boolean useJourney = context.get("value");
              boolean existing = objective.getConfig().getBoolean(path, false);
              if (useJourney == existing) {
                context.getSender().sendMessage(Color.RED + "Objective " + objective.getObjectiveID() + " already has useJourney set to " + existing);
                return;
              }
              if (useJourney) {
                objectiveHolder.getConfig().set(path, true);
                boolean hasLocation = objectiveHolder.getConfig().isConfigurationSection(objectiveHolder.getInitialConfigPath() + ".objectives." + objective.getObjectiveID() + ".location");
                if (hasLocation) {
                  context.getSender().sendMessage(Color.GREEN + "Journey will now help navigate players to this objective, " + Color.YELLOW + " but only after you set a location for it");
                } else {
                  context.getSender().sendMessage(Color.GREEN + "Journey will now help navigate players to this objective's location");
                }
              } else {
                objectiveHolder.getConfig().set(path, null);
                context.getSender().sendMessage(Color.YELLOW + "Journey will not navigate players to this objective");
              }
              objectiveHolder.saveConfig();
            }));
  }

}
