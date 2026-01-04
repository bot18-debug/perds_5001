package perds.algorithms;

import perds.models.*;

import java.util.*;

/**
 * Advanced resource pre-positioning system
 * Dynamically repositions units based on predictive analysis
 * Implements load balancing across dispatch centers
 */
public class ResourcePositioner {
    private final EmergencyNetwork network;
    private final PredictiveAnalyzer predictiveAnalyzer;
    private final DijkstraPathfinder pathfinder;
    
    // Configuration
    private static final double REPOSITIONING_THRESHOLD = 0.6; // 60% demand threshold
    private static final int MAX_REPOSITIONS_PER_CYCLE = 3;
    
    public ResourcePositioner(EmergencyNetwork network, PredictiveAnalyzer predictiveAnalyzer) {
        this.network = network;
        this.predictiveAnalyzer = predictiveAnalyzer;
        this.pathfinder = new DijkstraPathfinder();
    }
    
    /**
     * Analyze current distribution and recommend repositioning
     * Time Complexity: O(U * L * (V + E) log V) where U=units, L=locations
     */
    public List<RepositioningRecommendation> analyzeAndRecommend(List<ResponseUnit> availableUnits) {
        List<RepositioningRecommendation> recommendations = new ArrayList<>();
        
        if (availableUnits.isEmpty()) {
            return recommendations;
        }
        
        // Get demand scores for all locations
        Map<Location, Double> demandScores = predictiveAnalyzer.calculateDemandScores();
        
        if (demandScores.isEmpty()) {
            return recommendations; // No historical data yet
        }
        
        // Identify high-demand locations that need more coverage
        List<Location> highDemandLocations = identifyUnderservedLocations(demandScores, availableUnits);
        
        // Generate repositioning recommendations
        for (Location highDemandLoc : highDemandLocations) {
            if (recommendations.size() >= MAX_REPOSITIONS_PER_CYCLE) {
                break;
            }
            
            // Find best unit to reposition
            ResponseUnit bestUnit = findBestUnitToReposition(availableUnits, highDemandLoc);
            
            if (bestUnit != null) {
                double benefit = calculateRepositioningBenefit(bestUnit, highDemandLoc, demandScores);
                
                if (benefit > REPOSITIONING_THRESHOLD) {
                    DijkstraPathfinder.PathResult path = pathfinder.findShortestPath(
                        network, bestUnit.getCurrentLocation(), highDemandLoc
                    );
                    
                    recommendations.add(new RepositioningRecommendation(
                        bestUnit, highDemandLoc, benefit, path
                    ));
                    
                    // Remove from available units to avoid duplicate recommendations
                    availableUnits = new ArrayList<>(availableUnits);
                    availableUnits.remove(bestUnit);
                }
            }
        }
        
        return recommendations;
    }
    
    /**
     * Identify locations with high demand but low unit coverage
     */
    private List<Location> identifyUnderservedLocations(Map<Location, Double> demandScores, 
                                                        List<ResponseUnit> availableUnits) {
        // Calculate coverage score for each location
        Map<Location, Double> coverageScores = new HashMap<>();
        
        for (Location location : demandScores.keySet()) {
            double coverage = calculateCoverageScore(location, availableUnits);
            coverageScores.put(location, coverage);
        }
        
        // Find locations where demand exceeds coverage
        List<Location> underserved = new ArrayList<>();
        
        for (Map.Entry<Location, Double> entry : demandScores.entrySet()) {
            Location location = entry.getKey();
            double demand = entry.getValue();
            double coverage = coverageScores.getOrDefault(location, 0.0);
            
            // Underserved if demand significantly exceeds coverage
            if (demand > coverage * 1.5) {
                underserved.add(location);
            }
        }
        
        // Sort by severity of undersupply
        underserved.sort((l1, l2) -> {
            double gap1 = demandScores.get(l1) - coverageScores.getOrDefault(l1, 0.0);
            double gap2 = demandScores.get(l2) - coverageScores.getOrDefault(l2, 0.0);
            return Double.compare(gap2, gap1); // Descending
        });
        
        return underserved;
    }
    
    /**
     * Calculate coverage score for a location based on nearby units
     */
    private double calculateCoverageScore(Location location, List<ResponseUnit> units) {
        double coverageScore = 0.0;
        
        for (ResponseUnit unit : units) {
            if (!unit.isAvailable()) {
                continue;
            }
            
            // Calculate distance from unit to location
            DijkstraPathfinder.PathResult path = pathfinder.findShortestPath(
                network, unit.getCurrentLocation(), location
            );
            
            if (path.isValid()) {
                // Closer units contribute more to coverage (inverse distance)
                double distance = path.getTotalDistance();
                double contribution = 10.0 / (1.0 + distance); // Diminishes with distance
                coverageScore += contribution;
            }
        }
        
        return coverageScore;
    }
    
    /**
     * Find the best unit to reposition to a high-demand location
     */
    private ResponseUnit findBestUnitToReposition(List<ResponseUnit> availableUnits, 
                                                  Location targetLocation) {
        ResponseUnit bestUnit = null;
        double bestScore = Double.MAX_VALUE;
        
        for (ResponseUnit unit : availableUnits) {
            if (!unit.isAvailable()) {
                continue;
            }
            
            // Calculate repositioning cost (distance + opportunity cost)
            DijkstraPathfinder.PathResult path = pathfinder.findShortestPath(
                network, unit.getCurrentLocation(), targetLocation
            );
            
            if (path.isValid()) {
                double repositioningCost = path.getTotalDistance();
                
                // Consider opportunity cost (leaving current area)
                double currentLocationDemand = predictiveAnalyzer
                    .predictIncidentProbability(unit.getCurrentLocation());
                double opportunityCost = currentLocationDemand * 10.0;
                
                double totalScore = repositioningCost + opportunityCost;
                
                if (totalScore < bestScore) {
                    bestScore = totalScore;
                    bestUnit = unit;
                }
            }
        }
        
        return bestUnit;
    }
    
    /**
     * Calculate benefit of repositioning a unit
     */
    private double calculateRepositioningBenefit(ResponseUnit unit, Location targetLocation,
                                                 Map<Location, Double> demandScores) {
        double targetDemand = demandScores.getOrDefault(targetLocation, 0.0);
        double currentDemand = demandScores.getOrDefault(unit.getCurrentLocation(), 0.0);
        
        // Benefit is the increase in coverage of high-demand area
        // minus the loss of coverage in current area
        double benefit = (targetDemand - currentDemand) / Math.max(targetDemand, 1.0);
        
        return benefit;
    }
    
    /**
     * Apply repositioning recommendations
     */
    public void applyRepositioning(RepositioningRecommendation recommendation) {
        ResponseUnit unit = recommendation.getUnit();
        Location newLocation = recommendation.getTargetLocation();
        
        unit.setCurrentLocation(newLocation);
        System.out.println("Repositioned " + unit.getName() + " to " + newLocation.getName());
    }
    
    /**
     * Calculate load balance score (lower is better, 0 is perfect balance)
     */
    public double calculateLoadBalance(List<ResponseUnit> units, 
                                      Map<Location, Double> demandScores) {
        if (units.isEmpty()) {
            return Double.MAX_VALUE;
        }
        
        // Calculate coverage per location
        Map<Location, Integer> unitCounts = new HashMap<>();
        for (ResponseUnit unit : units) {
            if (unit.isAvailable()) {
                unitCounts.merge(unit.getCurrentLocation(), 1, Integer::sum);
            }
        }
        
        // Calculate variance in coverage relative to demand
        double totalVariance = 0.0;
        int locationCount = 0;
        
        for (Location location : demandScores.keySet()) {
            double demand = demandScores.get(location);
            int unitCount = unitCounts.getOrDefault(location, 0);
            
            // Ideal unit count proportional to demand
            double idealUnits = demand / demandScores.values().stream()
                .mapToDouble(Double::doubleValue).sum() * units.size();
            
            double variance = Math.pow(unitCount - idealUnits, 2);
            totalVariance += variance;
            locationCount++;
        }
        
        return locationCount > 0 ? Math.sqrt(totalVariance / locationCount) : 0.0;
    }
    
    /**
     * Repositioning recommendation
     */
    public static class RepositioningRecommendation {
        private final ResponseUnit unit;
        private final Location targetLocation;
        private final double benefitScore;
        private final DijkstraPathfinder.PathResult path;
        
        public RepositioningRecommendation(ResponseUnit unit, Location targetLocation,
                                          double benefitScore, DijkstraPathfinder.PathResult path) {
            this.unit = unit;
            this.targetLocation = targetLocation;
            this.benefitScore = benefitScore;
            this.path = path;
        }
        
        public ResponseUnit getUnit() {
            return unit;
        }
        
        public Location getTargetLocation() {
            return targetLocation;
        }
        
        public double getBenefitScore() {
            return benefitScore;
        }
        
        public DijkstraPathfinder.PathResult getPath() {
            return path;
        }
        
        @Override
        public String toString() {
            return String.format("Reposition %s from %s to %s (Benefit: %.2f, Distance: %.2f)",
                unit.getName(),
                unit.getCurrentLocation().getName(),
                targetLocation.getName(),
                benefitScore,
                path.getTotalDistance());
        }
    }
}
