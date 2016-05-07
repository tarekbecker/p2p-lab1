package de.tudarmstadt.maki.simonstrator.overlay.flooding.messages;

import de.tudarmstadt.maki.simonstrator.api.Message;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.FloodingContact;

/**
 * Basic Interface for all messages in the flooding overlay
 */
public interface FloodingMessage extends Message {

	/**
	 * Used in the {@link TransitMessageHandler} to mark messages that are to be
	 * answered via sendReply.
	 */
	public void setWantsReply();

	/**
	 * Reply to this message via sendReply(), used in the
	 * {@link TransitMessageHandler} to correctly dispatch a reply to this
	 * message.
	 * 
	 * @return
	 */
	public boolean wantsReply();

	/**
	 * The {@link TransitContact} of the sender of this message.
	 * 
	 * @return
	 */
	public FloodingContact getSenderContact();


}
