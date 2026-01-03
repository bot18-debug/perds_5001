package perds.algorithms;

import perds.models.*;
import java.util.*;
import java.time.*;

/**
 * Machine Learning-based Incident Predictor using Linear Regression
 * Implements adaptive learning to improve predictions over time
 * 
 * Time Complexity: O(n) for prediction, O(n²) for training
 * Space Complexity: O(n) where n = number of features
 * 
 * Features used:
 * - Time of day
 * - Day of week
 * - Historical frequency
 * - Recent trend
 * - Seasonal patterns
 * 
 * @author PERDS Team
 * @version 2.0
 */
public class MachineLearningPredictor {
    
    private Map<Location, FeatureVector> locationFeatures;
    private Map<Location, List<PredictionRecord>> predictionHistory;
    private double learningRate = 0.01;
    private int trainingEpochs = 100;
    
    // Weights for linear regression model
    private static class Weights {
        double timeOfDay = 0.5;
        double dayOfWeek = 0.3;
        double historicalFrequency = 0.8;
        double recentTrend = 0.6;
        double seasonalPattern = 0.4;
        double bias = 0.1;
    }
    
    private Weights weights;
    
    /**
     * Feature vector for a location at a given time
     */
    private static class FeatureVector {
        double timeOfDay;        // 0-23 normalized to 0-1
        double dayOfWeek;        // 0-6 normalized to 0-1
        double historicalFreq;   // incidents per hour
        double recentTrend;      // change in last 24h
        double seasonalPattern;  // monthly variation
        
        public FeatureVector(double time, double day, double freq, double trend, double season) {
            this.timeOfDay = time / 24.0;
            this.dayOfWeek = day / 7.0;
            this.historicalFreq = Math.min(freq / 10.0, 1.0); // normalize
            this.recentTrend = Math.tanh(trend); // squash to [-1, 1]
            this.seasonalPattern = season;
        }
    }
    
    /**
     * Record of a prediction and its actual outcome
     */
    private static class PredictionRecord {
        LocalDateTime timestamp;
        double predicted;
        double actual;
        double error;
        
        public PredictionRecord(LocalDateTime time, double pred, double act) {
            this.timestamp = time;
            this.predicted = pred;
            this.actual = act;
            this.error = Math.abs(pred - act);
        }
    }
    
    /**
     * Constructor initializes the ML model
     */
    public MachineLearningPredictor() {
        this.locationFeatures = new HashMap<>();
        this.predictionHistory = new HashMap<>();
        this.weights = new Weights();
    }
    
    /**
     * Predict incident probability for a location at current time
     * 
     * Uses linear regression: P = w1*f1 + w2*f2 + ... + bias
     * 
     * @param location Location to predict for
     * @param currentTime Current timestamp
     * @return Probability of incident (0-1)
     * 
     * Time Complexity: O(1)
     * Space Complexity: O(1)
     */
    public double predictIncidentProbability(Location location, LocalDateTime currentTime) {
        FeatureVector features = extractFeatures(location, currentTime);
        
        // Linear combination of features
        double prediction = 
            weights.timeOfDay * features.timeOfDay +
            weights.dayOfWeek * features.dayOfWeek +
            weights.historicalFrequency * features.historicalFreq +
            weights.recentTrend * features.recentTrend +
            weights.seasonalPattern * features.seasonalPattern +
            weights.bias;
        
        // Apply sigmoid to get probability [0, 1]
        return sigmoid(prediction);
    }
    
    /**
     * Extract feature vector for a location at given time
     * 
     * @param location Location to analyze
     * @param time Timestamp
     * @return FeatureVector containing all features
     * 
     * Time Complexity: O(1)
     */
    private FeatureVector extractFeatures(Location location, LocalDateTime time) {
        double timeOfDay = time.getHour();
        double dayOfWeek = time.getDayOfWeek().getValue();
        
        // Calculate historical frequency (incidents per hour)
        double historicalFreq = calculateHistoricalFrequency(location);
        
        // Calculate recent trend (change in last 24 hours)
        double recentTrend = calculateRecentTrend(location, time);
        
        // Calculate seasonal pattern (monthly variation)
        double seasonalPattern = calculateSeasonalPattern(location, time);
        
        return new FeatureVector(timeOfDay, dayOfWeek, historicalFreq, recentTrend, seasonalPattern);
    }
    
    /**
     * Calculate historical incident frequency for location
     * 
     * @param location Location to analyze
     * @return Incidents per hour
     */
    private double calculateHistoricalFrequency(Location location) {
        if (!predictionHistory.containsKey(location)) {
            return 0.5; // Default value
        }
        
        List<PredictionRecord> history = predictionHistory.get(location);
        if (history.isEmpty()) {
            return 0.5;
        }
        
        // Calculate average actual incidents
        double sum = history.stream().mapToDouble(r -> r.actual).sum();
        return sum / history.size();
    }
    
    /**
     * Calculate recent trend in incident rate
     * 
     * @param location Location to analyze
     * @param currentTime Current timestamp
     * @return Trend value (positive = increasing, negative = decreasing)
     */
    private double calculateRecentTrend(Location location, LocalDateTime currentTime) {
        if (!predictionHistory.containsKey(location)) {
            return 0.0;
        }
        
        List<PredictionRecord> history = predictionHistory.get(location);
        if (history.size() < 2) {
            return 0.0;
        }
        
        // Get recent records (last 24 hours)
        LocalDateTime cutoff = currentTime.minusHours(24);
        List<PredictionRecord> recent = history.stream()
            .filter(r -> r.timestamp.isAfter(cutoff))
            .toList();
        
        if (recent.size() < 2) {
            return 0.0;
        }
        
        // Calculate trend using linear regression
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int n = recent.size();
        
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = recent.get(i).actual;
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        
        // Slope of regression line
        double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        return slope;
    }
    
    /**
     * Calculate seasonal pattern for location
     * 
     * @param location Location to analyze
     * @param time Current timestamp
     * @return Seasonal factor (0-1)
     */
    private double calculateSeasonalPattern(Location location, LocalDateTime time) {
        int month = time.getMonthValue();
        
        // Simple seasonal pattern: higher in summer months
        // This could be enhanced with actual historical data
        double[] monthlyFactors = {
            0.6, 0.7, 0.8, 0.9, 1.0, 1.0,  // Jan-Jun
            1.0, 0.9, 0.8, 0.7, 0.6, 0.5   // Jul-Dec
        };
        
        return monthlyFactors[month - 1];
    }
    
    /**
     * Record actual incident for model learning
     * 
     * @param location Location where incident occurred
     * @param time Timestamp of incident
     * @param occurred Whether incident actually occurred (1.0 or 0.0)
     * 
     * Time Complexity: O(1)
     */
    public void recordActual(Location location, LocalDateTime time, double occurred) {
        // Get previous prediction
        double predicted = predictIncidentProbability(location, time);
        
        // Create prediction record
        PredictionRecord record = new PredictionRecord(time, predicted, occurred);
        
        // Store in history
        predictionHistory.computeIfAbsent(location, k -> new ArrayList<>()).add(record);
        
        // Adaptive learning: update weights based on error
        updateWeights(extractFeatures(location, time), predicted, occurred);
        
        // Keep history size manageable (last 1000 records per location)
        List<PredictionRecord> history = predictionHistory.get(location);
        if (history.size() > 1000) {
            history.remove(0);
        }
    }
    
    /**
     * Update model weights using gradient descent
     * 
     * @param features Feature vector
     * @param predicted Predicted value
     * @param actual Actual value
     * 
     * Time Complexity: O(1)
     */
    private void updateWeights(FeatureVector features, double predicted, double actual) {
        // Calculate error
        double error = actual - predicted;
        
        // Gradient descent update: w = w + α * error * feature
        weights.timeOfDay += learningRate * error * features.timeOfDay;
        weights.dayOfWeek += learningRate * error * features.dayOfWeek;
        weights.historicalFrequency += learningRate * error * features.historicalFreq;
        weights.recentTrend += learningRate * error * features.recentTrend;
        weights.seasonalPattern += learningRate * error * features.seasonalPattern;
        weights.bias += learningRate * error;
        
        // Prevent weights from exploding
        clipWeights();
    }
    
    /**
     * Clip weights to reasonable range
     */
    private void clipWeights() {
        weights.timeOfDay = Math.max(-2.0, Math.min(2.0, weights.timeOfDay));
        weights.dayOfWeek = Math.max(-2.0, Math.min(2.0, weights.dayOfWeek));
        weights.historicalFrequency = Math.max(-2.0, Math.min(2.0, weights.historicalFrequency));
        weights.recentTrend = Math.max(-2.0, Math.min(2.0, weights.recentTrend));
        weights.seasonalPattern = Math.max(-2.0, Math.min(2.0, weights.seasonalPattern));
        weights.bias = Math.max(-2.0, Math.min(2.0, weights.bias));
    }
    
    /**
     * Sigmoid activation function
     * 
     * @param x Input value
     * @return Output in range (0, 1)
     */
    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }
    
    /**
     * Get prediction accuracy for a location
     * 
     * @param location Location to analyze
     * @return Mean Absolute Error (MAE)
     * 
     * Time Complexity: O(n) where n = number of predictions
     */
    public double getPredictionAccuracy(Location location) {
        if (!predictionHistory.containsKey(location)) {
            return 0.0;
        }
        
        List<PredictionRecord> history = predictionHistory.get(location);
        if (history.isEmpty()) {
            return 0.0;
        }
        
        double totalError = history.stream().mapToDouble(r -> r.error).sum();
        return 1.0 - (totalError / history.size()); // Convert MAE to accuracy
    }
    
    /**
     * Get model performance metrics
     * 
     * @return Map of location to accuracy
     */
    public Map<Location, Double> getModelPerformance() {
        Map<Location, Double> performance = new HashMap<>();
        
        for (Location location : predictionHistory.keySet()) {
            performance.put(location, getPredictionAccuracy(location));
        }
        
        return performance;
    }
    
    /**
     * Train model on historical data
     * 
     * @param historicalIncidents List of past incidents
     * 
     * Time Complexity: O(n * e) where n = incidents, e = epochs
     */
    public void train(List<Incident> historicalIncidents) {
        if (historicalIncidents.isEmpty()) {
            return;
        }
        
        // Multiple training epochs for better convergence
        for (int epoch = 0; epoch < trainingEpochs; epoch++) {
            for (Incident incident : historicalIncidents) {
                LocalDateTime time = LocalDateTime.now(); // In real system, use incident.getTimestamp()
                recordActual(incident.getLocation(), time, 1.0);
            }
        }
    }
    
    /**
     * Reset model to initial state
     */
    public void reset() {
        weights = new Weights();
        predictionHistory.clear();
        locationFeatures.clear();
    }
    
    /**
     * Get current model weights (for debugging/analysis)
     * 
     * @return String representation of weights
     */
    public String getWeightsString() {
        return String.format(
            "Weights: TimeOfDay=%.3f, DayOfWeek=%.3f, HistFreq=%.3f, Trend=%.3f, Seasonal=%.3f, Bias=%.3f",
            weights.timeOfDay, weights.dayOfWeek, weights.historicalFrequency,
            weights.recentTrend, weights.seasonalPattern, weights.bias
        );
    }
    
    /**
     * Set learning rate for gradient descent
     * 
     * @param rate Learning rate (typically 0.001 - 0.1)
     */
    public void setLearningRate(double rate) {
        this.learningRate = Math.max(0.001, Math.min(0.5, rate));
    }
    
    /**
     * Set number of training epochs
     * 
     * @param epochs Number of epochs (typically 10-1000)
     */
    public void setTrainingEpochs(int epochs) {
        this.trainingEpochs = Math.max(1, Math.min(1000, epochs));
    }
}
