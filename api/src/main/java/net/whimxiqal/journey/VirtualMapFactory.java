package net.whimxiqal.journey;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Supplier;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
interface VirtualMapFactory {

  VirtualMapFactory INSTANCE = ServiceLoader.load(
          VirtualMapFactory.class,
          VirtualMapFactory.class.getClassLoader())
      .findFirst().orElseThrow();

  <X> VirtualMap<X> of(Supplier<Map<String, ? extends X>> supplier, int size);

  <X> VirtualMap<X> of(Map<String, ? extends X> map);

  <X> VirtualMap<X> ofSingleton(String id, X item);

  <X> VirtualMap<X> empty();

}
