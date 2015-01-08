package movement;

import java.util.ArrayList;

public class ScheduleRoute {
	public int route_id;
	public int layer_id;
	public ArrayList<Integer> stops;
	public ArrayList<VehicleSchedule> vehicles;

	public ScheduleRoute() {
		super();
	}

	@Override
	public String toString() {
		return "ScheduleRoute [route_id=" + route_id + ", layer_id=" + layer_id
				+ ", stops=" + stops + ", vehicles=" + vehicles + "]";
	}


}
