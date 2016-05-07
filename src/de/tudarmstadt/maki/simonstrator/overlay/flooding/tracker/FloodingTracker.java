package de.tudarmstadt.maki.simonstrator.overlay.flooding.tracker;


import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.tudarmstadt.maki.simonstrator.api.Host;
import de.tudarmstadt.maki.simonstrator.api.Message;
import de.tudarmstadt.maki.simonstrator.api.component.network.NetInterface;
import de.tudarmstadt.maki.simonstrator.api.component.transport.MessageBasedTransport;
import de.tudarmstadt.maki.simonstrator.api.component.transport.ProtocolNotAvailableException;
import de.tudarmstadt.maki.simonstrator.api.component.transport.TransInfo;
import de.tudarmstadt.maki.simonstrator.api.component.transport.TransMessageListener;
import de.tudarmstadt.maki.simonstrator.api.component.transport.protocol.TCPMessageBased;
import de.tudarmstadt.maki.simonstrator.overlay.AbstractOverlayNode;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.FloodingContact;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.FloodingContact.ContactType;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.FloodingSettings;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.FloodingSettings.Params;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.messages.FloodingMessage;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.messages.JoinTrackerMessage;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.messages.JoinTrackerReplyMessage;

/**
 * This is a Tracker in the Flooding Overlay. 
 */
public class FloodingTracker extends AbstractOverlayNode implements TransMessageListener {

	/**
	 * The settings object used to define settings of the overlay
	 */
	private final FloodingSettings settings;

	/**
	 * Tracker contact
	 */
	private FloodingContact localContact;

	/**
	 * The port that the tracker listens to
	 */
	private final int localPort;

	/**
	 * The transport used to send and receive messages
	 */
	private MessageBasedTransport transport;

	/**
	 * All nodes that are know to the tracker
	 */
	private Set<FloodingContact> availableNodes = new LinkedHashSet<FloodingContact>();

	/**
	 * The constructor called by the node factory
	 * 
	 * @param peerId
	 * @param host
	 * @param port
	 */
	public FloodingTracker(Host host, int port,
			FloodingSettings settings) {
		super(host);
		this.localPort = port;
		this.settings = settings;
	}

	@Override
	public void initialize() {

		/*
		 * Bind the first Network Interface
		 */
		NetInterface net = getHost().getNetworkComponent().getNetworkInterfaces().iterator().next();
		try {
			transport = getHost().getTransportComponent().getProtocol(TCPMessageBased.class, net.getLocalInetAddress(),
					localPort);
		} catch (ProtocolNotAvailableException e) {
			// Something went terribly wrong
			e.printStackTrace();
		}

		localContact = new FloodingContact(getHost().getId(), transport.getTransInfo(), ContactType.TRACKER);

		transport.setTransportMessageListener(this);

		setPeerStatus(PeerStatus.PRESENT);
	}




	@Override
	public void messageArrived(Message omsg, TransInfo sender, int commID) {
		FloodingMessage msg = (FloodingMessage) omsg;
		FloodingMessage reply = null;
		if (msg instanceof JoinTrackerMessage) {
			// Build answer to join message
			JoinTrackerMessage jtm = (JoinTrackerMessage) msg;
			reply = new JoinTrackerReplyMessage(localContact,
					getInitialNeighborsForNode(jtm.getSenderContact()));

			// Add sender to list of nodes for later neighbors
			availableNodes.add(msg.getSenderContact());
		} else {
			throw new AssertionError("Unknown message type " + msg.getClass().getSimpleName());
		}
		if (reply != null) {
			// Send answer back
			transport.sendReply(reply, sender.getNetId(), sender.getPort(), commID);
		}
	}

	/**
	 * Generate a random set of initial neighbors
	 * 
	 * @param requester
	 * @return
	 */
	private List<FloodingContact> getInitialNeighborsForNode(
			FloodingContact requester) {
		List<FloodingContact> initialNeigbors = new LinkedList<FloodingContact>(availableNodes);

		// Remove own contact from list
		initialNeigbors.remove(requester);

		// Determine the maximum number of neighbors to be returned
		int maxNumNeighbors = settings.getParam(Params.MAX_NUM_NEW_NEIGHBORS);
		
		if (initialNeigbors.size() < maxNumNeighbors)
			return initialNeigbors;

		// Shuffle list and return max number of neighbors
		Collections.shuffle(initialNeigbors);
		return initialNeigbors.subList(0, maxNumNeighbors - 1);
	}

	@Override
	public FloodingContact getLocalOverlayContact() {
		return localContact;
	}

	@Override
	public void wentOffline(Host host, NetInterface netInterface) {
		// Nothing to do right now
	}

	@Override
	public void wentOnline(Host host, NetInterface netInterface) {
		// Nothing to do right now
	}
}
