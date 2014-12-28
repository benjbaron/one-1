/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package movement;

import core.DTNHost;
import core.Settings;

/**
 * This class controls the movement of busses. It informs the bus control system
 * the bus is registered with every time the bus stops.
 * 
 * @author Frans Ekman
 */
public class BusMovement extends PublicTransportMovement {
	
	/**
	 * Creates a new instance of BusMovement
	 * @param settings
	 */
	public BusMovement(Settings settings) {
		super(settings);
	}
	
	/**
	 * Create a new instance from a prototype
	 * @param proto
	 */
	public BusMovement(BusMovement proto) {
		super(proto);
	}
	
	@Override
	protected void setLayer() {
		getHost().setLayer(DTNHost.LAYER_DEFAULT);
	}

	@Override
	public BusMovement replicate() {
		return new BusMovement(this);
	}

	@Override
	public int getLayer() {
		return DTNHost.LAYER_DEFAULT;
	}	
}
