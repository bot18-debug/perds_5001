package perds.models;

/**
 * Represents a weighted edge between two locations in the network
 * Includes distance, travel time, and resource availability factors
 */
public class Edge {
    private final Location source;
    private final Location destination;
    private double distance;
    private double travelTime;
    private double congestionFactor;
    private boolean isBlocked;
    
    public Edge(Location source, Location destination, double distance, double travelTime) {
        this.source = source;
        this.destination = destination;
        this.distance = distance;
        this.travelTime = travelTime;
        this.congestionFactor = 1.0;
        this.isBlocked = false;
    }
    
    public Location getSource() {
        return source;
    }
    
    public Location getDestination() {
        return destination;
    }
    
    public double getDistance() {
        return distance;
    }
    
    public void setDistance(double distance) {
        this.distance = distance;
    }
    
    public double getTravelTime() {
        return travelTime;
    }
    
    public void setTravelTime(double travelTime) {
        this.travelTime = travelTime;
    }
    
    public double getCongestionFactor() {
        return congestionFactor;
    }
    
    public void setCongestionFactor(double congestionFactor) {
        this.congestionFactor = congestionFactor;
    }
    
    public boolean isBlocked() {
        return isBlocked;
    }
    
    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }
    
    /**
     * Calculate effective weight considering congestion
     */
    public double getEffectiveWeight() {
        if (isBlocked) {
            return Double.MAX_VALUE;
        }
        return travelTime * congestionFactor;
    }
    
    @Override
    public String toString() {
        return "Edge{" +
                "from=" + source.getName() +
                ", to=" + destination.getName() +
                ", distance=" + distance +
                ", travelTime=" + travelTime +
                ", congestion=" + congestionFactor +
                '}';
    }
}
