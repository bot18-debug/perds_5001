package perds.evaluation;

import perds.models.*;
import perds.algorithms.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Advanced Performance Profiler and Complexity Analyzer
 * 
 * Provides empirical validation of theoretical complexity by:
 * - Measuring actual runtime for different input sizes
 * - Comparing against theoretical O() bounds
 * - Identifying performance bottlenecks
 * - Generating performance reports
 * 
 * Time Complexity: Varies by profiled operation
 * Space Complexity: O(n) for storing measurements
 * 
 * @author PERDS Team
 * @version 2.0
 */
public class PerformanceProfiler {
    
    /**
     * Performance measurement result
     */
    public static class PerformanceMeasurement {
        public final String operation;
        public final int inputSize;
        public final long executionTimeNanos;
        public final double executionTimeMs;
        public final long memoryUsedBytes;
        
        public PerformanceMeasurement(String op, int size, long nanos, long memory) {
            this.operation = op;
            this.inputSize = size;
            this.executionTimeNanos = nanos;
            this.executionTimeMs = nanos / 1_000_000.0;
            this.memoryUsedBytes = memory;
        }
        
        @Override
        public String toString() {
            return String.format("%s(n=%d): %.3f ms, Memory: %d KB",
                operation, inputSize, executionTimeMs, memoryUsedBytes / 1024);
        }
    }
    
    /**
     * Complexity analysis result
     */
    public static class ComplexityAnalysis {
        public final String operation;
        public final String theoreticalComplexity;
        public final List<PerformanceMeasurement> measurements;
        public final double rSquared; // Goodness of fit (R²)
        public final boolean matchesTheory;
        
        public ComplexityAnalysis(String op, String theory, 
                                 List<PerformanceMeasurement> meas, 
                                 double r2, boolean matches) {
            this.operation = op;
            this.theoreticalComplexity = theory;
            this.measurements = new ArrayList<>(meas);
            this.rSquared = r2;
            this.matchesTheory = matches;
        }
        
        @Override
        public String toString() {
            return String.format("%s - Theoretical: %s, R²=%.4f, Validates: %s",
                operation, theoreticalComplexity, rSquared, matchesTheory ? "YES" : "NO");
        }
    }
    
    /**
     * Benchmark Dijkstra's algorithm with different graph sizes
     * 
     * Theoretical: O((V+E) log V)
     * 
     * @return Complexity analysis
     */
    public static ComplexityAnalysis benchmarkDijkstra() {
        List<PerformanceMeasurement> measurements = new ArrayList<>();
        int[] graphSizes = {10, 25, 50, 100, 200, 500, 1000};
        
        System.out.println("\nBenchmarking Dijkstra's Algorithm:");
        System.out.println("═══════════════════════════════════════════════════════════");
        
        for (int size : graphSizes) {
            // Create test graph
            EmergencyNetwork network = createTestGraph(size);
            DijkstraPathfinder pathfinder = new DijkstraPathfinder();
            
            List<Location> locations = new ArrayList<>(network.getAllLocations());
            Location source = locations.get(0);
            Location dest = locations.get(size - 1);
            
            // Warm-up
            for (int i = 0; i < 3; i++) {
                pathfinder.findShortestPath(network, source, dest);
            }
            
            // Measure
            System.gc(); // Suggest garbage collection
            long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            
            long startTime = System.nanoTime();
            for (int i = 0; i < 10; i++) {
                pathfinder.findShortestPath(network, source, dest);
            }
            long endTime = System.nanoTime();
            
            long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            
            long avgTime = (endTime - startTime) / 10;
            long memUsed = Math.max(0, memAfter - memBefore);
            
            PerformanceMeasurement measurement = new PerformanceMeasurement(
                "Dijkstra", size, avgTime, memUsed);
            measurements.add(measurement);
            
            System.out.printf("  n=%4d: %8.3f ms (V+E=%d, log V=%.2f)\n",
                size, measurement.executionTimeMs, 
                size + size * 3, Math.log(size) / Math.log(2));
        }
        
        // Calculate R² for O((V+E) log V) fit
        double rSquared = calculateRSquared(measurements, "nlogn");
        boolean matches = rSquared > 0.90; // Good fit threshold
        
        ComplexityAnalysis analysis = new ComplexityAnalysis(
            "Dijkstra's Algorithm", "O((V+E) log V)", measurements, rSquared, matches);
        
        System.out.println("\nComplexity Validation: " + analysis);
        System.out.println("═══════════════════════════════════════════════════════════\n");
        
        return analysis;
    }
    
    /**
     * Benchmark A* algorithm
     * 
     * Theoretical: O((V+E) log V) with better average case
     * 
     * @return Complexity analysis
     */
    public static ComplexityAnalysis benchmarkAStar() {
        List<PerformanceMeasurement> measurements = new ArrayList<>();
        int[] graphSizes = {10, 25, 50, 100, 200, 500, 1000};
        
        System.out.println("\nBenchmarking A* Algorithm:");
        System.out.println("═══════════════════════════════════════════════════════════");
        
        for (int size : graphSizes) {
            EmergencyNetwork network = createTestGraph(size);
            AStarPathfinder pathfinder = new AStarPathfinder();
            
            List<Location> locations = new ArrayList<>(network.getAllLocations());
            Location source = locations.get(0);
            Location dest = locations.get(size - 1);
            
            // Warm-up
            for (int i = 0; i < 3; i++) {
                pathfinder.findShortestPath(network, source, dest);
            }
            
            // Measure
            System.gc();
            long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            
            long startTime = System.nanoTime();
            for (int i = 0; i < 10; i++) {
                pathfinder.findShortestPath(network, source, dest);
            }
            long endTime = System.nanoTime();
            
            long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            
            long avgTime = (endTime - startTime) / 10;
            long memUsed = Math.max(0, memAfter - memBefore);
            
            PerformanceMeasurement measurement = new PerformanceMeasurement(
                "A*", size, avgTime, memUsed);
            measurements.add(measurement);
            
            System.out.printf("  n=%4d: %8.3f ms\n", size, measurement.executionTimeMs);
        }
        
        double rSquared = calculateRSquared(measurements, "nlogn");
        boolean matches = rSquared > 0.85; // A* can be faster, so lower threshold
        
        ComplexityAnalysis analysis = new ComplexityAnalysis(
            "A* Algorithm", "O((V+E) log V)", measurements, rSquared, matches);
        
        System.out.println("\nComplexity Validation: " + analysis);
        System.out.println("═══════════════════════════════════════════════════════════\n");
        
        return analysis;
    }
    
    /**
     * Benchmark dispatch operations
     * 
     * Theoretical: O(U * (V+E) log V) where U = units
     * 
     * @return Complexity analysis
     */
    public static ComplexityAnalysis benchmarkDispatch() {
        List<PerformanceMeasurement> measurements = new ArrayList<>();
        int[] graphSizes = {10, 25, 50, 100, 200};
        
        System.out.println("\nBenchmarking Dispatch Manager:");
        System.out.println("═══════════════════════════════════════════════════════════");
        
        for (int size : graphSizes) {
            EmergencyNetwork network = createTestGraph(size);
            DispatchManager dispatcher = new DispatchManager(network);
            
            List<Location> locations = new ArrayList<>(network.getAllLocations());
            
            // Add units
            for (int i = 0; i < Math.min(10, size / 5); i++) {
                Location loc = locations.get(i * 5 % size);
                ResponseUnit unit = new ResponseUnit(
                    "UNIT-" + i, "Unit " + i,
                    ResponseUnit.UnitType.AMBULANCE, loc);
                dispatcher.registerUnit(unit);
            }
            
            // Add incidents
            for (int i = 0; i < 5; i++) {
                Location loc = locations.get(i * 10 % size);
                Incident incident = new Incident(
                    "INC-" + i, loc,
                    Incident.IncidentType.MEDICAL,
                    Incident.IncidentSeverity.HIGH);
                dispatcher.reportIncident(incident);
            }
            
            // Measure
            System.gc();
            long memBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            
            long startTime = System.nanoTime();
            dispatcher.dispatchAll();
            long endTime = System.nanoTime();
            
            long memAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            
            long execTime = endTime - startTime;
            long memUsed = Math.max(0, memAfter - memBefore);
            
            PerformanceMeasurement measurement = new PerformanceMeasurement(
                "Dispatch", size, execTime, memUsed);
            measurements.add(measurement);
            
            System.out.printf("  n=%4d: %8.3f ms\n", size, measurement.executionTimeMs);
        }
        
        double rSquared = calculateRSquared(measurements, "n2logn");
        boolean matches = rSquared > 0.85;
        
        ComplexityAnalysis analysis = new ComplexityAnalysis(
            "Dispatch Manager", "O(U * (V+E) log V)", measurements, rSquared, matches);
        
        System.out.println("\nComplexity Validation: " + analysis);
        System.out.println("═══════════════════════════════════════════════════════════\n");
        
        return analysis;
    }
    
    /**
     * Calculate R² (coefficient of determination) for goodness of fit
     * 
     * @param measurements Performance measurements
     * @param complexityType Expected complexity type
     * @return R² value (0-1, higher is better fit)
     */
    private static double calculateRSquared(
            List<PerformanceMeasurement> measurements, String complexityType) {
        
        if (measurements.size() < 2) return 0.0;
        
        // Calculate mean of observed times
        double meanTime = measurements.stream()
            .mapToDouble(m -> m.executionTimeMs)
            .average()
            .orElse(0.0);
        
        // Calculate predicted times based on complexity
        List<Double> predicted = new ArrayList<>();
        for (PerformanceMeasurement m : measurements) {
            double pred = predictTime(m.inputSize, complexityType, measurements);
            predicted.add(pred);
        }
        
        // Calculate SS_tot and SS_res
        double ssTot = 0;
        double ssRes = 0;
        
        for (int i = 0; i < measurements.size(); i++) {
            double observed = measurements.get(i).executionTimeMs;
            double pred = predicted.get(i);
            
            ssTot += Math.pow(observed - meanTime, 2);
            ssRes += Math.pow(observed - pred, 2);
        }
        
        // R² = 1 - (SS_res / SS_tot)
        return 1.0 - (ssRes / ssTot);
    }
    
    /**
     * Predict execution time based on complexity type
     */
    private static double predictTime(int n, String complexityType, 
                                     List<PerformanceMeasurement> measurements) {
        // Use first measurement as baseline
        PerformanceMeasurement baseline = measurements.get(0);
        double baseTime = baseline.executionTimeMs;
        int baseN = baseline.inputSize;
        
        double ratio;
        switch (complexityType) {
            case "constant":
                ratio = 1.0;
                break;
            case "linear":
                ratio = (double) n / baseN;
                break;
            case "nlogn":
                double baseNLogN = baseN * Math.log(baseN);
                double nLogN = n * Math.log(n);
                ratio = nLogN / baseNLogN;
                break;
            case "n2":
                ratio = Math.pow((double) n / baseN, 2);
                break;
            case "n2logn":
                double baseN2LogN = Math.pow(baseN, 2) * Math.log(baseN);
                double n2LogN = Math.pow(n, 2) * Math.log(n);
                ratio = n2LogN / baseN2LogN;
                break;
            default:
                ratio = (double) n / baseN;
        }
        
        return baseTime * ratio;
    }
    
    /**
     * Create test graph of specified size
     * 
     * Creates a graph with n nodes and approximately 3n edges (sparse graph)
     * 
     * @param size Number of nodes
     * @return Emergency network
     */
    private static EmergencyNetwork createTestGraph(int size) {
        EmergencyNetwork network = new EmergencyNetwork();
        Random random = new Random(42); // Fixed seed for reproducibility
        
        // Create locations
        List<Location> locations = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            double x = random.nextDouble() * 100;
            double y = random.nextDouble() * 100;
            Location loc = new Location(
                "LOC-" + i, "Location " + i,
                x, y, Location.LocationType.CITY);
            network.addLocation(loc);
            locations.add(loc);
        }
        
        // Create edges (approximately 3 per node)
        for (int i = 0; i < size; i++) {
            Location from = locations.get(i);
            
            // Connect to next 3 nodes (circular)
            for (int j = 1; j <= 3 && i + j < size; j++) {
                Location to = locations.get(i + j);
                double distance = from.distanceTo(to);
                network.addEdge(from, to, distance, distance * 1.5);
            }
        }
        
        return network;
    }
    
    /**
     * Generate comprehensive performance profiling report
     * 
     * @return Formatted report
     */
    public static String generateProfilingReport() {
        StringBuilder report = new StringBuilder();
        
        report.append("╔════════════════════════════════════════════════════════════╗\n");
        report.append("║         PERFORMANCE PROFILING & COMPLEXITY VALIDATION      ║\n");
        report.append("╚════════════════════════════════════════════════════════════╝\n\n");
        
        // Run all benchmarks
        ComplexityAnalysis dijkstra = benchmarkDijkstra();
        ComplexityAnalysis aStar = benchmarkAStar();
        ComplexityAnalysis dispatch = benchmarkDispatch();
        
        // Summary
        report.append("COMPLEXITY VALIDATION SUMMARY:\n");
        report.append("═══════════════════════════════════════════════════════════\n");
        report.append(dijkstra).append("\n");
        report.append(aStar).append("\n");
        report.append(dispatch).append("\n");
        report.append("═══════════════════════════════════════════════════════════\n\n");
        
        // Performance comparison
        report.append("ALGORITHM PERFORMANCE COMPARISON (n=100):\n");
        report.append("═══════════════════════════════════════════════════════════\n");
        
        PerformanceMeasurement dijkstra100 = dijkstra.measurements.stream()
            .filter(m -> m.inputSize == 100)
            .findFirst()
            .orElse(null);
        
        PerformanceMeasurement aStar100 = aStar.measurements.stream()
            .filter(m -> m.inputSize == 100)
            .findFirst()
            .orElse(null);
        
        if (dijkstra100 != null && aStar100 != null) {
            double speedup = dijkstra100.executionTimeMs / aStar100.executionTimeMs;
            report.append(String.format("  Dijkstra: %.3f ms\n", dijkstra100.executionTimeMs));
            report.append(String.format("  A*:       %.3f ms\n", aStar100.executionTimeMs));
            report.append(String.format("  A* Speedup: %.2fx faster\n", speedup));
        }
        
        report.append("═══════════════════════════════════════════════════════════\n");
        
        return report.toString();
    }
}
