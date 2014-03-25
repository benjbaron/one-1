package movement;

import core.DTNHost;
import core.Settings;

public class MetroMovement extends PublicTransportMovement {
	/**
	 * Creates a new instance of BusMovement
	 * @param settings
	 */
	public MetroMovement(Settings settings) {
		super(settings);
	}
	
	/**
	 * Create a new instance from a prototype
	 * @param proto
	 */
	public MetroMovement(MetroMovement proto) {
		super(proto);
	}
	
	@Override
	protected void setLayer() {
		getHost().setLayer(DTNHost.LAYER_UNDERGROUND);
	}

	@Override
	public MetroMovement replicate() {
		return new MetroMovement(this);
	}

	@Override
	public int getLayer() {
		return DTNHost.LAYER_UNDERGROUND;
	}
	
	
}
