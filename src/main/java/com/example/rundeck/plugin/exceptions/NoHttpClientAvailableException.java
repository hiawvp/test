package com.example.rundeck.plugin.exceptions;

public class NoHttpClientAvailableException extends RuntimeException {
    public NoHttpClientAvailableException(String details) {
        super(details);
    }
}
