package de.tudarmstadt.maki.simonstrator.overlay.flooding.messages;

import java.util.List;

import de.tudarmstadt.maki.simonstrator.overlay.flooding.FloodingContact;

/**
 * This is the reply a tracker sends when receiving a {@link JoinTrackerMessage}
 */
public class JoinTrackerReplyMessage extends AbstractFloodingMessage {

	private static final long serialVersionUID = 3804130614398484964L;

	private final List<FloodingContact> initialNeighbors;

	public JoinTrackerReplyMessage(FloodingContact sender,
			List<FloodingContact> initialNeighbors) {
		super(sender);
		this.initialNeighbors = initialNeighbors;
	}

	public List<FloodingContact> getNeighbors() {
		return initialNeighbors;
	}

	@Override
	public long getSize() {
		/*
		 * This is NOT the full video file, therefore getSize would not work!
		 */
		long size = 0;
		for (FloodingContact c : initialNeighbors) {
			size += c.getTransmissionSize();
		}
		
		return super.getSize() + size;
	}

	@Override
	public String toString() {
		return "JOIN REPLY neighbors:" + getNeighbors().toString();
	}

}
