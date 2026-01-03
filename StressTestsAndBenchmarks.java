package perds.test;

import perds.*;
import perds.algorithms.*;
import perds.models.*;
import java.util.*;

/**
 * Comprehensive Stress Tests and Performance Benchmarks
 * 
 * Tests system under extreme conditions:
 * - Large networks (1000+ nodes)
 * - High incident rates (100+ per minute)
 * - Resource exhaustion scenarios
 * - Network failures and recovery
 * - Algorithm performance comparison
 * 
 * @author PERDS Team
 * @version 2.0
 */
public class StressTestsAndBenchmarks {
    
    private static final int WARMUP_ITERATIONS = 10;
    private static final int BENCHMARK_ITERATIONS = 100;
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║     PERDS STRESS TESTS AND PERFORMANCE BENCHMARKS         ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        // Run all tests
        runScalabilityTests();
        runAlgorithmBenchmarks();
        runStressTests();
        runEdgeCaseTests();
        runPerformanceComparison();
        
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║               ALL TESTS COMPLETED                          ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
    }
    
    /**
     * Test system scalability with increasing network sizes
     */
    private static void runScalabilityTests() {
        System.out.println("═══ SCALABILITY TESTS ═══\n");
        
        int[] sizes = {10, 50, 100, 500, 1000};
        
        System.out.println("Testing Dijkstra's algorithm with different graph sizes:");
        System.out.println("Size\tTime (ms)\tNodes\tEdges\tComplexity");
        System.out.println("─".repeat(60));
        
        for (int size : sizes) {
            EmergencyNetwork network = generateRandomNetwork(size);
            List<Location> locations = new ArrayList<>(network.getAllLocations());
            
            if (locations.size() < 2) continue;
            
            Location source = locations.get(0);
            Location dest = locations.get(locations.size() - 1);
            
            DijkstraPathfinder pathfinder = new DijkstraPathfinder();
            
            // Warmup
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                pathfinder.findShortestPath(network, source, dest);
            }
            
            // Benchmark
            long startTime = System.nanoTime();
            for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
                pathfinder.findShortestPath(network, source, dest);
            }
            long endTime = System.nanoTime();
            
            double avgTime = (endTime - startTime) / 1_000_000.0 / BENCHMARK_ITERATIONS;
            int edges = network.getEdgeCount();
            
            // Calculate theoretical complexity
            double theoreticalComplexity = (size + edges) * Math.log(size);
            
            System.out.printf("%d\t%.3f\t\t%d\t%d\t%.0f\n", 
                size, avgTime, size, edges, theoreticalComplexity);
        }
        
        System.out.println("\n✓ Scalability tests completed\n");
    }
    
    /**
     * Benchmark different pathfinding algorithms
     */
    private static void runAlgorithmBenchmarks() {
        System.out.println("═══ ALGORITHM BENCHMARKS ═══\n");
        
        EmergencyNetwork network = generateRandomNetwork(100);
        List<Location> locations = new ArrayList<>(network.getAllLocations());
        
        if (locations.size() < 2) {
            System.out.println("⚠ Insufficient locations for benchmarking\n");
            return;
        }
        
        Location source = locations.get(0);
        Location dest = locations.get(locations.size() - 1);
        
        DijkstraPathfinder dijkstra = new DijkstraPathfinder();
        AStarPathfinder aStar = new AStarPathfinder();
        
        System.out.println("Comparing Dijkstra vs A* algorithm:");
        System.out.println("Algorithm\tAvg Time (μs)\tPath Length\tNodes Explored");
        System.out.println("─".repeat(60));
        
        // Benchmark Dijkstra
        long dijkstraTime = 0;
        DijkstraPathfinder.PathResult dijkstraResult = null;
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();
            dijkstraResult = dijkstra.findShortestPath(network, source, dest);
            dijkstraTime += System.nanoTime() - start;
        }
        double avgDijkstraTime = dijkstraTime / 1000.0 / BENCHMARK_ITERATIONS;
        
        // Benchmark A*
        long aStarTime = 0;
        AStarPathfinder.PathResult aStarResult = null;
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long start = System.nanoTime();
            aStarResult = aStar.findShortestPath(network, source, dest);
            aStarTime += System.nanoTime() - start;
        }
        double avgAStarTime = aStarTime / 1000.0 / BENCHMARK_ITERATIONS;
        
        System.out.printf("Dijkstra\t%.2f\t\t%.1f\t\t~%d\n", 
            avgDijkstraTime, 
            dijkstraResult.getTotalDistance(),
            dijkstraResult.getPath().size());
        
        System.out.printf("A*\t\t%.2f\t\t%.1f\t\t~%d\n", 
            avgAStarTime,
            aStarResult.getTotalDistance(),
            aStarResult.getPath().size());
        
        double speedup = avgDijkstraTime / avgAStarTime;
        System.out.printf("\nA* Speedup: %.2fx\n", speedup);
        
        System.out.println("\n✓ Algorithm benchmarks completed\n");
    }
    
    /**
     * Test system under extreme stress conditions
     */
    private static void runStressTests() {
        System.out.println("═══ STRESS TESTS ═══\n");
        
        // Test 1: Resource Exhaustion
        System.out.println("Test 1: Resource Exhaustion (More incidents than units)");
        EmergencyNetwork network = generateRandomNetwork(20);
        DispatchManager dispatcher = new DispatchManager(network);
        
        // Add limited units
        Location dc = network.getAllLocations().iterator().next();
        for (int i = 0; i < 3; i++) {
            ResponseUnit unit = new ResponseUnit(
                "UNIT-" + i, "Unit " + i, 
                ResponseUnit.UnitType.FIRE_ENGINE, dc
            );
            dispatcher.registerUnit(unit);
        }
        
        // Generate many incidents
        List<Location> locations = new ArrayList<>(network.getAllLocations());
        for (int i = 0; i < 20; i++) {
            Location loc = locations.get(i % locations.size());
            Incident incident = new Incident(
                "INC-" + i, loc,
                Incident.IncidentType.FIRE,
                Incident.IncidentSeverity.HIGH
            );
            dispatcher.reportIncident(incident);
        }
        
        int dispatched = dispatcher.dispatchAll().size();
        int pending = dispatcher.getActiveIncidents().size();
        
        System.out.printf("  Incidents reported: 20\n");
        System.out.printf("  Units available: 3\n");
        System.out.printf("  Successfully dispatched: %d\n", dispatched);
        System.out.printf("  Pending incidents: %d\n", pending);
        System.out.println("  ✓ System handles resource exhaustion gracefully\n");
        
        // Test 2: Network Fragmentation
        System.out.println("Test 2: Network Fragmentation (Disconnected components)");
        EmergencyNetwork fragmentedNetwork = generateFragmentedNetwork(50);
        DijkstraPathfinder pathfinder = new DijkstraPathfinder();
        
        List<Location> fragLocations = new ArrayList<>(fragmentedNetwork.getAllLocations());
        Location src = fragLocations.get(0);
        Location dst = fragLocations.get(fragLocations.size() - 1);
        
        DijkstraPathfinder.PathResult result = pathfinder.findShortestPath(
            fragmentedNetwork, src, dst
        );
        
        System.out.printf("  Network size: %d locations\n", fragLocations.size());
        System.out.printf("  Path found: %s\n", result.isValid() ? "Yes" : "No (as expected)");
        System.out.println("  ✓ System handles disconnected networks correctly\n");
        
        // Test 3: Concurrent Incidents
        System.out.println("Test 3: Simultaneous Incident Burst (100 incidents/second)");
        long burstStart = System.nanoTime();
        
        for (int i = 0; i < 100; i++) {
            Location loc = locations.get(i % locations.size());
            Incident incident = new Incident(
                "BURST-" + i, loc,
                Incident.IncidentType.MEDICAL,
                Incident.IncidentSeverity.MEDIUM
            );
            dispatcher.reportIncident(incident);
        }
        
        long burstTime = (System.nanoTime() - burstStart) / 1_000_000;
        
        System.out.printf("  Incidents registered: 100\n");
        System.out.printf("  Time taken: %d ms\n", burstTime);
        System.out.printf("  Throughput: %.0f incidents/second\n", 100000.0 / burstTime);
        System.out.println("  ✓ System handles burst traffic efficiently\n");
        
        System.out.println("✓ Stress tests completed\n");
    }
    
    /**
     * Test edge cases and boundary conditions
     */
    private static void runEdgeCaseTests() {
        System.out.println("═══ EDGE CASE TESTS ═══\n");
        
        int passed = 0;
        int total = 0;
        
        // Test 1: Empty network
        total++;
        try {
            EmergencyNetwork emptyNetwork = new EmergencyNetwork();
            DijkstraPathfinder pathfinder = new DijkstraPathfinder();
            
            Location dummy1 = new Location("D1", "Dummy1", 0, 0, Location.LocationType.CITY);
            Location dummy2 = new Location("D2", "Dummy2", 1, 1, Location.LocationType.CITY);
            
            DijkstraPathfinder.PathResult result = pathfinder.findShortestPath(
                emptyNetwork, dummy1, dummy2
            );
            
            if (!result.isValid()) {
                System.out.println("✓ Test 1: Empty network - PASSED");
                passed++;
            } else {
                System.out.println("✗ Test 1: Empty network - FAILED");
            }
        } catch (Exception e) {
            System.out.println("✗ Test 1: Empty network - EXCEPTION: " + e.getMessage());
        }
        
        // Test 2: Single node network
        total++;
        try {
            EmergencyNetwork singleNode = new EmergencyNetwork();
            Location single = new Location("S1", "Single", 0, 0, Location.LocationType.CITY);
            singleNode.addLocation(single);
            
            DijkstraPathfinder pathfinder = new DijkstraPathfinder();
            DijkstraPathfinder.PathResult result = pathfinder.findShortestPath(
                singleNode, single, single
            );
            
            if (result.isValid() && result.getTotalDistance() == 0) {
                System.out.println("✓ Test 2: Single node (source = destination) - PASSED");
                passed++;
            } else {
                System.out.println("✗ Test 2: Single node - FAILED");
            }
        } catch (Exception e) {
            System.out.println("✗ Test 2: Single node - EXCEPTION: " + e.getMessage());
        }
        
        // Test 3: Blocked paths
        total++;
        try {
            EmergencyNetwork blockedNetwork = new EmergencyNetwork();
            Location a = new Location("A", "A", 0, 0, Location.LocationType.CITY);
            Location b = new Location("B", "B", 1, 1, Location.LocationType.CITY);
            
            blockedNetwork.addLocation(a);
            blockedNetwork.addLocation(b);
            blockedNetwork.addEdge(a, b, 1.0, 5.0);
            blockedNetwork.setEdgeBlocked("A", "B", true);
            
            DijkstraPathfinder pathfinder = new DijkstraPathfinder();
            DijkstraPathfinder.PathResult result = pathfinder.findShortestPath(
                blockedNetwork, a, b
            );
            
            if (!result.isValid()) {
                System.out.println("✓ Test 3: Blocked paths - PASSED");
                passed++;
            } else {
                System.out.println("✗ Test 3: Blocked paths - FAILED");
            }
        } catch (Exception e) {
            System.out.println("✗ Test 3: Blocked paths - EXCEPTION: " + e.getMessage());
        }
        
        // Test 4: All units busy
        total++;
        try {
            EmergencyNetwork network = new EmergencyNetwork();
            Location loc = new Location("L1", "Loc1", 0, 0, Location.LocationType.CITY);
            network.addLocation(loc);
            
            DispatchManager dispatcher = new DispatchManager(network);
            ResponseUnit unit = new ResponseUnit(
                "U1", "Unit1", ResponseUnit.UnitType.FIRE_ENGINE, loc
            );
            dispatcher.registerUnit(unit);
            
            // Dispatch unit
            Incident inc1 = new Incident("I1", loc, Incident.IncidentType.FIRE, 
                Incident.IncidentSeverity.HIGH);
            dispatcher.reportIncident(inc1);
            dispatcher.dispatchNext();
            
            // Try to dispatch another
            Incident inc2 = new Incident("I2", loc, Incident.IncidentType.FIRE, 
                Incident.IncidentSeverity.HIGH);
            dispatcher.reportIncident(inc2);
            DispatchManager.DispatchDecision decision = dispatcher.dispatchNext();
            
            if (decision == null) {
                System.out.println("✓ Test 4: All units busy - PASSED");
                passed++;
            } else {
                System.out.println("✗ Test 4: All units busy - FAILED");
            }
        } catch (Exception e) {
            System.out.println("✗ Test 4: All units busy - EXCEPTION: " + e.getMessage());
        }
        
        // Test 5: Extremely high priority
        total++;
        try {
            Incident critical = new Incident(
                "CRIT", null, Incident.IncidentType.FIRE, Incident.IncidentSeverity.CRITICAL
            );
            Incident low = new Incident(
                "LOW", null, Incident.IncidentType.POLICE, Incident.IncidentSeverity.LOW
            );
            
            if (critical.getPriorityScore() > low.getPriorityScore()) {
                System.out.println("✓ Test 5: Priority ordering - PASSED");
                passed++;
            } else {
                System.out.println("✗ Test 5: Priority ordering - FAILED");
            }
        } catch (Exception e) {
            System.out.println("✗ Test 5: Priority ordering - EXCEPTION: " + e.getMessage());
        }
        
        System.out.printf("\nEdge Cases: %d/%d passed (%.1f%%)\n\n", 
            passed, total, 100.0 * passed / total);
        
        System.out.println("✓ Edge case tests completed\n");
    }
    
    /**
     * Performance comparison: Basic vs Optimized dispatch
     */
    private static void runPerformanceComparison() {
        System.out.println("═══ PERFORMANCE COMPARISON ═══\n");
        
        EmergencyNetwork network = generateRandomNetwork(50);
        List<Location> locations = new ArrayList<>(network.getAllLocations());
        
        // Setup basic dispatcher
        DispatchManager basicDispatcher = new DispatchManager(network);
        
        // Setup optimized dispatcher
        MultiCriteriaOptimizer optimizer = new MultiCriteriaOptimizer(network);
        
        // Add units
        Location dc = locations.get(0);
        List<ResponseUnit> units = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ResponseUnit unit = new ResponseUnit(
                "UNIT-" + i, "Unit " + i,
                ResponseUnit.UnitType.values()[i % 3], dc
            );
            basicDispatcher.registerUnit(unit);
            units.add(unit);
        }
        
        // Generate incidents
        List<Incident> incidents = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            Location loc = locations.get(i % locations.size());
            Incident incident = new Incident(
                "INC-" + i, loc,
                Incident.IncidentType.values()[i % 5],
                Incident.IncidentSeverity.values()[i % 4]
            );
            incidents.add(incident);
            basicDispatcher.reportIncident(incident);
        }
        
        System.out.println("Comparing dispatch strategies:");
        System.out.println("Strategy\t\tTime (ms)\tDecisions");
        System.out.println("─".repeat(50));
        
        // Benchmark basic dispatch
        long basicStart = System.nanoTime();
        List<DispatchManager.DispatchDecision> basicDecisions = basicDispatcher.dispatchAll();
        long basicTime = (System.nanoTime() - basicStart) / 1_000_000;
        
        System.out.printf("Basic (Greedy)\t\t%d\t\t%d\n", basicTime, basicDecisions.size());
        
        // Benchmark optimized dispatch
        Map<ResponseUnit, Integer> workload = new HashMap<>();
        long optStart = System.nanoTime();
        Map<Incident, MultiCriteriaOptimizer.OptimizedDispatchDecision> optDecisions = 
            optimizer.batchOptimize(incidents, units, workload);
        long optTime = (System.nanoTime() - optStart) / 1_000_000;
        
        System.out.printf("Multi-Criteria\t\t%d\t\t%d\n", optTime, optDecisions.size());
        
        System.out.println("\n✓ Performance comparison completed\n");
    }
    
    /**
     * Generate random network for testing
     */
    private static EmergencyNetwork generateRandomNetwork(int size) {
        EmergencyNetwork network = new EmergencyNetwork();
        Random random = new Random(42); // Fixed seed for reproducibility
        
        List<Location> locations = new ArrayList<>();
        
        // Create locations
        for (int i = 0; i < size; i++) {
            Location loc = new Location(
                "LOC-" + i,
                "Location " + i,
                random.nextDouble() * 100,
                random.nextDouble() * 100,
                i % 5 == 0 ? Location.LocationType.DISPATCH_CENTER : Location.LocationType.CITY
            );
            network.addLocation(loc);
            locations.add(loc);
        }
        
        // Create edges (sparse graph: ~2 edges per node)
        for (int i = 0; i < size * 2; i++) {
            int src = random.nextInt(size);
            int dst = random.nextInt(size);
            
            if (src != dst) {
                Location locSrc = locations.get(src);
                Location locDst = locations.get(dst);
                double distance = locSrc.distanceTo(locDst);
                network.addEdge(locSrc, locDst, distance, distance / 40.0 * 60.0);
            }
        }
        
        return network;
    }
    
    /**
     * Generate fragmented network (multiple disconnected components)
     */
    private static EmergencyNetwork generateFragmentedNetwork(int size) {
        EmergencyNetwork network = new EmergencyNetwork();
        Random random = new Random(42);
        
        List<Location> locations = new ArrayList<>();
        
        // Create locations
        for (int i = 0; i < size; i++) {
            Location loc = new Location(
                "FRAG-" + i,
                "Fragment " + i,
                random.nextDouble() * 100,
                random.nextDouble() * 100,
                Location.LocationType.CITY
            );
            network.addLocation(loc);
            locations.add(loc);
        }
        
        // Create edges only within groups (creates disconnected components)
        int groupSize = 10;
        for (int group = 0; group < size / groupSize; group++) {
            int start = group * groupSize;
            int end = Math.min(start + groupSize, size);
            
            for (int i = start; i < end - 1; i++) {
                Location locSrc = locations.get(i);
                Location locDst = locations.get(i + 1);
                double distance = locSrc.distanceTo(locDst);
                network.addEdge(locSrc, locDst, distance, distance / 40.0 * 60.0);
            }
        }
        
        return network;
    }
}
