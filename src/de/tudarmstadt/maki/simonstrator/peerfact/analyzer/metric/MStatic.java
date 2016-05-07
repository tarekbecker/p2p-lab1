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

import de.tudarmstadt.maki.simonstrator.api.Host;
import de.tudarmstadt.maki.simonstrator.api.common.metric.AbstractMetric;
import de.tudarmstadt.maki.simonstrator.api.common.metric.Metric.MetricValue;
import de.tudarmstadt.maki.simonstrator.api.util.XMLConfigurableConstructor;

/**
 * This metric is used whenever a static value is needed as an input for a
 * filter. It is always an overall metric.
 * 
 * @author Bjoern Richerzhagen
 * @version 1.0, 09.08.2012
 */
public abstract class MStatic extends AbstractMetric<MetricValue<Double>>
		implements MetricValue<Double> {

	private final double value;

	/**
	 * Creates a metric with a static value
	 * 
	 * @param value
	 */
	public MStatic(double value) {
		super("Static Value " + value, MetricUnit.NONE);
		this.value = value;
	}

	@Override
	public void initialize(List<Host> hosts) {
		setOverallMetric(this);
	}

	@Override
	public Double getValue() {
		return value;
	}

	@Override
	public boolean isValid() {
		return true;
	}

	/**
	 * Static time (configured with time="1h" for example)
	 * 
	 * @author Bjoern Richerzhagen
	 * @version 1.0, 09.08.2012
	 */
	public static class StaticTime extends MStatic {

		@XMLConfigurableConstructor({ "time" })
		public StaticTime(long time) {
			super(time);
		}
	}

	/**
	 * Static number (double)
	 * 
	 * @author Bjoern Richerzhagen
	 * @version 1.0, 09.08.2012
	 */
	public static class StaticNumber extends MStatic {

		@XMLConfigurableConstructor({ "value" })
		public StaticNumber(double value) {
			super(value);
		}
	}

}
