package de.tudarmstadt.maki.simonstrator.overlay.flooding;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.Properties;
import java.util.Set;

import de.tudarmstadt.maki.simonstrator.api.Time;


/**
 * All parameters, be they adaptive or fixed, are accessed via this component of
 * a {@link TransitNode}. This enables a better overview on all possible tweaks
 * and configurations.
 * 
 * @author Bjoern Richerzhagen
 * @version 1.0, 23.09.2012
 */
public class FloodingSettings {

	private final long[] times = new long[Times.values().length];

	private final int[] params = new int[Params.values().length];

	/**
	 * Generated for each node, as some settings might be adaptive and change
	 * over time
	 */
	public FloodingSettings() {
		// Default values
		for (Times time : Times.values()) {
			times[time.ordinal()] = time.getDefault();
		}
		for (Params param : Params.values()) {
			params[param.ordinal()] = param.getDefault();
		}
	}

	/**
	 * Copy-constructor
	 * 
	 * @param toClone
	 */
	private FloodingSettings(FloodingSettings toClone) {
		int i = 0;
		for (long value : toClone.times) {
			times[i] = value;
			i++;
		}
		i = 0;
		for (int value : toClone.params) {
			params[i] = value;
			i++;
		}
	}

	/**
	 * Read the current value of the parameter with the given name
	 * 
	 * @param name
	 * @return
	 */
	public int getParam(Params name) {
		return params[name.ordinal()];
	}

	/**
	 * Sets the given parameter
	 * 
	 * @param name
	 * @param value
	 */
	public void setParam(Params name, int value) {
		params[name.ordinal()] = value;
	}

	/**
	 * Get the current time that is associated to the setting specified by the
	 * enum TransitTimes
	 * 
	 * @param name
	 * @return
	 */
	public long getTime(Times name) {
		return times[name.ordinal()];
	}

	/**
	 * Update the value of a setting describing a time in Transit
	 * 
	 * @param name
	 * @param value
	 */
	public void setTime(Times name, long value) {
		times[name.ordinal()] = value;
	}

	/**
	 * Manually overwrite some config parameters by specifying an attribute in
	 * the form overwrite="propname:value;propname:value", might be usefull for
	 * batch processing of minor config variations
	 * 
	 * @param toOverwrite
	 */
	public void setOverwrite(String toOverwrite) {
		String[] props = toOverwrite.split(";");
		for (String prop : props) {
			String[] val = prop.split(":");
			parseProperty(val[0], val[1]);
		}
	}

	private Properties defaultProperties = null;

	private boolean loadedProps = false;

	/**
	 * Allows to specify a settings file for default values other than the ones
	 * specified in the code below. This default file can then be overwritten
	 * with another properties file via setFile.
	 * 
	 * @param filename
	 */
	public void setDefaults(String filename) {
		if (loadedProps) {
			throw new AssertionError(
					"Configure setDefaults before calling setFile!");
		}
		defaultProperties = new Properties();
		BufferedInputStream bin = null;
		try {
			bin = new BufferedInputStream(new FileInputStream(filename));
			defaultProperties.load(bin);
			bin.close();
		} catch (Exception e) {
			throw new AssertionError(e.getMessage());
		}
		parseProperties(defaultProperties);
	}

	/**
	 * Can be used to pass a .properties-file that contains values to overwrite
	 * the default values of this settings-class.
	 * 
	 * @param filename
	 */
	public void setFile(String filename) {
		loadedProps = true;
		Properties props = null;
		if (defaultProperties != null) {
			props = new Properties(defaultProperties);
		} else {
			props = new Properties();
		}
		BufferedInputStream bin = null;
		try {
			bin = new BufferedInputStream(
					new FileInputStream(filename));
			props.load(bin);
			bin.close();
		} catch (Exception e) {
			throw new AssertionError(e.getMessage());
		}
		parseProperties(props);
	}

	/**
	 * Writes the values set in the properties into the settings for transit
	 * 
	 * @param props
	 */
	private void parseProperties(Properties props) {
		Set<String> names = props.stringPropertyNames();
		for (String name : names) {
			parseProperty(name, props.getProperty(name));
		}
	}

	/**
	 * Set the given property
	 * 
	 * @param name
	 * @param value
	 */
	private void parseProperty(String name, String value) {
		try {
			params[Params.valueOf(name).ordinal()] = Integer
					.parseInt(value);
		} catch (IllegalArgumentException e) {
			//
		}
		try {
			times[Times.valueOf(name).ordinal()] = Time.parseTime(value);
		} catch (IllegalArgumentException e) {
			//
		}
	}

	/**
	 * All parameters used to configure the overlay
	 * 
	 */
	public enum Params {

		/**
		 * Some configuration parameter
		 */
		MAX_NUM_NEW_NEIGHBORS(10),
		
		MAX_NUM_CONNECTIONS(5);
		
		private final int defaultValue;

		private Params(int defaultValue) {
			this.defaultValue = defaultValue;
		}

		protected int getDefault() {
			return defaultValue;
		}

	}

	/**
	 * All times used in the overlay
	 * 
	 */
	public enum Times {

		/**
		 * Timeout for messages
		 */
		MSG_TIMEOUT(1 * Time.SECOND),

		/**
		 * Interval between maintenance operations
		 */
		MAINTENANCE_INTERVAL(10 * Time.SECOND);


		private final long defaultValue;

		private Times(long defaultValue) {
			this.defaultValue = defaultValue;
		}

		protected long getDefault() {
			return defaultValue;
		}
	}

	@Override
	public FloodingSettings clone() {
		return new FloodingSettings(this);
	}

}
