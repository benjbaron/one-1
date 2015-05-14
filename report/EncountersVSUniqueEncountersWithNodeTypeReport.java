package report;

import movement.MovementModel;
import movement.TransportMovement;
import movement.WorkingDayMovement;

import core.DTNHost;

public class EncountersVSUniqueEncountersWithNodeTypeReport extends
		EncountersVSUniqueEncountersReport {
	public EncountersVSUniqueEncountersWithNodeTypeReport() {
		super();
	}

	@Override
	public void done() {
		int[] totalEncounters = getTotalEncountersReport().getEncounters();
		int[][] nodeRelationships = getUniqueEncountersReport().getNodeRelationships();
		
		for (int i=0; i<totalEncounters.length; i++) {
			String row = "";
			row += i + "\t";
			row += totalEncounters[i] + "\t";
			
			int count = 0;
			for (int j=0; j<nodeRelationships.length; j++) {
				if (nodeRelationships[i][j] > 0) {
					count++;
				}
			}
			row += count;
			row += "\t" + getMovementType(getWorld().getHosts().get(i));
			write(row);
		}
	}

	public static String getMovementType(DTNHost host) {
		String ret = null;
		MovementModel mm = host.getMovement();
		if(mm instanceof WorkingDayMovement){
			TransportMovement transportMovement = ((WorkingDayMovement) mm).getMovementUsedForTransfers();
			ret = transportMovement.getClass().getName();
		}else {
			ret = mm.getClass().getName();
		}

		return ret;
	}

}
