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

package de.tudarmstadt.maki.simonstrator.peerfact.analyzer.common;

import java.util.LinkedList;
import java.util.List;

import de.tud.kom.p2psim.api.common.SimHost;
import de.tud.kom.p2psim.impl.util.db.dao.metric.MeasurementDAO;
import de.tud.kom.p2psim.impl.util.db.metric.MetricDescription;
import de.tud.kom.p2psim.impl.util.oracle.GlobalOracle;
import de.tudarmstadt.maki.simonstrator.api.Time;
import de.tudarmstadt.maki.simonstrator.api.component.ComponentNotAvailableException;
import de.tudarmstadt.maki.simonstrator.api.util.XMLConfigurableConstructor;
import de.tudarmstadt.maki.simonstrator.overlay.api.OverlayNode;
import de.tudarmstadt.maki.simonstrator.peerfact.analyzer.AbstractIntervalAnalyzer;

/**
 * This analyzer counts the peers that are PRESENT in their overlay (present is
 * usually defined as net.isOnline && peerStatus == PRESENT). To ease comparison
 * and to detect bugs in the overlay, it also monitors the online-status of the
 * netLayer.
 * 
 * @author Bjoern Richerzhagen
 * @version 1.0, 08.08.2012
 */
public class HostPresentAnalyzer extends AbstractIntervalAnalyzer {

	private List<OverlayNode> nodes;

	private static final MetricDescription metricPresent = new MetricDescription(
			HostPresentAnalyzer.class, "NODES_PRESENT",
			"number of present nodes", "");

	private static final MetricDescription metricOnline = new MetricDescription(
			HostPresentAnalyzer.class, "NODES_ONLINE",
			"number of online nodes",
			"");

	private final MeasurementDAO dao;

	private int present = 0;

	private int online = 0;

	private long time = Time.getCurrentTime();

	@XMLConfigurableConstructor({ "measurementInterval" })
	public HostPresentAnalyzer(long measurementInterval) {
		super(measurementInterval);
		dao = new MeasurementDAO();
	}

	@Override
	public void collectData() {
		for (OverlayNode node : nodes) {
			if (node.isPresent()) {
				present++;
			}
			if (((SimHost) node.getHost()).getNetworkComponent()
					.getSimNetworkInterfaces().iterator().next().isUp()) {
				online++;
			}
		}
		time = Time.getCurrentTime();
	}

	@Override
	public void initialize() {
		List<SimHost> hosts = GlobalOracle.getHosts();
		if (hosts == null || hosts.isEmpty()) {
			throw new AssertionError();
		}
		nodes = new LinkedList<OverlayNode>();
		for (SimHost host : hosts) {
			OverlayNode node;
			try {
				node = host.getComponent(OverlayNode.class);
				nodes.add(node);
			} catch (ComponentNotAvailableException e) {
				//
			}
		}
	}

	@Override
	public void resetCollectedData() {
		online = 0;
		present = 0;
	}

	@Override
	public void stop() {
		// not interested
	}

	@Override
	public void writeCollectedData() {
		dao.storeGlobalSingleMeasurement(metricOnline, time, online);
		dao.storeGlobalSingleMeasurement(metricPresent, time, present);
	}

}
