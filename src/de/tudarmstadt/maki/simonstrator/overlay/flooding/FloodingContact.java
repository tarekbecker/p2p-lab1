package de.tudarmstadt.maki.simonstrator.overlay.flooding;

import de.tudarmstadt.maki.simonstrator.api.common.graph.INodeID;
import de.tudarmstadt.maki.simonstrator.api.component.network.NetworkComponent.NetInterfaceName;
import de.tudarmstadt.maki.simonstrator.api.component.transport.TransInfo;
import de.tudarmstadt.maki.simonstrator.overlay.BasicOverlayContact;

public class FloodingContact extends BasicOverlayContact {

	private static final long serialVersionUID = 1L;

	public static enum ContactType {
		TRACKER, PEER
	}
	
	private final ContactType type;
	
	public FloodingContact(INodeID nodeID, TransInfo transInfo, ContactType nodeType) {
		super(nodeID, NetInterfaceName.ETHERNET, transInfo);
		this.type = nodeType;
	}

	/**
	 * A new TransitContact (not a tracker, not a source)
	 * 
	 * @param nodeID
	 * @param transInfo
	 */
	public FloodingContact(INodeID nodeID, TransInfo transInfo) {
		this(nodeID, transInfo, ContactType.PEER);
	}
	
	/**
	 * Type of this contact
	 * 
	 * @return
	 */
	public ContactType getType() {
		return type;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder("tC");
		str.append(String.format("%1$-" + 3 + "s", getNodeID().toString()));
		str.append(String.format("%1$-" + 8 + "s", type.toString()));
		str.append(" net: ");
		str.append(getTransInfo().toString());
		return str.toString();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((getNodeID() == null) ? 0 : getNodeID().hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		FloodingContact other = (FloodingContact) obj;
		if (getNodeID() == null) {
			if (other.getNodeID() != null) {
				return false;
			}
		} else if (!getNodeID().equals(other.getNodeID())) {
			return false;
		}
		return true;
	}

}
