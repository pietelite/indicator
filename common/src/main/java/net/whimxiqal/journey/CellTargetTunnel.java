package net.whimxiqal.journey;

public class CellTargetTunnel extends TunnelImpl {

  private final Cell entrance;

  public CellTargetTunnel(Cell entrance, Cell exit, int cost, Runnable prompt, String permission) {
    super(exit, cost, prompt, permission);
    this.entrance = entrance;
  }

  @Override
  public Target entrance() {
    return new CellTarget(entrance);
  }
}
