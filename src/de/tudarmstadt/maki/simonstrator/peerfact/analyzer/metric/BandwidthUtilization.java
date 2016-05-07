/*
 * Copyright (c) 2005-2013 KOM - Multimedia Communications Lab
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
 */

package de.tudarmstadt.maki.simonstrator.peerfact.analyzer.metric;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import de.tud.kom.p2psim.api.common.SimHost;
import de.tud.kom.p2psim.api.network.NetMessageEvent;
import de.tud.kom.p2psim.api.network.NetMessageListener;
import de.tud.kom.p2psim.api.network.SimNetInterface;
import de.tud.kom.p2psim.api.network.SimNetworkComponent;
import de.tudarmstadt.maki.simonstrator.api.Host;
import de.tudarmstadt.maki.simonstrator.api.common.metric.AbstractMetric;
import de.tudarmstadt.maki.simonstrator.api.common.metric.Metric;

/**
 * @author Fabio Zöllner
 * @version 1.0, 22.01.13
 */
public class BandwidthUtilization extends AbstractMetric<Metric.MetricValue<Double>> {

    public BandwidthUtilization() {
        super("Bandwidth utilization", MetricUnit.TRAFFIC);
    }

    @Override
	public void initialize(List<Host> hosts) {
		for (Host host : hosts) {
			addHost(host,
					new BandwidthUtilizationValue(((SimHost) host)
							.getNetworkComponent()));
        }
    }

    private class BandwidthUtilizationValue implements MetricValue<Double>,NetMessageListener {
        private final int DATA_POINTS = 50;
        private Queue<Long> messageSizes = new LinkedList<Long>();
        private int size = 0;

		public BandwidthUtilizationValue(SimNetworkComponent netLayer) {
			for (SimNetInterface netI : netLayer.getSimNetworkInterfaces()) {
				netI.addNetMsgListener(this);
			}
        }

        @Override
        public Double getValue() {
            return (double)size;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public void messageArrived(NetMessageEvent nme) {
            if (messageSizes.size() >= DATA_POINTS) {
                messageSizes.add(nme.getPayload().getSize());
                size += nme.getPayload().getSize();
                Long poll = messageSizes.poll();
                size -= poll;
            } else {
                messageSizes.add(nme.getPayload().getSize());
                size += nme.getPayload().getSize();
            }
        }
    }
}