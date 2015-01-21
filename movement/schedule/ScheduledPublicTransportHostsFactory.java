package movement.schedule;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import movement.BusMovement;
import movement.MapRouteMovement;
import movement.MetroMovement;
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
import core.SimScenario;

/**
 * @author linzhiqi
 * the factory class for creating scheduled public transport hosts
 */
public class ScheduledPublicTransportHostsFactory {
	HashMap<String, Coord> stopMap = null;
	HashMap<String, MapNode> stopMapTranslated = null;
	
	public List<DTNHost> buildHosts(SimScenario scenario, Settings s, 
			List<MessageListener> msgLs, List<MovementListener> movLs,
			String groupId, List<NetworkInterface> interf, 
			MessageRouter mRouterProto) {
		
		if (stopMap == null) {
			stopMap = loadStopMap();
		}
		
		String scheduleFilePath = s.getSetting(MapRouteMovement.SCHEDULE_FILE_S);
		ArrayList<RouteSchedule> schedules = loadSchedules(scheduleFilePath);
		
		List<DTNHost> hosts = new ArrayList<DTNHost>();
		
		if (schedules==null || schedules.isEmpty()) {
			throw new SettingsError("schedule data is empty.");
		}
		
		PublicTransportMovement mmProto = null;
		
		for (int j=0; j<schedules.size(); j++) {
			RouteSchedule route = schedules.get(j);
			if (route.layer_id == DTNHost.LAYER_DEFAULT) {
				mmProto = new BusMovement(s, route.route_id, true);	
			}else if (route.layer_id == DTNHost.LAYER_UNDERGROUND) {
				mmProto = new MetroMovement(s, route.route_id, true);
			}else {
				throw new SettingsError("layer_id of schedule route-" + route.route_id + " is invalid.");
			}
			
			if (stopMapTranslated == null) {
				stopMapTranslated = translateStopMap(stopMap, mmProto.getMap());
			}
			
			mmProto.setStopMap(stopMapTranslated);
			mmProto.setRouteStopIds(new ArrayList<String>(route.stops));
			mmProto.initProto();
			
			for (int n=0; n<route.vehicles.size(); n++) {
				VehicleSchedule vehicle = route.vehicles.get(n);
				ModuleCommunicationBus comBus = new ModuleCommunicationBus();
				PublicTransportMovement mm = mmProto.replicate();
				mm.setSchedule(vehicle);
				
				DTNHost host = new DTNHost(msgLs, 
						movLs,	groupId, interf, comBus, 
						mm, false, mRouterProto);
				hosts.add(host);
			}
		}
		
		scenario.setMap(mmProto.getMap());
		
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
}
