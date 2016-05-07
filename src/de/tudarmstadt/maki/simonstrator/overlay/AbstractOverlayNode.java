/*
 * Copyright (c) 2005-2011 KOM - Multimedia Communications Lab
 *
 * This file is part of PeerfactSim.KOM.
 * 
 * PeerfactSim.KOM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 * 
 * PeerfactSim.KOM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with PeerfactSim.KOM.  If not, see <http://www.gnu.org/licenses/>.
 *
 */


package de.tudarmstadt.maki.simonstrator.overlay;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.tudarmstadt.maki.simonstrator.api.Binder;
import de.tudarmstadt.maki.simonstrator.api.Host;
import de.tudarmstadt.maki.simonstrator.api.Monitor;
import de.tudarmstadt.maki.simonstrator.api.Monitor.Level;
import de.tudarmstadt.maki.simonstrator.api.component.ComponentNotAvailableException;
import de.tudarmstadt.maki.simonstrator.api.component.network.NetID;
import de.tudarmstadt.maki.simonstrator.api.component.network.NetInterface;
import de.tudarmstadt.maki.simonstrator.api.component.network.NetworkComponent;
import de.tudarmstadt.maki.simonstrator.api.component.overlay.IPeerStatusListener;
import de.tudarmstadt.maki.simonstrator.api.component.overlay.Serializer;
import de.tudarmstadt.maki.simonstrator.api.component.overlay.SerializerComponent;
import de.tudarmstadt.maki.simonstrator.api.component.transport.ConnectivityListener;
import de.tudarmstadt.maki.simonstrator.api.component.transport.ProtocolNotAvailableException;
import de.tudarmstadt.maki.simonstrator.api.component.transport.TransMessageListener;
import de.tudarmstadt.maki.simonstrator.api.component.transport.protocol.TCPMessageBased;
import de.tudarmstadt.maki.simonstrator.api.component.transport.protocol.UDP;
import de.tudarmstadt.maki.simonstrator.overlay.api.OverlayNode;

/**
 * This is the class all concrete overlay node classes should inherit from.
 * 
 */
public abstract class AbstractOverlayNode implements OverlayNode,
		ConnectivityListener {

	private PeerStatus peerStatus;

	private final Host host;

	private final Map<Integer, UDP> udps = new LinkedHashMap<Integer, UDP>();

	private final Map<Integer, TCPMessageBased> tcps = new LinkedHashMap<Integer, TCPMessageBased>();

	private final List<IPeerStatusListener> listeners = new LinkedList<IPeerStatusListener>();

	/**
	 * 
	 * @param host
	 */
	public AbstractOverlayNode(Host host) {
		this.host = host;
		this.peerStatus = PeerStatus.ABSENT;
	}

	@Override
	public void initialize() {
		try {
			NetworkComponent nets = host.getComponent(NetworkComponent.class);
			for (NetInterface net : nets.getNetworkInterfaces()) {
				net.addConnectivityListener(this);
			}
		} catch (ComponentNotAvailableException e) {
			Monitor.log(AbstractOverlayNode.class, Level.WARN,
					"I was not able to register the node as a ConnectivityListener!");
		}
	}

	@Override
	public void shutdown() {
		// default behavior - do nothing.
	}

	/**
	 * Convenience method that can be used during initialization to bind UDP on
	 * the given net and port. You still have to register a
	 * {@link TransMessageListener}!
	 * 
	 * @param localNetId
	 * @param port
	 * @return
	 * @throws ProtocolNotAvailableException
	 * @deprecated Please make use of the method with serializer instead!
	 */
	@Deprecated
	protected UDP getAndBindUDP(NetID localNetId, int port)
			throws ProtocolNotAvailableException {
		return getAndBindUDP(localNetId, port, null);
	}

	/**
	 * Convenience method that can be used during initialization to bind a TCP
	 * implementation on the given net and port. You still have to register a
	 * {@link TransMessageListener}!
	 * 
	 * @param localNetId
	 * @param port
	 * @return
	 * @throws ProtocolNotAvailableException
	 * @deprecated Please make use of the method with serializer instead!
	 */
	@Deprecated
	protected TCPMessageBased getAndBindTCP(NetID localNetId, int port)
			throws ProtocolNotAvailableException {
		return getAndBindTCP(localNetId, port, null);
	}

	/**
	 * Convenience method that can be used during initialization to bind UDP on
	 * the given net and port. You still have to register a
	 * {@link TransMessageListener}!
	 * 
	 * @param localNetId
	 * @param port
	 * @param serializer
	 *            an optional serializer. not used in simulations. Return null,
	 *            if not needed.
	 * @return
	 * @throws ProtocolNotAvailableException
	 */
	public UDP getAndBindUDP(NetID localNetId, int port,
			Serializer serializer)
			throws ProtocolNotAvailableException {
		UDP udp = udps.get(port);
		if (udp == null) {
			// Reg. Serializer first
			try {
				SerializerComponent sComp = Binder
						.getComponent(SerializerComponent.class);
				sComp.addSerializer(serializer, port);
			} catch (ComponentNotAvailableException e) {
				// fail silently in sim-environments
			}
			// Bind
			if (localNetId == null) {
				throw new ProtocolNotAvailableException();
			}
			udp = host.getTransportComponent().getProtocol(UDP.class,
					localNetId, port);
			assert udp != null;
			udps.put(port, udp);
		}
		return udp;
	}

	/**
	 * Convenience method that can be used during initialization to bind a TCP
	 * implementation on the given net and port. You still have to register a
	 * {@link TransMessageListener}!
	 * 
	 * @param localNetId
	 * @param port
	 * @param serializer
	 *            an optional serializer. not used in simulations. Return null,
	 *            if not needed.
	 * @return
	 * @throws ProtocolNotAvailableException
	 */
	public TCPMessageBased getAndBindTCP(NetID localNetId, int port,
			Serializer serializer)
			throws ProtocolNotAvailableException {
		TCPMessageBased tcp = tcps.get(port);
		if (tcp == null) {
			// Reg. Serializer first
			try {
				SerializerComponent sComp = Binder
						.getComponent(SerializerComponent.class);
				sComp.addSerializer(serializer, port);
			} catch (ComponentNotAvailableException e) {
				// fail silently in sim-environments
			}
			// Bind
			tcp = host.getTransportComponent().getProtocol(
					TCPMessageBased.class, localNetId, port);
			assert tcp != null;
			tcps.put(port, tcp);
		}
		return tcp;
	}

	/**
	 * Get information about the current status of the overlay of the peer.
	 * 
	 * Note: This does not give any information about the connectivity status of
	 * the network layer.
	 * 
	 * @return the current overlay status of the peer.
	 */
	@Override
	public PeerStatus getPeerStatus() {
		return peerStatus;
	}

	/**
	 * Set a new overlay status.
	 * 
	 * Hint: This should be done for example when a peer starts joining,
	 * finishes joining or disconnects.
	 * 
	 * @param peerStatus
	 *            the new overlay status of the peer
	 */
	public void setPeerStatus(PeerStatus peerStatus) {
		if (this.peerStatus == peerStatus) {
			return;
		}
		this.peerStatus = peerStatus;
		for (IPeerStatusListener l : listeners) {
			l.peerStatusChanged(this, peerStatus);
		}
	}

	@Override
	public String toString() {
		return "{host:" + host.getHostId() + " peerStatus="
				+ peerStatus + '}';
	}
	@Override
	public Host getHost() {
		return this.host;
	}

	@Override
	public boolean isPresent() {
		return getPeerStatus() == PeerStatus.PRESENT;
	}
	
	@Override
	public void addPeerStatusListener(IPeerStatusListener l) {
		listeners.add(l);
	}
	
	@Override
	public void removePeerStatusListener(IPeerStatusListener l) {
		listeners.remove(l);
	}

}
