package perds.algorithms;

import perds.models.*;

import java.util.*;

/**
 * A* pathfinding algorithm implementation
 * Uses heuristic (Euclidean distance) for more efficient pathfinding
 * Typically faster than Dijkstra for single source-destination queries
 */
public class AStarPathfinder {
    
    /**
     * Find shortest path using A* algorithm
     * Time Complexity: O((V + E) log V) - similar to Dijkstra but often faster in practice
     * Space Complexity: O(V)
     */
    public PathResult findShortestPath(EmergencyNetwork network, Location source, Location destination) {
        if (source == null || destination == null) {
            return new PathResult(false, Double.POSITIVE_INFINITY, new ArrayList<>());
        }
        
        // Priority queue: ordered by f(n) = g(n) + h(n)
        PriorityQueue<AStarNode> openSet = new PriorityQueue<>(
            Comparator.comparingDouble(AStarNode::getFScore)
        );
        
        // Track visited nodes
        Set<Location> closedSet = new HashSet<>();
        
        // Track best path to each node
        Map<Location, Location> cameFrom = new HashMap<>();
        
        // g(n) - actual cost from start to n
        Map<Location, Double> gScore = new HashMap<>();
        gScore.put(source, 0.0);
        
        // f(n) = g(n) + h(n)
        double heuristicEstimate = calculateHeuristic(source, destination);
        openSet.offer(new AStarNode(source, 0.0, heuristicEstimate));
        
        while (!openSet.isEmpty()) {
            AStarNode current = openSet.poll();
            Location currentLocation = current.getLocation();
            
            // Goal reached
            if (currentLocation.equals(destination)) {
                return reconstructPath(cameFrom, currentLocation, gScore.get(currentLocation));
            }
            
            // Skip if already processed
            if (closedSet.contains(currentLocation)) {
                continue;
            }
            
            closedSet.add(currentLocation);
            
            // Explore neighbors
            for (Edge edge : network.getNeighbors(currentLocation)) {
                if (edge.isBlocked()) {
                    continue;
                }
                
                Location neighbor = edge.getDestination();
                
                if (closedSet.contains(neighbor)) {
                    continue;
                }
                
                // Calculate tentative g score
                double tentativeGScore = gScore.get(currentLocation) + edge.getTravelTime();
                
                // If this path is better than any previous one
                if (tentativeGScore < gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    // Update best path
                    cameFrom.put(neighbor, currentLocation);
                    gScore.put(neighbor, tentativeGScore);
                    
                    // Calculate f score and add to open set
                    double hScore = calculateHeuristic(neighbor, destination);
                    double fScore = tentativeGScore + hScore;
                    openSet.offer(new AStarNode(neighbor, tentativeGScore, fScore));
                }
            }
        }
        
        // No path found
        return new PathResult(false, Double.POSITIVE_INFINITY, new ArrayList<>());
    }
    
    /**
     * Heuristic function: Euclidean distance scaled by average speed
     * Admissible and consistent heuristic for A*
     */
    private double calculateHeuristic(Location from, Location to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double euclideanDistance = Math.sqrt(dx * dx + dy * dy);
        
        // Assume average speed of 60 km/h = 1 km per minute
        // This makes the heuristic admissible (never overestimates)
        return euclideanDistance;
    }
    
    /**
     * Reconstruct path from start to destination
     */
    private PathResult reconstructPath(Map<Location, Location> cameFrom, 
                                      Location destination, double totalDistance) {
        List<Location> path = new ArrayList<>();
        Location current = destination;
        
        // Build path backwards
        while (current != null) {
            path.add(0, current);
            current = cameFrom.get(current);
        }
        
        return new PathResult(true, totalDistance, path);
    }
    
    /**
     * Inner class for A* priority queue nodes
     */
    private static class AStarNode {
        private final Location location;
        private final double gScore; // Actual cost from start
        private final double fScore; // f(n) = g(n) + h(n)
        
        public AStarNode(Location location, double gScore, double fScore) {
            this.location = location;
            this.gScore = gScore;
            this.fScore = fScore;
        }
        
        public Location getLocation() {
            return location;
        }
        
        public double getGScore() {
            return gScore;
        }
        
        public double getFScore() {
            return fScore;
        }
    }
    
    /**
     * Result of pathfinding operation
     */
    public static class PathResult {
        private final boolean valid;
        private final double totalDistance;
        private final List<Location> path;
        
        public PathResult(boolean valid, double totalDistance, List<Location> path) {
            this.valid = valid;
            this.totalDistance = totalDistance;
            this.path = path;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public double getTotalDistance() {
            return totalDistance;
        }
        
        public List<Location> getPath() {
            return path;
        }
        
        @Override
        public String toString() {
            if (!valid) {
                return "No valid path";
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("Path (Distance: ").append(String.format("%.2f", totalDistance)).append("): ");
            for (int i = 0; i < path.size(); i++) {
                sb.append(path.get(i).getName());
                if (i < path.size() - 1) {
                    sb.append(" -> ");
                }
            }
            return sb.toString();
        }
    }
}
