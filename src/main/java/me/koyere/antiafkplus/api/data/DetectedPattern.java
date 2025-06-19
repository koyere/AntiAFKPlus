package me.koyere.antiafkplus.api.data;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a detected suspicious pattern.
 */
public class DetectedPattern {
    private final String patternType;
    private final double confidence;
    private final Instant detectionTime;
    private final Map<String, Object> patternData;
    
    public DetectedPattern(String patternType, double confidence, Instant detectionTime, Map<String, Object> patternData) {
        this.patternType = patternType;
        this.confidence = confidence;
        this.detectionTime = detectionTime;
        this.patternData = patternData;
    }
    
    public String getPatternType() { return patternType; }
    public double getConfidence() { return confidence; }
    public Instant getDetectionTime() { return detectionTime; }
    public Map<String, Object> getPatternData() { return patternData; }
}