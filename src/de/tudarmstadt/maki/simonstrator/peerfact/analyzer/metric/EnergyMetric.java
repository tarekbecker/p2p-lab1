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

import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.tud.kom.p2psim.api.analyzer.EnergyAnalyzer;
import de.tud.kom.p2psim.api.common.SimHost;
import de.tud.kom.p2psim.api.energy.EnergyComponent;
import de.tudarmstadt.maki.simonstrator.api.Host;
import de.tudarmstadt.maki.simonstrator.api.Monitor;
import de.tudarmstadt.maki.simonstrator.api.Time;
import de.tudarmstadt.maki.simonstrator.api.common.metric.AbstractMetric;
import de.tudarmstadt.maki.simonstrator.api.common.metric.Metric;
import de.tudarmstadt.maki.simonstrator.peerfact.analyzer.AbstractEnergyAnalyzer;

/**
 * @author Fabio Zöllner
 * @version 1.0, 25.01.13
 */
public class EnergyMetric extends AbstractMetric<Metric.MetricValue<Double>>
		implements EnergyAnalyzer {
	private Map<SimHost, EnergyValue> hostValueMap = new LinkedHashMap<SimHost, EnergyValue>();

    public EnergyMetric() {
        super("Average consumed energy", MetricUnit.NONE);
		// DefaultMonitor.getInstance().setAnalyzer(this);
		Monitor.registerAnalyzer(this);
    }

    @Override
	public void initialize(List<Host> hosts) {
		for (Host host : hosts) {
            EnergyValue energyValue = new EnergyValue();
			hostValueMap.put((SimHost) host, energyValue);
            addHost(host, energyValue);
        }
    }

    @Override
	public void consumeEnergy(SimHost host, double energy,
			EnergyComponent consumer) {
        EnergyValue value = hostValueMap.get(host);
        if (value != null) {
            value.energyConsumed(energy);
        }
    }

    private class EnergyValue extends AbstractEnergyAnalyzer implements MetricValue<Double> {
        private long lastConsumptionSecond = 0;
        private double consumedEnergy = 0;

        public EnergyValue() {

        }

        @Override
        public Double getValue() {
            return consumedEnergy / 1000000.0;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        public void energyConsumed(double energy) {
			long currentSecond = Time.getCurrentTime() / Time.SECOND;
            if (currentSecond != lastConsumptionSecond) {
                lastConsumptionSecond = currentSecond;
                consumedEnergy = 0;
            }
            consumedEnergy += energy;
        }
    }

    @Override
    public void batteryIsEmpty(SimHost host) { }

    @Override
	public void highPowerMode(SimHost host, long time, double consumedEnergy,
			EnergyComponent component) {
	}

    @Override
	public void lowPowerMode(SimHost host, long time, double consumedEnergy,
			EnergyComponent component) {
	}

    @Override
	public void tailMode(SimHost host, long time, double consumedEnergy,
			EnergyComponent component) {
	}

    @Override
	public void offMode(SimHost host, long time, double consumedEnergy,
			EnergyComponent component) {
	}

    @Override
    public void start() { }

    @Override
    public void stop(Writer output) { }
}
