package com.test.safetyconnect.exception;

/**
 */
public class ApiResponseException extends Exception {
    private int statusCode;
    private String message;

    public ApiResponseException(int statusCode) {
        this.statusCode = statusCode;
    }

    public ApiResponseException(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }
}
