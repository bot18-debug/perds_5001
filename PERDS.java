package perds;

import perds.algorithms.*;
import perds.models.*;
import perds.simulation.*;
import perds.utils.*;

import java.util.*;

/**
 * Main Predictive Emergency Response Dispatch System (Enhanced Version)
 * Integrates network management, dispatch, predictive analysis, and performance monitoring
 * 
 * Features:
 * - Advanced pathfinding (Dijkstra & A*)
 * - Adaptive resource positioning
 * - Performance metrics and visualization
 * - Time-based simulation engine
 */
public class PERDS {
    private final EmergencyNetwork network;
    private final DispatchManager dispatchManager;
    private final PredictiveAnalyzer predictiveAnalyzer;
    private final ResourcePositioner resourcePositioner;
    private final PerformanceMetrics metrics;
    private int incidentCounter;
    
    public PERDS() {
        this.network = new EmergencyNetwork();
        this.dispatchManager = new DispatchManager(network);
        this.predictiveAnalyzer = new PredictiveAnalyzer();
        this.resourcePositioner = new ResourcePositioner(network, predictiveAnalyzer);
        this.metrics = new PerformanceMetrics();
        this.incidentCounter = 0;
    }
    
    /**
     * Initialize the emergency network with locations and connections
     */
    public void initializeNetwork() {
        System.out.println("=== Initializing Emergency Network ===\n");
        
        // Create dispatch centers
        Location dc1 = new Location("DC1", "Central Dispatch", 0.0, 0.0, 
            Location.LocationType.DISPATCH_CENTER);
        Location dc2 = new Location("DC2", "North Dispatch", 10.0, 10.0, 
            Location.LocationType.DISPATCH_CENTER);
        Location dc3 = new Location("DC3", "South Dispatch", -10.0, -10.0, 
            Location.LocationType.DISPATCH_CENTER);
        
        // Create cities
        Location city1 = new Location("C1", "Downtown", 2.0, 2.0, 
            Location.LocationType.CITY);
        Location city2 = new Location("C2", "Riverside", 5.0, 5.0, 
            Location.LocationType.CITY);
        Location city3 = new Location("C3", "Hillside", 8.0, 3.0, 
            Location.LocationType.CITY);
        Location city4 = new Location("C4", "Westend", -5.0, -5.0, 
            Location.LocationType.CITY);
        Location city5 = new Location("C5", "Eastside", 5.0, -3.0, 
            Location.LocationType.CITY);
        
        // Add locations to network
        network.addLocation(dc1);
        network.addLocation(dc2);
        network.addLocation(dc3);
        network.addLocation(city1);
        network.addLocation(city2);
        network.addLocation(city3);
        network.addLocation(city4);
        network.addLocation(city5);
        
        // Add connections (bidirectional edges with distance and travel time)
        network.addEdge(dc1, city1, 2.8, 5.0);
        network.addEdge(dc1, city4, 7.1, 12.0);
        network.addEdge(dc1, city5, 5.8, 10.0);
        network.addEdge(city1, city2, 4.2, 8.0);
        network.addEdge(city2, city3, 4.5, 9.0);
        network.addEdge(city2, dc2, 7.1, 13.0);
        network.addEdge(city3, dc2, 7.8, 14.0);
        network.addEdge(city4, dc3, 7.1, 12.0);
        network.addEdge(city5, city3, 6.7, 11.0);
        network.addEdge(dc2, dc3, 28.3, 45.0);
        
        System.out.println(network);
        System.out.println();
    }
    
    /**
     * Initialize response units at various dispatch centers
     */
    public void initializeResponseUnits() {
        System.out.println("=== Initializing Response Units ===\n");
        
        Location dc1 = network.getLocation("DC1");
        Location dc2 = network.getLocation("DC2");
        Location dc3 = network.getLocation("DC3");
        
        // Fire units
        ResponseUnit fire1 = new ResponseUnit("FIRE-01", "Engine 1", 
            ResponseUnit.UnitType.FIRE_ENGINE, dc1);
        ResponseUnit fire2 = new ResponseUnit("FIRE-02", "Engine 2", 
            ResponseUnit.UnitType.FIRE_ENGINE, dc2);
        
        // Medical units
        ResponseUnit med1 = new ResponseUnit("MED-01", "Ambulance 1", 
            ResponseUnit.UnitType.AMBULANCE, dc1);
        ResponseUnit med2 = new ResponseUnit("MED-02", "Ambulance 2", 
            ResponseUnit.UnitType.AMBULANCE, dc2);
        ResponseUnit med3 = new ResponseUnit("MED-03", "Ambulance 3", 
            ResponseUnit.UnitType.AMBULANCE, dc3);
        
        // Police units
        ResponseUnit police1 = new ResponseUnit("POL-01", "Police Car 1", 
            ResponseUnit.UnitType.POLICE_CAR, dc1);
        ResponseUnit police2 = new ResponseUnit("POL-02", "Police Car 2", 
            ResponseUnit.UnitType.POLICE_CAR, dc3);
        
        // Register units
        dispatchManager.registerUnit(fire1);
        dispatchManager.registerUnit(fire2);
        dispatchManager.registerUnit(med1);
        dispatchManager.registerUnit(med2);
        dispatchManager.registerUnit(med3);
        dispatchManager.registerUnit(police1);
        dispatchManager.registerUnit(police2);
        
        System.out.println("Registered " + dispatchManager.getResponseUnits().size() + " units");
        for (ResponseUnit unit : dispatchManager.getResponseUnits()) {
            System.out.println("  - " + unit);
        }
        System.out.println();
    }
    
    /**
     * Simulate incidents occurring in the network
     */
    public void simulateIncidents() {
        System.out.println("=== Simulating Incidents ===\n");
        
        Location city1 = network.getLocation("C1");
        Location city2 = network.getLocation("C2");
        Location city3 = network.getLocation("C3");
        Location city4 = network.getLocation("C4");
        Location city5 = network.getLocation("C5");
        
        // Create various incidents
        createAndReportIncident(city1, Incident.IncidentType.MEDICAL, 
            Incident.IncidentSeverity.HIGH);
        createAndReportIncident(city2, Incident.IncidentType.FIRE, 
            Incident.IncidentSeverity.CRITICAL);
        createAndReportIncident(city3, Incident.IncidentType.POLICE, 
            Incident.IncidentSeverity.MEDIUM);
        createAndReportIncident(city4, Incident.IncidentType.MEDICAL, 
            Incident.IncidentSeverity.LOW);
        createAndReportIncident(city5, Incident.IncidentType.MEDICAL, 
            Incident.IncidentSeverity.CRITICAL);
        
        System.out.println();
    }
    
    /**
     * Helper method to create and report an incident
     */
    private void createAndReportIncident(Location location, Incident.IncidentType type, 
                                        Incident.IncidentSeverity severity) {
        String id = "INC-" + String.format("%03d", ++incidentCounter);
        Incident incident = new Incident(id, location, type, severity);
        dispatchManager.reportIncident(incident);
        predictiveAnalyzer.recordIncident(incident);
    }
    
    /**
     * Process all pending incidents
     */
    public void processDispatches() {
        System.out.println("=== Processing Dispatches ===\n");
        
        List<DispatchManager.DispatchDecision> decisions = dispatchManager.dispatchAll();
        
        System.out.println("\nDispatched " + decisions.size() + " units:");
        for (DispatchManager.DispatchDecision decision : decisions) {
            System.out.println(decision);
            metrics.recordDispatch(decision);
        }
        
        System.out.println("Available units remaining: " + 
            dispatchManager.getAvailableUnitsCount());
        System.out.println();
    }
    
    /**
     * Compare Dijkstra vs A* pathfinding performance
     */
    public void comparePathfindingAlgorithms() {
        System.out.println("=== Pathfinding Algorithm Comparison ===\n");
        
        DijkstraPathfinder dijkstra = new DijkstraPathfinder();
        AStarPathfinder aStar = new AStarPathfinder();
        
        Location source = network.getLocation("DC1");
        Location dest = network.getLocation("C3");
        
        if (source == null || dest == null) {
            System.out.println("Required locations not found for comparison.");
            return;
        }
        
        // Benchmark Dijkstra
        long dijkstraStart = System.nanoTime();
        DijkstraPathfinder.PathResult dijkstraResult = dijkstra.findShortestPath(network, source, dest);
        long dijkstraTime = System.nanoTime() - dijkstraStart;
        
        // Benchmark A*
        long aStarStart = System.nanoTime();
        AStarPathfinder.PathResult aStarResult = aStar.findShortestPath(network, source, dest);
        long aStarTime = System.nanoTime() - aStarStart;
        
        System.out.println("Finding path from " + source.getName() + " to " + dest.getName());
        System.out.println("\nDijkstra's Algorithm:");
        System.out.println("  " + dijkstraResult);
        System.out.println("  Execution Time: " + (dijkstraTime / 1000.0) + " μs");
        
        System.out.println("\nA* Algorithm:");
        System.out.println("  " + aStarResult);
        System.out.println("  Execution Time: " + (aStarTime / 1000.0) + " μs");
        
        System.out.printf("\nSpeedup: %.2fx\n", (double)dijkstraTime / aStarTime);
        System.out.println();
    }
    
    /**
     * Demonstrate resource repositioning
     */
    public void demonstrateRepositioning() {
        System.out.println("=== Resource Repositioning Analysis ===\n");
        
        List<ResponseUnit> availableUnits = dispatchManager.getResponseUnits().stream()
            .filter(ResponseUnit::isAvailable)
            .toList();
        
        if (availableUnits.isEmpty()) {
            System.out.println("No available units for repositioning.\n");
            return;
        }
        
        List<ResourcePositioner.RepositioningRecommendation> recommendations = 
            resourcePositioner.analyzeAndRecommend(new ArrayList<>(availableUnits));
        
        if (recommendations.isEmpty()) {
            System.out.println("No repositioning needed - current distribution is optimal.\n");
        } else {
            System.out.println("Repositioning Recommendations:");
            for (ResourcePositioner.RepositioningRecommendation rec : recommendations) {
                System.out.println("  • " + rec);
            }
            
            // Apply first recommendation
            if (!recommendations.isEmpty()) {
                System.out.println("\nApplying first recommendation...");
                resourcePositioner.applyRepositioning(recommendations.get(0));
            }
        }
        
        // Calculate load balance
        Map<Location, Double> demandScores = predictiveAnalyzer.calculateDemandScores();
        double loadBalance = resourcePositioner.calculateLoadBalance(
            dispatchManager.getResponseUnits(), demandScores);
        
        System.out.printf("\nCurrent Load Balance Score: %.2f (lower is better)\n", loadBalance);
        System.out.println();
    }
    
    /**
     * Perform predictive analysis
     */
    public void performPredictiveAnalysis() {
        System.out.println("=== Predictive Analysis ===\n");
        
        Map<Location, Double> demandScores = predictiveAnalyzer.calculateDemandScores();
        
        System.out.println("Demand Scores by Location:");
        demandScores.entrySet().stream()
            .sorted(Map.Entry.<Location, Double>comparingByValue().reversed())
            .forEach(entry -> 
                System.out.printf("  %s: %.2f\n", 
                    entry.getKey().getName(), entry.getValue())
            );
        
        System.out.println("\nHigh-Demand Locations (Top 3):");
        List<Location> highDemand = predictiveAnalyzer.identifyHighDemandLocations(3);
        for (int i = 0; i < highDemand.size(); i++) {
            Location loc = highDemand.get(i);
            double prob = predictiveAnalyzer.predictIncidentProbability(loc);
            System.out.printf("  %d. %s (Incident Probability: %.2f%%)\n", 
                i + 1, loc.getName(), prob * 100);
        }
        System.out.println();
    }
    
    /**
     * Demonstrate dynamic network updates
     */
    public void demonstrateDynamicUpdates() {
        System.out.println("=== Dynamic Network Updates ===\n");
        
        // Simulate road closure
        System.out.println("Simulating road closure between Downtown and Riverside...");
        network.setEdgeBlocked("C1", "C2", true);
        
        // Simulate traffic congestion
        System.out.println("Simulating traffic congestion on route to Hillside...");
        network.updateEdgeWeight("C2", "C3", 18.0); // Double the travel time
        
        // Add new incident after network changes
        Location city3 = network.getLocation("C3");
        System.out.println("\nNew incident reported at Hillside after network changes...");
        createAndReportIncident(city3, Incident.IncidentType.FIRE, 
            Incident.IncidentSeverity.HIGH);
        
        // Show how dispatch adapts
        DispatchManager.DispatchDecision decision = dispatchManager.dispatchNext();
        if (decision != null) {
            System.out.println("\nDispatch decision with updated network:");
            System.out.println(decision);
            System.out.println("Note: Route automatically adjusted for blocked roads and congestion");
        }
        
        System.out.println();
    }
    
    /**
     * Display system statistics
     */
    public void displayStatistics() {
        System.out.println("=== System Statistics ===\n");
        System.out.println("Network:");
        System.out.println("  Locations: " + network.getLocationCount());
        System.out.println("  Connections: " + network.getEdgeCount());
        System.out.println("\nResponse Units:");
        System.out.println("  Total: " + dispatchManager.getResponseUnits().size());
        System.out.println("  Available: " + dispatchManager.getAvailableUnitsCount());
        System.out.println("\nIncidents:");
        System.out.println("  Active: " + dispatchManager.getActiveIncidents().size());
        System.out.println("  Total Reported: " + incidentCounter);
        System.out.println();
    }
    
    /**
     * Run advanced simulation with performance tracking
     */
    public void runAdvancedSimulation() {
        System.out.println("=== Advanced Simulation with Performance Tracking ===\n");
        
        SimulationEngine.SimulationConfig config = SimulationEngine.SimulationConfig.getDefault();
        SimulationEngine simulation = new SimulationEngine(
            network, dispatchManager, predictiveAnalyzer, config
        );
        
        // Run 8-hour simulation
        int simulationDuration = 8 * 60; // 480 minutes
        SimulationEngine.SimulationResult result = simulation.runSimulation(simulationDuration);
        
        // Display results
        System.out.println(result.getMetrics().generateReport());
        
        // Visualizations
        System.out.println(DataVisualizer.generatePerformanceSummaryTable(result.getMetrics()));
        System.out.println(DataVisualizer.generateResponseTimeHistogram(result.getMetrics()));
        System.out.println(DataVisualizer.generateUnitTypeComparison(result.getMetrics()));
        System.out.println(DataVisualizer.generateSeverityDistribution(result.getMetrics()));
        System.out.println(DataVisualizer.generateIncidentTimeline(result.getEvents(), 30));
    }
    
    /**
     * Display comprehensive performance metrics
     */
    public void displayPerformanceMetrics() {
        System.out.println("=== Performance Metrics ===\n");
        System.out.println(metrics.generateReport());
        System.out.println(DataVisualizer.generatePerformanceSummaryTable(metrics));
    }
    
    /**
     * Export data for external analysis
     */
    public void exportData() {
        System.out.println("=== Exporting Data ===\n");
        
        String csvData = DataVisualizer.exportToCSV(metrics);
        System.out.println("CSV Export (first 5 rows):");
        String[] lines = csvData.split("\n");
        for (int i = 0; i < Math.min(6, lines.length); i++) {
            System.out.println(lines[i]);
        }
        System.out.println("...");
        System.out.println("Total records: " + (lines.length - 1));
        System.out.println();
    }
    
    // Getters for testing and external access
    public EmergencyNetwork getNetwork() {
        return network;
    }
    
    public DispatchManager getDispatchManager() {
        return dispatchManager;
    }
    
    public PredictiveAnalyzer getPredictiveAnalyzer() {
        return predictiveAnalyzer;
    }
    
    public PerformanceMetrics getMetrics() {
        return metrics;
    }
    
    /**
     * Main method to run comprehensive demonstration
     */
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  Predictive Emergency Response Dispatch System (PERDS)    ║");
        System.out.println("║  Enhanced Version with Advanced Features                  ║");
        System.out.println("║  CPS5001 - Data Structures and Algorithms                 ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        PERDS system = new PERDS();
        
        // Initialize system
        system.initializeNetwork();
        system.initializeResponseUnits();
        
        // Basic simulation
        system.simulateIncidents();
        system.processDispatches();
        
        // Predictive analysis
        system.performPredictiveAnalysis();
        
        // Algorithm comparison
        system.comparePathfindingAlgorithms();
        
        // Resource repositioning
        system.demonstrateRepositioning();
        
        // Dynamic updates
        system.demonstrateDynamicUpdates();
        
        // Display basic statistics
        system.displayStatistics();
        
        // Display performance metrics
        system.displayPerformanceMetrics();
        
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  RUNNING ADVANCED TIME-BASED SIMULATION                   ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        // Run advanced simulation
        system.runAdvancedSimulation();
        
        // Export data
        system.exportData();
        
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║  ALL DEMONSTRATIONS COMPLETE                               ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
    }
}
