package perds.algorithms;

import perds.models.Edge;
import perds.models.EmergencyNetwork;
import perds.models.Location;

import java.util.*;

/**
 * Implements Dijkstra's algorithm for finding shortest paths in the emergency network
 * Time Complexity: O((V + E) log V) with priority queue
 * Space Complexity: O(V)
 */
public class DijkstraPathfinder {
    
    /**
     * Node class for priority queue, storing location and distance
     */
    private static class Node implements Comparable<Node> {
        Location location;
        double distance;
        
        Node(Location location, double distance) {
            this.location = location;
            this.distance = distance;
        }
        
        @Override
        public int compareTo(Node other) {
            return Double.compare(this.distance, other.distance);
        }
    }
    
    /**
     * Find shortest path from source to destination
     * Returns a PathResult containing the path and total distance
     */
    public PathResult findShortestPath(EmergencyNetwork network, Location source, Location destination) {
        // Distance map: Location -> shortest distance from source
        Map<Location, Double> distances = new HashMap<>();
        // Previous location map for path reconstruction
        Map<Location, Location> previous = new HashMap<>();
        // Priority queue for selecting next location to visit
        PriorityQueue<Node> pq = new PriorityQueue<>();
        // Visited set to avoid reprocessing
        Set<Location> visited = new HashSet<>();
        
        // Initialize distances
        for (Location loc : network.getAllLocations()) {
            distances.put(loc, Double.MAX_VALUE);
        }
        distances.put(source, 0.0);
        
        // Start with source
        pq.offer(new Node(source, 0.0));
        
        while (!pq.isEmpty()) {
            Node current = pq.poll();
            Location currentLoc = current.location;
            
            // Skip if already visited
            if (visited.contains(currentLoc)) {
                continue;
            }
            visited.add(currentLoc);
            
            // Found destination
            if (currentLoc.equals(destination)) {
                break;
            }
            
            // Explore neighbors
            List<Edge> neighbors = network.getNeighbors(currentLoc);
            for (Edge edge : neighbors) {
                if (edge.isBlocked()) {
                    continue; // Skip blocked edges
                }
                
                Location neighbor = edge.getDestination();
                double newDistance = distances.get(currentLoc) + edge.getEffectiveWeight();
                
                // Update if we found a shorter path
                if (newDistance < distances.get(neighbor)) {
                    distances.put(neighbor, newDistance);
                    previous.put(neighbor, currentLoc);
                    pq.offer(new Node(neighbor, newDistance));
                }
            }
        }
        
        // Reconstruct path
        List<Location> path = reconstructPath(previous, source, destination);
        double totalDistance = distances.get(destination);
        
        return new PathResult(path, totalDistance);
    }
    
    /**
     * Reconstruct path from previous map
     */
    private List<Location> reconstructPath(Map<Location, Location> previous, 
                                           Location source, Location destination) {
        List<Location> path = new ArrayList<>();
        Location current = destination;
        
        // Build path backwards from destination to source
        while (current != null) {
            path.add(current);
            current = previous.get(current);
        }
        
        // Reverse to get source-to-destination order
        Collections.reverse(path);
        
        // Check if path is valid (should start with source)
        if (path.isEmpty() || !path.get(0).equals(source)) {
            return new ArrayList<>(); // No path found
        }
        
        return path;
    }
    
    /**
     * Find shortest distances from source to all other locations
     * Useful for predictive analysis
     */
    public Map<Location, Double> findShortestDistances(EmergencyNetwork network, Location source) {
        Map<Location, Double> distances = new HashMap<>();
        PriorityQueue<Node> pq = new PriorityQueue<>();
        Set<Location> visited = new HashSet<>();
        
        // Initialize
        for (Location loc : network.getAllLocations()) {
            distances.put(loc, Double.MAX_VALUE);
        }
        distances.put(source, 0.0);
        pq.offer(new Node(source, 0.0));
        
        while (!pq.isEmpty()) {
            Node current = pq.poll();
            Location currentLoc = current.location;
            
            if (visited.contains(currentLoc)) {
                continue;
            }
            visited.add(currentLoc);
            
            List<Edge> neighbors = network.getNeighbors(currentLoc);
            for (Edge edge : neighbors) {
                if (edge.isBlocked()) {
                    continue;
                }
                
                Location neighbor = edge.getDestination();
                double newDistance = distances.get(currentLoc) + edge.getEffectiveWeight();
                
                if (newDistance < distances.get(neighbor)) {
                    distances.put(neighbor, newDistance);
                    pq.offer(new Node(neighbor, newDistance));
                }
            }
        }
        
        return distances;
    }
    
    /**
     * Inner class to store path results
     */
    public static class PathResult {
        private final List<Location> path;
        private final double totalDistance;
        
        public PathResult(List<Location> path, double totalDistance) {
            this.path = path;
            this.totalDistance = totalDistance;
        }
        
        public List<Location> getPath() {
            return path;
        }
        
        public double getTotalDistance() {
            return totalDistance;
        }
        
        public boolean isValid() {
            return !path.isEmpty() && totalDistance != Double.MAX_VALUE;
        }
        
        @Override
        public String toString() {
            if (!isValid()) {
                return "PathResult{No path found}";
            }
            StringBuilder sb = new StringBuilder("PathResult{\n");
            sb.append("  Distance: ").append(String.format("%.2f", totalDistance)).append("\n");
            sb.append("  Path: ");
            for (int i = 0; i < path.size(); i++) {
                sb.append(path.get(i).getName());
                if (i < path.size() - 1) {
                    sb.append(" -> ");
                }
            }
            sb.append("\n}");
            return sb.toString();
        }
    }
}
