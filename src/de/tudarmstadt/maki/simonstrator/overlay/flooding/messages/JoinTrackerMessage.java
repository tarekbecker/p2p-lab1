/*
 * Copyright (c) 2005-2010 KOM ‚Äì Multimedia Communications Lab
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

package de.tudarmstadt.maki.simonstrator.overlay.flooding.messages;

import de.tudarmstadt.maki.simonstrator.overlay.flooding.FloodingContact;

/**
 * This message is used to join with a tracker in the
 * {@link JoinTrackerOperation}. This request is then confirmed by the tracker
 * which will return initial contacts.
 */
public class JoinTrackerMessage extends AbstractFloodingMessage{

	/**
	 * 
	 */
	private static final long serialVersionUID = -5811449042279624009L;

	public JoinTrackerMessage(FloodingContact sender) {
		super(sender);
	}

	@Override
	public String toString() {
		return "JOIN TRACKER " + getSenderContact().toString();
	}
}
