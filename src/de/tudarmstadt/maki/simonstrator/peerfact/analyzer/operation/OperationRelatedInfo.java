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

package de.tudarmstadt.maki.simonstrator.peerfact.analyzer.operation;

import de.tud.kom.p2psim.api.analyzer.MessageAnalyzer.Reason;
import de.tud.kom.p2psim.api.common.SimHost;
import de.tud.kom.p2psim.api.network.NetMessage;
import de.tud.kom.p2psim.impl.util.db.dao.metric.MeasurementDAO;
import de.tud.kom.p2psim.impl.util.db.metric.MetricDescription;
import de.tudarmstadt.maki.simonstrator.api.Time;
import de.tudarmstadt.maki.simonstrator.api.operation.Operation;
import de.tudarmstadt.maki.simonstrator.peerfact.analyzer.OverlayOperationAnalyzer;
import de.tudarmstadt.maki.simonstrator.peerfact.analyzer.OverlayOperationAnalyzer.OperationListener;

/**
 * An object of this type is created for each operation that is logged by this
 * class. Within this object you should aggregate all information that is needed
 * by your Analyzer.
 * 
 * @author Bjoern Richerzhagen
 * @version 1.0, 30.03.2012
 */
public abstract class OperationRelatedInfo {

	private OperationListener originator;

	private SimHost host = null;

	private double netMsgsCount = 0;

	private double netMsgsSize = 0;

	public OperationRelatedInfo() {
		//
	}

	public void setOriginator(OperationListener originator) {
		if (this.originator == null) {
			this.originator = originator;
		}
	}

	public void setHost(SimHost host) {
		if (this.host == null) {
			this.host = host;
		}
	}

	public long getHostId() {
		return host.getHostId();
	}

	public SimHost getHost() {
		return host;
	}

	/**
	 * Returns the {@link OperationListener} that originally created this
	 * {@link OperationRelatedInfo}
	 * 
	 * @return
	 */
	public OperationListener getOriginator() {
		return originator;
	}

	/**
	 * Has to return the Name of the Operation (for example AreaNodeLookup).
	 * 
	 * @return
	 */
	abstract public String getOperationName();

	/**
	 * Use this to store your data. Do <b>NOT</b> commit the DAO queue, this
	 * will be done later.
	 * 
	 * @param dao
	 */
	protected void storeData() {
		MeasurementDAO dao = new MeasurementDAO();
		long time = Time.getCurrentTime();

		/*
		 * Counter of sent netMessages for this operation
		 */
		dao.storeSingleMeasurement(new MetricDescription(
				OverlayOperationAnalyzer.class, "NETMESSAGE_SENT_OP_"
						+ getOperationName(),
				"Number of NetMessages sent for Operation "
						+ getOperationName(), ""), getHostId(), time,
				netMsgsCount);

		/*
		 * Total size of messages for this operation
		 */
		dao.storeSingleMeasurement(new MetricDescription(
				OverlayOperationAnalyzer.class, "NETMESSAGE_SENT_SIZE_OP_"
						+ getOperationName(),
				"Size of NetMessages sent for Operation " + getOperationName(),
				"byte"), getHostId(), time, netMsgsSize);

		netMsgsCount = 0;
		netMsgsSize = 0;
	}

	/**
	 * Called as soon as the Operation is finished. Should be used to clean up
	 * the analyzer and write relevant information into the database.
	 * 
	 * @param op
	 *            the finished operation. This is needed, if your
	 *            OperationRelatedInfo operates on the app-layer and collects
	 *            multiple Operations. This allows you to distinguish which
	 *            operation finished and how to react.
	 */
	public void analyzedOperationFinished(Operation<?> op) {
		// TODO
	}

	/**
	 * You are informed as soon as a message is received or sent that is
	 * belonging to this operation.
	 * 
	 * @param msg
	 * @param host
	 * @param reason
	 */
	public void netMsgEvent(NetMessage msg, SimHost host, Reason reason) {
		/*
		 * This basic implementation at least counts the number of messages and
		 * their size
		 */
		netMsgsSize += msg.getSize();
		netMsgsCount++;
	}

}
