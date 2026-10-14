package com.curlrunner.parser;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * POJO that holds the parsed cURL command components.
 * Converts to a java.net.http.HttpRequest via toHttpRequest().
 */
public class ParsedRequest {

    private String method;
    private String url;
    private Map<String, String> headers;
    private String body;
    private String basicAuth;

    public ParsedRequest() {
        this.method = "GET";
        this.headers = new LinkedHashMap<>();
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getBasicAuth() {
        return basicAuth;
    }

    public void setBasicAuth(String basicAuth) {
        this.basicAuth = basicAuth;
    }

    public void addHeader(String name, String value) {
        this.headers.put(name, value);
    }

    /**
     * Builds a java.net.http.HttpRequest from this ParsedRequest.
     *
     * @return a configured HttpRequest ready to be sent
     */
    public HttpRequest toHttpRequest() {
        return toHttpRequest(null);
    }

    /**
     * Builds a java.net.http.HttpRequest from this ParsedRequest, resolving
     * {{variable}} placeholders using the provided data map.
     *
     * @param data the data map for variable resolution (can be null for no substitution)
     * @return a configured HttpRequest ready to be sent
     */
    public HttpRequest toHttpRequest(Map<String, Object> data) {
        ResolvedRequest resolvedRequest = resolve(data);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(resolvedRequest.url()));

        // Set HTTP method with appropriate body publisher
        String upperMethod = resolvedRequest.method();
        switch (upperMethod) {
            case "GET":
            case "DELETE":
            case "HEAD":
                builder.method(upperMethod, BodyPublishers.noBody());
                break;
            case "POST":
            case "PUT":
            case "PATCH":
                if (resolvedRequest.body() != null) {
                    builder.method(upperMethod, BodyPublishers.ofString(resolvedRequest.body()));
                } else {
                    builder.method(upperMethod, BodyPublishers.noBody());
                }
                break;
            default:
                builder.method(upperMethod, BodyPublishers.noBody());
                break;
        }

        // Add all headers (with variable resolution)
        for (Map.Entry<String, String> entry : resolvedRequest.headers().entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }

        return builder.build();
    }

    /** Resolves the values used in one iteration, including data-driven variables. */
    public ResolvedRequest resolve(Map<String, Object> data) {
        Map<String, String> resolvedHeaders = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : this.headers.entrySet()) {
            resolvedHeaders.put(entry.getKey(), resolveValue(entry.getValue(), data));
        }
        if (this.basicAuth != null) {
            String resolvedAuth = resolveValue(this.basicAuth, data);
            String encoded = Base64.getEncoder().encodeToString(resolvedAuth.getBytes());
            resolvedHeaders.put("Authorization", "Basic " + encoded);
        }
        return new ResolvedRequest(
            this.method.toUpperCase(),
            resolveValue(this.url, data),
            resolvedHeaders,
            resolveValue(this.body, data)
        );
    }

    public record ResolvedRequest(String method, String url, Map<String, String> headers, String body) {
    }

    private String resolveValue(String value, Map<String, Object> data) {
        if (value == null) return null;
        if (data == null || data.isEmpty()) return value;
        return VariableResolverService.resolve(value, data);
    }
}
