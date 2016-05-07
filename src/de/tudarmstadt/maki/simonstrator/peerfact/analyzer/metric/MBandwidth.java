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
import de.tudarmstadt.maki.simonstrator.api.component.network.Bandwidth;

/**
 * Metric that provides the current max-Bandwidth that is available at each host
 * (upload and download), only measured for hosts that are online.
 * 
 * @author Bjoern Richerzhagen
 * @version 1.0, 08.08.2012
 */
public abstract class MBandwidth extends AbstractMetric<MetricValue<Double>> {

	private final boolean up;

	private final boolean max;

	public MBandwidth(String description, boolean up, boolean max) {
		super(description, MetricUnit.TRAFFIC);
		this.max = max;
		this.up = up;
	}

	@Override
	public void initialize(List<Host> hosts) {
		for (Host host : hosts) {
			Bandwidth bw = null;
			if (max) {
				bw = ((SimHost) host).getNetworkComponent()
						.getSimNetworkInterfaces().iterator().next()
						.getMaxBandwidth();
			} else {
				bw = ((SimHost) host).getNetworkComponent()
						.getSimNetworkInterfaces().iterator().next()
						.getCurrentBandwidth();
			}
			if (bw == null) {
				continue;
			} else {
				addHost(host,
						new MBandwidthMetricValue(((SimHost) host)
						.getNetworkComponent().getSimNetworkInterfaces()
						.iterator().next(), bw, up));
			}
		}
	}

	/**
	 * These metrics are considered valid, even if the host is not online. Use
	 * in conjunction with the {@link MHostOnline}-Metric to filter offline
	 * hosts!
	 * 
	 * Important: this metric assumes that the Bandwidth-Object does not change
	 * but is only updated with new values. This should be the case in all
	 * NetLayers, but if strange results are observed, consider checking this
	 * condition.
	 * 
	 * @author Bjoern Richerzhagen
	 * @version 1.0, 08.08.2012
	 */
	private class MBandwidthMetricValue implements MetricValue<Double> {

		private final boolean up;

		private final Bandwidth bw;

		private final SimNetInterface nl;

		public MBandwidthMetricValue(SimNetInterface nl, Bandwidth bw,
				boolean up) {
			this.up = up;
			this.bw = bw;
			this.nl = nl;
		}

		@Override
		public Double getValue() {
			if (up) {
				return bw.getUpBW();
			} else {
				return bw.getDownBW();
			}
		}

		@Override
		public String toString() {
			if (up) {
				return bw.getUpBW() / 1000 * 8 + " kbit/s";
			} else {
				return bw.getDownBW() / 1000 * 8 + " kbit/s";
			}
		}

		@Override
		public boolean isValid() {
			return nl.isUp();
		}

	}

	/**
	 * MAX upload BW (in a normal scenario, this will not change over time)
	 * 
	 * @author Bjoern Richerzhagen
	 * @version 1.0, 08.08.2012
	 */
	public static class UpMax extends MBandwidth {

		public UpMax() {
			super("Maximum upload bandwidth of a peer", true, true);
		}

        @Override
        protected String createName() {
			return "MBandwidthUpMax";
        }
	}

	/**
	 * Currently available (free) upload BW as returned by the NetLayer (not all
	 * NetLayers support this measurement!)
	 * 
	 * @author Bjoern Richerzhagen
	 * @version 1.0, 08.08.2012
	 */
	public static class Up extends MBandwidth {

		public Up() {
			super("Currently used upload Bandwidth", true, false);
		}

		@Override
		protected String createName() {
			return "MBandwidthUp";
		}
	}

	/**
	 * MAX download BW (in a normal scenario, this will not change over time)
	 * 
	 * @author Bjoern Richerzhagen
	 * @version 1.0, 08.08.2012
	 */
	public static class DownMax extends MBandwidth {

		public DownMax() {
			super("Maximum download bandwidth of a peer", false, true);
		}

        @Override
        protected String createName() {
			return "MBandwidthDownMax";
        }
    }

	/**
	 * Currently available (free) download BW as returned by the NetLayer (not
	 * all NetLayers support this measurement!)
	 * 
	 * @author Bjoern Richerzhagen
	 * @version 1.0, 08.08.2012
	 */
	public static class Down extends MBandwidth {

		public Down() {
			super("Currently used download Bandwidth", false, false);
		}

		@Override
		protected String createName() {
			return "MBandwidthDown";
		}
	}

}
