package de.tudarmstadt.maki.simonstrator.overlay.flooding.messages;


import de.tudarmstadt.maki.simonstrator.overlay.flooding.FloodingContact;

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
}
