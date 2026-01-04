package perds.utils;

import perds.models.*;
import perds.simulation.SimulationEngine;

import java.util.*;

/**
 * Generates text-based visualizations and charts
 * For statistical evaluation and performance analysis
 */
public class DataVisualizer {
    
    /**
     * Generate bar chart for response time distribution
     */
    public static String generateResponseTimeHistogram(PerformanceMetrics metrics) {
        List<PerformanceMetrics.DispatchRecord> records = metrics.getDispatchHistory().stream()
            .filter(PerformanceMetrics.DispatchRecord::isSuccessful)
            .toList();
        
        if (records.isEmpty()) {
            return "No data available for histogram.";
        }
        
        // Create bins
        int binCount = 10;
        double[] bins = new double[binCount];
        double maxTime = records.stream()
            .mapToDouble(PerformanceMetrics.DispatchRecord::getResponseTime)
            .max().orElse(0);
        double binSize = maxTime / binCount;
        
        // Count records in each bin
        for (PerformanceMetrics.DispatchRecord record : records) {
            int binIndex = Math.min((int)(record.getResponseTime() / binSize), binCount - 1);
            bins[binIndex]++;
        }
        
        // Generate chart
        StringBuilder chart = new StringBuilder();
        chart.append("\nRESPONSE TIME DISTRIBUTION:\n");
        chart.append("═══════════════════════════════════════════════════════════\n");
        
        double maxBinValue = Arrays.stream(bins).max().orElse(1);
        int chartWidth = 50;
        
        for (int i = 0; i < binCount; i++) {
            double rangeStart = i * binSize;
            double rangeEnd = (i + 1) * binSize;
            int barLength = (int)((bins[i] / maxBinValue) * chartWidth);
            
            chart.append(String.format("%5.1f-%5.1f min |", rangeStart, rangeEnd));
            chart.append("█".repeat(barLength));
            chart.append(String.format(" %d\n", (int)bins[i]));
        }
        
        chart.append("═══════════════════════════════════════════════════════════\n");
        return chart.toString();
    }
    
    /**
     * Generate comparison chart for unit type performance
     */
    public static String generateUnitTypeComparison(PerformanceMetrics metrics) {
        StringBuilder chart = new StringBuilder();
        chart.append("\nUNIT TYPE PERFORMANCE COMPARISON:\n");
        chart.append("═══════════════════════════════════════════════════════════\n");
        
        Map<ResponseUnit.UnitType, Double> avgResponses = new HashMap<>();
        double maxResponse = 0;
        
        for (ResponseUnit.UnitType type : ResponseUnit.UnitType.values()) {
            PerformanceMetrics.UnitPerformance perf = metrics.getUnitPerformance(type);
            if (perf.getDispatchCount() > 0) {
                double avgTime = perf.getAverageResponseTime();
                avgResponses.put(type, avgTime);
                maxResponse = Math.max(maxResponse, avgTime);
            }
        }
        
        if (avgResponses.isEmpty()) {
            return chart.append("No data available.\n").toString();
        }
        
        int chartWidth = 40;
        
        for (Map.Entry<ResponseUnit.UnitType, Double> entry : avgResponses.entrySet()) {
            int barLength = (int)((entry.getValue() / maxResponse) * chartWidth);
            chart.append(String.format("%-15s |", entry.getKey()));
            chart.append("█".repeat(barLength));
            chart.append(String.format(" %.2f min\n", entry.getValue()));
        }
        
        chart.append("═══════════════════════════════════════════════════════════\n");
        return chart.toString();
    }
    
    /**
     * Generate severity distribution pie chart (text-based)
     */
    public static String generateSeverityDistribution(PerformanceMetrics metrics) {
        StringBuilder chart = new StringBuilder();
        chart.append("\nINCIDENT SEVERITY DISTRIBUTION:\n");
        chart.append("═══════════════════════════════════════════════════════════\n");
        
        int totalIncidents = 0;
        Map<Incident.IncidentSeverity, Integer> counts = new HashMap<>();
        
        for (Incident.IncidentSeverity severity : Incident.IncidentSeverity.values()) {
            int count = metrics.getSeverityMetrics(severity).getIncidentCount();
            counts.put(severity, count);
            totalIncidents += count;
        }
        
        if (totalIncidents == 0) {
            return chart.append("No incidents recorded.\n").toString();
        }
        
        for (Incident.IncidentSeverity severity : Incident.IncidentSeverity.values()) {
            int count = counts.get(severity);
            double percentage = (count * 100.0) / totalIncidents;
            int barLength = (int)(percentage / 2); // Scale for display
            
            chart.append(String.format("%-10s: ", severity));
            chart.append("▓".repeat(barLength));
            chart.append(String.format(" %.1f%% (%d)\n", percentage, count));
        }
        
        chart.append("═══════════════════════════════════════════════════════════\n");
        return chart.toString();
    }
    
    /**
     * Generate time series of incidents
     */
    public static String generateIncidentTimeline(List<SimulationEngine.SimulationEvent> events, 
                                                  int timeWindow) {
        StringBuilder chart = new StringBuilder();
        chart.append("\nINCIDENT TIMELINE (Events per " + timeWindow + " minutes):\n");
        chart.append("═══════════════════════════════════════════════════════════\n");
        
        if (events.isEmpty()) {
            return chart.append("No events recorded.\n").toString();
        }
        
        // Group events by time window
        Map<Integer, Integer> timeBuckets = new TreeMap<>();
        int maxTime = events.stream()
            .mapToInt(SimulationEngine.SimulationEvent::getTimeMinutes)
            .max().orElse(0);
        
        for (SimulationEngine.SimulationEvent event : events) {
            if (event.getEventType().equals("INCIDENT_GENERATED")) {
                int bucket = event.getTimeMinutes() / timeWindow;
                timeBuckets.merge(bucket, 1, Integer::sum);
            }
        }
        
        int maxCount = timeBuckets.values().stream().max(Integer::compareTo).orElse(1);
        int chartWidth = 40;
        
        for (Map.Entry<Integer, Integer> entry : timeBuckets.entrySet()) {
            int timeStart = entry.getKey() * timeWindow;
            int barLength = (entry.getValue() * chartWidth) / maxCount;
            
            chart.append(String.format("%4d min |", timeStart));
            chart.append("█".repeat(barLength));
            chart.append(String.format(" %d\n", entry.getValue()));
        }
        
        chart.append("═══════════════════════════════════════════════════════════\n");
        return chart.toString();
    }
    
    /**
     * Generate performance summary table
     */
    public static String generatePerformanceSummaryTable(PerformanceMetrics metrics) {
        StringBuilder table = new StringBuilder();
        table.append("\nPERFORMANCE SUMMARY TABLE:\n");
        table.append("╔═══════════════════════════╦═══════════╦═══════════╦═════════════╗\n");
        table.append("║ Metric                    ║   Value   ║   Unit    ║   Status    ║\n");
        table.append("╠═══════════════════════════╬═══════════╬═══════════╬═════════════╣\n");
        
        // Success rate
        double successRate = metrics.getSuccessRate() * 100;
        String successStatus = successRate >= 95 ? "EXCELLENT" : 
                              successRate >= 85 ? "GOOD" : 
                              successRate >= 70 ? "FAIR" : "POOR";
        table.append(String.format("║ %-25s ║ %9.2f ║ %-9s ║ %-11s ║\n", 
            "Success Rate", successRate, "%", successStatus));
        
        // Average response time
        double avgResponse = metrics.getAverageResponseTime();
        String responseStatus = avgResponse <= 5 ? "EXCELLENT" : 
                               avgResponse <= 10 ? "GOOD" : 
                               avgResponse <= 15 ? "FAIR" : "POOR";
        table.append(String.format("║ %-25s ║ %9.2f ║ %-9s ║ %-11s ║\n", 
            "Avg Response Time", avgResponse, "minutes", responseStatus));
        
        // Average distance
        double avgDistance = metrics.getAverageResponseDistance();
        String distanceStatus = avgDistance <= 5 ? "EXCELLENT" : 
                               avgDistance <= 10 ? "GOOD" : 
                               avgDistance <= 15 ? "FAIR" : "POOR";
        table.append(String.format("║ %-25s ║ %9.2f ║ %-9s ║ %-11s ║\n", 
            "Avg Response Distance", avgDistance, "km", distanceStatus));
        
        // Total incidents
        table.append(String.format("║ %-25s ║ %9d ║ %-9s ║ %-11s ║\n", 
            "Total Incidents", metrics.getTotalIncidents(), "count", "-"));
        
        table.append("╚═══════════════════════════╩═══════════╩═══════════╩═════════════╝\n");
        
        return table.toString();
    }
    
    /**
     * Export metrics to CSV format
     */
    public static String exportToCSV(PerformanceMetrics metrics) {
        StringBuilder csv = new StringBuilder();
        
        // Header
        csv.append("IncidentID,UnitID,UnitType,IncidentType,Severity,ResponseTime,ResponseDistance,Successful\n");
        
        // Data rows
        for (PerformanceMetrics.DispatchRecord record : metrics.getDispatchHistory()) {
            csv.append(record.getIncident().getId()).append(",");
            csv.append(record.getUnit() != null ? record.getUnit().getId() : "N/A").append(",");
            csv.append(record.getUnit() != null ? record.getUnit().getType() : "N/A").append(",");
            csv.append(record.getIncident().getType()).append(",");
            csv.append(record.getIncident().getSeverity()).append(",");
            csv.append(String.format("%.2f", record.getResponseTime())).append(",");
            csv.append(String.format("%.2f", record.getResponseDistance())).append(",");
            csv.append(record.isSuccessful()).append("\n");
        }
        
        return csv.toString();
    }
}
