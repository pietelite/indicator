package net.whimxiqal.journey;

public class BoxTargetTunnel extends TunnelImpl {

  private final CellBox entrance;

  public BoxTargetTunnel(CellBox entrance, Cell exit, int cost, Runnable prompt, String permission) {
    super(exit, cost, prompt, permission);
    this.entrance = entrance;
  }

  @Override
  public Target entrance() {
    return new BoxTarget(entrance);
  }

  public CellBox box() {
    return entrance;
  }
}
