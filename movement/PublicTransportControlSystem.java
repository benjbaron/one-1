/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package movement;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import movement.map.SimMap;
import core.Coord;
import core.DTNHost;
import core.DTNSim;

/**
 * This class controls busses and passengers that can use the bus. 
 * There can be many bus BusControlSystems, but a bus or passenger can only 
 * belong to one system.
 * 
 * @author Frans Ekman
 */
public class PublicTransportControlSystem {
	public static final String BUS_CONTROL_SYSTEM_NR = "busControlSystemNr";
	
	private static HashMap<Integer, PublicTransportControlSystem> systems;
	
	private HashMap<Integer, PublicTransportMovement> busses;
	private HashMap<Integer, PublicTransportTravellerMovement> travellers;
	private List<Coord> busStops;
	private int layer = DTNHost.LAYER_DEFAULT;
	private SimMap simMap;
	
	static {
		DTNSim.registerForReset(PublicTransportControlSystem.class.getCanonicalName());
		reset();
	}
	
	/**
	 * Creates a new instance of BusControlSystem without any travelers or 
	 * busses
	 * @param systemID The unique ID of this system.
	 */
	private PublicTransportControlSystem(int systemID) {
		busses = new HashMap<Integer, PublicTransportMovement>();
		travellers = new HashMap<Integer, PublicTransportTravellerMovement>();
	}
	
	public static void reset() {
		systems = new HashMap<Integer, PublicTransportControlSystem>();
	}
	
	/**
	 * Called by busses belonging to this system every time the bus has stopped.
	 * It calls every passengers enterBus() method so that the passengers can 
	 * enter the bus if they want to.
	 * @param busID Unique identifier of the bus
	 * @param busStop Coordinates of the bus stop
	 * @param nextPath The path to the next stop
	 */
	public void busHasStopped(int busID, Coord busStop, Path nextPath) {
		Iterator<PublicTransportTravellerMovement> iterator = travellers.values().
			iterator();
		while (iterator.hasNext()) {
			PublicTransportTravellerMovement traveller = (PublicTransportTravellerMovement)iterator.
				next();
			if (traveller.getLocation() != null) {
				if ((traveller.getLocation()).equals(busStop)) {
					if (traveller.getState() == PublicTransportTravellerMovement.
							STATE_WAITING_FOR_BUS) {
						Path path = new Path(nextPath);
						traveller.enterBus(path);
					} 
				}
			}
		}
	}
	
	/**
	 * Returns a reference to a BusControlSystem with ID provided as parameter. 
	 * If a system does not already exist with the requested ID, a new one is 
	 * created. 
	 * @param systemID unique ID of the system
	 * @return The bus control system with the provided ID
	 */
	public static PublicTransportControlSystem getBusControlSystem(int systemID) {
		Integer id = new Integer(systemID);
		
		if (systems.containsKey(id)) {
			return systems.get(id);
		} else {
			PublicTransportControlSystem bcs = new PublicTransportControlSystem(systemID);
			systems.put(id, bcs);
			return bcs;
		}
	}
	
	/**
	 * Registers a bus to be part of a bus control system
	 * @param bus The bus to register
	 */
	public void registerBus(PublicTransportMovement bus) {
		busses.put(bus.getID(), bus);
	}
	
	/**
	 * Registers a traveller/passenger to be part of a bus control system
	 * @param traveller The traveller to register
	 */
	public void registerTraveller(PublicTransportTravellerMovement traveller) {
		travellers.put(traveller.getID(), traveller);
	}
	
	/**
	 * Provide the system with the map
	 * @param map
	 */
	public void setMap(SimMap map) {
		this.simMap = map;
	}
	
	/**
	 * Get the underlying map of the system
	 * @return The map
	 */
	public SimMap getMap() {
		return this.simMap;
	}

	/**
	 * @return A list of all bus stops belonging to this system
	 */
	public List<Coord> getBusStops() {
		return busStops;
	}

	/**
	 * Set the bus stops that belong to this system
	 * @param busStops
	 */
	public void setBusStops(List<Coord> busStops) {
		this.busStops = busStops;
	}
	
	/**
	 * @return the layer of the line controlled by this control system
	 */
	public int getLayer() {
		return layer;
	}

	/**
	 * set the layer of the line controlled by this control system
	 * @param layer
	 */
	public void setLayer(int layer) {
		this.layer = layer;
	}
	
	
	
}
