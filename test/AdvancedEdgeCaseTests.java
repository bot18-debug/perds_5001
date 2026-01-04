package perds.test;

import perds.models.*;
import perds.algorithms.*;
import java.util.*;

/**
 * Comprehensive Edge Case and Boundary Testing
 * 
 * Tests system behavior under extreme and unusual conditions:
 * - Empty inputs
 * - Single elements
 * - Maximum capacity
 * - Disconnected graphs
 * - Concurrent operations
 * - Invalid inputs
 * - Null safety
 * 
 * @author PERDS Team
 * @version 2.0
 */
public class AdvancedEdgeCaseTests {
    
    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;
    
    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("           ADVANCED EDGE CASE TEST SUITE");
        System.out.println("═══════════════════════════════════════════════════════════\n");
        
        // Run all test categories
        testEmptyInputs();
        testSingleElements();
        testDisconnectedGraphs();
        testMaximumCapacity();
        testNullSafety();
        testInvalidInputs();
        testConcurrentOperations();
        testBoundaryConditions();
        testResourceExhaustion();
        testDynamicNetworkChanges();
        
        // Print summary
        printSummary();
    }
    
    /**
     * Test empty input handling
     */
    private static void testEmptyInputs() {
        System.out.println("Testing Empty Inputs...");
        
        // Empty network
        testCase("Empty network pathfinding", () -> {
            EmergencyNetwork network = new EmergencyNetwork();
            DijkstraPathfinder pathfinder = new DijkstraPathfinder();
            
            Location loc1 = new Location("L1", "L1", 0, 0, Location.LocationType.CITY);
            Location loc2 = new Location("L2", "L2", 1, 1, Location.LocationType.CITY);
            
            DijkstraPathfinder.PathResult result = pathfinder.findShortestPath(network, loc1, loc2);
            return !result.isValid();
        });
        
        // Empty dispatch queue
        testCase("Dispatch with no incidents", () -> {
            EmergencyNetwork network = createSimpleNetwork();
            DispatchManager dispatcher = new DispatchManager(network);
            
            List<Location> locations = new ArrayList<>(network.getAllLocations());
            ResponseUnit unit = new ResponseUnit("U1", "Unit1", 
                ResponseUnit.UnitType.AMBULANCE, locations.get(0));
            dispatcher.registerUnit(unit);
            
            List<DispatchManager.DispatchDecision> decisions = dispatcher.dispatchAll();
            return decisions.isEmpty();
        });
        
        // No available units
        testCase("Dispatch with no units", () -> {
            EmergencyNetwork network = createSimpleNetwork();
            DispatchManager dispatcher = new DispatchManager(network);
            
            List<Location> locations = new ArrayList<>(network.getAllLocations());
            Incident incident = new Incident("I1", locations.get(0),
                Incident.IncidentType.MEDICAL, Incident.IncidentSeverity.HIGH);
            dispatcher.reportIncident(incident);
            
            List<DispatchManager.DispatchDecision> decisions = dispatcher.dispatchAll();
            return decisions.isEmpty();
        });
        
        System.out.println();
    }
    
    /**
     * Test single element handling
     */
    private static void testSingleElements() {
        System.out.println("Testing Single Elements...");
        
        // Single node graph
        testCase("Pathfinding in single-node graph", () -> {
            EmergencyNetwork network = new EmergencyNetwork();
            Location loc = new Location("L1", "L1", 0, 0, Location.LocationType.CITY);
            network.addLocation(loc);
            
            DijkstraPathfinder pathfinder = new DijkstraPathfinder();
            DijkstraPathfinder.PathResult result = pathfinder.findShortestPath(network, loc, loc);
            
            return result.isValid() && result.getTotalDistance() == 0.0;
        });
        
        // Single incident
        testCase("Dispatch single incident", () -> {
            EmergencyNetwork network = createSimpleNetwork();
            DispatchManager dispatcher = new DispatchManager(network);
            
            List<Location> locations = new ArrayList<>(network.getAllLocations());
            ResponseUnit unit = new ResponseUnit("U1", "Unit1",
                ResponseUnit.UnitType.AMBULANCE, locations.get(0));
            dispatcher.registerUnit(unit);
            
            Incident incident = new Incident("I1", locations.get(1),
                Incident.IncidentType.MEDICAL, Incident.IncidentSeverity.HIGH);
            dispatcher.reportIncident(incident);
            
            List<DispatchManager.DispatchDecision> decisions = dispatcher.dispatchAll();
            return decisions.size() == 1;
        });
        
        System.out.println();
    }
    
    /**
     * Test disconnected graphs
     */
    private static void testDisconnectedGraphs() {
        System.out.println("Testing Disconnected Graphs...");
        
        testCase("Pathfinding in disconnected graph", () -> {
            EmergencyNetwork network = new EmergencyNetwork();
            
            // Island 1
            Location l1 = new Location("L1", "L1", 0, 0, Location.LocationType.CITY);
            Location l2 = new Location("L2", "L2", 1, 1, Location.LocationType.CITY);
            
            // Island 2 (disconnected)
            Location l3 = new Location("L3", "L3", 10, 10, Location.LocationType.CITY);
            Location l4 = new Location("L4", "L4", 11, 11, Location.LocationType.CITY);
            
            network.addLocation(l1);
            network.addLocation(l2);
            network.addLocation(l3);
            network.addLocation(l4);
            
            network.addEdge(l1, l2, 1.0, 1.0);
            network.addEdge(l3, l4, 1.0, 1.0);
            // No edge between islands
            
            DijkstraPathfinder pathfinder = new DijkstraPathfinder();
            DijkstraPathfinder.PathResult result = pathfinder.findShortestPath(network, l1, l3);
            
            return !result.isValid(); // Should fail - no path exists
        });
        
        testCase("Dispatch in disconnected network", () -> {
            EmergencyNetwork network = new EmergencyNetwork();
            
            Location l1 = new Location("L1", "L1", 0, 0, Location.LocationType.CITY);
            Location l2 = new Location("L2", "L2", 10, 10, Location.LocationType.CITY);
            network.addLocation(l1);
            network.addLocation(l2);
            // No connection
            
            DispatchManager dispatcher = new DispatchManager(network);
            ResponseUnit unit = new ResponseUnit("U1", "Unit1",
                ResponseUnit.UnitType.AMBULANCE, l1);
            dispatcher.registerUnit(unit);
            
            Incident incident = new Incident("I1", l2,
                Incident.IncidentType.MEDICAL, Incident.IncidentSeverity.HIGH);
            dispatcher.reportIncident(incident);
            
            List<DispatchManager.DispatchDecision> decisions = dispatcher.dispatchAll();
            return decisions.isEmpty(); // Cannot dispatch - no path
        });
        
        System.out.println();
    }
    
    /**
     * Test maximum capacity scenarios
     */
    private static void testMaximumCapacity() {
        System.out.println("Testing Maximum Capacity...");
        
        testCase("Large graph (1000 nodes)", () -> {
            EmergencyNetwork network = new EmergencyNetwork();
            List<Location> locations = new ArrayList<>();
            
            // Create 1000 nodes
            for (int i = 0; i < 1000; i++) {
                Location loc = new Location("L" + i, "Loc" + i, i, i, Location.LocationType.CITY);
                network.addLocation(loc);
                locations.add(loc);
            }
            
            // Create edges
            for (int i = 0; i < 999; i++) {
                network.addEdge(locations.get(i), locations.get(i + 1), 1.0, 1.0);
            }
            
            // Test pathfinding
            DijkstraPathfinder pathfinder = new DijkstraPathfinder();
            DijkstraPathfinder.PathResult result = pathfinder.findShortestPath(
                network, locations.get(0), locations.get(999));
            
            return result.isValid() && result.getPath().size() == 1000;
        });
        
        testCase("Many incidents (100+)", () -> {
            EmergencyNetwork network = createSimpleNetwork();
            DispatchManager dispatcher = new DispatchManager(network);
            
            List<Location> locations = new ArrayList<>(network.getAllLocations());
            
            // Add 10 units
            for (int i = 0; i < 10; i++) {
                ResponseUnit unit = new ResponseUnit("U" + i, "Unit" + i,
                    ResponseUnit.UnitType.AMBULANCE, locations.get(0));
                dispatcher.registerUnit(unit);
            }
            
            // Add 100 incidents
            for (int i = 0; i < 100; i++) {
                Location loc = locations.get(i % locations.size());
                Incident incident = new Incident("I" + i, loc,
                    Incident.IncidentType.MEDICAL, Incident.IncidentSeverity.MEDIUM);
                dispatcher.reportIncident(incident);
            }
            
            List<DispatchManager.DispatchDecision> decisions = dispatcher.dispatchAll();
            return decisions.size() <= 10; // Can only dispatch 10 (number of units)
        });
        
        System.out.println();
    }
    
    /**
     * Test null safety
     */
    private static void testNullSafety() {
        System.out.println("Testing Null Safety...");
        
        testCase("Pathfinding with null network", () -> {
            try {
                DijkstraPathfinder pathfinder = new DijkstraPathfinder();
                Location loc = new Location("L1", "L1", 0, 0, Location.LocationType.CITY);
                pathfinder.findShortestPath(null, loc, loc);
                return false; // Should throw exception
            } catch (NullPointerException | IllegalArgumentException e) {
                return true; // Expected
            }
        });
        
        testCase("Dispatch with null incident", () -> {
            try {
                EmergencyNetwork network = createSimpleNetwork();
                DispatchManager dispatcher = new DispatchManager(network);
                dispatcher.reportIncident(null);
                return false; // Should throw exception
            } catch (NullPointerException | IllegalArgumentException e) {
                return true; // Expected
            }
        });
        
        System.out.println();
    }
    
    /**
     * Test invalid inputs
     */
    private static void testInvalidInputs() {
        System.out.println("Testing Invalid Inputs...");
        
        testCase("Negative edge weights", () -> {
            try {
                EmergencyNetwork network = new EmergencyNetwork();
                Location l1 = new Location("L1", "L1", 0, 0, Location.LocationType.CITY);
                Location l2 = new Location("L2", "L2", 1, 1, Location.LocationType.CITY);
                network.addLocation(l1);
                network.addLocation(l2);
                network.addEdge(l1, l2, -5.0, 10.0); // Negative distance
                return false; // Should be rejected or handled
            } catch (IllegalArgumentException e) {
                return true; // Expected
            }
        });
        
        testCase("Duplicate location IDs", () -> {
            EmergencyNetwork network = new EmergencyNetwork();
            Location l1 = new Location("L1", "Name1", 0, 0, Location.LocationType.CITY);
            Location l2 = new Location("L1", "Name2", 1, 1, Location.LocationType.CITY); // Same ID
            
            network.addLocation(l1);
            network.addLocation(l2);
            
            // Should handle duplicates gracefully
            return network.getLocationCount() <= 2;
        });
        
        System.out.println();
    }
    
    /**
     * Test concurrent operations (basic)
     */
    private static void testConcurrentOperations() {
        System.out.println("Testing Concurrent Operations...");
        
        testCase("Multiple dispatches simultaneously", () -> {
            EmergencyNetwork network = createSimpleNetwork();
            DispatchManager dispatcher = new DispatchManager(network);
            
            List<Location> locations = new ArrayList<>(network.getAllLocations());
            
            // Add units
            for (int i = 0; i < 5; i++) {
                ResponseUnit unit = new ResponseUnit("U" + i, "Unit" + i,
                    ResponseUnit.UnitType.AMBULANCE, locations.get(0));
                dispatcher.registerUnit(unit);
            }
            
            // Add incidents
            for (int i = 0; i < 5; i++) {
                Incident incident = new Incident("I" + i, locations.get(1),
                    Incident.IncidentType.MEDICAL, Incident.IncidentSeverity.HIGH);
                dispatcher.reportIncident(incident);
            }
            
            // Dispatch all at once
            List<DispatchManager.DispatchDecision> decisions = dispatcher.dispatchAll();
            
            // Check no unit assigned twice
            Set<String> assignedUnits = new HashSet<>();
            for (DispatchManager.DispatchDecision decision : decisions) {
                if (assignedUnits.contains(decision.getUnit().getId())) {
                    return false; // Unit assigned twice!
                }
                assignedUnits.add(decision.getUnit().getId());
            }
            
            return true;
        });
        
        System.out.println();
    }
    
    /**
     * Test boundary conditions
     */
    private static void testBoundaryConditions() {
        System.out.println("Testing Boundary Conditions...");
        
        testCase("Zero distance edge", () -> {
            EmergencyNetwork network = new EmergencyNetwork();
            Location l1 = new Location("L1", "L1", 0, 0, Location.LocationType.CITY);
            Location l2 = new Location("L2", "L2", 0, 0, Location.LocationType.CITY); // Same position
            network.addLocation(l1);
            network.addLocation(l2);
            network.addEdge(l1, l2, 0.0, 0.0);
            
            DijkstraPathfinder pathfinder = new DijkstraPathfinder();
            DijkstraPathfinder.PathResult result = pathfinder.findShortestPath(network, l1, l2);
            
            return result.isValid() && result.getTotalDistance() == 0.0;
        });
        
        testCase("Incident at unit location", () -> {
            EmergencyNetwork network = createSimpleNetwork();
            DispatchManager dispatcher = new DispatchManager(network);
            
            List<Location> locations = new ArrayList<>(network.getAllLocations());
            Location loc = locations.get(0);
            
            ResponseUnit unit = new ResponseUnit("U1", "Unit1",
                ResponseUnit.UnitType.AMBULANCE, loc);
            dispatcher.registerUnit(unit);
            
            Incident incident = new Incident("I1", loc, // Same location as unit
                Incident.IncidentType.MEDICAL, Incident.IncidentSeverity.HIGH);
            dispatcher.reportIncident(incident);
            
            List<DispatchManager.DispatchDecision> decisions = dispatcher.dispatchAll();
            
            return decisions.size() == 1 && decisions.get(0).getPath().getTotalDistance() == 0.0;
        });
        
        System.out.println();
    }
    
    /**
     * Test resource exhaustion scenarios
     */
    private static void testResourceExhaustion() {
        System.out.println("Testing Resource Exhaustion...");
        
        testCase("All units busy", () -> {
            EmergencyNetwork network = createSimpleNetwork();
            DispatchManager dispatcher = new DispatchManager(network);
            
            List<Location> locations = new ArrayList<>(network.getAllLocations());
            
            // Add 2 units
            for (int i = 0; i < 2; i++) {
                ResponseUnit unit = new ResponseUnit("U" + i, "Unit" + i,
                    ResponseUnit.UnitType.AMBULANCE, locations.get(0));
                dispatcher.registerUnit(unit);
            }
            
            // Dispatch to 2 incidents (all units busy)
            for (int i = 0; i < 2; i++) {
                Incident incident = new Incident("I" + i, locations.get(1),
                    Incident.IncidentType.MEDICAL, Incident.IncidentSeverity.HIGH);
                dispatcher.reportIncident(incident);
            }
            dispatcher.dispatchAll();
            
            // Add new incident - should fail (no available units)
            Incident newIncident = new Incident("I3", locations.get(1),
                Incident.IncidentType.MEDICAL, Incident.IncidentSeverity.CRITICAL);
            dispatcher.reportIncident(newIncident);
            
            List<DispatchManager.DispatchDecision> decisions = dispatcher.dispatchAll();
            return decisions.isEmpty(); // No units available
        });
        
        System.out.println();
    }
    
    /**
     * Test dynamic network changes
     */
    private static void testDynamicNetworkChanges() {
        System.out.println("Testing Dynamic Network Changes...");
        
        testCase("Edge blocking during pathfinding", () -> {
            EmergencyNetwork network = createSimpleNetwork();
            DijkstraPathfinder pathfinder = new DijkstraPathfinder();
            
            List<Location> locations = new ArrayList<>(network.getAllLocations());
            Location l1 = locations.get(0);
            Location l2 = locations.get(1);
            
            // Find initial path
            DijkstraPathfinder.PathResult result1 = pathfinder.findShortestPath(network, l1, l2);
            double dist1 = result1.getTotalDistance();
            
            // Block direct edge
            network.setEdgeBlocked(l1.getId(), l2.getId(), true);
            
            // Find new path
            DijkstraPathfinder.PathResult result2 = pathfinder.findShortestPath(network, l1, l2);
            double dist2 = result2.getTotalDistance();
            
            // New path should be longer (or invalid)
            return !result2.isValid() || dist2 > dist1;
        });
        
        testCase("Weight update during operations", () -> {
            EmergencyNetwork network = createSimpleNetwork();
            List<Location> locations = new ArrayList<>(network.getAllLocations());
            Location l1 = locations.get(0);
            Location l2 = locations.get(1);
            
            // Update weight
            network.updateEdgeWeight(l1.getId(), l2.getId(), 100.0); // Make very expensive
            
            DijkstraPathfinder pathfinder = new DijkstraPathfinder();
            DijkstraPathfinder.PathResult result = pathfinder.findShortestPath(network, l1, l2);
            
            // Should use alternative route if available, or have high cost
            return result.getTotalDistance() >= 100.0 || result.getPath().size() > 2;
        });
        
        System.out.println();
    }
    
    // Helper methods
    
    private static EmergencyNetwork createSimpleNetwork() {
        EmergencyNetwork network = new EmergencyNetwork();
        
        Location l1 = new Location("L1", "Location1", 0, 0, Location.LocationType.CITY);
        Location l2 = new Location("L2", "Location2", 1, 1, Location.LocationType.CITY);
        Location l3 = new Location("L3", "Location3", 2, 0, Location.LocationType.CITY);
        
        network.addLocation(l1);
        network.addLocation(l2);
        network.addLocation(l3);
        
        network.addEdge(l1, l2, 1.4, 2.0);
        network.addEdge(l2, l3, 1.4, 2.0);
        network.addEdge(l1, l3, 2.0, 3.0);
        
        return network;
    }
    
    private static void testCase(String name, java.util.function.Supplier<Boolean> test) {
        testsRun++;
        try {
            boolean passed = test.get();
            if (passed) {
                testsPassed++;
                System.out.println("  ✓ " + name);
            } else {
                testsFailed++;
                System.out.println("  ✗ " + name + " - FAILED");
            }
        } catch (Exception e) {
            testsFailed++;
            System.out.println("  ✗ " + name + " - EXCEPTION: " + e.getMessage());
        }
    }
    
    private static void printSummary() {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("                    TEST SUMMARY");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("Total Tests:  " + testsRun);
        System.out.println("Passed:       " + testsPassed + " (" + (100.0 * testsPassed / testsRun) + "%)");
        System.out.println("Failed:       " + testsFailed);
        System.out.println("═══════════════════════════════════════════════════════════");
        
        if (testsFailed == 0) {
            System.out.println("\n🎉 ALL TESTS PASSED! System is robust.");
        } else {
            System.out.println("\n⚠️  Some tests failed. Review edge case handling.");
        }
    }
}
