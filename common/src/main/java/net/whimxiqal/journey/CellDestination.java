package net.whimxiqal.journey;

import java.util.List;
import net.kyori.adventure.text.Component;

public class CellDestination extends DestinationImpl {

  private final Cell cell;

  CellDestination(Component name, List<Component> description, String permission,
                  Cell cell) {
    super(name, description, permission);
    this.cell = cell;
  }

  @Override
  public Target targetSnapshot() {
    return new CellTarget(cell);
  }

  static class Builder extends DestinationBuilderImpl {

    private final Cell cell;

    Builder(Cell cell) {
      this.cell = cell;
    }

    @Override
    public Destination build() {
      return new CellDestination(name, description, permission, cell);
    }
  }

}
