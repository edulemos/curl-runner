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
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(this.url));

        // Set HTTP method with appropriate body publisher
        String upperMethod = this.method.toUpperCase();
        switch (upperMethod) {
            case "GET":
            case "DELETE":
            case "HEAD":
                builder.method(upperMethod, BodyPublishers.noBody());
                break;
            case "POST":
            case "PUT":
            case "PATCH":
                if (this.body != null) {
                    builder.method(upperMethod, BodyPublishers.ofString(this.body));
                } else {
                    builder.method(upperMethod, BodyPublishers.noBody());
                }
                break;
            default:
                builder.method(upperMethod, BodyPublishers.noBody());
                break;
        }

        // Add all headers
        for (Map.Entry<String, String> entry : this.headers.entrySet()) {
            builder.header(entry.getKey(), entry.getValue());
        }

        // Add Basic Auth header if credentials are set
        if (this.basicAuth != null) {
            String encoded = Base64.getEncoder().encodeToString(this.basicAuth.getBytes());
            builder.header("Authorization", "Basic " + encoded);
        }

        return builder.build();
    }
}
