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
					Thread.sleep(1000 * 50);
					System.out.println("Start printing");
					for (FloodingNode fn : FloodingNode.allNodes) {
						StringBuilder sb = new StringBuilder(fn.getLocalOverlayContact().getNodeID().valueAsString());
						for(FloodingContact fc: fn.getConnectedNeighbors()) {
							sb.append(" ");
							sb.append(fc.getNodeID().valueAsString());
						}
						sb.append(String.format("\t\t[%2d %2d %2d]", fn.acceptIngoingConnections.availablePermits(),
								fn.establishOutgoingConnections.availablePermits(), fn.maxConnectionAttempts.availablePermits()));
						System.out.println(sb.toString());
					}
					System.out.println("finished");
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}).start();
		new de.tud.kom.p2psim.impl.util.guirunner.GUIRunner();
	}
}
