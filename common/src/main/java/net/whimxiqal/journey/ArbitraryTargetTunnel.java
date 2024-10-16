package net.whimxiqal.journey;

public class ArbitraryTargetTunnel extends TunnelImpl {

  private final Target entrance;

  public ArbitraryTargetTunnel(Target entrance, Cell exit, int cost, Runnable prompt, String permission) {
    super(exit, cost, prompt, permission);
    this.entrance = entrance;
  }

  @Override
  public Target entrance() {
    return entrance;
  }
}
