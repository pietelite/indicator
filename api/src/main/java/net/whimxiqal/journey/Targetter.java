package net.whimxiqal.journey;

import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface Targetter {

  @Nullable
  Target targetSnapshot();

}
