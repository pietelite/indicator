/*
 * MIT License
 *
 * Copyright (c) Pieter Svenson
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

package net.whimxiqal.journey.command;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.Destination;
import net.whimxiqal.journey.JourneyApi;
import net.whimxiqal.journey.JourneyApiProvider;
import net.whimxiqal.journey.VirtualMap;
import net.whimxiqal.journey.Scope;
import net.whimxiqal.journey.Journey;
import net.whimxiqal.journey.JourneyTestHarness;
import net.whimxiqal.journey.platform.TestJourneyPlayer;
import net.whimxiqal.journey.platform.TestPlatformProxy;
import net.whimxiqal.journey.platform.WorldLoader;
import net.whimxiqal.mantle.common.CommandResult;
import net.whimxiqal.mantle.common.CommandSource;
import net.whimxiqal.mantle.common.Mantle;
import net.whimxiqal.mantle.common.MantleCommand;
import net.whimxiqal.mantle.common.connector.CommandConnector;
import net.whimxiqal.mantle.common.connector.CommandRoot;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestCommands extends JourneyTestHarness {

  private final static Map<String, MantleCommand> commands = new HashMap<>();
  private final static UUID myUuid = UUID.randomUUID();
  private final static TestProxy testProxy = new TestProxy();

  @BeforeAll
  static void init() {
    Mantle.setProxy(testProxy);
    register(JourneyConnectorProvider.connector());
    TestPlatformProxy.onlinePlayers.add(new TestJourneyPlayer(myUuid));
  }

  static void register(CommandConnector connector) {
    for (CommandRoot root : connector.roots()) {
      commands.put(root.baseCommand(), new MantleCommand(connector, root));
    }
  }

  static CommandResult execute(String command) {
    String[] baseCommand = command.split(" ", 2);
    assert baseCommand.length == 1 || baseCommand.length == 2;
    MantleCommand mantleCommand = commands.get(baseCommand[0]);
    if (mantleCommand == null) {
      System.out.println("Mantle command could not be found for base command: " + baseCommand[0]);
      return CommandResult.failure();
    }
    return mantleCommand.process(new CommandSource(CommandSource.Type.PLAYER,
        myUuid,
        Journey.get().proxy().audienceProvider().console()), baseCommand.length > 1 ? baseCommand[1] : "");
  }

  static List<String> completions(String command) {
    String[] baseCommand = command.split(" ", 2);
    assert baseCommand.length == 1 || baseCommand.length == 2;
    MantleCommand mantleCommand = commands.get(baseCommand[0]);
    if (mantleCommand == null) {
      return Collections.emptyList();
    }
    return mantleCommand.complete(new CommandSource(CommandSource.Type.PLAYER,
        myUuid,
        Journey.get().proxy().audienceProvider().console()), baseCommand.length > 1 ? baseCommand[1] : "");
  }

  static void addHome() {
    Journey.get().dataManager().personalWaypointManager().add(myUuid, new Cell(0, 0, 0, WorldLoader.domain(0)), "home");
  }

  void commandSuccess(String command) {
    Assertions.assertEquals(CommandResult.Type.SUCCESS, execute(command).type(), "\"Test failed for command " + command + "\"");
  }

  void commandFailure(String command) {
    Assertions.assertEquals(CommandResult.Type.FAILURE, execute(command).type(), "\"Test failed for command " + command + "\"");
  }

  @Test
  void sampleTest() {
    commandSuccess("journey");
  }

  @Test
  void journeyToTest() {
    addHome();
    commandSuccess("journeyto home");
    commandSuccess("journeyto personal:home");
    Journey.get().dataManager().publicWaypointManager().add(new Cell(0, 0, 0, WorldLoader.domain(0)), "home");
    commandFailure("journeyto home");
    commandSuccess("journeyto personal:home");
    commandSuccess("journeyto server:home");

    commandSuccess("journeyto surface");
    commandFailure("journeyto world:" + WorldLoader.worldResources[0]);  // we are already in world 0
    Cell originalLocation = TestJourneyPlayer.LOCATION;
    TestJourneyPlayer.LOCATION = new Cell(0, 0, 0, WorldLoader.domain(1));
    commandSuccess("journeyto world:" + WorldLoader.worldResources[0]);
    TestJourneyPlayer.LOCATION = originalLocation;
    commandSuccess("journeyto world:" + WorldLoader.worldResources[1]);
    commandFailure("journeyto death");
    Journey.get().deathManager().setDeathLocation(myUuid, new Cell(0, 0, 0, 0));
    commandSuccess("journeyto death");

    testProxy.revokeAllPermissions(myUuid);
    commandFailure("journeyto home");
  }

  @Test
  void journeyToCompletionsTest() {
    addHome();
    List<String> completions = completions("journeyto personal:");
    Assertions.assertEquals(1, completions.size());
    Assertions.assertEquals("personal:home", completions.get(0));
  }

  @Test
  void complicatedScope() {
    JourneyApi api = JourneyApiProvider.get();
    Destination destination = Destination.of(new Cell(0, 0, 0, WorldLoader.domain(0)));
    testProxy.revokeAllPermissions(myUuid);
    api.registerScope("Journey", "complex", Scope.builder()
        .subScopes(() -> {
          Map<String, Scope> scopes = new HashMap<>();
          scopes.put("path-a", Scope.builder()
              .destinations(() -> {
                Map<String, Destination> destinations = new HashMap<>();
                destinations.put("path-a-1", destination);
                destinations.put("path-shared", destination);
                destinations.put("permission", Destination.builder(new Cell(0, 0, 0, WorldLoader.domain(0))).permission("you-dont-have-this").build());
                return VirtualMap.of(destinations);
              }).build());
          scopes.put("path-b", Scope.builder()
              .destinations(() -> {
                Map<String, Destination> destinations = new HashMap<>();
                destinations.put("path-b", destination);
                destinations.put("path-b-1", destination);
                destinations.put("path-b space", destination);
                destinations.put("path-shared", destination);
                return VirtualMap.of(destinations);
              }).build());
          scopes.put("contextually-necessary", Scope.builder()
              .destinations(() -> {
                Map<String, Destination> destinations = new HashMap<>();
                destinations.put("path-a-1", destination);
                destinations.put("hidden", destination);
                return VirtualMap.of(destinations);
              }).strict()
              .build());
          scopes.put("permission-scope", Scope.builder()
              .destinations(VirtualMap.ofSingleton("cant-reach", destination))
              .permission("you-also-dont-have-this")
              .build());
          return VirtualMap.of(scopes);
        }).build());
    List<String> completions = completions("journeyto ");

    // Full scope
    Assertions.assertTrue(completions.contains("complex:path-a:path-a-1"));
    Assertions.assertTrue(completions.contains("complex:path-a:path-shared"));
    Assertions.assertTrue(completions.contains("complex:path-b:path-b-1"));
    Assertions.assertTrue(completions.contains("complex:path-b:path-shared"));

    // Partial scope (skip highest level)
    Assertions.assertTrue(completions.contains("path-a:path-a-1"));
    Assertions.assertTrue(completions.contains("path-b:path-b-1"));

    // Partial scope (skip highest and second level)
    Assertions.assertTrue(completions.contains("path-a-1"));
    Assertions.assertTrue(completions.contains("path-b-1"));
    Assertions.assertTrue(completions.contains("\"path-b space\""));

    // Merge names if scope and its destination has the same one
    Assertions.assertTrue(completions.contains("complex:path-b"));
    Assertions.assertFalse(completions.contains("complex:path-b:path-b"));

    // Quotes around things with spaces
    Assertions.assertTrue(completions.contains("\"complex:path-b:path-b space\""));

    // Ambiguous
    Assertions.assertFalse(completions.contains("path-shared"));

    // Contextually specific things don't get in the way of other destinations
    Assertions.assertTrue(completions.contains("contextually-necessary:path-a-1"));
    Assertions.assertTrue(completions.contains("contextually-necessary:hidden"));
    Assertions.assertFalse(completions.contains("hidden"));

    // No Permission
    String[] permissionRequired = {"path-a:permission", "permission", "permission-scope:cant-reach", "cant-reach"};
    for (String string : permissionRequired) {
      Assertions.assertFalse(completions.contains(string), "The scope target " + string + " should be disallowed by permission restriction, but isn't disallowed");
    }
    testProxy.grantAllPermissions(myUuid);
    completions = completions("journeyto ");
    for (String string : permissionRequired) {
      Assertions.assertTrue(completions.contains(string));
    }

    // path-a-1
    commandSuccess("journeyto complex:path-a:path-a-1");
    commandSuccess("journeyto path-a:path-a-1");
    commandSuccess("journeyto complex:path-a-1");
    commandSuccess("journeyto path-a-1"); // still good, even though contextually-necessary also has it

    // path-shared in path-a scope
    commandSuccess("journeyto complex:path-a:path-shared");
    commandSuccess("journeyto path-a:path-shared");
    commandFailure("journeyto complex:path-shared"); // ambiguous with scope path-b
    commandFailure("journeyto path-shared"); // ambiguous with scope path-b

    // path-b
    commandSuccess("journeyto complex:path-b");
    commandSuccess("journeyto complex:path-b:path-b");  // redundant, but ok
    commandSuccess("journeyto path-b");  // redundant, but ok

    // path-b-1
    commandSuccess("journeyto complex:path-b:path-b-1");
    commandSuccess("journeyto path-b:path-b-1");
    commandSuccess("journeyto complex:path-b-1");
    commandSuccess("journeyto path-b-1");

    // path-b space
    commandSuccess("journeyto \"complex:path-b:path-b space\"");
    commandSuccess("journeyto \"path-b:path-b space\"");
    commandSuccess("journeyto \"complex:path-b space\"");
    commandSuccess("journeyto \"path-b space\"");

    // path-shared in path-b scope
    commandSuccess("journeyto complex:path-b:path-shared");
    commandSuccess("journeyto path-b:path-shared");

    // path-a-1 in contextually-necessary scope
    commandSuccess("journeyto contextually-necessary:path-a-1");

    // hidden
    commandSuccess("journeyto contextually-necessary:hidden");
    commandFailure("journeyto hidden");

    // no permission
    testProxy.revokeAllPermissions(myUuid);
    for (String string : permissionRequired) {
      commandFailure("journeyto " + string);
    }
    testProxy.grantAllPermissions(myUuid);
    for (String string : permissionRequired) {
      commandSuccess("journeyto " + string);
    }
  }
}
