package movement;

public class StopDataUnit {
	public int stop_id;
	public double arrT;
	public double depT;
	
	public StopDataUnit() {
		super();
	}
	
	@Override
	public String toString() {
		return "StopDataUnit [stop_id=" + stop_id + ", arrT=" + arrT
				+ ", depT=" + depT + "]";
	}
}
