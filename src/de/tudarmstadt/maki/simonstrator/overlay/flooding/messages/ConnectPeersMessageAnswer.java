package de.tudarmstadt.maki.simonstrator.overlay.flooding.messages;


import de.tudarmstadt.maki.simonstrator.overlay.flooding.FloodingContact;

public class ConnectPeersMessageAnswer extends AbstractFloodingMessage {

  private final boolean accept;

  public ConnectPeersMessageAnswer(FloodingContact sender, boolean accept) {
    super(sender);
    this.accept = accept;
  }

  public boolean isAccept() {
    return accept;
  }
}
