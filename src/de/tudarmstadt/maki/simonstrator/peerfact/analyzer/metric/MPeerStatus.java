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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.tudarmstadt.maki.simonstrator.api.Host;
import de.tudarmstadt.maki.simonstrator.api.common.metric.AbstractMetric;
import de.tudarmstadt.maki.simonstrator.api.common.metric.Metric.MetricValue;
import de.tudarmstadt.maki.simonstrator.api.component.ComponentNotAvailableException;
import de.tudarmstadt.maki.simonstrator.api.component.overlay.IPeerStatusListener;
import de.tudarmstadt.maki.simonstrator.api.component.overlay.OverlayComponent;
import de.tudarmstadt.maki.simonstrator.api.component.overlay.OverlayComponent.PeerStatus;
import de.tudarmstadt.maki.simonstrator.overlay.api.OverlayNode;

/**
 * Counts the number of active peers according to their peer-status. This works
 * on all overlays that implement {@link OverlayNode}.
 * 
 * @author Bjoern Richerzhagen
 * @version 1.0, 07.08.2012
 */
public abstract class MPeerStatus extends AbstractMetric<MetricValue<Double>>
		implements IPeerStatusListener {

	protected int active = 0;

	protected int maxActive = 0;

	protected Map<Long, PeerStatus> oldState = new LinkedHashMap<Long, PeerStatus>();

	public MPeerStatus(String description) {
		super(description, MetricUnit.NONE);
	}

	@Override
	public void initialize(List<Host> hosts) {

		for (Host host : hosts) {
			OverlayComponent node = null;
			try {
				node = host.getComponent(OverlayComponent.class);
			} catch (ComponentNotAvailableException e) {
				continue;
			}
			node.addPeerStatusListener(this);
			if (node.getPeerStatus() == PeerStatus.PRESENT) {
				active++;
				maxActive++;
				onActive(node);
			}
			oldState.put(host.getHostId(), node.getPeerStatus());
		}

		setOverallMetric(new MetricValue<Double>() {

			@Override
			public Double getValue() {
				return getCurrentValue();
			}

			@Override
			public boolean isValid() {
				return true;
			}
		});
	}

	protected abstract void onActive(OverlayComponent node);

	protected abstract void onInactive(OverlayComponent node);

	protected abstract Double getCurrentValue();

	@Override
	public void peerStatusChanged(OverlayComponent source, PeerStatus peerStatus) {
		if (oldState.get(source.getHost().getHostId()) != PeerStatus.PRESENT
				&& peerStatus == PeerStatus.PRESENT) {
			onActive(source);
			active++;
			if (active > maxActive) {
				maxActive = active;
			}
		}
		if (oldState.get(source.getHost().getHostId()) == PeerStatus.PRESENT
				&& peerStatus != PeerStatus.PRESENT) {
			onInactive(source);
			active--;
		}
		oldState.put(source.getHost().getHostId(), peerStatus);
	}

	/**
	 * Simple counter of currently present peers
	 * 
	 * @author Bjoern Richerzhagen
	 * @version 1.0, 25.08.2012
	 */
	public static class PeersPresent extends MPeerStatus {

		private Double present = Double.valueOf(0);

		public PeersPresent() {
			super("Number of peers currently present");
		}

		@Override
		protected void onActive(OverlayComponent node) {
			present++;
		}

		@Override
		protected void onInactive(OverlayComponent node) {
			present--;
			assert present >= 0;
		}

		@Override
		protected Double getCurrentValue() {
			return present;
		}

	}

	/**
	 * Simple counter of arriving peers. Together with a periodic delta filter,
	 * this can be used to plot arrival rates
	 * 
	 * @author Bjoern Richerzhagen
	 * @version 1.0, 25.08.2012
	 */
	public static class PeersArriving extends MPeerStatus {

		private Double arriving = Double.valueOf(0);

		public PeersArriving() {
			super("Number of peers that arrived up to now");
		}

		@Override
		protected void onActive(OverlayComponent node) {
			arriving++;
		}

		@Override
		protected void onInactive(OverlayComponent node) {
			// not used
		}

		@Override
		protected Double getCurrentValue() {
			return arriving;
		}
	}

}
