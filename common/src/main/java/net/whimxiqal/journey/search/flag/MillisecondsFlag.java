package net.whimxiqal.journey.search.flag;

import java.util.function.Supplier;
import net.whimxiqal.journey.util.TimeUtil;

public class MillisecondsFlag extends IntegerFlag {
  protected MillisecondsFlag(String name, Supplier<Integer> defaultValue, String permission, int min, int max, Integer... suggestions) {
    super(name, defaultValue, permission, min, max, suggestions);
  }

  @Override
  public String printValue(Integer val) {
    return val == 0 ? "-" : TimeUtil.toSimpleTimeMilliseconds(val);
  }
}
