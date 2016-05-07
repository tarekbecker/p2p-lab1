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

package de.tudarmstadt.maki.simonstrator.peerfact.analyzer;

import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.tud.kom.p2psim.api.analyzer.NetlayerAnalyzer;
import de.tud.kom.p2psim.api.analyzer.OperationAnalyzer;
import de.tud.kom.p2psim.api.common.SimHost;
import de.tud.kom.p2psim.api.network.NetMessage;
import de.tud.kom.p2psim.impl.util.db.dao.DAO;
import de.tud.kom.p2psim.impl.util.db.dao.metric.MeasurementDAO;
import de.tud.kom.p2psim.impl.util.db.metric.MetricDescription;
import de.tud.kom.p2psim.impl.util.oracle.GlobalOracle;
import de.tudarmstadt.maki.simonstrator.api.Event;
import de.tudarmstadt.maki.simonstrator.api.EventHandler;
import de.tudarmstadt.maki.simonstrator.api.Message;
import de.tudarmstadt.maki.simonstrator.api.Time;
import de.tudarmstadt.maki.simonstrator.api.operation.Operation;
import de.tudarmstadt.maki.simonstrator.overlay.AbstractOverlayMessage;
import de.tudarmstadt.maki.simonstrator.peerfact.analyzer.operation.OperationRelatedInfo;

/**
 * This Analyzer acts as an {@link OperationAnalyzer} and furthermore fetches
 * all messages related to the Operation via the {@link NetAnalyzer} interface.
 * To prevent logging of duplicate information, you should not extend this
 * class. Add your analyzer as an {@link OperationListener} instead. Most
 * commonly used metrics will be provided for free this way.
 * 
 * @author Bjoern Richerzhagen
 * @version 1.0, 30.03.2012
 */
public class OverlayOperationAnalyzer implements OperationAnalyzer,
		NetlayerAnalyzer, EventHandler {

	/**
	 * Directory of all currently active operations that are tracked by this
	 * analyzer.
	 */
	private Map<Integer, List<OperationRelatedInfo>> operations = new HashMap<Integer, List<OperationRelatedInfo>>();

	/**
	 * New Listener-Concept for this analyzer to allow multiple Operations to be
	 * analyzed in parallel. Just pass your analyzer as a Listener and get most
	 * metrics for free.
	 */
	private List<OperationListener> listeners = new LinkedList<OperationListener>();

	private Map<SimHost, HostRelatedInfo> hostInfos = new HashMap<SimHost, OverlayOperationAnalyzer.HostRelatedInfo>();

	private long intervalLength = 1 * Time.HOUR;

	private boolean intervalLengthStored = false;

	/**
	 * Is this analyzer active?
	 */
	private boolean running = false;

	boolean listOperationMode = false;

	/**
	 * Length of the interval between periodic metrics such as message count per
	 * host
	 * 
	 * @param measurementInterval
	 */
	public void setMeasurementInterval(long measurementInterval) {
		this.intervalLength = measurementInterval;
	}

	/**
	 * Add a new Listener (done via the configuration file)
	 * 
	 * @param listener
	 */
	public void setOperationListener(OperationListener<?> listener) {
		this.listeners.add(listener);
	}

	@Override
	public void start() {
		for (SimHost host : GlobalOracle.getHosts()) {
			hostInfos.put(host, new HostRelatedInfo(host.getHostId(),
					intervalLength));
		}

		running = true;
		Event.scheduleWithDelay(intervalLength, this, this, 0);
	}

	@Override
	public void eventOccurred(Object se, int type) {
		intervalEnded();
		Event.scheduleWithDelay(intervalLength, this, this, 0);
	}

	/**
	 * Called after intervalLength, used to write aggregated (per-host) metrics
	 * into the DB (for example traffic)
	 */
	protected void intervalEnded() {
		if (!intervalLengthStored) {
			MeasurementDAO dao = new MeasurementDAO();
			dao.storeGlobalSingleMeasurement(new MetricDescription(
					OverlayOperationAnalyzer.class, "INTERVAL_LENGTH",
					"Length of an Interval in seconds", "second"), Time
					.getCurrentTime(), intervalLength / Time.SECOND);
			intervalLengthStored = true;
		}

		for (HostRelatedInfo hostInfo : hostInfos.values()) {
			for (OperationListener<?> opListener : listeners) {
				opListener.writeHostRelatedInfo(hostInfo);
				hostInfo.writeHostRelatedInfo();
			}
		}
		// commit the queue
		DAO.commitQueue();
	}

	/**
	 * Is the analyzer running?
	 * 
	 * @return
	 */
	protected boolean isRunning() {
		return running;
	}

	/**
	 * Register each Analyzer that should act as an {@link OperationListener}
	 * with this {@link OverlayOperationAnalyzer} to add custom Metrics and to
	 * rely on base metrics collected by this class.
	 * 
	 * @author Bjoern Richerzhagen
	 * @version 1.0, 11.04.2012
	 * @param <I>
	 */
	public interface OperationListener<I extends OperationRelatedInfo> {

		/**
		 * Each {@link OperationListener} is asked exactly once for each
		 * operation if he is interested. Use the OperationID to create a
		 * {@link OperationRelatedInfo} and map it to this ID, because further
		 * calls will only contain the operationID.
		 * 
		 * This allows an App-Level Analyzer to collect data from multiple
		 * Overlay-Operations by showing interest in different Operation Types.
		 * Return an {@link OperationRelatedInfo} for the given operationId.
		 * This may be a simple container with a 1:1-mapping of an overlay
		 * operation to an analyzer. It may as well contain a complex app-level
		 * analyzer that gathers information from many different
		 * Overlay-Operations (in that case, the same object may be returned
		 * multiple times for different operationIDs)
		 * 
		 * @param op
		 * @return
		 */
		public I interestedIn(Operation<?> op);

		/**
		 * Use the {@link HostRelatedInfo} to add metrics that are collected on
		 * a per-host-basis, over a time interval. This method is called exactly
		 * once in each measurement interval (the interval length is provided
		 * within the HostRelatedInfo and is the same across all hosts)
		 * 
		 * @param hostId
		 * @return
		 */
		public void writeHostRelatedInfo(HostRelatedInfo hostInfo);

		// /**
		// * Called, as soon as a NetMessage event related to the Operation
		// * happened.
		// *
		// * @param msg
		// * @param host
		// * @param reason
		// * @param operationId
		// */
		// public void netMsgEvent(NetMessage msg, Host host, Reason reason,
		// int operationId);

	}

	/**
	 * Container collecting Metrics related to one specific host, for example
	 * traffic on the host grouped by operation. You may add additional Metrics
	 * in your {@link OperationListener}
	 * 
	 * @author Bjoern Richerzhagen
	 * @version 1.0, 11.04.2012
	 */
	public final class HostRelatedInfo {

		private final long hostId;

		private final long lengthOfInterval;

		private Map<String, InfoContainer> dataByOperation = new HashMap<String, OverlayOperationAnalyzer.HostRelatedInfo.InfoContainer>();

		/**
		 * Small container for msgs-statistics
		 * 
		 * @author Bjoern Richerzhagen
		 * @version 1.0, 11.04.2012
		 */
		private class InfoContainer {

			private List<Double> sendSizesList = new ArrayList<Double>();

			private List<Double> rcvSizesList = new ArrayList<Double>();

			private List<Double> totalSizesList = new ArrayList<Double>();

			private List<Double> sendCountList = new ArrayList<Double>();

			private List<Double> rcvCountList = new ArrayList<Double>();

			private List<Double> totalCountList = new ArrayList<Double>();

			private double sendSizes = 0;

			private double rcvSizes = 0;

			private double totalSizes = 0;

			private double sendCount = 0;

			private double rcvCount = 0;

			private double totalCount = 0;

			private double startedOps = 0;

			private final MetricDescription RECEIVE_SIZE;

			private final MetricDescription SEND_SIZE;

			private final MetricDescription TOTAL_SIZE;

			private final MetricDescription TOTAL_COUNT;

			private final MetricDescription TOTAL_COUNT_SENT;

			private final MetricDescription TOTAL_COUNT_RECEIVED;

			private final MetricDescription STARTED_OPS;

			private final long hostId;

			public InfoContainer(long hostId, String operationName) {
				this.hostId = hostId;
				this.SEND_SIZE = new MetricDescription(
						OverlayOperationAnalyzer.class, "NETMESSAGE_SENT_SIZE_"
								+ operationName,
						"Size of NetMessages sent in interval for Operation "
								+ operationName, "byte");
				this.RECEIVE_SIZE = new MetricDescription(
						OverlayOperationAnalyzer.class,
						"NETMESSAGE_RECEIVED_SIZE_" + operationName,
						"Size of NetMessages received in interval for Operation "
								+ operationName, "byte");
				this.TOTAL_SIZE = new MetricDescription(
						OverlayOperationAnalyzer.class,
						"NETMESSAGE_TOTAL_SIZE_" + operationName,
						"Size of NetMessages send and received in interval for Operation "
								+ operationName, "byte");

				this.TOTAL_COUNT = new MetricDescription(
						OverlayOperationAnalyzer.class,
						"NETMESSAGE_TOTAL_COUNT",
						"Number of NetMessages send and received in interval",
						"1");

				this.TOTAL_COUNT_SENT = new MetricDescription(
						OverlayOperationAnalyzer.class,
						"NETMESSAGE_TOTAL_COUNT_SENT",
						"Number of NetMessages send in interval", "1");

				this.TOTAL_COUNT_RECEIVED = new MetricDescription(
						OverlayOperationAnalyzer.class,
						"NETMESSAGE_TOTAL_COUNT_RECEIVED",
						"Number of NetMessages received in interval", "1");

				this.STARTED_OPS = new MetricDescription(
						OverlayOperationAnalyzer.class,
						"OPERATIONS_STARTED_" + operationName,
						"Number of started operations of type " + operationName,
						"byte");
			}

			public void newMessage(Message msg, Reason reason) {

				if (listOperationMode) {

					totalCountList.add(1.0);

					switch (reason) {
					case SEND:
						sendSizesList.add((double) msg.getSize());
						totalSizesList.add((double) msg.getSize());
						sendCountList.add(1.0);
						break;

					case RECEIVE:
						rcvSizesList.add((double) msg.getSize());
						totalSizesList.add((double) msg.getSize());
						rcvCountList.add(1.0);
						break;

					default:
						break;
					}
				} else {
					totalCount += 1;
					switch (reason) {
					case SEND:
						sendSizes += msg.getSize();
						totalSizes += msg.getSize();
						sendCount += 1;
						break;

					case RECEIVE:
						rcvSizes += msg.getSize();
						totalSizes += msg.getSize();
						rcvCount += 1;
						break;

					default:
						break;
					}
				}
			}

			public void newOperation() {
				startedOps++;
			}

			public void write() {
				MeasurementDAO dao = new MeasurementDAO();
				long time = Time.getCurrentTime();

				if (listOperationMode) {

					dao.storeListMeasurement(SEND_SIZE, hostId, time,
							sendSizesList);
					dao.storeListMeasurement(RECEIVE_SIZE, hostId, time,
							rcvSizesList);
					dao.storeListMeasurement(TOTAL_SIZE, hostId, time,
							totalSizesList);
					dao.storeListMeasurement(TOTAL_COUNT, hostId, time,
							totalCountList);
					dao.storeListMeasurement(TOTAL_COUNT_SENT, hostId, time,
							sendCountList);
					dao.storeListMeasurement(TOTAL_COUNT_RECEIVED, hostId,
							time, rcvCountList);
					dao.storeSingleMeasurement(STARTED_OPS, hostId, time,
							startedOps);

					clear();
				} else{
					
					dao.storeSingleMeasurement(SEND_SIZE, hostId, time,
							sendSizes);
					dao.storeSingleMeasurement(RECEIVE_SIZE, hostId, time,
							rcvSizes);
					dao.storeSingleMeasurement(TOTAL_SIZE, hostId, time,
							totalSizes);
					dao.storeSingleMeasurement(TOTAL_COUNT, hostId, time,
							totalCount);
					dao.storeSingleMeasurement(TOTAL_COUNT_SENT, hostId, time,
							sendCount);
					dao.storeSingleMeasurement(TOTAL_COUNT_RECEIVED, hostId,
							time, rcvCount);
					dao.storeSingleMeasurement(STARTED_OPS, hostId, time,
							startedOps);
					
					clear();
					
				}
			}

			private void clear() {
				
				sendSizes = 0;
				rcvSizes = 0;
				totalSizes = 0;
				rcvCount = 0;
				sendCount = 0;
				totalCount = 0;
				
				sendSizesList.clear();
				rcvSizesList.clear();
				totalSizesList.clear();
				rcvCountList.clear();
				sendCountList.clear();
				totalCountList.clear();
				startedOps = 0;
			}

		}

		protected HostRelatedInfo(long hostId, long lengthOfInterval) {
			this.hostId = hostId;
			this.lengthOfInterval = lengthOfInterval;
		}

		public long getHostId() {
			return hostId;
		}

		/**
		 * Length (in simulation units) of the interval between two collections
		 * of aggregated, per-host data.
		 * 
		 * @return
		 */
		public long getLengthOfInterval() {
			return lengthOfInterval;
		}

		/**
		 * Used for general statistics that should not be duplicated by other
		 * analyzers (ie. traffic, message size)
		 */
		protected void writeHostRelatedInfo() {
			for (InfoContainer info : dataByOperation.values()) {
				info.write();
			}
		}

		/**
		 * Message-related information for this host (size, traffic, count...)
		 * by operation.
		 * 
		 * @param msg
		 * @param reason
		 * @param opName
		 *            name of the operation or "undefined"
		 */
		private void netMsgEvent(NetMessage msg, Reason reason, String opName) {
			InfoContainer msgInfo = dataByOperation.get(opName);
			if (msgInfo == null) {
				msgInfo = new InfoContainer(hostId, opName);
				dataByOperation.put(opName, msgInfo);
			}
			msgInfo.newMessage(msg, reason);
		}

		protected void netMsgEvent(NetMessage msg, Reason reason, boolean total) {
			if (total) {
				this.netMsgEvent(msg, reason, "TOTAL");
			} else {
				this.netMsgEvent(msg, reason, "OTHER");
			}
		}

		protected void netMsgEvent(NetMessage msg, Reason reason,
				OperationRelatedInfo opInfo) {
			this.netMsgEvent(msg, reason, opInfo.getOperationName());
		}

		protected void operationStartedEvent(String opName) {
			InfoContainer msgInfo = dataByOperation.get(opName);
			if (msgInfo == null) {
				msgInfo = new InfoContainer(hostId, opName);
				dataByOperation.put(opName, msgInfo);
			}
			msgInfo.newOperation();
		}

	}

	/**
	 * Return the {@link OperationRelatedInfo} or null.
	 * 
	 * @param msg
	 * @param depth
	 *            1: Trans; 2: Net; 3: Link
	 * @return
	 */
	private List<OperationRelatedInfo> getOpInfo(Message msg, int depth) {
		Message payload = msg;
		for (int i = 0; i < depth; i++) {
			payload = payload.getPayload();
		}
		if (payload instanceof AbstractOverlayMessage) {
			AbstractOverlayMessage oMsg = (AbstractOverlayMessage) payload;
			if (oMsg._getOperationID() != -1) {
				List<OperationRelatedInfo> opInfo = operations.get(oMsg
						._getOperationID());
				return opInfo;
			}
		}
		return null;
	}

	/**
	 * Dispatch the NetMsgEvent to relevant operationListeners
	 * 
	 * @param msg
	 * @param host
	 * @param reason
	 */
	@Override
	public void netMsgEvent(NetMessage msg, SimHost host, Reason reason) {
		if (!running) {
			return;
		}

		List<OperationRelatedInfo> opInfos = getOpInfo(msg, 2);
		if (opInfos != null) {
			for (OperationRelatedInfo opInfo : opInfos) {
				opInfo.netMsgEvent(msg, host, reason);
				// belonging to the host
				hostInfos.get(host).netMsgEvent(msg, reason, opInfo);
			}
		} else {
			// overlay-maintenance
			hostInfos.get(host).netMsgEvent(msg, reason, false);
		}

		// total traffic
		hostInfos.get(host).netMsgEvent(msg, reason, true);
	}

	@Override
	public void operationInitiated(Operation<?> op) {
		if (!running) {
			return;
		}
		if (op.getOperationID() == -1) {
			return;
		}
		if (op.getComponent() == null) {
			return;
		}

		SimHost host = (SimHost) op.getComponent().getHost();
		List<OperationRelatedInfo> interested = new LinkedList<OperationRelatedInfo>();
		for (OperationListener<?> listener : listeners) {
			OperationRelatedInfo opInfo = listener.interestedIn(op);
			if (opInfo != null) {
				opInfo.setHost(host);
				opInfo.setOriginator(listener);
				interested.add(opInfo);
				hostInfos.get(host).operationStartedEvent(
						opInfo.getOperationName());
			}
		}
		if (!interested.isEmpty()) {
			operations.put(op.getOperationID(), interested);
		} else {
			hostInfos.get(host).operationStartedEvent("OTHER");
		}
	}

	@Override
	public void operationFinished(Operation<?> op) {
		if (!running)
			return;
		if (op.getOperationID() == -1) {
			return;
		}
		if (operations.containsKey(op.getOperationID())) {
			List<OperationRelatedInfo> opInfos = operations.remove(op
					.getOperationID());
			for (OperationRelatedInfo opInfo : opInfos) {
				opInfo.analyzedOperationFinished(op);
			}
		}
	}

	@Override
	public void stop(Writer output) {
		//
	}

	public void setListOperationMode(boolean flag) {
		this.listOperationMode = flag;

	}

}
