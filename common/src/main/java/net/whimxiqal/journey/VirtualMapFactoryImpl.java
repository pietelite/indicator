package net.whimxiqal.journey;

import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

public class VirtualMapFactoryImpl implements VirtualMapFactory {
  @Override
  public <X> VirtualMap<X> of(Supplier<Map<String, ? extends X>> supplier, int size) {
    return new VirtualMapImpl<>(supplier, size);
  }

  @Override
  public <X> VirtualMap<X> of(Map<String, ? extends X> map) {
    return new VirtualMapImpl<>(map);
  }

  @Override
  public <X> VirtualMap<X> ofSingleton(String id, X item) {
    return new VirtualMapImpl<>(Collections.singletonMap(id, item));
  }

  @Override
  public <X> VirtualMap<X> empty() {
    return new VirtualMapImpl<>(Collections.emptyMap());
  }
}
