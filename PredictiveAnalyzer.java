package perds.algorithms;

import perds.models.Incident;
import perds.models.Location;

import java.util.*;

/**
 * Analyzes incident patterns to predict high-demand areas
 * Uses historical data to inform resource pre-positioning
 */
public class PredictiveAnalyzer {
    private final Map<Location, IncidentHistory> locationHistory;
    private static final int HISTORY_WINDOW = 100; // Track last 100 incidents per location
    
    public PredictiveAnalyzer() {
        this.locationHistory = new HashMap<>();
    }
    
    /**
     * Record an incident for predictive analysis
     * Time Complexity: O(1) average case
     */
    public void recordIncident(Incident incident) {
        Location location = incident.getLocation();
        
        // Get or create history for this location
        IncidentHistory history = locationHistory.computeIfAbsent(
            location, 
            k -> new IncidentHistory(location)
        );
        
        history.addIncident(incident);
    }
    
    /**
     * Calculate demand score for each location based on historical data
     * Higher score indicates higher predicted demand
     * Time Complexity: O(n) where n is number of locations
     */
    public Map<Location, Double> calculateDemandScores() {
        Map<Location, Double> demandScores = new HashMap<>();
        
        for (Map.Entry<Location, IncidentHistory> entry : locationHistory.entrySet()) {
            Location location = entry.getKey();
            IncidentHistory history = entry.getValue();
            
            double score = history.calculateDemandScore();
            demandScores.put(location, score);
        }
        
        return demandScores;
    }
    
    /**
     * Identify high-demand locations (top N by demand score)
     * Time Complexity: O(n log k) where k is topN
     */
    public List<Location> identifyHighDemandLocations(int topN) {
        Map<Location, Double> demandScores = calculateDemandScores();
        
        // Use priority queue to get top N locations
        PriorityQueue<Map.Entry<Location, Double>> pq = new PriorityQueue<>(
            (e1, e2) -> Double.compare(e2.getValue(), e1.getValue())
        );
        
        pq.addAll(demandScores.entrySet());
        
        List<Location> highDemandLocations = new ArrayList<>();
        for (int i = 0; i < topN && !pq.isEmpty(); i++) {
            highDemandLocations.add(pq.poll().getKey());
        }
        
        return highDemandLocations;
    }
    
    /**
     * Predict likelihood of incident at a location
     * Returns probability between 0.0 and 1.0
     */
    public double predictIncidentProbability(Location location) {
        IncidentHistory history = locationHistory.get(location);
        
        if (history == null || history.getIncidentCount() == 0) {
            return 0.0;
        }
        
        // Simple probability based on incident frequency
        // More sophisticated models could use time-series analysis
        double recentIncidentRate = history.getRecentIncidentRate();
        return Math.min(1.0, recentIncidentRate / 10.0); // Normalize to 0-1
    }
    
    /**
     * Get incident statistics for a location
     */
    public IncidentHistory getLocationHistory(Location location) {
        return locationHistory.get(location);
    }
    
    /**
     * Get all locations being tracked
     */
    public Set<Location> getTrackedLocations() {
        return locationHistory.keySet();
    }
    
    /**
     * Inner class to track incident history for a location
     */
    public static class IncidentHistory {
        private final Location location;
        private final Queue<Incident> recentIncidents;
        private int totalIncidents;
        private final Map<Incident.IncidentType, Integer> typeCount;
        private final Map<Incident.IncidentSeverity, Integer> severityCount;
        
        public IncidentHistory(Location location) {
            this.location = location;
            this.recentIncidents = new LinkedList<>();
            this.totalIncidents = 0;
            this.typeCount = new HashMap<>();
            this.severityCount = new HashMap<>();
        }
        
        /**
         * Add incident to history
         */
        public void addIncident(Incident incident) {
            totalIncidents++;
            recentIncidents.offer(incident);
            
            // Maintain window size
            if (recentIncidents.size() > HISTORY_WINDOW) {
                recentIncidents.poll();
            }
            
            // Update type count
            typeCount.merge(incident.getType(), 1, Integer::sum);
            
            // Update severity count
            severityCount.merge(incident.getSeverity(), 1, Integer::sum);
        }
        
        /**
         * Calculate demand score based on frequency and severity
         */
        public double calculateDemandScore() {
            if (recentIncidents.isEmpty()) {
                return 0.0;
            }
            
            double frequencyScore = recentIncidents.size();
            double severityScore = 0.0;
            
            for (Incident incident : recentIncidents) {
                severityScore += incident.getSeverity().getPriority();
            }
            
            // Average severity weighted by frequency
            return (frequencyScore * severityScore) / recentIncidents.size();
        }
        
        /**
         * Get recent incident rate (incidents per time unit)
         */
        public double getRecentIncidentRate() {
            return recentIncidents.size();
        }
        
        /**
         * Get most common incident type
         */
        public Incident.IncidentType getMostCommonType() {
            return typeCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        }
        
        public Location getLocation() {
            return location;
        }
        
        public int getIncidentCount() {
            return totalIncidents;
        }
        
        public Map<Incident.IncidentType, Integer> getTypeDistribution() {
            return new HashMap<>(typeCount);
        }
        
        public Map<Incident.IncidentSeverity, Integer> getSeverityDistribution() {
            return new HashMap<>(severityCount);
        }
        
        @Override
        public String toString() {
            return "IncidentHistory{" +
                    "location=" + location.getName() +
                    ", totalIncidents=" + totalIncidents +
                    ", recentCount=" + recentIncidents.size() +
                    ", demandScore=" + String.format("%.2f", calculateDemandScore()) +
                    '}';
        }
    }
}
