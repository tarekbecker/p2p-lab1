package de.tudarmstadt.maki.simonstrator.overlay.flooding;

import de.tudarmstadt.maki.simonstrator.api.Host;
import de.tudarmstadt.maki.simonstrator.api.Monitor;
import de.tudarmstadt.maki.simonstrator.api.Monitor.Level;
import de.tudarmstadt.maki.simonstrator.api.Time;
import de.tudarmstadt.maki.simonstrator.api.component.HostComponentFactory;
import de.tudarmstadt.maki.simonstrator.api.component.overlay.OverlayComponent;
import de.tudarmstadt.maki.simonstrator.api.component.transport.MessageBasedTransport;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.FloodingContact.ContactType;
import de.tudarmstadt.maki.simonstrator.overlay.flooding.tracker.FloodingTracker;

/**
 * This factory creates all components that are configurable through the
 * simulator.
 */
public class FloodingNodeFactory implements HostComponentFactory {

	public static final int DEFAULT_NODE_PORT = 66666;

	public static final int DEFAULT_TRACKER_PORT = 77000;

	public static final int DEFAULT_TRACKER_ID = 0;

	public static final long TICK_INTERVAL = 120 * Time.MILLISECOND;

	/**
	 * TransportInfo of Tracker to be used (for non-simulations)
	 */
	private static String trackerAddress = null;

	/**
	 * TransportInfo of Tracker to be used (for non-simulations)
	 */
	private static int trackerPort = -1;

	/**
	 * Keep reference to tracker contact (to not generate it multiple times)
	 */
	private static FloodingContact trackerContact = null;

	/**
	 * Tracker reference (only interesting for simulation)
	 */
	public static FloodingTracker tracker = null;

	private static enum FactoryType {
		TRACKER, PEER
	}

	private FloodingSettings settings;

	private FactoryType type = null;

	/**
	 * Create a factory that produces components of the given type, which must
	 * be one out of {NODE, TRACKER, SERVER, DEBUGNODE}
	 * 
	 * @param factoryType
	 */
	public FloodingNodeFactory() {
		//
	}

	public void setFactoryType(String factoryType) {
		type = FactoryType.valueOf(factoryType);
		if (type == null) {
			throw new AssertionError("FactoryType must be one of {TRACKER, PEER}");
		}
	}

	/**
	 * Allows the configuration of settings on a per-host basis
	 * 
	 * @param settings
	 */
	public void setSettings(FloodingSettings settings) {
		this.settings = settings;
	}

	public void setDefaultTracker(String address, int port) {
		trackerAddress = address;
		trackerPort = port;
	}

	@Override
	public OverlayComponent createComponent(Host host) {

		if (settings == null) {
			System.err.println("You did not configure any settings. Default Settings will be used!");
			settings = new FloodingSettings();
		}

		if (type == null) {
			throw new AssertionError(
			        "You have to specify a type for the factory, by setting factoryType to one of {TRACKER, PEER}");
		}

		switch (type) {

		case PEER:
			FloodingNode node = new FloodingNode(host, DEFAULT_NODE_PORT, settings.clone());

			Monitor.log(FloodingNodeFactory.class, Level.DEBUG, "Factory: New peer created: " + host.getId());

			return node;
		case TRACKER:
			if (tracker != null)
				throw new AssertionError("There can be only one tracker!");

			tracker = new FloodingTracker(host, DEFAULT_TRACKER_PORT, settings.clone());

			Monitor.log(FloodingNodeFactory.class, Level.DEBUG, "Factory: New tracker created: " + host.getId());

			return tracker;
		default:
			break;
		}
		throw new AssertionError("Unable to create component...");
	}

	/**
	 * @param transport
	 * @return the contact information of the tracker
	 */
	public static FloodingContact getTrackerContact(FloodingNode node) {

		MessageBasedTransport transport = node.getTransport();

		if (trackerContact != null)
			return trackerContact;

		if (trackerAddress != null && trackerPort > -1) {
			// Use trackerAddress to contact tracker
			trackerContact = new FloodingContact(node.getHost().getId(),
					transport.getTransInfo(transport.getNetInterface()
							.getByName(FloodingNodeFactory.trackerAddress),
							trackerPort), ContactType.TRACKER);

		} else if (FloodingNodeFactory.tracker != null) {
			// Use tracker instance (most likely a simulation)
			trackerContact = FloodingNodeFactory.tracker.getLocalOverlayContact();

		} else {
			throw new AssertionError("No Tracker found!");

		}
		return trackerContact;
	}
}
