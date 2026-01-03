package perds.test;

import perds.PERDS;
import perds.algorithms.DijkstraPathfinder;
import perds.models.*;

/**
 * Basic test class for PERDS functionality
 * For comprehensive testing, consider using JUnit
 */
public class PERDSTest {
    
    public static void main(String[] args) {
        System.out.println("=== Running PERDS Tests ===\n");
        
        testNetworkCreation();
        testPathfinding();
        testIncidentPriority();
        testDynamicUpdates();
        
        System.out.println("\n=== All Tests Complete ===");
    }
    
    private static void testNetworkCreation() {
        System.out.println("Test 1: Network Creation");
        
        EmergencyNetwork network = new EmergencyNetwork();
        Location loc1 = new Location("L1", "Location 1", 0, 0, Location.LocationType.CITY);
        Location loc2 = new Location("L2", "Location 2", 5, 5, Location.LocationType.CITY);
        
        network.addLocation(loc1);
        network.addLocation(loc2);
        network.addEdge(loc1, loc2, 7.07, 10.0);
        
        assert network.getLocationCount() == 2 : "Location count should be 2";
        assert network.getEdgeCount() == 1 : "Edge count should be 1";
        
        System.out.println("✓ Network creation test passed\n");
    }
    
    private static void testPathfinding() {
        System.out.println("Test 2: Pathfinding Algorithm");
        
        EmergencyNetwork network = new EmergencyNetwork();
        Location a = new Location("A", "A", 0, 0, Location.LocationType.CITY);
        Location b = new Location("B", "B", 1, 1, Location.LocationType.CITY);
        Location c = new Location("C", "C", 2, 2, Location.LocationType.CITY);
        
        network.addLocation(a);
        network.addLocation(b);
        network.addLocation(c);
        
        network.addEdge(a, b, 1.0, 5.0);
        network.addEdge(b, c, 1.0, 5.0);
        network.addEdge(a, c, 3.0, 15.0);
        
        DijkstraPathfinder pathfinder = new DijkstraPathfinder();
        DijkstraPathfinder.PathResult result = pathfinder.findShortestPath(network, a, c);
        
        assert result.isValid() : "Path should be valid";
        assert result.getTotalDistance() == 10.0 : "Shortest path should be 10.0";
        assert result.getPath().size() == 3 : "Path should have 3 locations";
        
        System.out.println("✓ Pathfinding test passed");
        System.out.println("  Shortest path: A -> B -> C (distance: " + result.getTotalDistance() + ")\n");
    }
    
    private static void testIncidentPriority() {
        System.out.println("Test 3: Incident Priority");
        
        Location loc = new Location("L1", "Test Location", 0, 0, Location.LocationType.CITY);
        
        Incident low = new Incident("I1", loc, Incident.IncidentType.MEDICAL, 
            Incident.IncidentSeverity.LOW);
        Incident critical = new Incident("I2", loc, Incident.IncidentType.FIRE, 
            Incident.IncidentSeverity.CRITICAL);
        
        assert critical.getPriorityScore() > low.getPriorityScore() : 
            "Critical incidents should have higher priority";
        
        System.out.println("✓ Incident priority test passed");
        System.out.println("  Low priority score: " + low.getPriorityScore());
        System.out.println("  Critical priority score: " + critical.getPriorityScore() + "\n");
    }
    
    private static void testDynamicUpdates() {
        System.out.println("Test 4: Dynamic Network Updates");
        
        EmergencyNetwork network = new EmergencyNetwork();
        Location a = new Location("A", "A", 0, 0, Location.LocationType.CITY);
        Location b = new Location("B", "B", 1, 1, Location.LocationType.CITY);
        
        network.addLocation(a);
        network.addLocation(b);
        network.addEdge(a, b, 1.0, 5.0);
        
        // Update travel time
        network.updateEdgeWeight("A", "B", 10.0);
        
        DijkstraPathfinder pathfinder = new DijkstraPathfinder();
        DijkstraPathfinder.PathResult result = pathfinder.findShortestPath(network, a, b);
        
        assert result.getTotalDistance() == 10.0 : "Updated weight should be reflected";
        
        // Block edge
        network.setEdgeBlocked("A", "B", true);
        result = pathfinder.findShortestPath(network, a, b);
        
        assert !result.isValid() : "Blocked edge should prevent path";
        
        System.out.println("✓ Dynamic updates test passed\n");
    }
}
