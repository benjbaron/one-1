package movement.schedule;

import input.WKTMapReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import movement.MapBasedMovement;
import movement.MapRouteMovement;
import movement.PublicTransportControlSystem;
import movement.PublicTransportMovement;
import movement.map.MapNode;
import movement.map.SimMap;
import routing.MessageRouter;
import core.Coord;
import core.DTNHost;
import core.MessageListener;
import core.ModuleCommunicationBus;
import core.MovementListener;
import core.NetworkInterface;
import core.Settings;
import core.SettingsError;
import core.SimError;
import core.SimScenario;

/**
 * @author linzhiqi
 * the factory class for creating scheduled public transport hosts
 */
public class ScheduledPublicTransportHostsFactory {
	private HashMap<String, Coord> stopMap = null;
	private HashMap<String, MapNode> stopMapTranslated = null;
	private SimMap simMap = null;
	
	/**
	 * build hosts for the group
	 * @param scenario	scenario is needed for setting its SimMap
	 * @param s	the Settings object of the group's namespace
	 * @param msgLs	list of message listeners
	 * @param movLs	list of movement listeners
	 * @param groupId	the group id
	 * @param interf	list of network interfaces
	 * @param mRouterProto	prototype instance of message router
	 * @return	a list of hosts
	 */
	public List<DTNHost> buildHostsForGroup(SimScenario scenario, Settings s, 
			List<MessageListener> msgLs, List<MovementListener> movLs,
			String groupId, List<NetworkInterface> interf, 
			MessageRouter mRouterProto) {
		
		if (stopMap == null) {
			stopMap = loadStopMap();
		}
		
		if (scenario.getMap()==null) {
			simMap = readMap(scenario.getWorldSizeX(), scenario.getWorldSizeY());
			scenario.setMap(simMap);
		} else {
			simMap = scenario.getMap();
		}
		
		if (stopMapTranslated == null) {
			stopMapTranslated = translateStopMap(stopMap, simMap);
		}
		
		String scheduleFilePath = s.getSetting(MapRouteMovement.SCHEDULE_FILE_S);
		ArrayList<RouteSchedule> schedules = loadSchedules(scheduleFilePath);
		
		List<DTNHost> hosts = new ArrayList<DTNHost>();
		
		if (schedules==null || schedules.isEmpty()) {
			throw new SettingsError("schedule data is empty.");
		}
		
		for (int j=0; j<schedules.size(); j++) {
			RouteSchedule route = schedules.get(j);
			List<String> stopIDList = new ArrayList<String>(route.stops);
			PublicTransportControlSystem bcs = null;
			
			// initialize the route and the control system
			if (route.layer_id == DTNHost.LAYER_DEFAULT || route.layer_id == DTNHost.LAYER_UNDERGROUND) {
				bcs = PublicTransportControlSystem.getBusControlSystem(route.route_id);
				bcs.setMap(simMap);
				bcs.setLayer(route.layer_id);
				List<MapNode> stopNodes = MapRouteMovement.getRouteStops(stopIDList, stopMapTranslated);
				
				LinkedList<Coord> stops = new LinkedList<Coord>();
				for (MapNode node : stopNodes) {
					stops.add(node.getLocation().clone());
				}
				bcs.setBusStops(stops);
			} else {
				throw new SettingsError("layer_id of schedule route-" + route.route_id + " is invalid.");
			}
			
			for (int n=0; n<route.vehicles.size(); n++) {
				VehicleSchedule vehicle = route.vehicles.get(n);
				ModuleCommunicationBus comBus = new ModuleCommunicationBus();
				PublicTransportMovement mm = new PublicTransportMovement(s, bcs, route.layer_id, vehicle, stopIDList, stopMapTranslated, true);
				
				DTNHost host = new DTNHost(msgLs, 
						movLs,	groupId, interf, comBus, 
						mm, false, mRouterProto);
				hosts.add(host);
			}
		}
		
		return hosts;
	}
	
	/**
	 * load the global single stop map from the file path specified in setting file
	 * @return the stop map
	 */
	public static HashMap<String, Coord> loadStopMap() {
		Settings s = new Settings(MapRouteMovement.SCHEDULED_MOVEMENT_NS_S);
		if (!s.contains(MapRouteMovement.STOP_FILE_S)) {
			return null;
		}else {	
			String path = s.getSetting(MapRouteMovement.STOP_FILE_S);
			return loadStopMap(path);	
		}	
	}
	
	/**
	 * do the actual loading job for loadStopMap()
	 * @param path the path of file containing stop map json string
	 * @return
	 */
	public static HashMap<String, Coord> loadStopMap(String path) {
		HashMap<String, Coord> stopMap = null;
		ObjectMapper mapper = new ObjectMapper();
		
		try {
			stopMap = mapper.readValue(new FileReader(new File(path)), 
					    new TypeReference<HashMap<String, Coord>>(){});
		} catch (JsonParseException e) {
			System.err.println("file " + path + " failed to be parsed.");
			e.printStackTrace();
			System.exit(-1);
		} catch (JsonMappingException e) {
			System.err.println("file " + path + " failed to be parsed.");
			e.printStackTrace();
			System.exit(-1);
		} catch (FileNotFoundException e) {
			System.err.println("file " + path + " is not found.");
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return stopMap;
	}

	/**
	 * align stops with the map
	 * @param stopMap
	 * @param map
	 * @return
	 */
	public static HashMap<String, MapNode> translateStopMap(
			HashMap<String, Coord> stopMap, SimMap map) {
		boolean mirror = map.isMirrored();
		double xOffset = map.getOffset().getX();
		double yOffset = map.getOffset().getY();
		HashMap<String, MapNode> nodes = new HashMap<String, MapNode>();
		
		Set<String> ids = stopMap.keySet();
		for ( String id : ids) {
			Coord c = stopMap.get(id);
			Coord cc = c.clone();
			if (mirror) {
				cc.setLocation(c.getX(), -c.getY());
			}
			cc.translate(xOffset, yOffset);
			
			MapNode node = map.getNodeByCoord(cc);
			if (node == null) {				
				throw new SettingsError("stop file for scheduled transport" +
						" contained invalid coordinate " + cc + " orig: " +
						c);
			}
			nodes.put(id, node);
		}
		return nodes;
	}
	
	/**
	 * load the schedules from the file containing json strings
	 * @param scheduleFilePath
	 * @return schedules loaded from the scheduleFilePath
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<RouteSchedule> loadSchedules(String scheduleFilePath) {
		ArrayList<RouteSchedule> ret = null;
		ObjectMapper mapper = new ObjectMapper();
		TypeFactory typeFactory = mapper.getTypeFactory();
		try {
			ret = (ArrayList<RouteSchedule>)mapper.readValue(new FileReader(new File(scheduleFilePath)), 
					typeFactory.constructCollectionType(ArrayList.class, RouteSchedule.class));
		}catch (FileNotFoundException e) {
			System.err.println("file " + scheduleFilePath + " is not found.");
			System.exit(-1);
		}catch (IOException e) {
			System.err.println("file " + scheduleFilePath + " failed to be parsed.");
			e.printStackTrace();
			System.exit(-1);	
		}
		
		return ret;
	}
	
	/**
	 * This is modified based on the readMap() method of MapBasedMovement class.
	 * In future refactory, this map reading functionality should be placed in 
	 * SimScenario class.  
	 * As the simMap of SimScenario is project unique, it should be read, constructed 
	 * and hold by SimScenario object and allow MapBasedMovement objects referring to
	 * it, instead of letting MapBasedMovement read and construct the map(hidden dependency).
	 * @param maxX
	 * @param maxY
	 * @return the read and constructed SimMap object
	 */
	public static SimMap readMap(double maxX, double maxY) {
		SimMap simMap;
		Settings settings = new Settings(MapBasedMovement.MAP_BASE_MOVEMENT_NS);
		WKTMapReader r = new WKTMapReader(true);
		
		ArrayList<String> cachedMapFiles = new ArrayList<String>();

	
		
		try {
			int nrofMapFiles = settings.getInt(MapBasedMovement.NROF_FILES_S);

			for (int i = 1; i <= nrofMapFiles; i++ ) {
				String pathFile = settings.getSetting(MapBasedMovement.FILE_S + i);
				cachedMapFiles.add(pathFile);
				r.addPaths(new File(pathFile), i);
			}
			
		} catch (IOException e) {
			throw new SimError(e.toString(),e);
		}

		simMap = r.getMap();
		checkMapConnectedness(simMap.getNodes());
		// mirrors the map (y' = -y) and moves its upper left corner to origo
		simMap.mirror();
		Coord offset = simMap.getMinBound().clone();		
		simMap.translate(-offset.getX(), -offset.getY());
		checkCoordValidity(simMap.getNodes(), maxX, maxY);
		
		return simMap;
	}
	
	public static void checkMapConnectedness(List<MapNode> nodes) {
		Set<MapNode> visited = new HashSet<MapNode>();
		Queue<MapNode> unvisited = new LinkedList<MapNode>();
		MapNode firstNode;
		MapNode next = null;
		
		if (nodes.size() == 0) {
			throw new SimError("No map nodes in the given map");
		}
		
		firstNode = nodes.get(0);
		
		visited.add(firstNode);
		unvisited.addAll(firstNode.getNeighbors());
		
		while ((next = unvisited.poll()) != null) {
			visited.add(next);
			for (MapNode n: next.getNeighbors()) {
				if (!visited.contains(n) && ! unvisited.contains(n)) {
					unvisited.add(n);
				}
			}
		}
		
		if (visited.size() != nodes.size()) { // some node couldn't be reached
			MapNode disconnected = null;
			for (MapNode n : nodes) { // find an example node
				if (!visited.contains(n)) {
					disconnected = n;
					break;
				}
			}
			throw new SettingsError("SimMap is not fully connected. Only " + 
					visited.size() + " out of " + nodes.size() + " map nodes " +
					"can be reached from " + firstNode + ". E.g. " + 
					disconnected + " can't be reached");
		}
	}
	
	/**
	 * Checks that all coordinates of map nodes are within the min&max limits
	 * of the movement model
	 * @param nodes The list of nodes to check
	 * @throws SettingsError if some map node is out of bounds
	 */
	public static void checkCoordValidity(List<MapNode> nodes, double maxX, double maxY) {
		 // Check that all map nodes are within world limits
		for (MapNode n : nodes) {
			double x = n.getLocation().getX();
			double y = n.getLocation().getY();
			if (x < 0 || x > maxX || y < 0 || y > maxY) {
				throw new SettingsError("Map node " + n.getLocation() + 
						" is out of world  bounds "+
						"(x: 0..." + maxX + " y: 0..." + maxY + ")");
			}
		}
	}
}
