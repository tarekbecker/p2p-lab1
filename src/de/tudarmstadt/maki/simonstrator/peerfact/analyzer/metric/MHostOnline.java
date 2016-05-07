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
import de.tud.kom.p2psim.api.network.SimNetInterface;
import de.tudarmstadt.maki.simonstrator.api.Host;
import de.tudarmstadt.maki.simonstrator.api.common.metric.AbstractMetric;
import de.tudarmstadt.maki.simonstrator.api.common.metric.Metric.MetricValue;

/**
 * Per-Host metric that defines if the host is online or not. Not to be confused
 * with {@link MHostsOnline}, which is a numerical group metric that counts the
 * number of online hosts.
 * 
 * @author Bjoern Richerzhagen
 * @version 1.0, 08.08.2012
 */
public class MHostOnline extends AbstractMetric<MetricValue<Boolean>> {

	public MHostOnline() {
		super("Boolean showing if a host is currently online", MetricUnit.NONE);
	}

	@Override
	public void initialize(List<Host> hosts) {
		for (Host host : hosts) {
			addHost(host, new HostOnlineMetricValue(((SimHost) host)
					.getNetworkComponent().getSimNetworkInterfaces().iterator()
					.next()));
		}
	}

	/**
	 * 
	 * @author Bjoern Richerzhagen
	 * @version 1.0, 08.08.2012
	 */
	private class HostOnlineMetricValue implements MetricValue<Boolean> {

		private final SimNetInterface net;

		public HostOnlineMetricValue(SimNetInterface net) {
			this.net = net;
		}

		@Override
		public Boolean getValue() {
			return net.isOnline();
		}

		@Override
		public boolean isValid() {
			return true;
		}
	}
}
