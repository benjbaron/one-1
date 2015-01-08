package movement;

import java.util.ArrayList;

import movement.StopDataUnit;

public class VehicleSchedule {
	@Override
	public String toString() {
		return "VehicleSchedule [vehicle_id=" + vehicle_id + ", trips=" + trips
				+ "]";
	}

	public int vehicle_id;
	public ArrayList<ArrayList<StopDataUnit>> trips;
	
	public VehicleSchedule() {
		super();
	}
	
}
