package me.koyere.antiafkplus.api.data;

/**
 * Information about a Bedrock Edition player.
 */
public class BedrockPlayerInfo {
    private final String deviceType;
    private final String inputMode;
    private final String uiProfile;
    private final String bedrockVersion;
    private final String detectionMethod;
    private final double confidence;
    
    public BedrockPlayerInfo(String deviceType, String inputMode, String uiProfile, String bedrockVersion,
                            String detectionMethod, double confidence) {
        this.deviceType = deviceType;
        this.inputMode = inputMode;
        this.uiProfile = uiProfile;
        this.bedrockVersion = bedrockVersion;
        this.detectionMethod = detectionMethod;
        this.confidence = confidence;
    }
    
    // Getters
    public String getDeviceType() { return deviceType; }
    public String getInputMode() { return inputMode; }
    public String getUiProfile() { return uiProfile; }
    public String getBedrockVersion() { return bedrockVersion; }
    public String getDetectionMethod() { return detectionMethod; }
    public double getConfidence() { return confidence; }
    
    public boolean isMobile() { return "MOBILE".equalsIgnoreCase(deviceType); }
    public boolean isConsole() { return "CONSOLE".equalsIgnoreCase(deviceType); }
    public boolean isTouchInput() { return "TOUCH".equalsIgnoreCase(inputMode); }
}