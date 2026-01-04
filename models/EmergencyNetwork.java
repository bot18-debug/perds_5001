package perds.models;

import java.util.*;

/**
 * Graph-based representation of the emergency response network
 * Uses adjacency list structure for efficient traversal
 */
public class EmergencyNetwork {
    // Adjacency list: Location -> List of Edges
    private final Map<Location, List<Edge>> adjacencyList;
    // Quick lookup for locations by ID
    private final Map<String, Location> locationMap;
    
    public EmergencyNetwork() {
        this.adjacencyList = new HashMap<>();
        this.locationMap = new HashMap<>();
    }
    
    /**
     * Add a location (node) to the network
     * Time Complexity: O(1)
     */
    public void addLocation(Location location) {
        if (!locationMap.containsKey(location.getId())) {
            locationMap.put(location.getId(), location);
            adjacencyList.put(location, new ArrayList<>());
        }
    }
    
    /**
     * Remove a location from the network
     * Time Complexity: O(V + E) where V is vertices and E is edges
     */
    public void removeLocation(String locationId) {
        Location location = locationMap.remove(locationId);
        if (location != null) {
            adjacencyList.remove(location);
            // Remove all edges pointing to this location
            for (List<Edge> edges : adjacencyList.values()) {
                edges.removeIf(edge -> edge.getDestination().equals(location));
            }
        }
    }
    
    /**
     * Add a bidirectional edge between two locations
     * Time Complexity: O(1)
     */
    public void addEdge(Location source, Location destination, double distance, double travelTime) {
        // Ensure both locations exist in the network
        addLocation(source);
        addLocation(destination);
        
        // Add bidirectional edges
        Edge forwardEdge = new Edge(source, destination, distance, travelTime);
        Edge reverseEdge = new Edge(destination, source, distance, travelTime);
        
        adjacencyList.get(source).add(forwardEdge);
        adjacencyList.get(destination).add(reverseEdge);
    }
    
    /**
     * Update edge weight (for dynamic changes like congestion)
     * Time Complexity: O(E) where E is number of edges from source
     */
    public void updateEdgeWeight(String sourceId, String destId, double newTravelTime) {
        Location source = locationMap.get(sourceId);
        Location dest = locationMap.get(destId);
        
        if (source != null && dest != null) {
            List<Edge> edges = adjacencyList.get(source);
            for (Edge edge : edges) {
                if (edge.getDestination().equals(dest)) {
                    edge.setTravelTime(newTravelTime);
                    break;
                }
            }
        }
    }
    
    /**
     * Block/unblock an edge (e.g., road closure)
     * Time Complexity: O(E)
     */
    public void setEdgeBlocked(String sourceId, String destId, boolean blocked) {
        Location source = locationMap.get(sourceId);
        Location dest = locationMap.get(destId);
        
        if (source != null && dest != null) {
            List<Edge> edges = adjacencyList.get(source);
            for (Edge edge : edges) {
                if (edge.getDestination().equals(dest)) {
                    edge.setBlocked(blocked);
                    break;
                }
            }
        }
    }
    
    /**
     * Get all neighbors of a location
     * Time Complexity: O(1)
     */
    public List<Edge> getNeighbors(Location location) {
        return adjacencyList.getOrDefault(location, new ArrayList<>());
    }
    
    /**
     * Get location by ID
     * Time Complexity: O(1)
     */
    public Location getLocation(String locationId) {
        return locationMap.get(locationId);
    }
    
    /**
     * Get all locations in the network
     * Time Complexity: O(1)
     */
    public Collection<Location> getAllLocations() {
        return locationMap.values();
    }
    
    /**
     * Get total number of locations
     * Time Complexity: O(1)
     */
    public int getLocationCount() {
        return locationMap.size();
    }
    
    /**
     * Get total number of edges
     * Time Complexity: O(V)
     */
    public int getEdgeCount() {
        int count = 0;
        for (List<Edge> edges : adjacencyList.values()) {
            count += edges.size();
        }
        return count / 2; // Divide by 2 for bidirectional edges
    }
    
    /**
     * Check if network contains a location
     * Time Complexity: O(1)
     */
    public boolean containsLocation(String locationId) {
        return locationMap.containsKey(locationId);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("EmergencyNetwork{\n");
        sb.append("  Locations: ").append(getLocationCount()).append("\n");
        sb.append("  Edges: ").append(getEdgeCount()).append("\n");
        for (Map.Entry<Location, List<Edge>> entry : adjacencyList.entrySet()) {
            sb.append("  ").append(entry.getKey().getName()).append(" -> ");
            for (Edge edge : entry.getValue()) {
                sb.append(edge.getDestination().getName()).append(" ");
            }
            sb.append("\n");
        }
        sb.append("}");
        return sb.toString();
    }
}
