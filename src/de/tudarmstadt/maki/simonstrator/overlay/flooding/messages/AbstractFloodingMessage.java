package de.tudarmstadt.maki.simonstrator.overlay.flooding.messages;

import de.tudarmstadt.maki.simonstrator.api.Message;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.FloodingContact;

/**
 * The base message for all flooding messages. 
 */
public abstract class AbstractFloodingMessage implements FloodingMessage {

	private static final long serialVersionUID = 8557459335611808771L;

	private final FloodingContact sender;

	/**
	 * A marker, if this message is to be replied to via sendReply()
	 */
	private boolean wantsReply = false;

	/**
	 * 
	 * @param sender
	 *            this will be cloned
	 */
	public AbstractFloodingMessage(FloodingContact sender) {
		this.sender = sender;
	}

	@Override
	public void setWantsReply() {
		wantsReply = true;
	}

	@Override
	public boolean wantsReply() {
		return wantsReply;
	}

	@Override
	public FloodingContact getSenderContact() {
		return sender;
	}

	@Override
	public long getSize() {
		return sender.getTransmissionSize();
	}

	@Override
	public Message getPayload() {
		/*
		 * We just assume that there is no additional payload for most messages.
		 */
		return null;
	}

}
