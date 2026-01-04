package perds.models;

/**
 * Represents an emergency response unit (vehicle/team)
 */
public class ResponseUnit {
    private final String id;
    private final String name;
    private final UnitType type;
    private Location currentLocation;
    private UnitStatus status;
    private Incident currentIncident;
    
    public enum UnitType {
        FIRE_ENGINE,
        AMBULANCE,
        POLICE_CAR,
        RESCUE_TEAM,
        HAZMAT_TEAM
    }
    
    public enum UnitStatus {
        AVAILABLE,
        DISPATCHED,
        ON_SCENE,
        RETURNING,
        OUT_OF_SERVICE
    }
    
    public ResponseUnit(String id, String name, UnitType type, Location currentLocation) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.currentLocation = currentLocation;
        this.status = UnitStatus.AVAILABLE;
        this.currentIncident = null;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public UnitType getType() {
        return type;
    }
    
    public Location getCurrentLocation() {
        return currentLocation;
    }
    
    public void setCurrentLocation(Location currentLocation) {
        this.currentLocation = currentLocation;
    }
    
    public UnitStatus getStatus() {
        return status;
    }
    
    public void setStatus(UnitStatus status) {
        this.status = status;
    }
    
    public Incident getCurrentIncident() {
        return currentIncident;
    }
    
    public void setCurrentIncident(Incident currentIncident) {
        this.currentIncident = currentIncident;
    }
    
    /**
     * Check if this unit can respond to a given incident type
     */
    public boolean canRespondTo(Incident.IncidentType incidentType) {
        switch (this.type) {
            case FIRE_ENGINE:
                return incidentType == Incident.IncidentType.FIRE;
            case AMBULANCE:
                return incidentType == Incident.IncidentType.MEDICAL;
            case POLICE_CAR:
                return incidentType == Incident.IncidentType.POLICE;
            case RESCUE_TEAM:
                return incidentType == Incident.IncidentType.RESCUE;
            case HAZMAT_TEAM:
                return incidentType == Incident.IncidentType.HAZMAT;
            default:
                return false;
        }
    }
    
    public boolean isAvailable() {
        return status == UnitStatus.AVAILABLE;
    }
    
    @Override
    public String toString() {
        return "ResponseUnit{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", location=" + currentLocation.getName() +
                '}';
    }
}
