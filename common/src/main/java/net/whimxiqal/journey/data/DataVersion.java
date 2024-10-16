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

package net.whimxiqal.journey.data;

import java.util.HashMap;
import java.util.Map;
import net.whimxiqal.journey.Journey;

public enum DataVersion implements Comparable<DataVersion> {
  ERROR(-1),
  V000(0),
  V001(1),
  V002(2),
  V003(3);

  private static final Map<Integer, DataVersion> VERSIONS = new HashMap<>();

  private static final DataVersion latestVersion;

  static {
    DataVersion highestVersion = DataVersion.V000;

    for (DataVersion version : DataVersion.values()) {
      VERSIONS.put(version.internalVersion, version);
      if (version.internalVersion > highestVersion.internalVersion) highestVersion = version;
    }

    latestVersion = highestVersion;
  }

  public final int internalVersion;

  DataVersion(int version) {
    if (version > 9999) {
      throw new IllegalArgumentException("We don't support versions above 9999");
    }
    this.internalVersion = version;
  }

  public static DataVersion fromString(String versionString) {
    int parsedVersion;
    try {
      parsedVersion = Integer.parseInt(versionString);
    } catch (NumberFormatException e) {
      Journey.logger().error("The database version file is in the incorrect format.");
      return DataVersion.ERROR;
    }
    return fromInt(parsedVersion);
  }

  public static DataVersion fromInt(int versionInt) {
    DataVersion version = VERSIONS.get(versionInt);
    if (version == null) {
      Journey.logger().error("Journey doesn't support a database version " + versionInt);
      return DataVersion.ERROR;
    }
    return version;
  }

  public static DataVersion latest() {
    return latestVersion;
  }

  public boolean hasError() {
    return internalVersion < 0;
  }

  public String printVersion() {
    if (hasError()) {
      throw new IllegalStateException("The version is in an error state");
    }
    return String.format("%04d", internalVersion);
  }

  @Override
  public String toString() {
    return hasError() ? "ERROR" : ("v" + printVersion());
  }

}
