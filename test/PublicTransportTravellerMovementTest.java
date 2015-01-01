package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import core.Coord;
import core.DTNHost;

import movement.Path;
import movement.PublicTransportControlSystem;
import movement.PublicTransportTravellerMovement;
import movement.PublicTransportTravellerMovement.ContinueBusTripDecider;
import movement.map.MapNode;
import movement.map.SimMap;
import junit.framework.TestCase;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PublicTransportControlSystem.class, PublicTransportTravellerMovement.class})

public class PublicTransportTravellerMovementTest extends TestCase {
	private PublicTransportTravellerMovement travellerM;
	private DummyControlSystem controlSystem;
	private TestSettings s;
	private DTNHost host;

	ArrayList<Coord> coords;
	ArrayList<MapNode> nodes;
	SimMap map;

	@Before
	public void setUp() {
		// create map
		createMap();
		
		// create control system
		controlSystem = new DummyControlSystem(0);
		controlSystem.setMap(map);
		
		// create setting
		s = new TestSettings();
		s.putSetting(PublicTransportControlSystem.BUS_CONTROL_SYSTEM_NR, "1");
		
		// mock the static method
		// PublicTransportControlSystem.getBusControlSystem(int)
		PowerMockito.mockStatic(PublicTransportControlSystem.class);
		Mockito.when(PublicTransportControlSystem
				.getBusControlSystem(Mockito.anyInt()))
				.thenReturn(controlSystem);
		
		host = Mockito.mock(DTNHost.class);
	}
	
	/**
	 * this tests for the course that traveller movement are used by extended movement model.
	 * One dependency we do not mock is the DijkstraPathFinder. This test should not break as long
	 * as the DijkstraPathFinder provide same shortest path.
	 * we do not verify the state flags, because later we might change how states are handled
	 * we verify real state, like locations, isReady, setLayer, and the values some methods should return,
	 * which determines whether this movement model works as we expect
	 */
	@Test
	public void testStateTransition4ExtendedMovement() {
		
		// create traveller mm
		travellerM = new PublicTransportTravellerMovement(s, map, 1);
		travellerM.setHost(host);
		
		// verify the static method is really called
		PowerMockito.verifyStatic();
		PublicTransportControlSystem.getBusControlSystem(Mockito.anyInt());
		
		// set location before setNextRoute()
		travellerM.setLocation(coords.get(0));
		assertEquals(coords.get(0),travellerM.getLocation());
		
		
		travellerM.setNextRoute(coords.get(0), coords.get(7));
		//verify the start and end stop is found correctly
		assertEquals(coords.get(0),travellerM.getLocation());
		assertEquals(coords.get(1),travellerM.getStartBusStop());
		assertEquals(coords.get(6),travellerM.getEndBusStop());
		assertEquals(coords.get(1),travellerM.getLatestBusStop());
		
		//first getPath() return the path to start stop
		Path p = travellerM.getPath();
		assertEquals(2, p.getCoords().size());
		assertEquals(coords.get(0), p.getNextWaypoint());
		assertEquals(coords.get(1), p.getNextWaypoint());
		assertEquals(coords.get(1),travellerM.getLocation());
		//assertFalse(travellerM.isReady());
		
		//now arrived at start stop, start waiting for vehicle's signal
		//host.setLayer() should be called
		Mockito.verify(host, Mockito.times(0)).setLayer(Mockito.anyInt());
		assertEquals(0d,travellerM.nextPathAvailable());
		Mockito.verify(host, Mockito.times(1)).setLayer(Mockito.anyInt());
		
		//move() is called again
		assertFalse(travellerM.isReady());
		this.assertNull(travellerM.getPath());
		assertEquals(0d,travellerM.nextPathAvailable());
		//move() called again
		assertFalse(travellerM.isReady());
		this.assertNull(travellerM.getPath());
		assertEquals(0d,travellerM.nextPathAvailable());
		
		//vehicle comes at the start stop
		p = new Path();
		p.setSpeed(5.0d);
		p.addWaypoint(coords.get(1));
		p.addWaypoint(coords.get(4));
		travellerM.enterBus(p);
		//location should not be updated yet
		assertEquals(coords.get(1),travellerM.getLocation());
		
		//getPath() is called, now location is updated
		assertFalse(travellerM.isReady());
		p = travellerM.getPath();
		assertEquals(2, p.getCoords().size());
		assertEquals(coords.get(1), p.getNextWaypoint());
		assertEquals(coords.get(4), p.getNextWaypoint());
		assertEquals(5.0d, p.getSpeed());
		assertEquals(coords.get(4),travellerM.getLocation());
		
		// second stop arrived, but bus has some wait time, so wait for bus's signal
		//move() is called
		assertEquals(0d,travellerM.nextPathAvailable());
		//move() called again
		assertFalse(travellerM.isReady());
		this.assertNull(travellerM.getPath());
		assertEquals(0d,travellerM.nextPathAvailable());
		//move() called again
		assertFalse(travellerM.isReady());
		this.assertNull(travellerM.getPath());
		assertEquals(0d,travellerM.nextPathAvailable());
		
		//vehicle arrives at the second stop
		//nextPathAvailable() called between enterBus() and getPath() should be ok, 
		//normally it doesn't happen
		p = new Path();
		p.setSpeed(5.0d);
		p.addWaypoint(coords.get(4));
		p.addWaypoint(coords.get(5));
		travellerM.enterBus(p);
		//location should not be updated yet
		assertEquals(coords.get(4),travellerM.getLocation());

		assertFalse(travellerM.isReady());
		//getPath is called, location should be updated		
		p = travellerM.getPath();
		assertEquals(2, p.getCoords().size());
		assertEquals(coords.get(4), p.getNextWaypoint());
		assertEquals(coords.get(5), p.getNextWaypoint());
		assertEquals(5.0d, p.getSpeed());
		assertEquals(coords.get(5),travellerM.getLocation());
		
		//wait for vehicle's signal again
		assertEquals(0d,travellerM.nextPathAvailable());	
		this.assertNull(travellerM.getPath());
		assertEquals(0d,travellerM.nextPathAvailable());		
		
		//vehicle arrives at the third stop
		p = new Path();
		p.setSpeed(6.0d);
		p.addWaypoint(coords.get(5));
		p.addWaypoint(coords.get(6));
		travellerM.enterBus(p);	
		assertEquals(coords.get(5),travellerM.getLocation());
		assertFalse(travellerM.isReady());
		p = travellerM.getPath();
		assertEquals(2, p.getCoords().size());
		assertEquals(coords.get(5), p.getNextWaypoint());
		assertEquals(coords.get(6), p.getNextWaypoint());
		assertEquals(6.0d, p.getSpeed());
		assertEquals(coords.get(6),travellerM.getLocation());
		
		//wait for vehicle's signal again
		assertEquals(0d,travellerM.nextPathAvailable());	
		this.assertNull(travellerM.getPath());
		assertEquals(0d,travellerM.nextPathAvailable());		
				
		//vehicle arrives at the end stop, host.setLayer() will be called
		p = new Path();
		p.setSpeed(6.0d);
		p.addWaypoint(coords.get(6));
		p.addWaypoint(coords.get(5));
		Mockito.verify(host, Mockito.times(1)).setLayer(Mockito.anyInt());
		travellerM.enterBus(p);	
		Mockito.verify(host, Mockito.times(2)).setLayer(Mockito.anyInt());
		assertEquals(coords.get(6),travellerM.getLocation());
		//update lastestBusStop
		assertEquals(coords.get(6),travellerM.getLatestBusStop());
		
		
		//before getPath(), the isReady() will be called by ExtendedMovementModel
		//now the traveller shoudl be ready
		assertTrue(travellerM.isReady());
	}
	
	@Test
	public void testStateTransitionNotTakeVehicle() {
							
		// create traveller mm
		travellerM = new PublicTransportTravellerMovement(s, map, 1);
		travellerM.setHost(host);
			
		// verify the static method is really called
		PowerMockito.verifyStatic();
		PublicTransportControlSystem.getBusControlSystem(Mockito.anyInt());
			
		// set location before setNextRoute()
		travellerM.setLocation(coords.get(0));
		assertEquals(coords.get(0),travellerM.getLocation());
				
		// it is shorter by foot than by pubilc transport, so it won't take vehicle		
		travellerM.setNextRoute(coords.get(0), coords.get(3));
		assertNull(travellerM.getPath());
		assertEquals(0d, travellerM.nextPathAvailable());
		assertTrue(travellerM.isReady());
		
		// setLayer() should not be called
		Mockito.verify(host, Mockito.times(0)).setLayer(Mockito.anyInt());
	}
	
	@Test
	/**
	 * ContinueBusTripDecider.continueTrip() is mocked always return true
	 * @throws Exception
	 */
	public void testStateTransition4StandAlone() throws Exception {
		
		//mock the ContinueBusTripDecider constructor	
		ContinueBusTripDecider cbtd = Mockito.mock(ContinueBusTripDecider.class);
		Mockito.when(cbtd.continueTrip()).thenReturn(true);	
		PowerMockito.whenNew(ContinueBusTripDecider.class)
			.withArguments(Mockito.any(Random.class), Mockito.any(double[].class))
			.thenReturn(cbtd);     	
		
		// create traveller mm
		travellerM = new PublicTransportTravellerMovement(s, map, 1);
		travellerM.setHost(host);
		
		// verify the static method is really called
		PowerMockito.verifyStatic();
		PublicTransportControlSystem.getBusControlSystem(Mockito.anyInt());
		
		// verify mocked ContinueBusTripDecider object is created
		PowerMockito.verifyNew(ContinueBusTripDecider.class, Mockito.atLeastOnce())
				.withArguments(Mockito.any(Random.class), Mockito.any(double[].class));
			
		// set random initial location
		travellerM.getInitialLocation();
		// force location != one of public transport stop location
		travellerM.setLocation(coords.get(0));
		
		// invoked by nextPathAvailable()
		travellerM.nextPathAvailable();
		
		// host asks for path to the closest stop
		assertNotNull(travellerM.getPath());
		
		// closest stop arrived, wait for vehicle
		// host.setLayer() should be called here
		Mockito.verify(host, Mockito.times(0)).setLayer(Mockito.anyInt());
		assertEquals(0.0d, travellerM.nextPathAvailable());
		Mockito.verify(host, Mockito.times(1)).setLayer(Mockito.anyInt());
		
		// keep trying until vehicle comes
		assertNull(travellerM.getPath());
		assertEquals(0.0d, travellerM.nextPathAvailable());
		assertNull(travellerM.getPath());
		assertEquals(0.0d, travellerM.nextPathAvailable());
		
		// vehicle arrives, this test does not care if it is a valid path
		Path p = new Path();
		p.setSpeed(5.0d);
		p.addWaypoint(coords.get(4));
		p.addWaypoint(coords.get(5));
		travellerM.enterBus(p);
		
		// verify continueTrip() is called
		Mockito.verify(cbtd).continueTrip();
		
		// as continue trip, it will return the path from the vehicle
		assertNotNull(travellerM.getPath());
	}
	
	@Test
	/**
	 * ContinueBusTripDecider.continueTrip() is mocked always return false
	 * @throws Exception
	 */
	public void testStateTransition4StandAloneNoContinue() throws Exception {
		
		//mock the ContinueBusTripDecider constructor	
		ContinueBusTripDecider cbtd = Mockito.mock(ContinueBusTripDecider.class);
		Mockito.when(cbtd.continueTrip()).thenReturn(false);	
		PowerMockito.whenNew(ContinueBusTripDecider.class)
			.withArguments(Mockito.any(Random.class), Mockito.any(double[].class))
			.thenReturn(cbtd);		
		
		// create traveller mm
		travellerM = new PublicTransportTravellerMovement(s, map, 1);
		travellerM.setHost(host);
		
		// verify the static method is really called
		PowerMockito.verifyStatic();
		PublicTransportControlSystem.getBusControlSystem(Mockito.anyInt());
		
		// verify mocked ContinueBusTripDecider object is created
		PowerMockito.verifyNew(ContinueBusTripDecider.class, Mockito.atLeastOnce())
				.withArguments(Mockito.any(Random.class), Mockito.any(double[].class));
		
		// set random initial location
		travellerM.getInitialLocation();
		// force location != one of public transport stop location
		travellerM.setLocation(coords.get(0));
		
		// invoked by nextPathAvailable()
		travellerM.nextPathAvailable();
		
		// host asks for path to the closest stop
		assertNotNull(travellerM.getPath());
		
		// closest stop arrived, wait for vehicle
		assertEquals(0.0d, travellerM.nextPathAvailable());
		
		// keep trying until vehicle comes
		assertNull(travellerM.getPath());
		assertEquals(0.0d, travellerM.nextPathAvailable());
		assertNull(travellerM.getPath());
		assertEquals(0.0d, travellerM.nextPathAvailable());
		
		// vehicle arrives, this test does not care if it is a valid path
		Path p = new Path();
		p.setSpeed(5.0d);
		p.addWaypoint(coords.get(4));
		p.addWaypoint(coords.get(5));
		travellerM.enterBus(p);
		
		// verify continueTrip() is called
		Mockito.verify(cbtd).continueTrip();
		
		// as not continue trip, it will return the path from the vehicle. 
		// seems current implementation has problem, 
		// it returns a path of coordN to coordN, so it doesn't pass the test
		assertNull(travellerM.getPath());
		
	}
	

	/**
	 * Creates a topology:
	 * 
	 * n1-10-n2---10---n3
	 * 10    10       / 10 
	 * n4-10-n5-5-n6-5-n7-5-n8
	 */
	private void createTopology() {
		nodes.get(0).addNeighbor(nodes.get(1));
		nodes.get(0).addNeighbor(nodes.get(3));
		nodes.get(1).addNeighbor(nodes.get(0));
		nodes.get(1).addNeighbor(nodes.get(4));
		nodes.get(1).addNeighbor(nodes.get(2));
		nodes.get(2).addNeighbor(nodes.get(1));
		nodes.get(2).addNeighbor(nodes.get(5));
		nodes.get(2).addNeighbor(nodes.get(6));
		nodes.get(3).addNeighbor(nodes.get(0));
		nodes.get(3).addNeighbor(nodes.get(4));
		nodes.get(4).addNeighbor(nodes.get(3));
		nodes.get(4).addNeighbor(nodes.get(1));
		nodes.get(4).addNeighbor(nodes.get(5));
		nodes.get(5).addNeighbor(nodes.get(4));
		nodes.get(5).addNeighbor(nodes.get(2));
		nodes.get(5).addNeighbor(nodes.get(6));
		nodes.get(6).addNeighbor(nodes.get(5));
		nodes.get(6).addNeighbor(nodes.get(2));
		nodes.get(6).addNeighbor(nodes.get(7));
		nodes.get(7).addNeighbor(nodes.get(6));
	}

	private void createNodes() {
		coords = new ArrayList<Coord>();
		coords.add(new Coord(0, 0));
		coords.add(new Coord(10, 0));
		coords.add(new Coord(20, 0));
		coords.add(new Coord(0, 10));
		coords.add(new Coord(10, 10));
		coords.add(new Coord(15, 10));
		coords.add(new Coord(20, 10));
		coords.add(new Coord(25, 10));

		nodes = new ArrayList<MapNode>();
		for (int i = 0; i < 8; i++) {
			nodes.add(new MapNode(coords.get(i)));
		}
	}

	public void createMap() {
		createNodes();
		createTopology();

		HashMap<Coord, MapNode> hm = new HashMap<Coord, MapNode>();
		for (int i = 0; i < 8; i++) {
			hm.put(coords.get(i), nodes.get(i));
		}
		map = new SimMap(hm);
	}

	public class DummyControlSystem extends PublicTransportControlSystem {
		SimMap map;

		public DummyControlSystem(int id) {
			super(id);
		}

		public void setMap(SimMap map) {
			this.map = map;
		}

		@Override
		public List<Coord> getBusStops() {
			ArrayList<Coord> ret = new ArrayList<Coord>();
			ret.add(coords.get(1));
			ret.add(coords.get(4));
			ret.add(coords.get(5));
			ret.add(coords.get(6));
			ret.add(coords.get(2));
			return ret;
		}

		@Override
		public SimMap getMap() {
			return map;
		}

		@Override
		public int getLayer() {
			return 0;
		}
	}

}
