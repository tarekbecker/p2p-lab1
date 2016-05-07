/*
 * Copyright (c) 2005-2010 KOM – Multimedia Communications Lab
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

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import de.tudarmstadt.maki.simonstrator.api.common.graph.INodeID;
import de.tudarmstadt.maki.simonstrator.api.component.network.NetID;
import de.tudarmstadt.maki.simonstrator.api.component.network.NetworkComponent.NetInterfaceName;
import de.tudarmstadt.maki.simonstrator.api.component.overlay.OverlayContact;
import de.tudarmstadt.maki.simonstrator.api.component.transport.TransInfo;

/**
 * Base for Overlay-Contacts
 * 
 * @author Bjoern Richerzhagen
 * @version 1.0, 07/07/2011
 */
public class BasicOverlayContact implements OverlayContact, Serializable {

	private static final long serialVersionUID = 1L;

	private INodeID nodeId;

	private Map<NetInterfaceName, TransInfo> transInfos = new LinkedHashMap<>();

	private transient int _cachedSize = -1;

	@SuppressWarnings("unused")
	private BasicOverlayContact() {
		// for Kryo
	}

	/**
	 * Using the {@link INodeID} of the host
	 * 
	 * @param nodeId
	 */
	public BasicOverlayContact(INodeID nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Convenience constructor contacts with only one netInterface
	 * 
	 * @param nodeID
	 * @param transInfo
	 */
	public BasicOverlayContact(INodeID nodeId, NetInterfaceName netName,
			TransInfo transInfo) {
		this.nodeId = nodeId;
		this.transInfos.put(netName, transInfo);
	}

	/**
	 * @deprecated explicitly specify the NetInterfaceName Instead!
	 * @param nodeID
	 * @param transInfo
	 */
	@Deprecated
	public BasicOverlayContact(INodeID nodeID, TransInfo transInfo) {
		this(nodeID, NetInterfaceName.ETHERNET, transInfo);
	}
	/**
	 * Add a transInfo to this contact. Only one info per NetName is stored -
	 * existing entries are replaced by new ones if the netName is already in
	 * use.
	 * 
	 * @param netName
	 * @param transInfo
	 * @return
	 */
	public BasicOverlayContact addTransInfo(NetInterfaceName netName,
			TransInfo transInfo) {
		this.transInfos.put(netName, transInfo);
		_cachedSize = -1;
		return this;
	}

	/**
	 * Removes the TransInfo for the given NetName
	 * 
	 * @param netName
	 * @return
	 */
	public BasicOverlayContact removeTransInfo(NetInterfaceName netName) {
		this.transInfos.remove(netName);
		_cachedSize = -1;
		return this;
	}

	@Override
	public int getTransmissionSize() {
		if (_cachedSize == -1) {
			_cachedSize += nodeId.getTransmissionSize();
			for (TransInfo transInfo : transInfos.values()) {
				_cachedSize += transInfo.getTransmissionSize();
			}
		}
		return _cachedSize;
	}

	@Override
	public INodeID getNodeID() {
		return nodeId;
	}

	@Override
	public NetID getNetID(NetInterfaceName netInterface) {
		if (!transInfos.containsKey(netInterface)) {
			return null;
		}
		return transInfos.get(netInterface).getNetId();
	}

	@Override
	public int getPort(NetInterfaceName netInterface) {
		if (!transInfos.containsKey(netInterface)) {
			return -1;
		}
		return transInfos.get(netInterface).getPort();
	}

	@Override
	public TransInfo getTransInfo() {
		if (transInfos.size() != 1) {
			throw new AssertionError();
		}
		for (TransInfo transInfo : transInfos.values()) {
			return transInfo;
		}
		throw new AssertionError();
	}

	@Override
	public NetID getNetID() {
		if (transInfos.size() != 1) {
			throw new AssertionError();
		}
		for (TransInfo transInfo : transInfos.values()) {
			return transInfo.getNetId();
		}
		throw new AssertionError();
	}

	@Override
	public String toString() {
		return "Contact: " + transInfos.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BasicOverlayContact other = (BasicOverlayContact) obj;
		if (nodeId == null) {
			if (other.nodeId != null)
				return false;
		} else if (!nodeId.equals(other.nodeId))
			return false;
		return true;
	}

}
