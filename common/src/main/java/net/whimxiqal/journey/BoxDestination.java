package net.whimxiqal.journey;

import java.util.List;
import net.kyori.adventure.text.Component;

public class BoxDestination extends DestinationImpl {

  private final CellBox box;

  BoxDestination(Component name, List<Component> description, String permission, CellBox box) {
    super(name, description, permission);
    this.box = box;
  }

  @Override
  public Target targetSnapshot() {
    return new BoxTarget(box);
  }

  public CellBox box() {
    return box;
  }

  static class Builder extends DestinationBuilderImpl {

    private final CellBox box;

    Builder(CellBox box) {
      this.box = box;
    }

    @Override
    public Destination build() {
      return new BoxDestination(name, description, permission, box);
    }
  }

}
