package perds.models;

import java.time.LocalDateTime;

/**
 * Represents an emergency incident requiring response
 */
public class Incident {
    private final String id;
    private final Location location;
    private final IncidentType type;
    private IncidentSeverity severity;
    private IncidentStatus status;
    private final LocalDateTime reportedTime;
    private ResponseUnit assignedUnit;
    
    public enum IncidentType {
        FIRE,
        MEDICAL,
        POLICE,
        RESCUE,
        HAZMAT
    }
    
    public enum IncidentSeverity {
        LOW(1),
        MEDIUM(2),
        HIGH(3),
        CRITICAL(4);
        
        private final int priority;
        
        IncidentSeverity(int priority) {
            this.priority = priority;
        }
        
        public int getPriority() {
            return priority;
        }
    }
    
    public enum IncidentStatus {
        REPORTED,
        DISPATCHED,
        IN_PROGRESS,
        RESOLVED,
        CANCELLED
    }
    
    public Incident(String id, Location location, IncidentType type, IncidentSeverity severity) {
        this.id = id;
        this.location = location;
        this.type = type;
        this.severity = severity;
        this.status = IncidentStatus.REPORTED;
        this.reportedTime = LocalDateTime.now();
        this.assignedUnit = null;
    }
    
    public String getId() {
        return id;
    }
    
    public Location getLocation() {
        return location;
    }
    
    public IncidentType getType() {
        return type;
    }
    
    public IncidentSeverity getSeverity() {
        return severity;
    }
    
    public void setSeverity(IncidentSeverity severity) {
        this.severity = severity;
    }
    
    public IncidentStatus getStatus() {
        return status;
    }
    
    public void setStatus(IncidentStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getReportedTime() {
        return reportedTime;
    }
    
    public ResponseUnit getAssignedUnit() {
        return assignedUnit;
    }
    
    public void setAssignedUnit(ResponseUnit assignedUnit) {
        this.assignedUnit = assignedUnit;
    }
    
    /**
     * Calculate priority score for dispatch ordering
     */
    public double getPriorityScore() {
        return severity.getPriority() * 100.0;
    }
    
    @Override
    public String toString() {
        return "Incident{" +
                "id='" + id + '\'' +
                ", location=" + location.getName() +
                ", type=" + type +
                ", severity=" + severity +
                ", status=" + status +
                '}';
    }
}
