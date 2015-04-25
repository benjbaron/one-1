/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package movement;

import java.util.List;
import java.util.Random;

import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import movement.map.SimMap;
import core.Coord;
import core.DTNHost;
import core.Settings;
import core.SimClock;

/**
 * 
 * This class controls the movement of bus travellers. A bus traveller belongs 
 * to a bus control system. A bus traveller has a destination and a start 
 * location. If the direct path to the destination is longer than the path the 
 * node would have to walk if it would take the bus, the node uses the bus. If 
 * the destination is not provided, the node will pass a random number of stops
 * determined by Markov chains (defined in settings).
 * 
 * @author Frans Ekman
 *
 */
public class PublicTransportTravellerMovement extends MapBasedMovement implements 
	SwitchableMovement, TransportMovement {

	public static final String PROBABILITIES_STRING = "probs";
	public static final String PROBABILITY_TAKE_OTHER_BUS = "probTakeOtherBus";
	
	public static final int STATE_INITIAL = 0;
	public static final int STATE_WALKING = 1;
	public static final int STATE_WAITING = 2;
	public static final int STATE_BOARDING = 3;
	public static final int STATE_ONVEHICLE = 4;
	public static final int STATE_READY = 5;
	
	private int state;
	private Path nextPath;
	private Coord location;
	private Coord latestBusStop;
	private PublicTransportControlSystem controlSystem;
	private int id;
	private ContinueBusTripDecider cbtd;
	private double[] probabilities;
	private double probTakeOtherBus;
	private DijkstraPathFinder pathFinder;
	
	private Coord startBusStop;
	private Coord endBusStop;
	
	private boolean takeBus;
	
	private static int nextID = 0;
	
	/**
	 * Creates a BusTravellerModel 
	 * @param settings
	 */
	public PublicTransportTravellerMovement(Settings settings) {
		super(settings);
		int bcs = settings.getInt(PublicTransportControlSystem.BUS_CONTROL_SYSTEM_NR);
		controlSystem = PublicTransportControlSystem.getBusControlSystem(bcs);
		id = nextID++;
		controlSystem.registerTraveller(this);
		nextPath = new Path();
		state = STATE_INITIAL;
		if (settings.contains(PROBABILITIES_STRING)) {
			probabilities = settings.getCsvDoubles(PROBABILITIES_STRING);
		}
		if (settings.contains(PROBABILITY_TAKE_OTHER_BUS)) {
			probTakeOtherBus = settings.getDouble(PROBABILITY_TAKE_OTHER_BUS);
		}
		cbtd = new ContinueBusTripDecider(rng, probabilities);
		pathFinder = new DijkstraPathFinder(null);
		takeBus = true;
	}
	
	/**
	 * constructor convenient for unit test
	 */
	public PublicTransportTravellerMovement(Settings settings, SimMap newMap, int nrofMaps){
		super(settings, newMap, nrofMaps);
		int bcs = settings.getInt(PublicTransportControlSystem.BUS_CONTROL_SYSTEM_NR);
		controlSystem = PublicTransportControlSystem.getBusControlSystem(bcs);
		id = nextID++;
		controlSystem.registerTraveller(this);
		nextPath = new Path();
		state = STATE_INITIAL;
		if (settings.contains(PROBABILITIES_STRING)) {
			probabilities = settings.getCsvDoubles(PROBABILITIES_STRING);
		}
		if (settings.contains(PROBABILITY_TAKE_OTHER_BUS)) {
			probTakeOtherBus = settings.getDouble(PROBABILITY_TAKE_OTHER_BUS);
		}
		cbtd = new ContinueBusTripDecider(rng, probabilities);
		pathFinder = new DijkstraPathFinder(null);
		takeBus = true;
	}
	
	/**
	 * Creates a BusTravellerModel from a prototype
	 * @param proto
	 */
	public PublicTransportTravellerMovement(PublicTransportTravellerMovement proto) {
		super(proto);
		state = proto.state;
		controlSystem = proto.controlSystem;
		if (proto.location != null) {
			location = proto.location.clone();
		}
		nextPath = proto.nextPath;
		id = nextID++;
		controlSystem.registerTraveller(this);
		probabilities = proto.probabilities;
		cbtd = new ContinueBusTripDecider(rng, probabilities);
		pathFinder = proto.pathFinder;
		this.probTakeOtherBus = proto.probTakeOtherBus;
		takeBus = true;
	}
	
	@Override
	public Coord getInitialLocation() {
		
		MapNode[] mapNodes = (MapNode[])getMap().getNodes().
			toArray(new MapNode[0]);
		int index = rng.nextInt(mapNodes.length - 1);
		location = mapNodes[index].getLocation().clone();
		
		List<Coord> allStops = controlSystem.getBusStops();
		Coord closestToNode = getClosestCoordinate(allStops, location.clone());
		latestBusStop = closestToNode.clone();
		
		return location.clone();
	}

	@Override
	public Path getPath() {
		if (!takeBus) {
			return null;
		}
		
		if (getState() == STATE_INITIAL) {
			// Try to find back to the bus stop
			SimMap map = controlSystem.getMap();
			if (map == null) {
				return null;
			}
			MapNode thisNode = map.getNodeByCoord(location);
			MapNode destinationNode = map.getNodeByCoord(latestBusStop);
			List<MapNode> nodes = pathFinder.getShortestPath(thisNode, 
								destinationNode);
			Path path = new Path(generateSpeed());
			for (MapNode node : nodes) {
				path.addWaypoint(node.getLocation());
			}
			location = latestBusStop.clone();
			setState(STATE_WALKING);
			return path;
		} else if (getState() == STATE_BOARDING) {
			List<Coord> coords = nextPath.getCoords();
			location = (coords.get(coords.size() - 1)).clone();
			setState(STATE_ONVEHICLE);
			return nextPath;
		} else {
			return null;
		}
	}

	/**
	 * Switches state between getPath() calls
	 * @return Always 0 
	 */
	protected double generateWaitTime() {
		
		if (getState() == STATE_WALKING) {		
			setState(STATE_WAITING);
			getHost().setLayer(this.controlSystem.getLayer());
		} else if (getState() == STATE_ONVEHICLE) {
			setState(STATE_WAITING);
		}
		
		return 0;
	}
	
	@Override
	public MapBasedMovement replicate() {
		return new PublicTransportTravellerMovement(this);
	}

	public int getState() {
		return state;
	}
	
	/**
	 * Get the location where the bus is located when it has moved its path
	 * @return The end point of the last path returned
	 */
	public Coord getLocation() {
		if (location == null) {
			return null;
		}
		return location.clone();
	}
	
	/**
	 * Notifies the node at the bus stop that a bus is there. Nodes inside 
	 * busses are also notified.
	 * @param nextPath The next path the bus is going to take
	 */
	public void enterBus(Path nextPath) {
		if (getState() != STATE_WAITING){
			return;
		}
		
		if (startBusStop != null && endBusStop != null) {
			if (location.equals(endBusStop)) {
				setState(STATE_READY);
				latestBusStop = location.clone();
				getHost().setLayer(DTNHost.LAYER_DEFAULT);	
			} else {
				setState(STATE_BOARDING);
				this.nextPath = nextPath;
			}
			return;
		}
		
		if (!cbtd.continueTrip()) {
			setState(STATE_WAITING);
		} else {
			setState(STATE_BOARDING);
			this.nextPath = nextPath;
		}
	}
	
	public int getID() {
		return id;
	}
	
	
	/**
	 * Small class to help nodes decide if they should continue the bus trip. 
	 * Keeps the state of nodes, i.e. how many stops they have passed so far. 
	 * Markov chain probabilities for the decisions. 
	 * 
	 * NOT USED BY THE WORKING DAY MOVEMENT MODEL
	 * 
	 * @author Frans Ekman
	 */
	public class ContinueBusTripDecider {
		
		private double[] probabilities; // Probability to travel with bus
		private int state;
		private Random rng;
		
		public ContinueBusTripDecider(Random rng, double[] probabilities) {
			this.rng = rng;
			this.probabilities = probabilities;
			state = 0;
		}
		
		/**
		 * 
		 * @return true if node should continue
		 */
		public boolean continueTrip() {
			double rand = rng.nextDouble();
			if (rand < probabilities[state]) {
				incState();
				return true;
			} else {
				resetState();
				return false;
			}
		}
		
		/**
		 * Call when a stop has been passed
		 */
		private void incState() {
			if (state < probabilities.length  - 1) {
				state++;
			}
		}
		
		/**
		 * Call when node has finished it's trip
		 */
		private void resetState() {
			state = 0;
		}	
	}

	/**
	 * Help method to find the closest coordinate from a list of coordinates,
	 * to a specific location
	 * @param allCoords list of coordinates to compare
	 * @param coord destination node
	 * @return closest to the destination
	 */
	private static Coord getClosestCoordinate(List<Coord> allCoords, 
			Coord coord) {
		Coord closestCoord = null;
		double minDistance = Double.POSITIVE_INFINITY;
		for (Coord temp : allCoords) {
			double distance = temp.distance(coord);
			if (distance < minDistance) {
				minDistance = distance;
				closestCoord = temp;
			}
		}
		return closestCoord.clone();
	}
	
	/**
	 * Sets the next route for the traveller, so that it can decide wether it 
	 * should take the bus or not. 
	 * @param nodeLocation
	 * @param nodeDestination
	 */
	public void setNextRoute(Coord nodeLocation, Coord nodeDestination) {
			
		// Find closest stops to current location and destination
		List<Coord> allStops = controlSystem.getBusStops();
		
		Coord closestToNode = getClosestCoordinate(allStops, nodeLocation);
		Coord closestToDestination = getClosestCoordinate(allStops, 
				nodeDestination);
		
		// Check if it is shorter to walk than take the bus 
		double directDistance = nodeLocation.distance(nodeDestination);
		double busDistance = nodeLocation.distance(closestToNode) + 
			nodeDestination.distance(closestToDestination);
		
		if (directDistance < busDistance) {
			takeBus = false;
			setState(STATE_READY);
		} else {
			takeBus = true;
			setState(STATE_INITIAL);
		}
		
		this.startBusStop = closestToNode;
		this.endBusStop = closestToDestination;
		this.latestBusStop = startBusStop.clone();
	}
	
	/**
	 * @see SwitchableMovement
	 */
	public Coord getLastLocation() {
		return location.clone();
	}

	/**
	 * @see SwitchableMovement
	 */
	public void setLocation(Coord lastWaypoint) {
		location = lastWaypoint.clone();
	}

	/**
	 * @see SwitchableMovement
	 */
	public boolean isReady() {
		if (getState() == STATE_READY) {
			return true;
		} else {
			return false;
		}
	}
	
	public static void reset() {
		nextID = 0;
	}
	
	public Coord getLatestBusStop() {
		return latestBusStop;
	}
	
	public Coord getStartBusStop() {
		return startBusStop;
	}
	
	public Coord getEndBusStop() {
		return endBusStop;
	}
	
	public void setState(int state){
		this.state = state;
	}

	/*
	 * detach passengers already onboard from public transport vehicle. And prevent passengers
	 * waiting at stop to attach.
	 * This is necessary for real-world scheduled vehicles that might stop serving at any time point.
	 * Then when the vehicle is at the last stop and finds there is no more path it will produce,
	 * it should indicate that via the busHasStopped(), and in turn the control system will call 
	 * offBoadNow() instead of enterBus()
	 */
	public void offBoardNow() {
		if (getState() != STATE_WAITING){
			return;
		}
		
		if (startBusStop != null && endBusStop != null) {
			if (location.equals(startBusStop)) {
				//do not get onboard, keep waiting
			} else {
				//get off now
				setState(STATE_READY);
				latestBusStop = location.clone();
				getHost().setLayer(DTNHost.LAYER_DEFAULT);	
			}
			return;
		}
	}
}
