package perds.algorithms;

import perds.models.*;

import java.util.*;

/**
 * Manages dispatch decisions and unit allocation
 * Uses priority queue for incident prioritization
 */
public class DispatchManager {
    private final EmergencyNetwork network;
    private final DijkstraPathfinder pathfinder;
    private final List<ResponseUnit> responseUnits;
    private final PriorityQueue<Incident> incidentQueue;
    private final Map<String, Incident> activeIncidents;
    
    public DispatchManager(EmergencyNetwork network) {
        this.network = network;
        this.pathfinder = new DijkstraPathfinder();
        this.responseUnits = new ArrayList<>();
        this.activeIncidents = new HashMap<>();
        
        // Priority queue ordered by incident severity (highest first)
        this.incidentQueue = new PriorityQueue<>((i1, i2) -> 
            Double.compare(i2.getPriorityScore(), i1.getPriorityScore())
        );
    }
    
    /**
     * Register a response unit in the system
     * Time Complexity: O(1)
     */
    public void registerUnit(ResponseUnit unit) {
        responseUnits.add(unit);
    }
    
    /**
     * Report a new incident to the system
     * Time Complexity: O(log n) for priority queue insertion
     */
    public void reportIncident(Incident incident) {
        activeIncidents.put(incident.getId(), incident);
        incidentQueue.offer(incident);
        System.out.println("Incident reported: " + incident);
    }
    
    /**
     * Find the best available unit for an incident
     * Considers: unit availability, unit type compatibility, and distance
     * Time Complexity: O(n * (V + E) log V) where n is number of units
     */
    public DispatchDecision findBestUnit(Incident incident) {
        ResponseUnit bestUnit = null;
        DijkstraPathfinder.PathResult bestPath = null;
        double bestScore = Double.MAX_VALUE;
        
        for (ResponseUnit unit : responseUnits) {
            // Check if unit is available and can respond to this incident type
            if (!unit.isAvailable() || !unit.canRespondTo(incident.getType())) {
                continue;
            }
            
            // Calculate path from unit's location to incident location
            DijkstraPathfinder.PathResult path = pathfinder.findShortestPath(
                network,
                unit.getCurrentLocation(),
                incident.getLocation()
            );
            
            if (!path.isValid()) {
                continue; // No valid path
            }
            
            // Calculate score (lower is better)
            // Score considers distance and severity
            double score = path.getTotalDistance() / incident.getSeverity().getPriority();
            
            if (score < bestScore) {
                bestScore = score;
                bestUnit = unit;
                bestPath = path;
            }
        }
        
        if (bestUnit != null) {
            return new DispatchDecision(bestUnit, incident, bestPath);
        }
        
        return null; // No suitable unit found
    }
    
    /**
     * Dispatch the next highest-priority incident
     * Time Complexity: O(n * (V + E) log V)
     */
    public DispatchDecision dispatchNext() {
        if (incidentQueue.isEmpty()) {
            return null;
        }
        
        // Get highest priority incident
        Incident incident = incidentQueue.poll();
        
        // Skip if already dispatched
        if (incident.getStatus() != Incident.IncidentStatus.REPORTED) {
            return dispatchNext(); // Try next incident
        }
        
        // Find best unit
        DispatchDecision decision = findBestUnit(incident);
        
        if (decision != null) {
            // Update incident and unit status
            incident.setStatus(Incident.IncidentStatus.DISPATCHED);
            incident.setAssignedUnit(decision.getUnit());
            
            decision.getUnit().setStatus(ResponseUnit.UnitStatus.DISPATCHED);
            decision.getUnit().setCurrentIncident(incident);
            
            System.out.println("Dispatched: " + decision);
            return decision;
        } else {
            // No available unit, requeue incident
            incidentQueue.offer(incident);
            System.out.println("No available unit for incident: " + incident.getId());
            return null;
        }
    }
    
    /**
     * Dispatch all pending incidents
     * Time Complexity: O(k * n * (V + E) log V) where k is number of incidents
     */
    public List<DispatchDecision> dispatchAll() {
        List<DispatchDecision> decisions = new ArrayList<>();
        
        while (!incidentQueue.isEmpty()) {
            DispatchDecision decision = dispatchNext();
            if (decision != null) {
                decisions.add(decision);
            } else {
                break; // No more available units
            }
        }
        
        return decisions;
    }
    
    /**
     * Mark an incident as resolved and free up the assigned unit
     * Time Complexity: O(1)
     */
    public void resolveIncident(String incidentId) {
        Incident incident = activeIncidents.get(incidentId);
        if (incident != null) {
            incident.setStatus(Incident.IncidentStatus.RESOLVED);
            
            ResponseUnit unit = incident.getAssignedUnit();
            if (unit != null) {
                unit.setStatus(ResponseUnit.UnitStatus.AVAILABLE);
                unit.setCurrentIncident(null);
                unit.setCurrentLocation(incident.getLocation());
            }
            
            activeIncidents.remove(incidentId);
            System.out.println("Incident resolved: " + incidentId);
        }
    }
    
    /**
     * Get all active incidents
     */
    public Collection<Incident> getActiveIncidents() {
        return activeIncidents.values();
    }
    
    /**
     * Get all response units
     */
    public List<ResponseUnit> getResponseUnits() {
        return responseUnits;
    }
    
    /**
     * Get available units count
     */
    public long getAvailableUnitsCount() {
        return responseUnits.stream()
            .filter(ResponseUnit::isAvailable)
            .count();
    }
    
    /**
     * Inner class to represent a dispatch decision
     */
    public static class DispatchDecision {
        private final ResponseUnit unit;
        private final Incident incident;
        private final DijkstraPathfinder.PathResult path;
        
        public DispatchDecision(ResponseUnit unit, Incident incident, 
                               DijkstraPathfinder.PathResult path) {
            this.unit = unit;
            this.incident = incident;
            this.path = path;
        }
        
        public ResponseUnit getUnit() {
            return unit;
        }
        
        public Incident getIncident() {
            return incident;
        }
        
        public DijkstraPathfinder.PathResult getPath() {
            return path;
        }
        
        @Override
        public String toString() {
            return "DispatchDecision{\n" +
                    "  Unit: " + unit.getName() + " (" + unit.getType() + ")\n" +
                    "  Incident: " + incident.getId() + " (" + incident.getSeverity() + ")\n" +
                    "  Distance: " + String.format("%.2f", path.getTotalDistance()) + "\n" +
                    "}";
        }
    }
}
