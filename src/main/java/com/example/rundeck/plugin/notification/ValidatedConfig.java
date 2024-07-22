package com.example.rundeck.plugin.notification;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents a validated configuration for a notification plugin.
 * 
 * This class encapsulates the configuration parameters used by the plugin,
 * including the remote URL,
 * HTTP method, headers, content type, request body, timeout, and retry
 * settings.
 */
@AllArgsConstructor
@Data
public class ValidatedConfig {
    private String remoteUrl;
    private String method;
    private String headers;
    private String contentType;
    private String body;
    private int timeout;
    private int numRetries;
}
