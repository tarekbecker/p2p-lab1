package de.tudarmstadt.maki.simonstrator.overlay.flooding.messages;


import de.tudarmstadt.maki.simonstrator.overlay.flooding.FloodingContact;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.FloodingNode;

import java.util.List;
import java.util.Set;

public class ConnectPeersMessage extends AbstractFloodingMessage{

  private final Set<FloodingContact> neighbors;

  public ConnectPeersMessage(FloodingContact sender, Set<FloodingContact> neighbors) {
    super(sender);
    this.neighbors = neighbors;
  }

  public Set<FloodingContact> getNeighbors() {
    return neighbors;
  }

  @Override
  public long getSize() {
    long size = super.getSize();
    if (FloodingNode.FLAG_LAST_TASK) {
      for (FloodingContact fc : neighbors) {
        size += fc.getTransmissionSize();
      }
    }
    return size;
  }
}
