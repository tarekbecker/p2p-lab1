package de.tudarmstadt.maki.simonstrator.overlay.flooding.operations;

import java.util.List;

import de.tudarmstadt.maki.simonstrator.api.Message;
import de.tudarmstadt.maki.simonstrator.api.Monitor;
import de.tudarmstadt.maki.simonstrator.api.Monitor.Level;
import de.tudarmstadt.maki.simonstrator.api.component.transport.TransInfo;
import de.tudarmstadt.maki.simonstrator.api.component.transport.TransMessageCallback;
import de.tudarmstadt.maki.simonstrator.api.operation.AbstractOperation;
import de.tudarmstadt.maki.simonstrator.api.operation.OperationCallback;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.FloodingContact;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.FloodingNode;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.FloodingNodeFactory;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.FloodingSettings.Times;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.messages.JoinTrackerMessage;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.messages.JoinTrackerReplyMessage;

/**
 * This operation finds a tracker and registers the node with the tracker. It
 * masks the bootstrapping process which allows for arbitrary complex ways to
 * find the tracker, if that becomes interesting in the future. For now, we just
 * rely on global knowledge.
 * 
 * At the end, an initial neighborhood set is returned.
 * 
 */
public class JoinTrackerOperation extends AbstractOperation<FloodingNode, List<FloodingContact>> implements
        TransMessageCallback {

	private List<FloodingContact> initialContacts;

	/**
	 * Instantiates a new join tracker operation.
	 * 
	 * @param component
	 *            the component
	 * @param callback
	 *            the callback
	 */
	public JoinTrackerOperation(FloodingNode component, OperationCallback<List<FloodingContact>> callback) {
		super(component, callback);
	}

	@Override
	protected void execute() {

		FloodingContact trackerContact = FloodingNodeFactory.getTrackerContact(getComponent());

		JoinTrackerMessage joinMsg = new JoinTrackerMessage((FloodingContact)getComponent().getLocalOverlayContact());
		getComponent().getTransport().sendAndWait(joinMsg, trackerContact.getTransInfo().getNetId(), trackerContact.getTransInfo().getPort(), this, getComponent().getSettings().getTime(Times.MSG_TIMEOUT));
	} 

	@Override
	public void messageTimeoutOccured(int commId) {
		/*
		 * TODO: what about retries? 
		 */
		Monitor.log(FloodingNode.class, Level.WARN,
				JoinTrackerOperation.this + ": JoinTracker failed due to a message timeout!");

		operationFinished(false);
	}

	@Override
	public void receive(Message msg, TransInfo senderInfo, int commId) {
		if (isFinished()) {
			return;
		}

		JoinTrackerReplyMessage reply = (JoinTrackerReplyMessage) msg;

		/*
		 * The answer also contains a set of nodes that we will use as a
		 * starting point for the signaling mesh
		 */
		initialContacts = reply.getNeighbors();

		operationFinished(true);
	}

	/**
	 * List of contacts returned by the tracker as a starting point for the
	 * signaling mesh
	 * 
	 * @return
	 */
	@Override
	public List<FloodingContact> getResult() {
		return initialContacts;
	}

}
