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

package net.whimxiqal.journey.platform;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import net.whimxiqal.journey.Cell;
import net.whimxiqal.journey.Journey;
import net.whimxiqal.journey.Tunnel;

public final class WorldLoader {

  public static final String[] worldResources = {"world1", "world2"};
  public static final Map<Character, CellType> cellTypes = new HashMap<>();

  static {
    cellTypes.put(' ', CellType.BLANK);
    cellTypes.put('X', CellType.BARRIER);
    // all the rest are "blank" but serve some other purpose too
  }

  public static int domain(int index) {
    TestWorld world = TestPlatformProxy.worlds.get(index);
    if (world == null) {
      throw new IllegalArgumentException("No world at index " + index + ". Worlds: " + TestPlatformProxy.worlds);
    }
    return Journey.get().domainManager().domainIndex(world.uuid);
  }

  public static void initWorlds() {
    Map<Character, Cell> pendingPorts = new HashMap<>();

    for (int resourceIdx = 0; resourceIdx < worldResources.length; resourceIdx++) {
      String resource = worldResources[resourceIdx];
      UUID uuid = UUID.randomUUID();
      int domain = Journey.get().domainManager().domainIndex(uuid);
      System.out.println("Getting domain for uuid: " + uuid + ": id is " + domain);
      TestWorld world = new TestWorld();
      world.name = resource;

      URL url = Thread.currentThread().getContextClassLoader().getResource("worlds/" + resource + ".txt");
      File file = new File(url.getPath());
      List<CellType[]> allLines = new LinkedList<>();
      int maxLineLength = 0;
      try {
        Scanner scanner = new Scanner(file);
        CellType[] cellLine;
        int y = 0;
        while (scanner.hasNextLine()) {
          char[] line = scanner.nextLine().toCharArray();
          cellLine = new CellType[line.length];
          for (int x = 0; x < line.length; x++) {
            char c = line[x];
            cellLine[x] = cellTypes.getOrDefault(c, CellType.BLANK);

            // POI
            if (Character.isDigit(c)) {
              TestPlatformProxy.pois.put(Character.toString(c), new Cell(x, 0, y, domain));
            }

            // TUNNEL
            if (Character.isLetter(c)) {
              Cell cell = new Cell(x, 0, y, domain);
              if (Character.isUpperCase(c)) {
                if (pendingPorts.containsKey(Character.toLowerCase(c))) {
                  // complete the tunnel
                  TestPlatformProxy.tunnels.add(Tunnel.builder(pendingPorts.get(Character.toLowerCase(c)), cell).build());
                } else {
                  pendingPorts.put(c, cell);
                }
              } else {
                // lower case
                if (pendingPorts.containsKey(Character.toUpperCase(c))) {
                  // complete the tunnel
                  TestPlatformProxy.tunnels.add(Tunnel.builder(cell, pendingPorts.get(Character.toUpperCase(c))).build());
                } else {
                  pendingPorts.put(c, cell);
                }
              }
            }
          }
          allLines.add(cellLine);
          maxLineLength = Math.max(maxLineLength, line.length);
          y++;
        }
      } catch (FileNotFoundException e) {
        e.printStackTrace();
      }

      world.uuid = uuid;
      world.lengthX = maxLineLength;
      world.lengthY = allLines.size();
      world.cells = new CellType[world.lengthY][world.lengthX];
      for (int i = 0; i < allLines.size(); i++) {
        CellType[] line = allLines.get(i);
        for (int j = 0; j < line.length; j++) {
          world.cells[i][j] = line[j];
        }
      }

      TestPlatformProxy.worlds.put(domain, world);
    }
  }

  public static TestWorld getWorld(String name) {
    for (TestWorld world : TestPlatformProxy.worlds.values()) {
      if (world.name.equals(name)) {
        return world;
      }
    }
    throw new IllegalArgumentException();
  }

}
