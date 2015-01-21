/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package movement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.SettingsError;
import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import movement.map.MapRoute;
import movement.schedule.StopDataUnit;
import movement.schedule.VehicleSchedule;
import core.Coord;
import core.Settings;
import core.SimClock;

/**
 * Map based movement model that uses predetermined paths within the map area.
 * Nodes using this model (can) stop on every route waypoint and find their
 * way to next waypoint using {@link DijkstraPathFinder}. There can be
 * different type of routes; see {@link #ROUTE_TYPE_S}.
 */
public class MapRouteMovement extends MapBasedMovement implements 
	SwitchableMovement {
	
	/** Per node group setting used for selecting a route file ({@value}) */
	public static final String ROUTE_FILE_S = "routeFile";
	/** 
	 * Per node group setting used for selecting a route's type ({@value}).
	 * Integer value from {@link MapRoute} class.
	 */
	public static final String ROUTE_TYPE_S = "routeType";
	
	/** 
	 * Per node group setting for selecting which stop (counting from 0 from
	 * the start of the route) should be the first one. By default, or if a 
	 * negative value is given, a random stop is selected.
	 */
	public static final String ROUTE_FIRST_STOP_S = "routeFirstStop";
	
	public static final String IS_SCHEDULED_S = "isScheduled";
	
	/** the file containing all schedules for all vehicles ({@value}) */
	public static final String SCHEDULE_FILE_S = "scheduleFile";
	/** the file containing all stops' coordinates */
	public static final String STOP_FILE_S = "scheduledTansportStopFile";
	public static final String SCHEDULED_MOVEMENT_NS_S = "scheduledMovementNamespace";
	
	/** the Dijkstra shortest path finder */
	private DijkstraPathFinder pathFinder;

	/** Prototype's reference to all routes read for the group */
	private List<MapRoute> allRoutes = null;
	/** next route's index to give by prototype */
	private Integer nextRouteIndex = null;
	/** Index of the first stop for a group of nodes (or -1 for random) */
	private int firstStopIndex = -1;
	
	/** Route of the movement model's instance */
	private MapRoute route;
	
	/** the schedule of this movement model instance */
	private VehicleSchedule schedule;
	private HashMap<String, MapNode> stopMap;
	private boolean isScheduled = false;
	private StopDataUnit currStop;
	private int nextTripIndex=0;
	private int nextStopIndex=0;
	private List<String> routeStopIds;

	
	/**
	 * Creates a new movement model based on a Settings object's settings.
	 * @param settings The Settings object where the settings are read from
	 */
	public MapRouteMovement(Settings settings) {
		super(settings);		
		String fileName = settings.getSetting(ROUTE_FILE_S);
		int type = settings.getInt(ROUTE_TYPE_S);
		allRoutes = MapRoute.readRoutes(fileName, type, getMap());
		nextRouteIndex = 0;
		pathFinder = new DijkstraPathFinder(getOkMapNodeTypes());
		this.route = this.allRoutes.get(this.nextRouteIndex).replicate();
		if (this.nextRouteIndex >= this.allRoutes.size()) {
			this.nextRouteIndex = 0;
		}
		
		if (settings.contains(ROUTE_FIRST_STOP_S)) {
			this.firstStopIndex = settings.getInt(ROUTE_FIRST_STOP_S);
			if (this.firstStopIndex >= this.route.getNrofStops()) {
				throw new SettingsError("Too high first stop's index (" + 
						this.firstStopIndex + ") for route with only " + 
						this.route.getNrofStops() + " stops");
			}
		}			
	}
	
	public MapRouteMovement(Settings settings, boolean isScheduled) {
		super(settings);
		pathFinder = new DijkstraPathFinder(getOkMapNodeTypes());
		if (!isScheduled) {
			String fileName = settings.getSetting(ROUTE_FILE_S);
			int type = settings.getInt(ROUTE_TYPE_S);
			allRoutes = MapRoute.readRoutes(fileName, type, getMap());
			nextRouteIndex = 0;
			this.route = this.allRoutes.get(this.nextRouteIndex).replicate();
			if (this.nextRouteIndex >= this.allRoutes.size()) {
				this.nextRouteIndex = 0;
			}
			
			if (settings.contains(ROUTE_FIRST_STOP_S)) {
				this.firstStopIndex = settings.getInt(ROUTE_FIRST_STOP_S);
				if (this.firstStopIndex >= this.route.getNrofStops()) {
					throw new SettingsError("Too high first stop's index (" + 
							this.firstStopIndex + ") for route with only " + 
							this.route.getNrofStops() + " stops");
				}
			}			
		}else {
			this.isScheduled = isScheduled;
		}
		
	}
	
	/**
	 * Copyconstructor. Gives a route to the new movement model from the
	 * list of routes and randomizes the starting position.
	 * @param proto The MapRouteMovement prototype
	 */
	protected MapRouteMovement(MapRouteMovement proto) {
		super(proto);
		this.pathFinder = proto.pathFinder;
		this.isScheduled = proto.isScheduled;
		this.stopMap = proto.stopMap;
		this.routeStopIds = proto.routeStopIds;
		
		if (!isScheduled) {
			this.route = proto.allRoutes.get(proto.nextRouteIndex).replicate();
			this.firstStopIndex = proto.firstStopIndex;
			
			if (firstStopIndex < 0) {
				/* set a random starting position on the route */
				this.route.setNextIndex(rng.nextInt(route.getNrofStops()-1));
			} else {
				/* use the one defined in the config file */
				this.route.setNextIndex(this.firstStopIndex);
			}
			
			proto.nextRouteIndex++; // give routes in order
			if (proto.nextRouteIndex >= proto.allRoutes.size()) {
				proto.nextRouteIndex = 0;
			}
		}
	}
	
	@Override
	protected double generateWaitTime() {
		double ret = 0.0;
		if (isLastStop(nextTripIndex, nextStopIndex, schedule)) {
			// this is already the last stop of the schedule
			ret = Double.MAX_VALUE;	
		} else if (nextTripIndex==0 && nextStopIndex==0) {
			// before start
			currStop = schedule.trips.get(0).get(0);
			nextStopIndex++;
			ret = schedule.trips.get(0).get(0).depT - SimClock.getTime(); 
		} else if (shouldStartNextTrip(nextTripIndex, nextStopIndex, schedule)) {		
			ret = schedule.trips.get(nextTripIndex).get(nextStopIndex).depT - SimClock.getTime();
			nextTripIndex++;
			nextStopIndex=0;
		} else {
			ret = schedule.trips.get(nextTripIndex).get(nextStopIndex).depT - SimClock.getTime();
//			if(this.host.getAddress()==0){
//				System.out.println("(else)host-"+this.host.getAddress()+" simTime="+SimClock.getTime()+" gwt="
//				+ret +" next_departure_time="+(ret+SimClock.getTime())+ 
//				" for stop-"+schedule.trips.get(nextTripIndex).get(nextStopIndex).stop_id);
//				}
			nextStopIndex++;
			assert ret>=-1:ret + " sim time > departure time of next stop for vehicle-" + 
			schedule.vehicle_id+". Do not continue, schedule after this is screwed up.";
		} 
		
		return ret;
	};

	public static boolean isLastStop(int nextTripIndex2, int nextStopIndex2,
			VehicleSchedule schedule2) {
		if (nextTripIndex2==(schedule2.trips.size()-1) 
				&& nextStopIndex2==(schedule2.trips.get(nextTripIndex2).size()-1)) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean shouldStartNextTrip(int nextTripIndex2, int nextStopIndex2,
			VehicleSchedule schedule2) {
		if (nextTripIndex2==(schedule2.trips.size()-1)){
			// this is already the last trip
			return false;
		} else if (nextStopIndex2==(schedule2.trips.get(nextTripIndex2).size()-1)) {
			return true;
		} else {
			return false;
		}	
	}

	@Override
	public Path getPath() {
		if (!isScheduled) {
			Path p = new Path(generateSpeed());
			MapNode to = route.nextStop();
			
			List<MapNode> nodePath = pathFinder.getShortestPath(lastMapNode, to);
			// this assertion should never fire if the map is checked in read phase
			assert nodePath.size() > 0 : "No path from " + lastMapNode + " to " +
				to + ". The simulation map isn't fully connected";
					
			for (MapNode node : nodePath) { // create a Path from the shortest path
				p.addWaypoint(node.getLocation());
			}
			
			lastMapNode = to;
			
			return p;
		}else {
			if(this.host.getAddress()==0){
				System.out.println("host-"+this.host.getAddress()+" getPath is called at "+SimClock.getTime()+
						" for stop-"+schedule.trips.get(nextTripIndex).get(nextStopIndex).stop_id +" currStop-"+currStop.stop_id);
			}
			
			Path p = getPathToNextStop(nextTripIndex, nextStopIndex, schedule, 
					SimClock.getTime(), stopMap, currStop, pathFinder);
			currStop = schedule.trips.get(nextTripIndex).get(nextStopIndex);
			lastMapNode = stopMap.get(currStop.stop_id);
			return p;
		}
		
	}	
	
	public static Path getPathToNextStop(int nextTripIndex2, int nextStopIndex2,
			VehicleSchedule schedule2, double time, HashMap<String, MapNode> stopMap2, 
			StopDataUnit currStop2, DijkstraPathFinder pathFinder2) {
		
		StopDataUnit nextStopData = schedule2.trips.get(nextTripIndex2).get(nextStopIndex2);
		MapNode to = stopMap2.get(nextStopData.stop_id);
		MapNode from = stopMap2.get(currStop2.stop_id);
		
		List<MapNode> nodePath = pathFinder2.getShortestPath(from, to);
		double distance = getTotalDistance(nodePath);
		if (SimClock.getTime()!=currStop2.depT) {
			System.out.println("vehicle's departure is late from schedule for " + 
					(SimClock.getTime()-currStop2.depT) + "seconds.");
		}
		Path p = new Path(distance/(nextStopData.arrT - SimClock.getTime()));
		for (MapNode node : nodePath) { 
			// create a Path from the shortest path
			p.addWaypoint(node.getLocation());
		}
		return p;
	}

	public static double getTotalDistance(List<MapNode> nodePath) {
		double totalDistance = 0;
		for (int i=0; i<(nodePath.size()-1); i++) {
			totalDistance += nodePath.get(i).getLocation().
					distance(nodePath.get(i+1).getLocation());
		}
		return totalDistance;
	}

	/**
	 * Returns the first stop on the route
	 */
	@Override
	public Coord getInitialLocation() {
		if (!isScheduled) {
			if (lastMapNode == null) {
				lastMapNode = route.nextStop();
			}
			
			return lastMapNode.getLocation().clone();
		}else {
			String index = schedule.trips.get(0).get(0).stop_id;
			lastMapNode = stopMap.get(index);
			return lastMapNode.getLocation().clone();
		}
		
	}
	
	@Override
	public Coord getLastLocation() {
		if (lastMapNode != null) {
			return lastMapNode.getLocation().clone();
		} else {
			return null;
		}
	}
	
	
	@Override
	public MapRouteMovement replicate() {
		return new MapRouteMovement(this);
	}	

	/**
	 * Returns the list of stops on the route
	 * @return The list of stops
	 */
	public List<MapNode> getStops() {
		if (!isScheduled) {
			return route.getStops();
		} else {
			return getRouteStops(this.routeStopIds, this.stopMap);
		}	
	}
	
	public static List<MapNode> getRouteStops(List<String> ids, Map<String, MapNode> map) {
		ArrayList<MapNode> stops = new ArrayList<MapNode>();
		for (String index : ids){
			stops.add(map.get(index));
		}
		return stops;
	}
	
	public void setSchedule(VehicleSchedule schedule) {
		this.schedule = schedule;
		nextTripIndex = 0;
		nextStopIndex = 0;
	}

	public boolean isScheduled() {
		return isScheduled;
	}
	
	public void setStopMap(HashMap<String, MapNode> stopMap) {
		this.stopMap = stopMap;
	}

	public void setRouteStopIds(List<String> routeStopIds) {
		this.routeStopIds = routeStopIds;
	}
}
