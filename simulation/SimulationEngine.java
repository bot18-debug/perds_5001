package perds.simulation;

import perds.models.*;
import perds.algorithms.*;
import perds.utils.PerformanceMetrics;

import java.util.*;

/**
 * Time-based simulation engine for PERDS
 * Simulates realistic incident patterns over time
 * Enables performance testing under various load conditions
 */
public class SimulationEngine {
    private final EmergencyNetwork network;
    private final DispatchManager dispatchManager;
    private final PredictiveAnalyzer predictiveAnalyzer;
    private final ResourcePositioner resourcePositioner;
    private final PerformanceMetrics metrics;
    
    private int currentTime; // Simulation time in minutes
    private int incidentCounter;
    private final Random random;
    
    // Simulation configuration
    private final SimulationConfig config;
    
    public SimulationEngine(EmergencyNetwork network, DispatchManager dispatchManager,
                          PredictiveAnalyzer predictiveAnalyzer, SimulationConfig config) {
        this.network = network;
        this.dispatchManager = dispatchManager;
        this.predictiveAnalyzer = predictiveAnalyzer;
        this.resourcePositioner = new ResourcePositioner(network, predictiveAnalyzer);
        this.metrics = new PerformanceMetrics();
        this.config = config;
        this.currentTime = 0;
        this.incidentCounter = 0;
        this.random = new Random(config.getRandomSeed());
    }
    
    /**
     * Run simulation for specified duration
     */
    public SimulationResult runSimulation(int durationMinutes) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║          STARTING TIME-BASED SIMULATION                    ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        System.out.printf("Duration: %d minutes (%d hours)\n", durationMinutes, durationMinutes / 60);
        System.out.printf("Incident Rate: %.2f per hour\n", config.getIncidentRate());
        System.out.printf("Repositioning Enabled: %s\n\n", config.isRepositioningEnabled());
        
        long startTime = System.currentTimeMillis();
        List<SimulationEvent> events = new ArrayList<>();
        
        // Main simulation loop
        for (currentTime = 0; currentTime < durationMinutes; currentTime++) {
            // Generate incidents probabilistically
            if (shouldGenerateIncident()) {
                Incident incident = generateRandomIncident();
                dispatchManager.reportIncident(incident);
                predictiveAnalyzer.recordIncident(incident);
                events.add(new SimulationEvent(currentTime, "INCIDENT_GENERATED", incident.getId()));
            }
            
            // Process dispatches
            DispatchManager.DispatchDecision decision = dispatchManager.dispatchNext();
            if (decision != null) {
                metrics.recordDispatch(decision);
                events.add(new SimulationEvent(currentTime, "UNIT_DISPATCHED", 
                    decision.getUnit().getName() + " -> " + decision.getIncident().getId()));
            }
            
            // Resolve incidents (simulate completion after response time)
            resolveCompletedIncidents();
            
            // Periodic resource repositioning
            if (config.isRepositioningEnabled() && currentTime % config.getRepositioningInterval() == 0) {
                performRepositioning();
            }
            
            // Progress indicator
            if (currentTime % (durationMinutes / 10) == 0) {
                System.out.printf("Progress: %d%% (Time: %d min)\n", 
                    (currentTime * 100) / durationMinutes, currentTime);
            }
        }
        
        long endTime = System.currentTimeMillis();
        double executionTime = (endTime - startTime) / 1000.0;
        
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║          SIMULATION COMPLETE                               ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");
        
        System.out.printf("Execution Time: %.2f seconds\n", executionTime);
        System.out.printf("Total Incidents: %d\n", incidentCounter);
        System.out.printf("Incidents per Hour: %.2f\n\n", (double) incidentCounter / (durationMinutes / 60.0));
        
        return new SimulationResult(metrics, events, executionTime, incidentCounter);
    }
    
    /**
     * Determine if incident should be generated at current time
     */
    private boolean shouldGenerateIncident() {
        // Poisson process: incident rate per minute
        double lambda = config.getIncidentRate() / 60.0; // Convert hourly rate to per-minute
        double probability = 1.0 - Math.exp(-lambda);
        
        return random.nextDouble() < probability;
    }
    
    /**
     * Generate random incident based on historical patterns
     */
    private Incident generateRandomIncident() {
        // Select random location (weighted by historical demand if available)
        Location location = selectLocationByDemand();
        
        // Random incident type
        Incident.IncidentType[] types = Incident.IncidentType.values();
        Incident.IncidentType type = types[random.nextInt(types.length)];
        
        // Random severity (weighted toward lower severity)
        Incident.IncidentSeverity severity = selectSeverityWeighted();
        
        String id = "SIM-" + String.format("%04d", ++incidentCounter);
        return new Incident(id, location, type, severity);
    }
    
    /**
     * Select location weighted by demand probability
     */
    private Location selectLocationByDemand() {
        Collection<Location> locations = network.getAllLocations();
        
        if (locations.isEmpty()) {
            throw new IllegalStateException("Network has no locations");
        }
        
        // Get demand probabilities
        Map<Location, Double> demandScores = predictiveAnalyzer.calculateDemandScores();
        
        if (demandScores.isEmpty()) {
            // No historical data, select randomly
            List<Location> locationList = new ArrayList<>(locations);
            return locationList.get(random.nextInt(locationList.size()));
        }
        
        // Weighted random selection
        double totalDemand = demandScores.values().stream().mapToDouble(Double::doubleValue).sum();
        double randomValue = random.nextDouble() * totalDemand;
        
        double cumulative = 0.0;
        for (Map.Entry<Location, Double> entry : demandScores.entrySet()) {
            cumulative += entry.getValue();
            if (cumulative >= randomValue) {
                return entry.getKey();
            }
        }
        
        // Fallback
        return demandScores.keySet().iterator().next();
    }
    
    /**
     * Select severity with realistic weighting
     */
    private Incident.IncidentSeverity selectSeverityWeighted() {
        double rand = random.nextDouble();
        
        // Weighted distribution: LOW 50%, MEDIUM 30%, HIGH 15%, CRITICAL 5%
        if (rand < 0.50) return Incident.IncidentSeverity.LOW;
        if (rand < 0.80) return Incident.IncidentSeverity.MEDIUM;
        if (rand < 0.95) return Incident.IncidentSeverity.HIGH;
        return Incident.IncidentSeverity.CRITICAL;
    }
    
    /**
     * Resolve incidents that have been ongoing for their response time
     */
    private void resolveCompletedIncidents() {
        List<Incident> toResolve = new ArrayList<>();
        
        for (Incident incident : dispatchManager.getActiveIncidents()) {
            if (incident.getStatus() == Incident.IncidentStatus.DISPATCHED) {
                // Simulate incident resolution after some time
                // In real system, would track actual response completion
                if (random.nextDouble() < 0.1) { // 10% chance per minute
                    toResolve.add(incident);
                }
            }
        }
        
        for (Incident incident : toResolve) {
            dispatchManager.resolveIncident(incident.getId());
        }
    }
    
    /**
     * Perform resource repositioning based on predictive analysis
     */
    private void performRepositioning() {
        List<ResponseUnit> availableUnits = dispatchManager.getResponseUnits().stream()
            .filter(ResponseUnit::isAvailable)
            .toList();
        
        if (availableUnits.isEmpty()) {
            return;
        }
        
        List<ResourcePositioner.RepositioningRecommendation> recommendations = 
            resourcePositioner.analyzeAndRecommend(new ArrayList<>(availableUnits));
        
        if (!recommendations.isEmpty()) {
            System.out.printf("\n[Time: %d] Resource Repositioning:\n", currentTime);
            for (ResourcePositioner.RepositioningRecommendation rec : recommendations) {
                resourcePositioner.applyRepositioning(rec);
                System.out.println("  - " + rec);
            }
            System.out.println();
        }
    }
    
    public PerformanceMetrics getMetrics() {
        return metrics;
    }
    
    /**
     * Simulation configuration
     */
    public static class SimulationConfig {
        private final double incidentRate; // Incidents per hour
        private final boolean repositioningEnabled;
        private final int repositioningInterval; // Minutes between repositioning checks
        private final long randomSeed;
        
        public SimulationConfig(double incidentRate, boolean repositioningEnabled, 
                              int repositioningInterval, long randomSeed) {
            this.incidentRate = incidentRate;
            this.repositioningEnabled = repositioningEnabled;
            this.repositioningInterval = repositioningInterval;
            this.randomSeed = randomSeed;
        }
        
        public static SimulationConfig getDefault() {
            return new SimulationConfig(
                12.0,  // 12 incidents per hour
                true,  // Enable repositioning
                30,    // Reposition every 30 minutes
                42     // Random seed for reproducibility
            );
        }
        
        public double getIncidentRate() { return incidentRate; }
        public boolean isRepositioningEnabled() { return repositioningEnabled; }
        public int getRepositioningInterval() { return repositioningInterval; }
        public long getRandomSeed() { return randomSeed; }
    }
    
    /**
     * Simulation event for tracking
     */
    public static class SimulationEvent {
        private final int timeMinutes;
        private final String eventType;
        private final String description;
        
        public SimulationEvent(int timeMinutes, String eventType, String description) {
            this.timeMinutes = timeMinutes;
            this.eventType = eventType;
            this.description = description;
        }
        
        public int getTimeMinutes() { return timeMinutes; }
        public String getEventType() { return eventType; }
        public String getDescription() { return description; }
    }
    
    /**
     * Simulation results
     */
    public static class SimulationResult {
        private final PerformanceMetrics metrics;
        private final List<SimulationEvent> events;
        private final double executionTimeSeconds;
        private final int totalIncidents;
        
        public SimulationResult(PerformanceMetrics metrics, List<SimulationEvent> events,
                              double executionTimeSeconds, int totalIncidents) {
            this.metrics = metrics;
            this.events = events;
            this.executionTimeSeconds = executionTimeSeconds;
            this.totalIncidents = totalIncidents;
        }
        
        public PerformanceMetrics getMetrics() { return metrics; }
        public List<SimulationEvent> getEvents() { return events; }
        public double getExecutionTimeSeconds() { return executionTimeSeconds; }
        public int getTotalIncidents() { return totalIncidents; }
    }
}
