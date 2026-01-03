package perds.utils;

import perds.models.*;
import perds.algorithms.DispatchManager;

import java.util.*;

/**
 * Tracks and analyzes system performance metrics
 * Provides statistical evaluation of dispatch efficiency
 */
public class PerformanceMetrics {
    private final List<DispatchRecord> dispatchHistory;
    private final Map<ResponseUnit.UnitType, UnitPerformance> unitPerformance;
    private final Map<Incident.IncidentSeverity, SeverityMetrics> severityMetrics;
    
    private int totalIncidents;
    private int successfulDispatches;
    private int failedDispatches;
    private double totalResponseTime;
    private double totalResponseDistance;
    
    public PerformanceMetrics() {
        this.dispatchHistory = new ArrayList<>();
        this.unitPerformance = new HashMap<>();
        this.severityMetrics = new HashMap<>();
        
        // Initialize performance trackers
        for (ResponseUnit.UnitType type : ResponseUnit.UnitType.values()) {
            unitPerformance.put(type, new UnitPerformance(type));
        }
        
        for (Incident.IncidentSeverity severity : Incident.IncidentSeverity.values()) {
            severityMetrics.put(severity, new SeverityMetrics(severity));
        }
    }
    
    /**
     * Record a dispatch decision
     */
    public void recordDispatch(DispatchManager.DispatchDecision decision) {
        totalIncidents++;
        
        if (decision != null) {
            successfulDispatches++;
            
            double responseTime = decision.getPath().getTotalDistance(); // Using distance as proxy
            double responseDistance = decision.getPath().getTotalDistance();
            
            totalResponseTime += responseTime;
            totalResponseDistance += responseDistance;
            
            DispatchRecord record = new DispatchRecord(
                decision.getIncident(),
                decision.getUnit(),
                responseTime,
                responseDistance,
                true,
                System.currentTimeMillis()
            );
            
            dispatchHistory.add(record);
            
            // Update unit performance
            unitPerformance.get(decision.getUnit().getType()).recordDispatch(responseTime, true);
            
            // Update severity metrics
            severityMetrics.get(decision.getIncident().getSeverity())
                .recordDispatch(responseTime, true);
        } else {
            failedDispatches++;
        }
    }
    
    /**
     * Record a failed dispatch attempt
     */
    public void recordFailedDispatch(Incident incident) {
        totalIncidents++;
        failedDispatches++;
        
        DispatchRecord record = new DispatchRecord(
            incident,
            null,
            0.0,
            0.0,
            false,
            System.currentTimeMillis()
        );
        
        dispatchHistory.add(record);
        severityMetrics.get(incident.getSeverity()).recordDispatch(0.0, false);
    }
    
    /**
     * Get overall dispatch success rate
     */
    public double getSuccessRate() {
        return totalIncidents > 0 ? (double) successfulDispatches / totalIncidents : 0.0;
    }
    
    /**
     * Get average response time
     */
    public double getAverageResponseTime() {
        return successfulDispatches > 0 ? totalResponseTime / successfulDispatches : 0.0;
    }
    
    /**
     * Get average response distance
     */
    public double getAverageResponseDistance() {
        return successfulDispatches > 0 ? totalResponseDistance / successfulDispatches : 0.0;
    }
    
    /**
     * Get performance by unit type
     */
    public UnitPerformance getUnitPerformance(ResponseUnit.UnitType type) {
        return unitPerformance.get(type);
    }
    
    /**
     * Get metrics by incident severity
     */
    public SeverityMetrics getSeverityMetrics(Incident.IncidentSeverity severity) {
        return severityMetrics.get(severity);
    }
    
    /**
     * Calculate response time percentiles (50th, 90th, 95th)
     */
    public Map<String, Double> getResponseTimePercentiles() {
        if (dispatchHistory.isEmpty()) {
            return new HashMap<>();
        }
        
        List<Double> responseTimes = dispatchHistory.stream()
            .filter(DispatchRecord::isSuccessful)
            .map(DispatchRecord::getResponseTime)
            .sorted()
            .toList();
        
        Map<String, Double> percentiles = new HashMap<>();
        
        if (!responseTimes.isEmpty()) {
            percentiles.put("50th", getPercentile(responseTimes, 0.5));
            percentiles.put("90th", getPercentile(responseTimes, 0.9));
            percentiles.put("95th", getPercentile(responseTimes, 0.95));
            percentiles.put("99th", getPercentile(responseTimes, 0.99));
        }
        
        return percentiles;
    }
    
    /**
     * Calculate percentile value from sorted list
     */
    private double getPercentile(List<Double> sortedValues, double percentile) {
        int index = (int) Math.ceil(percentile * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }
    
    /**
     * Get comprehensive performance report
     */
    public String generateReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("╔════════════════════════════════════════════════════════════╗\n");
        report.append("║           SYSTEM PERFORMANCE REPORT                        ║\n");
        report.append("╚════════════════════════════════════════════════════════════╝\n\n");
        
        // Overall metrics
        report.append("OVERALL METRICS:\n");
        report.append(String.format("  Total Incidents: %d\n", totalIncidents));
        report.append(String.format("  Successful Dispatches: %d\n", successfulDispatches));
        report.append(String.format("  Failed Dispatches: %d\n", failedDispatches));
        report.append(String.format("  Success Rate: %.2f%%\n", getSuccessRate() * 100));
        report.append(String.format("  Avg Response Time: %.2f minutes\n", getAverageResponseTime()));
        report.append(String.format("  Avg Response Distance: %.2f km\n\n", getAverageResponseDistance()));
        
        // Response time percentiles
        Map<String, Double> percentiles = getResponseTimePercentiles();
        if (!percentiles.isEmpty()) {
            report.append("RESPONSE TIME DISTRIBUTION:\n");
            percentiles.forEach((key, value) -> 
                report.append(String.format("  %s percentile: %.2f minutes\n", key, value))
            );
            report.append("\n");
        }
        
        // Unit type performance
        report.append("PERFORMANCE BY UNIT TYPE:\n");
        for (ResponseUnit.UnitType type : ResponseUnit.UnitType.values()) {
            UnitPerformance perf = unitPerformance.get(type);
            if (perf.getDispatchCount() > 0) {
                report.append(String.format("  %s:\n", type));
                report.append(String.format("    Dispatches: %d\n", perf.getDispatchCount()));
                report.append(String.format("    Avg Response: %.2f min\n", perf.getAverageResponseTime()));
                report.append(String.format("    Success Rate: %.2f%%\n", perf.getSuccessRate() * 100));
            }
        }
        report.append("\n");
        
        // Severity metrics
        report.append("PERFORMANCE BY INCIDENT SEVERITY:\n");
        for (Incident.IncidentSeverity severity : Incident.IncidentSeverity.values()) {
            SeverityMetrics metrics = severityMetrics.get(severity);
            if (metrics.getIncidentCount() > 0) {
                report.append(String.format("  %s:\n", severity));
                report.append(String.format("    Incidents: %d\n", metrics.getIncidentCount()));
                report.append(String.format("    Avg Response: %.2f min\n", metrics.getAverageResponseTime()));
                report.append(String.format("    Success Rate: %.2f%%\n", metrics.getSuccessRate() * 100));
            }
        }
        
        return report.toString();
    }
    
    // Getters
    public int getTotalIncidents() { return totalIncidents; }
    public int getSuccessfulDispatches() { return successfulDispatches; }
    public int getFailedDispatches() { return failedDispatches; }
    public List<DispatchRecord> getDispatchHistory() { return new ArrayList<>(dispatchHistory); }
    
    /**
     * Dispatch record for historical tracking
     */
    public static class DispatchRecord {
        private final Incident incident;
        private final ResponseUnit unit;
        private final double responseTime;
        private final double responseDistance;
        private final boolean successful;
        private final long timestamp;
        
        public DispatchRecord(Incident incident, ResponseUnit unit, double responseTime,
                            double responseDistance, boolean successful, long timestamp) {
            this.incident = incident;
            this.unit = unit;
            this.responseTime = responseTime;
            this.responseDistance = responseDistance;
            this.successful = successful;
            this.timestamp = timestamp;
        }
        
        public Incident getIncident() { return incident; }
        public ResponseUnit getUnit() { return unit; }
        public double getResponseTime() { return responseTime; }
        public double getResponseDistance() { return responseDistance; }
        public boolean isSuccessful() { return successful; }
        public long getTimestamp() { return timestamp; }
    }
    
    /**
     * Performance metrics for a unit type
     */
    public static class UnitPerformance {
        private final ResponseUnit.UnitType unitType;
        private int dispatchCount;
        private int successfulDispatches;
        private double totalResponseTime;
        
        public UnitPerformance(ResponseUnit.UnitType unitType) {
            this.unitType = unitType;
        }
        
        public void recordDispatch(double responseTime, boolean successful) {
            dispatchCount++;
            if (successful) {
                successfulDispatches++;
                totalResponseTime += responseTime;
            }
        }
        
        public double getAverageResponseTime() {
            return successfulDispatches > 0 ? totalResponseTime / successfulDispatches : 0.0;
        }
        
        public double getSuccessRate() {
            return dispatchCount > 0 ? (double) successfulDispatches / dispatchCount : 0.0;
        }
        
        public int getDispatchCount() { return dispatchCount; }
        public ResponseUnit.UnitType getUnitType() { return unitType; }
    }
    
    /**
     * Metrics for incident severity
     */
    public static class SeverityMetrics {
        private final Incident.IncidentSeverity severity;
        private int incidentCount;
        private int successfulResponses;
        private double totalResponseTime;
        
        public SeverityMetrics(Incident.IncidentSeverity severity) {
            this.severity = severity;
        }
        
        public void recordDispatch(double responseTime, boolean successful) {
            incidentCount++;
            if (successful) {
                successfulResponses++;
                totalResponseTime += responseTime;
            }
        }
        
        public double getAverageResponseTime() {
            return successfulResponses > 0 ? totalResponseTime / successfulResponses : 0.0;
        }
        
        public double getSuccessRate() {
            return incidentCount > 0 ? (double) successfulResponses / incidentCount : 0.0;
        }
        
        public int getIncidentCount() { return incidentCount; }
        public Incident.IncidentSeverity getSeverity() { return severity; }
    }
}
