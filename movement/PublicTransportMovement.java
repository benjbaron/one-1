package movement;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import movement.map.MapNode;
import movement.schedule.VehicleSchedule;
import core.Coord;
import core.DTNHost;
import core.Settings;

public class PublicTransportMovement extends MapRouteMovement {

	public static final String LAYER_S = "layerID";
	
	protected PublicTransportControlSystem controlSystem;
	protected int id;
	protected static int nextID = 0;
	protected boolean startMode;
	protected int layerID = DTNHost.LAYER_DEFAULT;
	
	/**
	 * Creates a new instance of PublicTransportMovement
	 * @param settings
	 */
	public PublicTransportMovement(Settings settings) {
		super(settings);
		int bcs = settings.getInt(PublicTransportControlSystem.BUS_CONTROL_SYSTEM_NR);
		if(settings.contains(PublicTransportMovement.LAYER_S)){
			this.layerID = settings.getInt(PublicTransportMovement.LAYER_S);
		}
		controlSystem = PublicTransportControlSystem.getBusControlSystem(bcs);
		this.id = nextID++;
		startMode = true;
		LinkedList<Coord> stops = new LinkedList<Coord>();
		List<MapNode> stopNodes = super.getStops();
		for (MapNode node : stopNodes) {
			stops.add(node.getLocation().clone());
		}
		controlSystem.setMap(super.getMap());
		controlSystem.registerBus(this);
		controlSystem.setLayer(this.layerID);
		controlSystem.setBusStops(stops);
	}
	
	public PublicTransportMovement(Settings settings, PublicTransportControlSystem bcs, int layerID, VehicleSchedule schedule,
			List<String> stopIDList, HashMap<String, MapNode> stopMap, boolean isScheduled) {
		super(settings, isScheduled, schedule, stopIDList, stopMap);
		this.layerID = layerID;
		controlSystem = bcs;
		this.id = nextID++;
		startMode = true;
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
