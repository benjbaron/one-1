package movement.schedule;

import java.util.List;
import java.util.Set;

/**
 * 
 * @author linzhiqi
 * 
 * The fields' names must match the keys in json.
 */
public class RouteSchedule {
	public int route_id;
	public int layer_id;
	public Set<String> stops;
	public List<VehicleSchedule> vehicles;

	public RouteSchedule() {
		super();
	}

	@Override
	public String toString() {
		return "ScheduleRoute [route_id=" + route_id + ", layer_id=" + layer_id
				+ ", stops=" + stops + ", vehicles=" + vehicles + "]";
	}

}
