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

package de.tudarmstadt.maki.simonstrator.overlay.flooding.operations;

import java.util.Set;

import de.tudarmstadt.maki.simonstrator.api.Monitor;
import de.tudarmstadt.maki.simonstrator.api.operation.OperationCallback;
import de.tudarmstadt.maki.simonstrator.api.operation.PeriodicOperation;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.FloodingContact;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.FloodingNode;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.FloodingNodeFactory;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.FloodingSettings.Params;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.FloodingSettings.Times;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.messages.ConnectPeersMessage;

/**
 * This operation checks for new connections
 */
public class MaintenanceOperation extends
		PeriodicOperation<FloodingNode, Object> {

	private FloodingNode node;

	public MaintenanceOperation(FloodingNode node,
			OperationCallback<Object> callback) {
		super(node, callback, node.getSettings().getTime(
				Times.MAINTENANCE_INTERVAL));

		this.node = node;
	}

	@Override
	protected void executeOnce() {
		// System.out.println(node.toString() +
		// " Maintenance operation started");

		int maxNumConnections = node.getSettings().getParam(
				Params.MAX_NUM_CONNECTIONS);

		Set<FloodingContact> potentialNeighbors = node.getPotentialNeighbors();
		Set<FloodingContact> alreadyConnected = node.getConnectedNeighbors();

		// Remove all already connected neighbors
		potentialNeighbors.removeAll(alreadyConnected);

		//Monitor.log(MaintenanceOperation.class, Monitor.Level.DEBUG, node + ": Currently there are " + alreadyConnected.size()
		// + " peers connected, " + potentialNeighbors.size() + " potential neighbors.");

    node.setBudget(maxNumConnections);

		/*
		 * TODO Connect to neighbors that are not connected yet up to a maximum
		 * number of connections
		 */

    // Send a message to another peer
    while(node.maxConnectionAttempts.tryAcquire() && node.leaseOutgoingConnection()) {
        node.connectToSomeone();
    }
	}

	@Override
	public Object getResult() {
		return null;
	}


}
