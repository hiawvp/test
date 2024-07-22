package com.example.rundeck.plugin.notification;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin;
import com.example.rundeck.plugin.exceptions.InvalidConfigException;
import com.example.rundeck.plugin.exceptions.NoHttpClientAvailableException;

@Plugin(service = "Notification", name = "Simple")
@PluginDescription(title = "Simple Notification Plugin", description = "Developer Candidate Exercise - HTTP Notification Plugin")
public class SimpleNotificationPlugin implements NotificationPlugin {

    static final String HTTP_URL = "url";
    static final String HTTP_METHOD = "method";
    static final String HTTP_HEADERS = "headers";
    static final String HTTP_CONTENT_TYPE = "contentType";
    static final String HTTP_BODY = "body";
    static final String HTTP_TIMEOUT = "timeout";
    static final String NUM_RETRIES = "numRetries";
    protected static final List<String> AVAILABLE_HTTP_METHODS = Arrays.asList("GET", "POST", "PUT", "DELETE");

    @PluginProperty(name = HTTP_TIMEOUT, title = "Timeout", description = "Timeout for HTTP requests in milliseconds", defaultValue = "30000")
    private int timeout = 30000;

    @PluginProperty(name = NUM_RETRIES, title = "Retries", description = "Number of retry attempts for failed requests", defaultValue = "3")
    private int numRetries = 3;

    @PluginProperty(name = HTTP_URL, title = "Remote URL", description = "The URL to send the request to", required = true)
    private String url;

    @PluginProperty(name = HTTP_METHOD, title = "HTTP Method", description = "HTTP method to use for the request", defaultValue = "POST")
    private String method = "POST";

    // TODO: parse and include headers...
    @PluginProperty(name = HTTP_HEADERS, title = "Headers", description = "HTTP headers for the request (comma separated values of 'HeaderName:HeaderValue')")
    private String headers;

    @PluginProperty(name = HTTP_CONTENT_TYPE, title = "Content Type", description = "Content type of the request", defaultValue = "application/json")
    private String contentType = "application/json";

    @PluginProperty(name = HTTP_BODY, title = "Body", description = "Body of the HTTP request")
    private String body = "";

    private final CloseableHttpClient httpClient;

    private static final Logger log = LogManager.getLogger();

    public SimpleNotificationPlugin() {
        this.httpClient = null;
    }

    /**
     * Constructs a {@link SimpleNotificationPlugin} with the specified HTTP client.
     *
     * @param httpClient The {@link CloseableHttpClient} used to send HTTP requests.
     */
    public SimpleNotificationPlugin(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * @param timeout
     * @return CloseableHttpClient
     */
    private CloseableHttpClient createHttpClient(int timeout) {
        ConnectionConfig connConfig = ConnectionConfig.custom()
                .setConnectTimeout(timeout, TimeUnit.MILLISECONDS)
                .setSocketTimeout(timeout, TimeUnit.MILLISECONDS)
                .build();

        BasicHttpClientConnectionManager cm = new BasicHttpClientConnectionManager();
        cm.setConnectionConfig(connConfig);

        return HttpClientBuilder.create()
                .setConnectionManager(cm)
                .build();
    }

    /**
     * @param trigger
     * @param executionData
     * @param config
     * @return boolean
     */
    @Override
    public boolean postNotification(String trigger, Map executionData, Map config) {

        ValidatedConfig validatedConfig = readAndValidateConfig(config);

        CloseableHttpClient clientToUse = (httpClient != null) ? httpClient : createHttpClient(timeout);
        if (clientToUse == null) {
            throw new NoHttpClientAvailableException("Client to use is null");
        }

        for (int i = 0; i < validatedConfig.getNumRetries(); i++) {
            try {
                ClassicHttpRequest request = createRequest(validatedConfig.getMethod(), validatedConfig.getRemoteUrl(),
                        validatedConfig.getBody(), validatedConfig.getContentType());
                return clientToUse.execute(request, response -> {
                    int statusCode = response.getCode();
                    log.debug("Response status code: {}", statusCode);
                    return statusCode >= 200 && statusCode < 300;
                });

            } catch (IOException e) {
                if (i == numRetries - 1) {
                    log.warn("Max attempts reached");
                    e.printStackTrace();
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Creates an HTTP request based on the provided method, URL, body, and content
     * type.
     * 
     * @param method      The HTTP method to use (e.g., "GET", "POST", "PUT",
     *                    "DELETE").
     * @param url         The URL for the request.
     * @param body        The body content for the request, or {@code null} if there
     *                    is no body.
     * @param contentType The content type of the body, or {@code null} if not
     *                    applicable.
     * @return A {@link ClassicHttpRequest} object configured according to the
     *         provided parameters.
     */
    private ClassicHttpRequest createRequest(String method, String url, String body, String contentType) {
        ClassicHttpRequest request;

        switch (method.toUpperCase()) {
            case "PUT":
                HttpPut put = new HttpPut(url);
                if (body != null) {
                    put.setEntity(new StringEntity(body));
                    put.setHeader("Content-Type", contentType);
                }
                request = put;
                break;
            case "DELETE":
                request = new HttpDelete(url);
                break;
            case "GET":
                request = new HttpGet(url);
                break;
            default:
            case "POST":
                HttpPost post = new HttpPost(url);
                if (body != null) {
                    post.setEntity(new StringEntity(body));
                    post.setHeader("Content-Type", contentType);
                }
                request = post;
                break;
        }
        return request;
    }

    /**
     * Reads and validates the given configuration.
     *
     * @param config A map containing configuration parameters.
     * @return A {@link ValidatedConfig} object representing the validated
     *         configuration.
     */
    private ValidatedConfig readAndValidateConfig(Map config) {
        String urlVal = (String) config.get(HTTP_URL);

        if (urlVal == null) {
            throw new InvalidConfigException(String.format("Missing required config parameter '%s'", HTTP_URL));
        }

        try {
            new URL(urlVal);
        } catch (MalformedURLException e) {
            throw new InvalidConfigException(String.format("Invalid %s provided: %s", HTTP_URL, urlVal));
        }

        int timeoutVal = (config.get(HTTP_TIMEOUT) != null)
                ? Integer.parseInt((String) config.get(HTTP_TIMEOUT))
                : this.timeout;

        if (timeoutVal < 0) {
            throw new InvalidConfigException(
                    String.format("'%s' value cannot be negative: %d", HTTP_TIMEOUT, timeoutVal));
        }

        int numRetriesVal = (config.get(NUM_RETRIES) != null)
                ? Integer.parseInt((String) config.get(NUM_RETRIES))
                : this.numRetries;

        if (numRetriesVal < 0) {
            throw new InvalidConfigException(
                    String.format("'%s' value cannot be negative: %d", NUM_RETRIES, numRetriesVal));
        }

        String methodVal = ((String) config.getOrDefault(HTTP_METHOD, this.method)).toUpperCase();

        if (!AVAILABLE_HTTP_METHODS.contains(methodVal)) {
            throw new InvalidConfigException(String.format("Invalid HTTP method provided: %s", methodVal));
        }

        String headersVal = (String) config.getOrDefault(HTTP_HEADERS, this.headers);
        String contentTypeVal = (String) config.getOrDefault(HTTP_CONTENT_TYPE, this.contentType);
        String bodyVal = (String) config.getOrDefault(HTTP_BODY, this.body);

        return new ValidatedConfig(
                urlVal,
                methodVal,
                headersVal,
                contentTypeVal,
                bodyVal,
                timeoutVal,
                numRetriesVal);
    }

}
