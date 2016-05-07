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

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import de.tud.kom.p2psim.api.analyzer.EnergyAnalyzer;
import de.tud.kom.p2psim.api.common.SimHost;
import de.tud.kom.p2psim.api.energy.EnergyComponent;
import de.tud.kom.p2psim.impl.util.db.dao.metric.MeasurementDAO;
import de.tud.kom.p2psim.impl.util.db.metric.MetricDescription;
import de.tud.kom.p2psim.impl.util.oracle.GlobalOracle;
import de.tudarmstadt.maki.simonstrator.api.Event;
import de.tudarmstadt.maki.simonstrator.api.EventHandler;
import de.tudarmstadt.maki.simonstrator.api.Time;

public class EnergyStatisticAnalyzer implements EnergyAnalyzer, EventHandler {

	private static final MetricDescription ENERGY_LEVEL_PERCENTAGE = new MetricDescription(
            EnergyStatisticAnalyzer.class,
			"ENERGY_LEVEL_PERCENTAGE",
			"The energy level of every host, in percentage", "%");

	private static final MetricDescription CONSUMED_ENERGY = new MetricDescription(
            EnergyStatisticAnalyzer.class,
			"CONSUMED_ENERGY",
			"The consumend energy since the start of this simulation", "Joule");

	private static final MetricDescription EMPTY_BATTERY_TIME = new MetricDescription(
            EnergyStatisticAnalyzer.class,
			"EMPTY_BATTERY_TIME",
			"The simulation time, wherever the battry is discharged", "µs");

	private static final String ENERGY_CONSUMER_UNIT = "µJ";

	private static final String ENERGY_CONSUMER_COMMENT = "Contains the Energy consum for the consumer. The consumer is described in the column name.";

	private static final String ENERGY_CONSUMER_PREFIX = "ENERGY_CONSUMER_";

	private static final String TIME_CONSUMER_UNIT = "µs per interval";

	private static final String TIME_CONSUMER_COMMENT = "Describes the consumers time in the specific mode. The time is specified per interval.";

	private static final String TIME_CONSUMER_PREFIX = "TIME_CONSUMER_";

	private static final String TAIL_INFIX = "TAIL_";

	private static final String HIGH_POWER_INFIX = "HIGH_POWER_";

	private static final String LOW_POWER_INFIX = "LOW_POWER_";

	private static final String OFF_INFIX = "OFF_";

	private long measurementInterval;

	/*
	 * Time maps
	 */

	// <host, consumer, duration>
	private Map<SimHost, Map<String, Long>> tailTimeMap = new HashMap<SimHost, Map<String, Long>>();

	// <host, consumer, duration>
	private Map<SimHost, Map<String, Long>> highPowerTimeMap = new HashMap<SimHost, Map<String, Long>>();

	// <host, consumer, duration>
	private Map<SimHost, Map<String, Long>> lowPowerTimeMap = new HashMap<SimHost, Map<String, Long>>();

	// <host, consumer, duration>
	private Map<SimHost, Map<String, Long>> offTimeMap = new HashMap<SimHost, Map<String, Long>>();

	/*
	 * Energy Maps
	 */
	// <host, consumer, consumed Energy>
	private Map<SimHost, Map<String, Double>> tailEnergyMap = new HashMap<SimHost, Map<String, Double>>();

	// <host, consumer, consumed Energy>
	private Map<SimHost, Map<String, Double>> highPowerEnergyMap = new HashMap<SimHost, Map<String, Double>>();

	// <host, consumer, consumed Energy>
	private Map<SimHost, Map<String, Double>> lowPowerEnergyMap = new HashMap<SimHost, Map<String, Double>>();

	// <host, consumer, consumed Energy>
	private Map<SimHost, Map<String, Double>> hostConsumerEnergyMap = new HashMap<SimHost, Map<String, Double>>();

	// <host, consumer, consumed Energy>
	private Map<SimHost, Map<String, Double>> offEnergyMap = new HashMap<SimHost, Map<String, Double>>();

	private boolean active = false;

	@Override
	public void start() {
		this.active = true;
		Event.scheduleImmediately(this, null, 0);
	}

	@Override
	public void stop(Writer output) {
		this.active = false;

	}

	@Override
	public void batteryIsEmpty(SimHost host) {
		if (active) {
			long id = host.getHostId();
			new MeasurementDAO().storeSingleMeasurement(EMPTY_BATTERY_TIME, id,
					Time.getCurrentTime(), Time.getCurrentTime());
		}
	}

	@Override
	public void eventOccurred(Object content, int type) {
		if (active) {
			Map<Long, Double> hostEnergyLevel = collectEnergyLevel();
			Map<Long, Double> hostConsumedEnergy = collectConsumedEnergy();
			MeasurementDAO measurementDAO = new MeasurementDAO();
			for (Long hostId : hostEnergyLevel.keySet()) {
				Double energyLevel = hostEnergyLevel.get(hostId);
				measurementDAO.storeSingleMeasurement(ENERGY_LEVEL_PERCENTAGE,
						hostId, Time.getCurrentTime(), energyLevel);
			}
			for (Long hostId : hostEnergyLevel.keySet()) {
				Double consumedEnergy = hostConsumedEnergy.get(hostId);
				measurementDAO.storeSingleMeasurement(CONSUMED_ENERGY, hostId,
						Time.getCurrentTime(), consumedEnergy);
			}

			writeHostConsumerMap(hostConsumerEnergyMap, ENERGY_CONSUMER_PREFIX,
					ENERGY_CONSUMER_COMMENT, ENERGY_CONSUMER_UNIT,
					measurementDAO);
			writeHostConsumerMap(tailEnergyMap, ENERGY_CONSUMER_PREFIX
					+ TAIL_INFIX, ENERGY_CONSUMER_COMMENT,
					ENERGY_CONSUMER_UNIT, measurementDAO);
			writeHostConsumerMap(highPowerEnergyMap, ENERGY_CONSUMER_PREFIX
					+ HIGH_POWER_INFIX, ENERGY_CONSUMER_COMMENT,
					ENERGY_CONSUMER_UNIT, measurementDAO);
			writeHostConsumerMap(lowPowerEnergyMap, ENERGY_CONSUMER_PREFIX
					+ LOW_POWER_INFIX, ENERGY_CONSUMER_COMMENT,
					ENERGY_CONSUMER_UNIT, measurementDAO);
			writeHostConsumerMap(offEnergyMap, ENERGY_CONSUMER_PREFIX
					+ OFF_INFIX, ENERGY_CONSUMER_COMMENT,
					ENERGY_CONSUMER_UNIT, measurementDAO);

			writeHostConsumerMap(tailTimeMap,
					TIME_CONSUMER_PREFIX + TAIL_INFIX, TIME_CONSUMER_COMMENT,
					TIME_CONSUMER_UNIT, measurementDAO);
			writeHostConsumerMap(highPowerTimeMap, TIME_CONSUMER_PREFIX
					+ HIGH_POWER_INFIX, TIME_CONSUMER_COMMENT,
					TIME_CONSUMER_UNIT, measurementDAO);
			writeHostConsumerMap(lowPowerTimeMap, TIME_CONSUMER_PREFIX
					+ LOW_POWER_INFIX, TIME_CONSUMER_COMMENT,
					TIME_CONSUMER_UNIT, measurementDAO);
			writeHostConsumerMap(offTimeMap, TIME_CONSUMER_PREFIX + OFF_INFIX,
					TIME_CONSUMER_COMMENT, TIME_CONSUMER_UNIT, measurementDAO);

			reset();
			Event.scheduleWithDelay(measurementInterval, this, null, 0);
		}
	}

	private <T extends Number> void writeHostConsumerMap(
			Map<SimHost, Map<String, T>> map, String namePrefix, String comment,
			String unit, MeasurementDAO measurementDAO) {
		for (SimHost host : map.keySet()) {
			Map<String, T> consumerMap = map.get(host);
			Long hostId = host.getHostId();
			for (String consumer : consumerMap.keySet()) {
				T consumed = consumerMap.get(consumer);
				MetricDescription description = new MetricDescription(
                        EnergyStatisticAnalyzer.class,
						namePrefix + consumer, comment, unit);

				measurementDAO.storeSingleMeasurement(description, hostId,
						Time.getCurrentTime(), consumed.doubleValue());
			}
		}
	}

	private void reset() {
		highPowerTimeMap.clear();
		lowPowerTimeMap.clear();
		tailTimeMap.clear();
		offTimeMap.clear();

		highPowerEnergyMap.clear();
		lowPowerEnergyMap.clear();
		tailEnergyMap.clear();
		offEnergyMap.clear();
		hostConsumerEnergyMap.clear();
	}

	private Map<Long, Double> collectEnergyLevel() {
		Map<Long, Double> result = new HashMap<Long, Double>();
		for (SimHost host : GlobalOracle.getHosts()) {
			if (host.getEnergyModel() != null) {
				result.put(host.getHostId(), host.getEnergyModel().getInfo()
						.getCurrentPercentage());
			}
		}
		return result;
	}

	private Map<Long, Double> collectConsumedEnergy() {
		Map<Long, Double> result = new HashMap<Long, Double>();
		for (SimHost host : GlobalOracle.getHosts()) {
			if (host.getEnergyModel() != null) {
				result.put(host.getHostId(), host.getEnergyModel().getInfo()
						.getBattery().getConsumedEnergy());
			}
		}
		return result;
	}

	// ************************************
	// Setting and preparing the analyzer
	// ************************************

	public void setMeasurementInterval(long timeInterval) {
		this.measurementInterval = timeInterval;
	}

	@Override
	public void consumeEnergy(SimHost host, double energy,
			EnergyComponent consumer) {
		addEnergyMap(hostConsumerEnergyMap, host, energy, consumer);
	}

	@Override
	public void highPowerMode(SimHost host, long time, double consumedEnergy,
			EnergyComponent component) {
		addTimeMap(highPowerTimeMap, host, time, component);
		addEnergyMap(highPowerEnergyMap, host, consumedEnergy, component);
	}

	@Override
	public void lowPowerMode(SimHost host, long time, double consumedEnergy,
			EnergyComponent component) {
		addTimeMap(lowPowerTimeMap, host, time, component);
		addEnergyMap(lowPowerEnergyMap, host, consumedEnergy, component);
	}

	@Override
	public void tailMode(SimHost host, long time, double consumedEnergy,
			EnergyComponent component) {
		addTimeMap(tailTimeMap, host, time, component);
		addEnergyMap(tailEnergyMap, host, consumedEnergy, component);
	}

	@Override
	public void offMode(SimHost host, long time, double consumedEnergy,
			EnergyComponent component) {
		addTimeMap(offTimeMap, host, time, component);
		addEnergyMap(offEnergyMap, host, consumedEnergy, component);
	}

	/**
	 * Add to the timeMaps like Map<Host, Map<String, Long>> -> <Host,
	 * <Component, time>>, the given time. This mean, if not exist in the map,
	 * then will be added otherwise will be add to the old value.
	 * 
	 * @param map
	 *            The Map like Map<Host, Map<String, Long>>
	 * @param host
	 *            A host
	 * @param time
	 *            The time, which should be added.
	 * @param component
	 *            The description for the component.
	 */
	private void addTimeMap(Map<SimHost, Map<String, Long>> map, SimHost host,
			long time, EnergyComponent component) {
		if (!map.containsKey(host)) {
			map.put(host, new HashMap<String, Long>());
		}
		Map<String, Long> internMap = map.get(host);

		if (!internMap.containsKey(component)) {
			internMap.put(component.toString(), 0l);
		}
		long newTime = time + internMap.get(component);
		internMap.put(component.toString(), newTime);
	}

	private void addEnergyMap(Map<SimHost, Map<String, Double>> map, SimHost host,
 double energy, EnergyComponent component) {
		if (!map.containsKey(host)) {
			map.put(host, new HashMap<String, Double>());
		}
		Map<String, Double> internMap = map.get(host);

		if (!internMap.containsKey(component)) {
			internMap.put(component.toString(), 0.0);
		}
		double newEnergy = energy + internMap.get(component);
		internMap.put(component.toString(), newEnergy);
	}
}
