package com.example.rundeck.plugin.notification;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.rundeck.plugin.exceptions.InvalidConfigException;;

@ExtendWith(MockitoExtension.class)
class SimpleNotificationPluginTest {

    @Mock
    private CloseableHttpClient httpClient;

    @InjectMocks
    private SimpleNotificationPlugin plugin;

    private Map<String, String> createConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("url", "http://example.com");
        config.put("method", "POST");
        config.put("body", "test body");
        config.put("contentType", "application/json");
        return config;
    }

    @Test
    void testPostNotificationSuccess() throws IOException, HttpException {
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
        when(mockResponse.getCode()).thenReturn(200);

        when(httpClient.execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class)))
                .thenAnswer(invocation -> {
                    HttpClientResponseHandler<CloseableHttpResponse> responseHandler = invocation.getArgument(1);
                    return responseHandler.handleResponse(mockResponse);
                });

        plugin = new SimpleNotificationPlugin(httpClient);
        Map<String, String> config = createConfig();
        boolean result = plugin.postNotification("trigger", new HashMap<>(), config);

        assertTrue(result);
        verify(httpClient).execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class));
    }

    @Test
    void testPostNotificationFailure() throws IOException {
        when(httpClient.execute(any(ClassicHttpRequest.class),
                any(HttpClientResponseHandler.class)))
                .thenThrow(new IOException("Simulated IOException"));

        plugin = new SimpleNotificationPlugin(httpClient);
        Map<String, String> config = createConfig();
        boolean result = plugin.postNotification("trigger", new HashMap<>(), config);
        assertFalse(result);
    }

    @Test
    void testPostNotificationEmptyUrl() {
        plugin = new SimpleNotificationPlugin(httpClient);
        Map<String, String> config = createConfig();
        config.put("url", "");
        Executable executable = () -> plugin.postNotification("trigger", new HashMap<>(), config);
        assertThrows(InvalidConfigException.class, executable);
    }

    @Test
    void testPostNotificationInvalidUrl() {
        plugin = new SimpleNotificationPlugin(httpClient);
        Map<String, String> config = createConfig();
        config.put("url", "[[[forsen]]]");
        Executable executable = () -> plugin.postNotification("trigger", new HashMap<>(), config);
        assertThrows(InvalidConfigException.class, executable);
    }

    @Test
    void testPostNotificationInvalidMethod() {
        plugin = new SimpleNotificationPlugin(httpClient);
        Map<String, String> config = createConfig();
        config.put("method", "forsen");
        Executable executable = () -> plugin.postNotification("trigger", new HashMap<>(), config);
        assertThrows(InvalidConfigException.class, executable);
    }

    @Test
    void testPostNotificationPutSuccess() throws IOException {
        CloseableHttpResponse mockResponse = mock(CloseableHttpResponse.class);
        when(mockResponse.getCode()).thenReturn(200);

        when(httpClient.execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class)))
                .thenAnswer(invocation -> {
                    HttpClientResponseHandler<CloseableHttpResponse> responseHandler = invocation.getArgument(1);
                    return responseHandler.handleResponse(mockResponse);
                });

        plugin = new SimpleNotificationPlugin(httpClient);
        Map<String, String> config = createConfig();
        config.put("method", "put");
        boolean result = plugin.postNotification("trigger", new HashMap<>(), config);
        assertTrue(result);

        // Verify that httpClient.execute was called with a request using the PUT method
        verify(httpClient).execute(argThat(request -> "PUT".equals(request.getMethod())),
                any(HttpClientResponseHandler.class));
    }
}