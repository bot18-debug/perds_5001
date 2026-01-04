package perds.evaluation;

import perds.models.*;
import perds.algorithms.*;
import perds.utils.PerformanceMetrics;
import java.util.*;
import java.util.stream.*;

/**
 * Statistical Validation and Evaluation Framework
 * 
 * Provides comprehensive statistical analysis of system performance including:
 * - Confidence intervals
 * - Hypothesis testing
 * - Cross-validation
 * - Performance comparisons
 * - Statistical significance testing
 * 
 * Time Complexity: O(n log n) for most operations where n = sample size
 * Space Complexity: O(n)
 * 
 * @author PERDS Team
 * @version 2.0
 */
public class StatisticalValidator {
    
    /**
     * Result of statistical test
     */
    public static class StatisticalTest {
        public final String testName;
        public final double testStatistic;
        public final double pValue;
        public final boolean isSignificant;
        public final double confidenceLevel;
        public final String interpretation;
        
        public StatisticalTest(String name, double stat, double p, double conf) {
            this.testName = name;
            this.testStatistic = stat;
            this.pValue = p;
            this.confidenceLevel = conf;
            this.isSignificant = p < (1.0 - conf);
            this.interpretation = generateInterpretation();
        }
        
        private String generateInterpretation() {
            if (isSignificant) {
                return String.format("Statistically significant at %.1f%% confidence level (p=%.4f)", 
                    confidenceLevel * 100, pValue);
            } else {
                return String.format("Not statistically significant at %.1f%% confidence level (p=%.4f)", 
                    confidenceLevel * 100, pValue);
            }
        }
        
        @Override
        public String toString() {
            return String.format("%s: statistic=%.4f, p-value=%.4f, %s", 
                testName, testStatistic, pValue, interpretation);
        }
    }
    
    /**
     * Confidence interval result
     */
    public static class ConfidenceInterval {
        public final double mean;
        public final double lowerBound;
        public final double upperBound;
        public final double confidenceLevel;
        public final int sampleSize;
        
        public ConfidenceInterval(double mean, double lower, double upper, double conf, int n) {
            this.mean = mean;
            this.lowerBound = lower;
            this.upperBound = upper;
            this.confidenceLevel = conf;
            this.sampleSize = n;
        }
        
        @Override
        public String toString() {
            return String.format("%.2f [%.2f, %.2f] at %.0f%% confidence (n=%d)", 
                mean, lowerBound, upperBound, confidenceLevel * 100, sampleSize);
        }
    }
    
    /**
     * Calculate mean and standard deviation
     * 
     * Time Complexity: O(n)
     */
    public static class DescriptiveStats {
        public final double mean;
        public final double stdDev;
        public final double median;
        public final double min;
        public final double max;
        public final int count;
        
        public DescriptiveStats(List<Double> data) {
            this.count = data.size();
            if (count == 0) {
                this.mean = this.stdDev = this.median = this.min = this.max = 0.0;
                return;
            }
            
            // Mean
            this.mean = data.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            
            // Standard deviation
            double variance = data.stream()
                .mapToDouble(x -> Math.pow(x - mean, 2))
                .average()
                .orElse(0.0);
            this.stdDev = Math.sqrt(variance);
            
            // Median
            List<Double> sorted = new ArrayList<>(data);
            Collections.sort(sorted);
            if (count % 2 == 0) {
                this.median = (sorted.get(count/2 - 1) + sorted.get(count/2)) / 2.0;
            } else {
                this.median = sorted.get(count/2);
            }
            
            // Min/Max
            this.min = sorted.get(0);
            this.max = sorted.get(count - 1);
        }
        
        @Override
        public String toString() {
            return String.format("Mean=%.2f±%.2f, Median=%.2f, Range=[%.2f, %.2f], n=%d",
                mean, stdDev, median, min, max, count);
        }
    }
    
    /**
     * Calculate confidence interval for mean
     * 
     * Uses t-distribution for small samples (n < 30)
     * Uses normal distribution for large samples (n >= 30)
     * 
     * @param data Sample data
     * @param confidenceLevel Confidence level (e.g., 0.95 for 95%)
     * @return Confidence interval
     * 
     * Time Complexity: O(n)
     */
    public static ConfidenceInterval calculateConfidenceInterval(
            List<Double> data, double confidenceLevel) {
        
        if (data.isEmpty()) {
            return new ConfidenceInterval(0, 0, 0, confidenceLevel, 0);
        }
        
        DescriptiveStats stats = new DescriptiveStats(data);
        int n = data.size();
        
        // Standard error
        double se = stats.stdDev / Math.sqrt(n);
        
        // Critical value (using t-distribution approximation)
        double criticalValue = getTCriticalValue(n - 1, confidenceLevel);
        
        // Margin of error
        double marginOfError = criticalValue * se;
        
        double lower = stats.mean - marginOfError;
        double upper = stats.mean + marginOfError;
        
        return new ConfidenceInterval(stats.mean, lower, upper, confidenceLevel, n);
    }
    
    /**
     * Get t-critical value (simplified approximation)
     * 
     * For proper implementation, use t-distribution table
     * This uses normal approximation for df > 30
     */
    private static double getTCriticalValue(int degreesOfFreedom, double confidenceLevel) {
        // Simplified: use normal approximation
        // For 95% confidence: z = 1.96
        // For 99% confidence: z = 2.576
        
        if (confidenceLevel >= 0.99) {
            return degreesOfFreedom < 30 ? 2.75 : 2.576;
        } else if (confidenceLevel >= 0.95) {
            return degreesOfFreedom < 30 ? 2.04 : 1.96;
        } else if (confidenceLevel >= 0.90) {
            return degreesOfFreedom < 30 ? 1.73 : 1.645;
        } else {
            return 1.96; // default
        }
    }
    
    /**
     * Perform two-sample t-test to compare two algorithms
     * 
     * H0: The two algorithms have equal mean performance
     * H1: The two algorithms have different mean performance
     * 
     * @param sample1 Performance data from algorithm 1
     * @param sample2 Performance data from algorithm 2
     * @param confidenceLevel Confidence level
     * @return Statistical test result
     * 
     * Time Complexity: O(n + m) where n, m are sample sizes
     */
    public static StatisticalTest twoSampleTTest(
            List<Double> sample1, List<Double> sample2, double confidenceLevel) {
        
        DescriptiveStats stats1 = new DescriptiveStats(sample1);
        DescriptiveStats stats2 = new DescriptiveStats(sample2);
        
        int n1 = sample1.size();
        int n2 = sample2.size();
        
        // Pooled standard deviation
        double pooledVar = ((n1 - 1) * Math.pow(stats1.stdDev, 2) + 
                           (n2 - 1) * Math.pow(stats2.stdDev, 2)) / (n1 + n2 - 2);
        double pooledStdDev = Math.sqrt(pooledVar);
        
        // Standard error of difference
        double se = pooledStdDev * Math.sqrt(1.0/n1 + 1.0/n2);
        
        // t-statistic
        double tStat = (stats1.mean - stats2.mean) / se;
        
        // Degrees of freedom
        int df = n1 + n2 - 2;
        
        // p-value (two-tailed, approximation)
        double pValue = 2 * (1 - normalCDF(Math.abs(tStat)));
        
        return new StatisticalTest("Two-Sample t-Test", tStat, pValue, confidenceLevel);
    }
    
    /**
     * Perform paired t-test for before/after comparison
     * 
     * @param before Performance before intervention
     * @param after Performance after intervention
     * @param confidenceLevel Confidence level
     * @return Statistical test result
     * 
     * Time Complexity: O(n)
     */
    public static StatisticalTest pairedTTest(
            List<Double> before, List<Double> after, double confidenceLevel) {
        
        if (before.size() != after.size()) {
            throw new IllegalArgumentException("Sample sizes must match for paired t-test");
        }
        
        // Calculate differences
        List<Double> differences = new ArrayList<>();
        for (int i = 0; i < before.size(); i++) {
            differences.add(after.get(i) - before.get(i));
        }
        
        DescriptiveStats diffStats = new DescriptiveStats(differences);
        int n = differences.size();
        
        // t-statistic
        double tStat = diffStats.mean / (diffStats.stdDev / Math.sqrt(n));
        
        // p-value (two-tailed, approximation)
        double pValue = 2 * (1 - normalCDF(Math.abs(tStat)));
        
        return new StatisticalTest("Paired t-Test", tStat, pValue, confidenceLevel);
    }
    
    /**
     * Normal CDF approximation
     * 
     * Uses Abramowitz and Stegun approximation
     */
    private static double normalCDF(double x) {
        // Constants
        double a1 =  0.254829592;
        double a2 = -0.284496736;
        double a3 =  1.421413741;
        double a4 = -1.453152027;
        double a5 =  1.061405429;
        double p  =  0.3275911;
        
        int sign = x < 0 ? -1 : 1;
        x = Math.abs(x) / Math.sqrt(2.0);
        
        double t = 1.0 / (1.0 + p * x);
        double y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * Math.exp(-x * x);
        
        return 0.5 * (1.0 + sign * y);
    }
    
    /**
     * Perform k-fold cross-validation
     * 
     * @param data All available data
     * @param k Number of folds
     * @return List of performance metrics for each fold
     * 
     * Time Complexity: O(k * n)
     */
    public static List<Double> kFoldCrossValidation(List<Double> data, int k) {
        if (data.size() < k) {
            throw new IllegalArgumentException("Data size must be >= k");
        }
        
        List<Double> foldPerformances = new ArrayList<>();
        int foldSize = data.size() / k;
        
        for (int i = 0; i < k; i++) {
            int start = i * foldSize;
            int end = (i == k - 1) ? data.size() : (i + 1) * foldSize;
            
            // Test fold
            List<Double> testFold = data.subList(start, end);
            
            // Training folds
            List<Double> trainingFolds = new ArrayList<>();
            trainingFolds.addAll(data.subList(0, start));
            trainingFolds.addAll(data.subList(end, data.size()));
            
            // Calculate performance (using mean as simple metric)
            DescriptiveStats stats = new DescriptiveStats(testFold);
            foldPerformances.add(stats.mean);
        }
        
        return foldPerformances;
    }
    
    /**
     * Calculate effect size (Cohen's d)
     * 
     * Measures the magnitude of difference between two groups
     * Small: 0.2, Medium: 0.5, Large: 0.8
     * 
     * @param sample1 First sample
     * @param sample2 Second sample
     * @return Cohen's d effect size
     * 
     * Time Complexity: O(n + m)
     */
    public static double calculateEffectSize(List<Double> sample1, List<Double> sample2) {
        DescriptiveStats stats1 = new DescriptiveStats(sample1);
        DescriptiveStats stats2 = new DescriptiveStats(sample2);
        
        int n1 = sample1.size();
        int n2 = sample2.size();
        
        // Pooled standard deviation
        double pooledVar = ((n1 - 1) * Math.pow(stats1.stdDev, 2) + 
                           (n2 - 1) * Math.pow(stats2.stdDev, 2)) / (n1 + n2 - 2);
        double pooledStdDev = Math.sqrt(pooledVar);
        
        // Cohen's d
        return (stats1.mean - stats2.mean) / pooledStdDev;
    }
    
    /**
     * Generate comprehensive statistical report
     * 
     * @param responseTimes List of response times
     * @param successRates List of success rates
     * @return Formatted statistical report
     */
    public static String generateStatisticalReport(
            List<Double> responseTimes, List<Double> successRates) {
        
        StringBuilder report = new StringBuilder();
        report.append("═══════════════════════════════════════════════════════════\n");
        report.append("           STATISTICAL VALIDATION REPORT\n");
        report.append("═══════════════════════════════════════════════════════════\n\n");
        
        // Response time statistics
        report.append("RESPONSE TIME ANALYSIS:\n");
        DescriptiveStats rtStats = new DescriptiveStats(responseTimes);
        report.append("  ").append(rtStats).append("\n");
        
        ConfidenceInterval rtCI = calculateConfidenceInterval(responseTimes, 0.95);
        report.append("  95% Confidence Interval: ").append(rtCI).append("\n\n");
        
        // Success rate statistics
        report.append("SUCCESS RATE ANALYSIS:\n");
        DescriptiveStats srStats = new DescriptiveStats(successRates);
        report.append("  ").append(srStats).append("\n");
        
        ConfidenceInterval srCI = calculateConfidenceInterval(successRates, 0.95);
        report.append("  95% Confidence Interval: ").append(srCI).append("\n\n");
        
        // Performance reliability
        double rtCV = (rtStats.stdDev / rtStats.mean) * 100; // Coefficient of variation
        report.append("RELIABILITY METRICS:\n");
        report.append(String.format("  Response Time Variability: %.2f%% (lower is better)\n", rtCV));
        
        String reliability = rtCV < 10 ? "EXCELLENT" : rtCV < 20 ? "GOOD" : rtCV < 30 ? "FAIR" : "POOR";
        report.append("  System Reliability: ").append(reliability).append("\n\n");
        
        report.append("═══════════════════════════════════════════════════════════\n");
        
        return report.toString();
    }
    
    /**
     * Compare two algorithms statistically
     * 
     * @param alg1Data Performance data from algorithm 1
     * @param alg2Data Performance data from algorithm 2
     * @param alg1Name Name of algorithm 1
     * @param alg2Name Name of algorithm 2
     * @return Comparison report
     */
    public static String compareAlgorithms(
            List<Double> alg1Data, List<Double> alg2Data,
            String alg1Name, String alg2Name) {
        
        StringBuilder report = new StringBuilder();
        report.append("═══════════════════════════════════════════════════════════\n");
        report.append("         ALGORITHM COMPARISON ANALYSIS\n");
        report.append("═══════════════════════════════════════════════════════════\n\n");
        
        DescriptiveStats stats1 = new DescriptiveStats(alg1Data);
        DescriptiveStats stats2 = new DescriptiveStats(alg2Data);
        
        report.append(alg1Name).append(": ").append(stats1).append("\n");
        report.append(alg2Name).append(": ").append(stats2).append("\n\n");
        
        // Statistical test
        StatisticalTest tTest = twoSampleTTest(alg1Data, alg2Data, 0.95);
        report.append("STATISTICAL SIGNIFICANCE:\n");
        report.append("  ").append(tTest).append("\n\n");
        
        // Effect size
        double effectSize = calculateEffectSize(alg1Data, alg2Data);
        String magnitude = Math.abs(effectSize) < 0.2 ? "negligible" :
                          Math.abs(effectSize) < 0.5 ? "small" :
                          Math.abs(effectSize) < 0.8 ? "medium" : "large";
        
        report.append("PRACTICAL SIGNIFICANCE:\n");
        report.append(String.format("  Effect Size (Cohen's d): %.3f (%s)\n", effectSize, magnitude));
        
        if (effectSize > 0) {
            report.append(String.format("  %s performs %.1f%% better than %s\n", 
                alg1Name, ((stats1.mean - stats2.mean) / stats2.mean) * 100, alg2Name));
        } else {
            report.append(String.format("  %s performs %.1f%% better than %s\n", 
                alg2Name, ((stats2.mean - stats1.mean) / stats1.mean) * 100, alg1Name));
        }
        
        report.append("\n═══════════════════════════════════════════════════════════\n");
        
        return report.toString();
    }
}
