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

package net.whimxiqal.journey.integration.betonquest;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.whimxiqal.journey.JourneyApi;
import net.whimxiqal.journey.bukkit.JourneyBukkitApi;
import net.whimxiqal.journey.navigation.NavigatorDetailsBuilder;
import net.whimxiqal.journey.search.SearchFlag;
import net.whimxiqal.journey.search.SearchFlags;
import net.whimxiqal.journey.search.SearchResult;
import org.betonquest.betonquest.Instruction;
import org.betonquest.betonquest.api.profiles.Profile;
import org.betonquest.betonquest.api.quest.event.Event;
import org.betonquest.betonquest.exceptions.InstructionParseException;
import org.betonquest.betonquest.exceptions.QuestRuntimeException;
import org.betonquest.betonquest.instruction.variable.location.VariableLocation;
import org.bukkit.Location;

public class JourneyQuestEvent implements Event {

  private static final Pattern FLAG_REGEX = Pattern.compile("([a-zA-Z:-]+)=(?:([^(].*)|\\((.+)\\))");
  private static final Pattern INCOMPLETE_FLAG_REGEX = Pattern.compile("([a-zA-Z:-]+)=\\([^)]*");
  private static final Pattern NAVIGATOR_FLAG_NAME_REGEX = Pattern.compile("navigator:([a-zA-Z-]+)");

  private static final PlainTextComponentSerializer TEXT_SERIALIZER = PlainTextComponentSerializer.builder().build();
  private final VariableLocation variableLocation;
  private final List<SearchFlag<?>> searchFlags = new LinkedList<>();
  private final Map<String, String> navigatorOptions = new HashMap<>();
  private Component successMessage;
  private Component failureMessage;
  private String navigatorType;

  public JourneyQuestEvent(Instruction instruction) throws InstructionParseException {
    // first parameter is always location
    this.variableLocation = instruction.getLocation();

    String flag = "";
    while (instruction.hasNext()) {
      flag += instruction.next();

      Matcher incompleteMatcher = INCOMPLETE_FLAG_REGEX.matcher(flag);
      if (incompleteMatcher.matches()) {
        continue;
      }

      Matcher matcher = FLAG_REGEX.matcher(flag);
      if (matcher.matches()) {
        recordFlag(matcher.group(1), matcher.group(2) == null ? matcher.group(3) : matcher.group(2));
        flag = "";
        continue;
      }

      throw new InstructionParseException("Could not parse instruction flag at " + flag);
    }

    if (!flag.isEmpty()) {
      throw new InstructionParseException("Incomplete flag: " + flag);
    }
  }

  private void recordFlag(String name, String value) throws InstructionParseException {
    switch (name.toLowerCase()) {
      case "timeout": {
        int timeout;
        try {
          timeout = Integer.parseInt(value);
        } catch (NumberFormatException e) {
          throw new InstructionParseException("Timeout value must be an integer, not " + value, e);
        }
        searchFlags.add(SearchFlag.of(SearchFlag.Type.TIMEOUT, timeout));
        return;
      }
      case "fly": {
        boolean flyValue;
        if (value.equalsIgnoreCase("true")) {
          flyValue = true;
        } else if (value.equalsIgnoreCase("false")) {
          flyValue = false;
        } else {
          throw new InstructionParseException("Invalid fly flag value: " + value);
        }
        searchFlags.add(SearchFlag.of(SearchFlag.Type.FLY, flyValue));
        return;
      }
      case "successmessage": {
        successMessage = TEXT_SERIALIZER.deserialize(value);
        return;
      }
      case "failuremessage": {
        failureMessage = TEXT_SERIALIZER.deserialize(value);
        return;
      }
    }

    Matcher navigatorMatcher = NAVIGATOR_FLAG_NAME_REGEX.matcher(name);
    if (navigatorMatcher.matches()) {
      String navigatorKey = navigatorMatcher.group(1);
      if (navigatorKey.equalsIgnoreCase("type")) {
        navigatorType = value;
      } else {
        navigatorOptions.put(navigatorKey, value);
      }
      return;
    }

    throw new InstructionParseException("Flag could not be parsed from key: " + name + ", value: " + value);
  }

  @Override
  public void execute(Profile profile) throws QuestRuntimeException {
    Location location = this.variableLocation.getValue(profile);
    JourneyApi.get().searching().runPlayerDestinationSearch(profile.getPlayerUUID(), JourneyBukkitApi.get().toCell(location), SearchFlags.of(searchFlags))
        .thenAccept(result -> {
          JourneyApi.get().navigating().stopNavigation(profile.getPlayerUUID()).thenAccept(stopResult -> {
            if (result.status() == SearchResult.Status.SUCCESS) {
              if (successMessage != null && profile.getOnlineProfile().isPresent()) {
                profile.getOnlineProfile().get().getPlayer().sendMessage(successMessage);
              }
              NavigatorDetailsBuilder<?> detailsBuilder;
              if (navigatorType == null) {
                detailsBuilder = JourneyApi.get().navigating().trailNavigatorDetailsBuilder();
              } else {
                detailsBuilder = JourneyApi.get().navigating().navigatorDetailsBuilder(navigatorType);
                navigatorOptions.forEach(detailsBuilder::setOption);
              }
              JourneyApi.get().navigating().navigatePlayer(profile.getPlayerUUID(), result.path(), detailsBuilder.build());
            } else {
              if (failureMessage != null && profile.getOnlineProfile().isPresent()) {
                profile.getOnlineProfile().get().getPlayer().sendMessage(failureMessage);
              }
            }
          });
        });
  }
}
