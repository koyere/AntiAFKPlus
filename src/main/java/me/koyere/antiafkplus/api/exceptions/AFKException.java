package me.koyere.antiafkplus.api.exceptions;

/**
 * Base exception for AFK-related operations.
 */
public class AFKException extends Exception {
    
    private final AFKErrorCode errorCode;
    
    public AFKException(String message) {
        super(message);
        this.errorCode = AFKErrorCode.GENERAL_ERROR;
    }
    
    public AFKException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = AFKErrorCode.GENERAL_ERROR;
    }
    
    public AFKException(AFKErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public AFKException(AFKErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    /**
     * Get the error code for this exception.
     * @return The error code
     */
    public AFKErrorCode getErrorCode() {
        return errorCode;
    }
    
    /**
     * Error codes for AFK exceptions.
     */
    public enum AFKErrorCode {
        GENERAL_ERROR("General error"),
        PLAYER_NOT_FOUND("Player not found"),
        PLAYER_OFFLINE("Player is offline"),
        INVALID_STATUS("Invalid AFK status"),
        PERMISSION_DENIED("Permission denied"),
        OPERATION_FAILED("Operation failed"),
        INVALID_DURATION("Invalid duration"),
        MODULE_DISABLED("Required module is disabled"),
        CONFIGURATION_ERROR("Configuration error"),
        INTERNAL_ERROR("Internal error");
        
        private final String description;
        
        AFKErrorCode(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}