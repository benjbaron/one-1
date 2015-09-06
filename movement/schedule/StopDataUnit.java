package movement.schedule;

/**
 * 
 * @author linzhiqi
 * 
 * The fields' names must match the keys in json
 */
public class StopDataUnit {
	public String stop_id;
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
