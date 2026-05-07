package com.industry.simulator.common.exception;

/**
 * Custom exception for service errors
 */
public class ServiceException extends RuntimeException {
    private final int statusCode;
    private final String errorCode;

    public ServiceException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = "SERVICE_ERROR";
    }

    public ServiceException(String message, int statusCode, String errorCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public ServiceException(String message, Throwable cause, int statusCode, String errorCode) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
