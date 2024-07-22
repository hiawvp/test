package com.example.rundeck.plugin.exceptions;

public class InvalidConfigException extends IllegalArgumentException {
    public InvalidConfigException(String message) {
        super(message);
    }
}
