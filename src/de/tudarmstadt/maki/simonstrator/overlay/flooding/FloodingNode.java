package de.tudarmstadt.maki.simonstrator.overlay.flooding;

import java.util.*;
import java.util.concurrent.Semaphore;

import de.tudarmstadt.maki.simonstrator.api.Host;
import de.tudarmstadt.maki.simonstrator.api.Message;
import de.tudarmstadt.maki.simonstrator.api.Monitor;
import de.tudarmstadt.maki.simonstrator.api.Monitor.Level;
import de.tudarmstadt.maki.simonstrator.api.component.network.NetInterface;
import de.tudarmstadt.maki.simonstrator.api.component.overlay.IPeerStatusListener;
import de.tudarmstadt.maki.simonstrator.api.component.overlay.OverlayComponent;
import de.tudarmstadt.maki.simonstrator.api.component.overlay.OverlayContact;
import de.tudarmstadt.maki.simonstrator.api.component.transport.MessageBasedTransport;
import de.tudarmstadt.maki.simonstrator.api.component.transport.ProtocolNotAvailableException;
import de.tudarmstadt.maki.simonstrator.api.component.transport.TransInfo;
import de.tudarmstadt.maki.simonstrator.api.component.transport.TransMessageListener;
import de.tudarmstadt.maki.simonstrator.api.component.transport.protocol.UDP;
import de.tudarmstadt.maki.simonstrator.api.operation.Operation;
import de.tudarmstadt.maki.simonstrator.api.operation.OperationCallback;
import de.tudarmstadt.maki.simonstrator.api.operation.Operations;
import de.tudarmstadt.maki.simonstrator.overlay.AbstractOverlayNode;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.messages.ConnectPeersMessage;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.messages.ConnectPeersMessageAnswer;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.messages.FloodingMessage;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.operations.JoinTrackerOperation;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.operations.MaintenanceOperation;
import org.apache.commons.collections.set.SynchronizedSet;

public class FloodingNode extends AbstractOverlayNode implements
 TransMessageListener, IPeerStatusListener {

  public static final boolean FLAG_LAST_TASK = false;

  public static final List<FloodingNode> allNodes = Collections.synchronizedList(new LinkedList<FloodingNode>());
  private static final Random random = new Random();

  public Semaphore acceptIngoingConnections;
  public Semaphore acceptIngoingConnectionsAccepted = new Semaphore(0);
  public Semaphore establishOutgoingConnections;
  public Semaphore establishOutgoingConnectionsAccepted = new Semaphore(0);

  public Semaphore maxConnectionAttempts;

	/**
	 * The settings object used to define settings of the overlay
	 */
	private final FloodingSettings settings;

	/**
	 * The contact of the local node
	 */
	private FloodingContact localContact;

	/**
	 * The port that the overlay listens to
	 */
	private int localPort;

	/**
	 * The transport object to be used to send messages
	 */
	private MessageBasedTransport transport;

	/**
	 * Potential overlay neighbors of the node
	 */
	private final Set<FloodingContact> potentialNeighbors = Collections.synchronizedSet(new LinkedHashSet<FloodingContact>());

	/**
	 * Connected neighbors of the node
	 */
	public Set<FloodingContact> connectedNeighbors = Collections.synchronizedSet(new LinkedHashSet<FloodingContact>());

	/**
	 * The operation that does maintenance of the connections etc.
	 */
	private MaintenanceOperation maintenanceOperation;

	/**
	 * The constructor, called by the node factory
	 * 
	 * @param host
	 * @param port
	 * @param settings
	 */
	public FloodingNode(Host host, int port, FloodingSettings settings) {
		super(host);
		this.localPort = port;
		this.settings = settings;
    allNodes.add(this);
	}

  public synchronized void setBudget(int count) {
    if (acceptIngoingConnections == null) {
      double random = FloodingNode.random.nextDouble();
      acceptIngoingConnections = new Semaphore(count / 2);
      establishOutgoingConnections = new Semaphore(count / 2);
      if (count % 2 == 1) {
        if (random <= 0.4) {
          acceptIngoingConnections.release();
        } else {
          establishOutgoingConnections.release();
        }
      }
      maxConnectionAttempts = new Semaphore(count * 100);
    }
  }

  public boolean leaseOutgoingConnection() {
    return establishOutgoingConnections.tryAcquire();
  }

  public void connectToSomeone() {
    if (maxConnectionAttempts.tryAcquire()) {
      FloodingContact fc = null;
      synchronized (potentialNeighbors) {
        if (!potentialNeighbors.isEmpty()) {
          ArrayList<FloodingContact> list = new ArrayList<>();
          for (FloodingContact potentialNeighbor : potentialNeighbors) {
            list.add(potentialNeighbor);
          }
          fc = list.remove(new Random().nextInt(list.size()));
          potentialNeighbors.remove(fc);
        }
      }
      if (fc != null) {
        getTransport().send(new ConnectPeersMessage(localContact, connectedNeighbors), fc.getNetID(), FloodingNodeFactory.DEFAULT_NODE_PORT);
      } else {
        establishOutgoingConnections.release();
      }
    } else {
      establishOutgoingConnections.release();
    }
  }

	@Override
	public void initialize() {
		super.initialize();

		/*
		 * Bind the first Network Interface
		 */
		NetInterface net = getHost().getNetworkComponent()
				.getNetworkInterfaces().iterator().next();
		try {
			transport = getHost().getTransportComponent().getProtocol(
					UDP.class, net.getLocalInetAddress(), localPort);
		} catch (ProtocolNotAvailableException e) {
			// Something went terribly wrong
			e.printStackTrace();
		}

		localContact = new FloodingContact(getHost().getId(), transport.getTransInfo());

		addPeerStatusListener(this);

		// Bind the message Handler
		transport.setTransportMessageListener(this);

		setPeerStatus(PeerStatus.ABSENT);
	}

	@Override
	public void messageArrived(Message msg, TransInfo sender, int commID) {
		// Handle incoming messages

		// To work with message, cast it to its specific type
		if (msg instanceof ConnectPeersMessage) { // replace by your own type
      boolean result = acceptIngoingConnections.tryAcquire();
      getTransport()
          .send(new ConnectPeersMessageAnswer(this.localContact, result),
              sender.getNetId(), FloodingNodeFactory.DEFAULT_NODE_PORT);
      if (result) {
        acceptIngoingConnectionsAccepted.release();
        connectedNeighbors.add(((ConnectPeersMessage) msg).getSenderContact());
      }
      if (FLAG_LAST_TASK) {
        synchronized (potentialNeighbors) {
          Set<FloodingContact> newContact = new HashSet<>();
          for (FloodingContact fc : ((ConnectPeersMessage) msg).getNeighbors()) {
            newContact.add(fc);
          }
          newContact.removeAll(connectedNeighbors);
          potentialNeighbors.addAll(newContact);
        }
      }
		} else if (msg instanceof ConnectPeersMessageAnswer) {
      ConnectPeersMessageAnswer cpma = (ConnectPeersMessageAnswer) msg;
      if (cpma.isAccept()) {
        establishOutgoingConnectionsAccepted.release();
        connectedNeighbors.add(cpma.getSenderContact());
      } else {
        connectToSomeone();
      }
    }

	}



	/**
	 * Join the overlay
	 */
	public void join() {

		/*
		 * Join with the tracker and inform the appCallback. The app can then
		 * start the streaming process. If we already have a tracker contact,
		 * nothing happens.
		 */
		if (getPeerStatus() != PeerStatus.ABSENT) {
			// Avoid duplicate joining
			return;
		}

		setPeerStatus(PeerStatus.TO_JOIN);

		JoinTrackerOperation op = new JoinTrackerOperation(this,
				new OperationCallback<List<FloodingContact>>() {

			/*
			 * Here, all joining related actions are handled
			 */

			@Override
			public void calledOperationSucceeded(Operation<List<FloodingContact>> joinOp) {
				// Add new contacts from tracker to neighborhood list
				potentialNeighbors.addAll(joinOp.getResult());

				// Set the peer present
				setPeerStatus(PeerStatus.PRESENT);

				Monitor.log(FloodingNode.class, Level.DEBUG, FloodingNode.this + ": Join succeeded!");

				/*
				 * Create and start maintenance operation
				 */
				maintenanceOperation = new MaintenanceOperation(FloodingNode.this, Operations.getEmptyCallback());
				maintenanceOperation.start();
			}

			@Override
			public void calledOperationFailed(Operation<List<FloodingContact>> arg0) {
				setPeerStatus(PeerStatus.ABSENT);
				Monitor.log(FloodingNode.class, Level.DEBUG, FloodingNode.this + ": Join failed!");
				// TODO: Retry?
			}
		});
		op.scheduleImmediately();
	}

	public MessageBasedTransport getTransport() {
		return transport;
	}

	public FloodingSettings getSettings() {
		return settings;
	}

	@Override
	public OverlayContact getLocalOverlayContact() {
		return localContact;
	}

	@Override
	public void wentOffline(Host arg0, NetInterface arg1) {
		// Host is disconnected from network. For now: do nothing!

	}

	@Override
	public void wentOnline(Host arg0, NetInterface arg1) {
		// Host is connected to network. For now: do nothing!

	}

	@Override
	public void peerStatusChanged(OverlayComponent node, PeerStatus status) {
		// The status of the peer is changed
	}

	public Set<FloodingContact> getPotentialNeighbors() {
		return potentialNeighbors;
	}

	public Set<FloodingContact> getConnectedNeighbors() {
		return connectedNeighbors;
	}

	@Override
	public String toString() {
		return "[peer " + localContact.getNodeID() + "]";
	}

}
