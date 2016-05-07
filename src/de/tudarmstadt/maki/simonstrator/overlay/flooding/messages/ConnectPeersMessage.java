package de.tudarmstadt.maki.simonstrator.overlay.flooding.messages;

import de.tudarmstadt.maki.simonstrator.api.Message;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.FloodingContact;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.FloodingNode;

import java.util.Set;

public class ConnectPeersMessage extends AbstractFloodingMessage{

  public ConnectPeersMessage(FloodingContact sender) {
    super(sender);
  }

}
