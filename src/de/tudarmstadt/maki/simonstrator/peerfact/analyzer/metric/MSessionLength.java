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

package de.tudarmstadt.maki.simonstrator.peerfact.analyzer.metric;

import java.util.List;

import de.tud.kom.p2psim.api.common.SimHost;
import de.tudarmstadt.maki.simonstrator.api.Host;
import de.tudarmstadt.maki.simonstrator.api.Time;
import de.tudarmstadt.maki.simonstrator.api.common.metric.AbstractMetric;
import de.tudarmstadt.maki.simonstrator.api.common.metric.Metric.MetricValue;
import de.tudarmstadt.maki.simonstrator.api.component.network.NetInterface;
import de.tudarmstadt.maki.simonstrator.api.component.transport.ConnectivityListener;

/**
 * 
 * @author Bjoern Richerzhagen
 * @version 1.0, 09.10.2012
 */
public class MSessionLength extends AbstractMetric<MetricValue<Double>> {

	public MSessionLength() {
		super("MSessionLength", "Length of the peer's current online session",
				MetricUnit.TIME);
	}

	@Override
	public void initialize(List<Host> hosts) {
		for (Host host : hosts) {
			MVSessionLength mv = new MVSessionLength(host);
			addHost(host, mv);
			host.getNetworkComponent().getNetworkInterfaces().iterator().next()
					.addConnectivityListener(mv);
			// host.getProperties().addConnectivityListener(mv);
		}
	}

	/**
	 * 
	 * @author Bjoern Richerzhagen
	 * @version 1.0, 09.10.2012
	 */
	private class MVSessionLength implements MetricValue<Double>,
			ConnectivityListener {

		long sessionLength = 0;

		long timestampWentOnline;

		long timestampWentOffline;

		private final SimHost host;

		public MVSessionLength(Host host) {
			this.host = (SimHost) host;
		}

		@Override
		public Double getValue() {
			if (host.getNetworkComponent().getSimNetworkInterfaces().iterator()
					.next().isUp()) {
				return Double.valueOf(Time.getCurrentTime()
						- timestampWentOnline);
			} else {
				return Double.valueOf(sessionLength);
			}
		}

		@Override
		public boolean isValid() {
			return true;
		}

		@Override
		public void wentOffline(Host host, NetInterface netInterface) {
			timestampWentOffline = Time.getCurrentTime();
			sessionLength = timestampWentOffline - timestampWentOnline;
		}

		@Override
		public void wentOnline(Host host, NetInterface netInterface) {
			timestampWentOnline = Time.getCurrentTime();
		}
	}
}
