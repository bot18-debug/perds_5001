package perds.models;

import java.util.Objects;

/**
 * Represents a location in the emergency network (city, dispatch center, or incident site)
 */
public class Location {
    private final String id;
    private final String name;
    private final double latitude;
    private final double longitude;
    private LocationType type;
    
    public enum LocationType {
        DISPATCH_CENTER,
        CITY,
        INCIDENT_SITE
    }
    
    public Location(String id, String name, double latitude, double longitude, LocationType type) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public double getLatitude() {
        return latitude;
    }
    
    public double getLongitude() {
        return longitude;
    }
    
    public LocationType getType() {
        return type;
    }
    
    public void setType(LocationType type) {
        this.type = type;
    }
    
    /**
     * Get X coordinate (longitude) for pathfinding algorithms
     */
    public double getX() {
        return longitude;
    }
    
    /**
     * Get Y coordinate (latitude) for pathfinding algorithms
     */
    public double getY() {
        return latitude;
    }
    
    /**
     * Calculate Euclidean distance to another location
     */
    public double distanceTo(Location other) {
        double dx = this.longitude - other.longitude;
        double dy = this.latitude - other.latitude;
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Location location = (Location) o;
        return Objects.equals(id, location.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Location{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                '}';
    }
}
