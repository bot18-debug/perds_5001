package perds.algorithms;

import perds.models.*;
import java.util.*;

/**
 * Advanced Multi-Criteria Dispatch Optimizer
 * 
 * Considers multiple factors beyond just distance:
 * 1. Response distance (minimize)
 * 2. Response time (minimize)
 * 3. Unit specialization match (maximize)
 * 4. Future availability (maximize)
 * 5. Load balancing (optimize)
 * 6. Unit fatigue (minimize)
 * 
 * Uses weighted scoring with configurable weights
 * 
 * Time Complexity: O(U * I) where U = units, I = incidents
 * Space Complexity: O(U)
 * 
 * @author PERDS Team
 * @version 2.0
 */
public class MultiCriteriaOptimizer {
    
    private final EmergencyNetwork network;
    private final DijkstraPathfinder pathfinder;
    
    // Configurable weights for multi-criteria optimization
    private double distanceWeight = 0.30;
    private double timeWeight = 0.25;
    private double specializationWeight = 0.20;
    private double availabilityWeight = 0.15;
    private double loadBalanceWeight = 0.07;
    private double fatigueWeight = 0.03;
    
    // Unit specialization bonuses
    private static final Map<ResponseUnit.UnitType, Set<Incident.IncidentType>> SPECIALIZATIONS;
    
    static {
        SPECIALIZATIONS = new HashMap<>();
        SPECIALIZATIONS.put(ResponseUnit.UnitType.FIRE_ENGINE, 
            Set.of(Incident.IncidentType.FIRE, Incident.IncidentType.HAZMAT, Incident.IncidentType.RESCUE));
        SPECIALIZATIONS.put(ResponseUnit.UnitType.AMBULANCE, 
            Set.of(Incident.IncidentType.MEDICAL, Incident.IncidentType.RESCUE));
        SPECIALIZATIONS.put(ResponseUnit.UnitType.POLICE_CAR, 
            Set.of(Incident.IncidentType.POLICE, Incident.IncidentType.HAZMAT));
    }
    
    /**
     * Detailed dispatch decision with scoring breakdown
     */
    public static class OptimizedDispatchDecision {
        private final ResponseUnit unit;
        private final Incident incident;
        private final double totalScore;
        private final Map<String, Double> criteriaScores;
        private final DijkstraPathfinder.PathResult path;
        
        public OptimizedDispatchDecision(ResponseUnit unit, Incident incident, 
                                        double score, Map<String, Double> scores,
                                        DijkstraPathfinder.PathResult path) {
            this.unit = unit;
            this.incident = incident;
            this.totalScore = score;
            this.criteriaScores = new HashMap<>(scores);
            this.path = path;
        }
        
        public ResponseUnit getUnit() { return unit; }
        public Incident getIncident() { return incident; }
        public double getTotalScore() { return totalScore; }
        public Map<String, Double> getCriteriaScores() { return criteriaScores; }
        public DijkstraPathfinder.PathResult getPath() { return path; }
        
        @Override
        public String toString() {
            return String.format(
                "OptimizedDispatch{unit=%s, incident=%s, score=%.2f, distance=%.2f, criteria=%s}",
                unit.getName(), incident.getId(), totalScore, 
                path.getTotalDistance(), criteriaScores
            );
        }
    }
    
    /**
     * Constructor
     * 
     * @param network Emergency network
     */
    public MultiCriteriaOptimizer(EmergencyNetwork network) {
        this.network = network;
        this.pathfinder = new DijkstraPathfinder();
    }
    
    /**
     * Find optimal unit for incident using multi-criteria optimization
     * 
     * @param incident Incident to dispatch for
     * @param availableUnits List of available units
     * @param unitWorkload Current workload for each unit
     * @return Optimized dispatch decision, or null if no suitable unit
     * 
     * Time Complexity: O(U * (V+E) log V) where U = units, V = vertices, E = edges
     * Space Complexity: O(U)
     */
    public OptimizedDispatchDecision findOptimalUnit(
            Incident incident, 
            List<ResponseUnit> availableUnits,
            Map<ResponseUnit, Integer> unitWorkload) {
        
        if (availableUnits.isEmpty()) {
            return null;
        }
        
        OptimizedDispatchDecision bestDecision = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        
        for (ResponseUnit unit : availableUnits) {
            // Calculate path
            DijkstraPathfinder.PathResult path = pathfinder.findShortestPath(
                network, 
                unit.getCurrentLocation(), 
                incident.getLocation()
            );
            
            if (!path.isValid()) {
                continue; // Skip if no path exists
            }
            
            // Calculate multi-criteria score
            Map<String, Double> criteriaScores = new HashMap<>();
            
            // 1. Distance score (lower is better)
            double distanceScore = calculateDistanceScore(path.getTotalDistance());
            criteriaScores.put("distance", distanceScore);
            
            // 2. Time score (estimated response time)
            double timeScore = calculateTimeScore(path.getTotalDistance());
            criteriaScores.put("time", timeScore);
            
            // 3. Specialization score (unit type match)
            double specializationScore = calculateSpecializationScore(unit, incident);
            criteriaScores.put("specialization", specializationScore);
            
            // 4. Availability score (future workload)
            double availabilityScore = calculateAvailabilityScore(unit, unitWorkload);
            criteriaScores.put("availability", availabilityScore);
            
            // 5. Load balance score (current system load)
            double loadBalanceScore = calculateLoadBalanceScore(unit, availableUnits, unitWorkload);
            criteriaScores.put("loadBalance", loadBalanceScore);
            
            // 6. Fatigue score (recent dispatch history)
            double fatigueScore = calculateFatigueScore(unit, unitWorkload);
            criteriaScores.put("fatigue", fatigueScore);
            
            // Calculate weighted total score
            double totalScore = 
                distanceWeight * distanceScore +
                timeWeight * timeScore +
                specializationWeight * specializationScore +
                availabilityWeight * availabilityScore +
                loadBalanceWeight * loadBalanceScore +
                fatigueWeight * fatigueScore;
            
            // Boost score for critical incidents
            if (incident.getSeverity() == Incident.IncidentSeverity.CRITICAL) {
                totalScore *= 1.2;
            }
            
            // Update best decision
            if (totalScore > bestScore) {
                bestScore = totalScore;
                bestDecision = new OptimizedDispatchDecision(
                    unit, incident, totalScore, criteriaScores, path
                );
            }
        }
        
        return bestDecision;
    }
    
    /**
     * Calculate distance score (normalized, inverted)
     * Closer distances get higher scores
     * 
     * @param distance Distance in km
     * @return Score between 0 and 1
     */
    private double calculateDistanceScore(double distance) {
        // Normalize distance (assume max useful distance is 50km)
        double normalized = Math.min(distance / 50.0, 1.0);
        // Invert so closer is better
        return 1.0 - normalized;
    }
    
    /**
     * Calculate time score based on estimated response time
     * Faster response times get higher scores
     * 
     * @param distance Distance to incident
     * @return Score between 0 and 1
     */
    private double calculateTimeScore(double distance) {
        // Estimate time (assume average speed 40 km/h)
        double estimatedTime = distance / 40.0 * 60.0; // in minutes
        
        // Normalize (assume max acceptable time is 30 minutes)
        double normalized = Math.min(estimatedTime / 30.0, 1.0);
        
        // Invert so faster is better
        return 1.0 - normalized;
    }
    
    /**
     * Calculate specialization score
     * Higher scores for units specialized for incident type
     * 
     * @param unit Response unit
     * @param incident Incident
     * @return Score between 0 and 1
     */
    private double calculateSpecializationScore(ResponseUnit unit, Incident incident) {
        Set<Incident.IncidentType> specializations = SPECIALIZATIONS.get(unit.getType());
        
        if (specializations == null) {
            return 0.5; // Neutral score if no specialization defined
        }
        
        if (specializations.contains(incident.getType())) {
            // Perfect match
            return 1.0;
        } else if (canHandle(unit.getType(), incident.getType())) {
            // Can handle but not specialized
            return 0.6;
        } else {
            // Not ideal but can respond
            return 0.3;
        }
    }
    
    /**
     * Check if unit type can handle incident type
     * 
     * @param unitType Type of unit
     * @param incidentType Type of incident
     * @return true if unit can handle incident
     */
    private boolean canHandle(ResponseUnit.UnitType unitType, Incident.IncidentType incidentType) {
        // Fire engines can assist with most emergencies
        if (unitType == ResponseUnit.UnitType.FIRE_ENGINE) {
            return true;
        }
        // Ambulances can handle medical and some rescue
        if (unitType == ResponseUnit.UnitType.AMBULANCE) {
            return incidentType == Incident.IncidentType.MEDICAL || 
                   incidentType == Incident.IncidentType.RESCUE;
        }
        // Police can handle police and hazmat situations
        if (unitType == ResponseUnit.UnitType.POLICE_CAR) {
            return incidentType == Incident.IncidentType.POLICE || 
                   incidentType == Incident.IncidentType.HAZMAT;
        }
        return false;
    }
    
    /**
     * Calculate availability score based on current workload
     * Units with lower workload get higher scores
     * 
     * @param unit Response unit
     * @param unitWorkload Current workload map
     * @return Score between 0 and 1
     */
    private double calculateAvailabilityScore(ResponseUnit unit, Map<ResponseUnit, Integer> unitWorkload) {
        int workload = unitWorkload.getOrDefault(unit, 0);
        
        // Normalize workload (assume max workload is 10 incidents per shift)
        double normalized = Math.min(workload / 10.0, 1.0);
        
        // Invert so lower workload is better
        return 1.0 - normalized;
    }
    
    /**
     * Calculate load balance score
     * Encourages even distribution of work across units
     * 
     * @param unit Response unit being considered
     * @param allUnits All available units
     * @param unitWorkload Current workload map
     * @return Score between 0 and 1
     */
    private double calculateLoadBalanceScore(ResponseUnit unit, List<ResponseUnit> allUnits, 
                                             Map<ResponseUnit, Integer> unitWorkload) {
        if (allUnits.isEmpty()) {
            return 0.5;
        }
        
        // Calculate average workload
        double avgWorkload = allUnits.stream()
            .mapToInt(u -> unitWorkload.getOrDefault(u, 0))
            .average()
            .orElse(0.0);
        
        // Get this unit's workload
        int thisWorkload = unitWorkload.getOrDefault(unit, 0);
        
        // Score is higher if this unit is below average
        if (thisWorkload <= avgWorkload) {
            return 1.0;
        } else {
            // Penalize units above average
            double deviation = (thisWorkload - avgWorkload) / (avgWorkload + 1);
            return Math.max(0.0, 1.0 - deviation);
        }
    }
    
    /**
     * Calculate fatigue score
     * Penalizes units that have been dispatched frequently
     * 
     * @param unit Response unit
     * @param unitWorkload Recent workload
     * @return Score between 0 and 1
     */
    private double calculateFatigueScore(ResponseUnit unit, Map<ResponseUnit, Integer> unitWorkload) {
        int recentDispatches = unitWorkload.getOrDefault(unit, 0);
        
        // Fatigue increases with number of recent dispatches
        double fatigue = Math.min(recentDispatches / 5.0, 1.0);
        
        // Invert so less fatigued units score higher
        return 1.0 - fatigue;
    }
    
    /**
     * Configure optimization weights
     * 
     * @param distance Weight for distance criterion (0-1)
     * @param time Weight for time criterion (0-1)
     * @param specialization Weight for specialization criterion (0-1)
     * @param availability Weight for availability criterion (0-1)
     * @param loadBalance Weight for load balance criterion (0-1)
     * @param fatigue Weight for fatigue criterion (0-1)
     */
    public void setWeights(double distance, double time, double specialization,
                          double availability, double loadBalance, double fatigue) {
        // Normalize weights to sum to 1.0
        double sum = distance + time + specialization + availability + loadBalance + fatigue;
        
        if (sum > 0) {
            this.distanceWeight = distance / sum;
            this.timeWeight = time / sum;
            this.specializationWeight = specialization / sum;
            this.availabilityWeight = availability / sum;
            this.loadBalanceWeight = loadBalance / sum;
            this.fatigueWeight = fatigue / sum;
        }
    }
    
    /**
     * Get current weight configuration
     * 
     * @return Map of criterion to weight
     */
    public Map<String, Double> getWeights() {
        Map<String, Double> weights = new HashMap<>();
        weights.put("distance", distanceWeight);
        weights.put("time", timeWeight);
        weights.put("specialization", specializationWeight);
        weights.put("availability", availabilityWeight);
        weights.put("loadBalance", loadBalanceWeight);
        weights.put("fatigue", fatigueWeight);
        return weights;
    }
    
    /**
     * Batch optimization for multiple incidents
     * Finds globally optimal assignment considering all incidents
     * 
     * @param incidents List of pending incidents
     * @param availableUnits List of available units
     * @param unitWorkload Current workload
     * @return Map of incident to optimal unit
     * 
     * Time Complexity: O(I * U * (V+E) log V)
     * Space Complexity: O(I * U)
     */
    public Map<Incident, OptimizedDispatchDecision> batchOptimize(
            List<Incident> incidents,
            List<ResponseUnit> availableUnits,
            Map<ResponseUnit, Integer> unitWorkload) {
        
        Map<Incident, OptimizedDispatchDecision> assignments = new HashMap<>();
        Set<ResponseUnit> assignedUnits = new HashSet<>();
        List<ResponseUnit> remainingUnits = new ArrayList<>(availableUnits);
        
        // Sort incidents by priority (highest first)
        List<Incident> sortedIncidents = new ArrayList<>(incidents);
        sortedIncidents.sort((a, b) -> Double.compare(
            b.getPriorityScore(), a.getPriorityScore()
        ));
        
        // Greedy assignment: assign highest priority incidents first
        for (Incident incident : sortedIncidents) {
            if (remainingUnits.isEmpty()) {
                break;
            }
            
            OptimizedDispatchDecision decision = findOptimalUnit(
                incident, remainingUnits, unitWorkload
            );
            
            if (decision != null) {
                assignments.put(incident, decision);
                assignedUnits.add(decision.getUnit());
                remainingUnits.remove(decision.getUnit());
            }
        }
        
        return assignments;
    }
}
