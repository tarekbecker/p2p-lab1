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

import java.io.Writer;
import java.util.List;

import de.tud.kom.p2psim.api.analyzer.LinklayerAnalyzer;
import de.tud.kom.p2psim.api.analyzer.TransportAnalyzer;
import de.tud.kom.p2psim.api.common.SimHost;
import de.tud.kom.p2psim.api.linklayer.LinkLayerMessage;
import de.tud.kom.p2psim.api.transport.TransMessage;
import de.tudarmstadt.maki.simonstrator.api.Host;
import de.tudarmstadt.maki.simonstrator.api.Message;
import de.tudarmstadt.maki.simonstrator.api.Monitor;
import de.tudarmstadt.maki.simonstrator.api.common.metric.AbstractMetric;
import de.tudarmstadt.maki.simonstrator.api.util.XMLConfigurableConstructor;
import de.tudarmstadt.maki.simonstrator.peerfact.analyzer.metric.MMessages.MessageMetricValue;

/**
 * Common metrics for messages on different layers (size, number). If you want
 * to compute a bandwidth, it might be easier to use the {@link MBandwidth}
 * -metric (especially when used with the new LinkLayer components). Instantiate
 * the inner classes in your config, the naming scheme for the metric is
 * ClassnameTypeReason or GClassnameTypeReason for global metrics.
 * 
 * @author Bjoern Richerzhagen
 * @version 1.0, 09.08.2012
 */
public abstract class MMessages extends AbstractMetric<MessageMetricValue>
		implements TransportAnalyzer, LinklayerAnalyzer {

	public enum AnalyzerType {
		LINK, NET, TRANS, OVERLAY;
	}

	private final AnalyzerType type;

	private final TransportAnalyzer.Reason monitorReason;

	private final boolean perHost;

	/**
	 * 
	 * @param name
	 *            name of the metric, global metrics will be prefixed with a "G"
	 * @param description
	 * @param unit
	 * @param type
	 *            LINK, NET, TRANS
	 * @param reason
	 *            SEND, RECEIVE, DROP
	 * @param perHost
	 *            if false, this is a global metric
	 */
	public MMessages(String name, String description, MetricUnit unit,
			AnalyzerType type, Reason reason, boolean perHost) {
		super(perHost ? name : "G" + name, description, unit);
		Monitor.registerAnalyzer(this);
		this.type = type;
		this.monitorReason = reason;
		this.perHost = perHost;
	}

	/**
	 * A metric value with a simple counter that can be incremented
	 * 
	 * @author Bjoern Richerzhagen
	 * @version 1.0, 14.08.2012
	 */
	public class MessageMetricValue
			implements
			de.tudarmstadt.maki.simonstrator.api.common.metric.Metric.MetricValue<Double> {

		private double value = 0;

		public MessageMetricValue() {
			//
		}

		@Override
		public Double getValue() {
			return value;
		}

		@Override
		public boolean isValid() {
			return value >= 0;
		}

		public void incValue(double inc) {
			this.value += inc;
		}

	}

	@Override
	public void initialize(List<Host> hosts) {
		if (perHost) {
			for (Host host : hosts) {
				addHost(host, createMessageMetricValue(host));
			}
		} else {
			setOverallMetric(createMessageMetricValue(null));
		}
	}

	/**
	 * Allows extending metrics to add more complex metric values.
	 * 
	 * @param host
	 *            or null, if global
	 * @return
	 */
	protected MessageMetricValue createMessageMetricValue(Host host) {
		return new MessageMetricValue();
	}

	/**
	 * 
	 * @param msg
	 * @param host
	 * @param reason
	 */
	private void messageEvent(Message msg, Host host, Reason reason) {
		if (reason == monitorReason) {
			if (perHost) {
				update(getPerHostMetric(host), msg, host);
			} else {
				update(getOverallMetric(), msg, null);
			}
		}
	}

	/**
	 * Called whenever a message is passed to the monitor that matches our
	 * filters.
	 * 
	 * @param mv
	 *            the MetricValue to update
	 * @param msg
	 *            the message
	 * @param host
	 *            the host or null, if overall metric
	 */
	protected abstract void update(MessageMetricValue mv, Message msg, Host host);

	@Override
	public void linkMsgEvent(LinkLayerMessage msg, SimHost host, Reason reason) {
		if (type == AnalyzerType.LINK) {
			messageEvent(msg, host, reason);
		}
	}

	@Override
	public final void transMsgEvent(TransMessage msg, SimHost host,
			Reason reason) {
		if (type == AnalyzerType.TRANS) {
			messageEvent(msg, host, reason);
		} else if (type == AnalyzerType.OVERLAY) {
			messageEvent(msg.getPayload(), host, reason);
		}
	}

	@Override
	public final void start() {
		// ignore, should use initialize instead.
	}

	@Override
	public final void stop(Writer output) {
		// ignore
	}

	/**
	 * Just a counter for SEND/RECEIVE/DROP messages on the given layer (type)
	 * 
	 * @author Bjoern Richerzhagen
	 * @version 1.0, 14.08.2012
	 */
	public static class Count extends MMessages {

		@XMLConfigurableConstructor({ "type", "reason", "perHost" })
		public Count(String type, String reason, boolean perHost) {
			super("Count" + type + reason, "Counts the number of messages on "
					+ type + " that are " + reason, MetricUnit.NONE,
					AnalyzerType.valueOf(type.toUpperCase()), Reason
							.valueOf(reason.toUpperCase()), perHost);
		}

		@Override
		protected void update(MessageMetricValue mv, Message msg, Host host) {
			// just a counter
			mv.incValue(1);
		}

	}

	/**
	 * Just a counter for SEND/RECEIVE/DROP messages on the given layer (type)
	 * 
	 * @author Bjoern Richerzhagen
	 * @version 1.0, 14.08.2012
	 */
	public static class Size extends MMessages {

		@XMLConfigurableConstructor({ "type", "reason", "perHost" })
		public Size(String type, String reason, boolean perHost) {
			super("Size" + type + reason, "Total message size for " + reason
					+ " on " + type, MetricUnit.DATA, AnalyzerType.valueOf(type
					.toUpperCase()), Reason.valueOf(reason.toUpperCase()),
					perHost);
		}

		@Override
		protected void update(MessageMetricValue mv, Message msg, Host host) {
			// message size
			mv.incValue(msg.getSize());
		}

	}

	/**
	 * Counter for a a given overlay Message type - specfied using the
	 * fully-qualifying class name. Support type hierarchies, i.e., you may
	 * specify a common base interface instead of each message class separately.
	 * 
	 * @author Bjoern Richerzhagen
	 * @version 1.0, Oct 26, 2013
	 */
	public static class CountType extends MMessages {

		private Class<?> msgClass;

		@XMLConfigurableConstructor({ "reason", "perHost", "type" })
		public CountType(String reason, boolean perHost, String type) {
			super("Count" + type.substring(type.lastIndexOf(".") + 1) + reason,
					"Total count for " + reason + " of "
							+ type.substring(type.lastIndexOf(".") + 1),
					MetricUnit.NONE,
					AnalyzerType.OVERLAY,
					Reason.valueOf(reason.toUpperCase()), perHost);
			// Find class
			try {
				msgClass = Class.forName(type);
			} catch (ClassNotFoundException e) {
				throw new AssertionError();
			}
		}

		@Override
		protected void update(MessageMetricValue mv, Message msg, Host host) {
			if (msgClass == null) {
				return;
			}
			if (msgClass.isAssignableFrom(msg.getClass())) {
				mv.incValue(1);
			}
		}
	}

	/**
	 * Message size aggregator for a single overlay message TYPE, identified by
	 * the simple class name. There is a magic name "AllOverlayMessages", that
	 * lets you count all messages arriving at the overlay layer. Furthermore,
	 * the message name may be an array of multiple messages, concatenated with
	 * a ";"
	 * 
	 * @author Bjoern Richerzhagen
	 * @version 1.0, Oct 26, 2013
	 */
	public static class SizeType extends MMessages {

		private Class<?> msgClass;

		@XMLConfigurableConstructor({ "reason", "perHost", "type" })
		public SizeType(String reason, boolean perHost, String type) {
			super("Size" + type.substring(type.lastIndexOf(".") + 1) + reason,
					"Total size for " + reason + " of "
							+ type.substring(type.lastIndexOf(".") + 1),
					MetricUnit.DATA,
					AnalyzerType.OVERLAY,
					Reason.valueOf(reason.toUpperCase()), perHost);
			// Find class
			try {
				msgClass = Class.forName(type);
			} catch (ClassNotFoundException e) {
				throw new AssertionError(e.getStackTrace());
			}
		}

		@Override
		protected void update(MessageMetricValue mv, Message msg, Host host) {
			if (msgClass == null) {
				return;
			}
			if (msgClass.isAssignableFrom(msg.getClass())) {
				mv.incValue(msg.getSize());
			}
		}
	}

}
