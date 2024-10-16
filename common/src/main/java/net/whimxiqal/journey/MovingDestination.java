package net.whimxiqal.journey;

import java.util.List;
import net.kyori.adventure.text.Component;

public class MovingDestination extends DestinationImpl {

  private final CellSupplier supplier;

  MovingDestination(Component name, List<Component> description, String permission, CellSupplier supplier) {
    super(name, description, permission);
    this.supplier = supplier;
  }

  @Override
  public Target targetSnapshot() {
    Cell cell = supplier.get();
    if (cell == null) {
      return null;
    }
    return new CellTarget(cell);
  }

  static class Builder extends DestinationBuilderImpl {

    private final CellSupplier supplier;

    Builder(CellSupplier supplier) {
      this.supplier = supplier;
    }

    @Override
    public Destination build() {
      return new MovingDestination(name, description, permission, supplier);
    }
  }
}
