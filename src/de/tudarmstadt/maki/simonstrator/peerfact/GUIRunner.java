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


package de.tudarmstadt.maki.simonstrator.peerfact;


import de.tudarmstadt.maki.simonstrator.overlay.flooding.FloodingContact;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.FloodingNode;

/**
 * Starts a window to select a configuration file to run PFS from. Useful for
 * developers switching to different configuration files many times, as well as
 * for presentations etc.
 * 
 * See <a href="http://www.student.informatik.tu-darmstadt.de/~l_nobach/docs/howto-visualization.pdf"
 * >PeerfactSim.KOM Visualization HOWTO</a> on how to use it.
 * 
 * @author Leo Nobach
 * @version 3.0, 25.11.2008
 * 
 */
public class GUIRunner {
	public static void main(String[] args) {
		System.setProperty("logfile.name", "simguilog.log");
		(new Thread() {
			@Override
			public synchronized void run() {
				try {
					Thread.sleep(1000 * 10);
					System.out.println("Start printing");
					long total = 0;
					for (FloodingNode fn : FloodingNode.allNodes) {
						total += fn.connectedNeighbors.size();
						StringBuilder sb = new StringBuilder();
						sb.append(String.format("%2d", fn.getLocalOverlayContact().getNodeID().value()));
						sb.append(":");
						for(FloodingContact fc: fn.getConnectedNeighbors()) {
							sb.append(" ");
							sb.append(String.format("%2d", fc.getNodeID().value()));
						}
						System.out.printf("%-20s %s (%d)\n", sb.toString(), String.format("\t\t[%02d (%02d) %02d (%02d) %03d]",
								fn.acceptIngoingConnections.availablePermits(),
								fn.acceptIngoingConnectionsAccepted.availablePermits(),
								fn.establishOutgoingConnections.availablePermits(),
								fn.establishOutgoingConnectionsAccepted.availablePermits(),
								fn.maxConnectionAttempts.availablePermits()),
								fn.connectedNeighbors.size());
					}
					System.out.printf("Average number of connections: %.3f\n", (((double) total) / FloodingNode.allNodes.size()));
					System.out.println("finished");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
		new de.tud.kom.p2psim.impl.util.guirunner.GUIRunner();
	}
}
