package movement;

import java.util.LinkedList;
import java.util.List;

import movement.map.MapNode;
import core.Coord;
import core.DTNHost;
import core.Settings;

public class PublicTransportMovement extends MapRouteMovement {

	public static final String LAYER_S = "layerID";
	
	protected PublicTransportControlSystem controlSystem;
	protected int id;
	protected static int nextID = 0;
	protected boolean startMode;
	protected List<Coord> stops;
	protected int layerID = DTNHost.LAYER_DEFAULT;
	
	/**
	 * Creates a new instance of PublicTransportMovement
	 * @param settings
	 */
	public PublicTransportMovement(Settings settings) {
		super(settings);
		int bcs = settings.getInt(PublicTransportControlSystem.BUS_CONTROL_SYSTEM_NR);
		this.layerID = settings.getInt(PublicTransportMovement.LAYER_S);
		controlSystem = PublicTransportControlSystem.getBusControlSystem(bcs);
	}
	
	public PublicTransportMovement(Settings settings, int bcs, int layerID, 
			List<String> stopIDList, boolean isScheduled) {
		super(settings, isScheduled, stopIDList);
		this.layerID = layerID;
		controlSystem = PublicTransportControlSystem.getBusControlSystem(bcs);
	}
	
	/**
	 * Create a new instance from a prototype
	 * @param proto
	 */
	public PublicTransportMovement(PublicTransportMovement proto) {
		super(proto);
		this.controlSystem = proto.controlSystem;
		this.id = nextID++;
		this.layerID = proto.layerID;
		controlSystem.registerBus(this);
		startMode = true;
	}
	
	// always invoked right after the prototype instance is built up
	@Override
	public void initProto() {
		controlSystem.setMap(super.getMap());
		this.id = nextID++;
		controlSystem.registerBus(this);
		controlSystem.setLayer(this.layerID);
		startMode = true;
		stops = new LinkedList<Coord>();
		List<MapNode> stopNodes = super.getStops();
		for (MapNode node : stopNodes) {
			stops.add(node.getLocation().clone());
		}
		controlSystem.setBusStops(stops);
	}
	
	@Override
	public Coord getInitialLocation() {
		setLayer();
		return (super.getInitialLocation()).clone();
	}
	
	public void setLayer() {
		getHost().setLayer(this.layerID);
	}

	@Override
	public Path getPath() {
		Coord lastLocation = (super.getLastLocation()).clone();
		Path path = super.getPath();
		// looks like random transport vehicles don't take passenger at starting stop. No change to it.
		// for scheduled transport, vehicles do take passengers at starting stop. -Zhiqi
		if (!startMode || isScheduled()) {
			controlSystem.busHasStopped(id, lastLocation, path);
		}
		startMode = false;
		return path;
	}

	@Override
	public PublicTransportMovement replicate() {
		return new PublicTransportMovement(this);
	}

	/**
	 * Returns unique ID of the bus
	 * @return unique ID of the bus
	 */
	public int getID() {
		return id;
	}

}
