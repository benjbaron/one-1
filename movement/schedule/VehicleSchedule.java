package movement.schedule;

import java.util.ArrayList;

/**
 * 
 * @author linzhiqi
 * 
 * The fields' names must match the keys in json
 */
public class VehicleSchedule {
	public int vehicle_id;
	public ArrayList<ArrayList<StopDataUnit>> trips;

	public VehicleSchedule() {
		super();
	}

	@Override
	public String toString() {
		return "VehicleSchedule [vehicle_id=" + vehicle_id + ", trips=" + trips
				+ "]";
	}

}
