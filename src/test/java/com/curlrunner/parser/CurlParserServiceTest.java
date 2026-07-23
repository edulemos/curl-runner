package com.curlrunner.parser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CurlParserServiceTest {

    private final CurlParserService parser = new CurlParserServiceImpl();

    @Test
    void basicGetRequest() {
        ParsedRequest result = parser.parse("curl https://api.example.com/users");
        assertEquals("GET", result.getMethod());
        assertEquals("https://api.example.com/users", result.getUrl());
    }

    @Test
    void getWithExplicitMethod() {
        ParsedRequest result = parser.parse("curl -X GET https://api.example.com/users");
        assertEquals("GET", result.getMethod());
        assertEquals("https://api.example.com/users", result.getUrl());
    }

    @Test
    void postWithDataAutoSetsMethod() {
        ParsedRequest result = parser.parse("curl -d '{\"key\":\"value\"}' https://api.example.com/data");
        assertEquals("POST", result.getMethod());
        assertEquals("{\"key\":\"value\"}", result.getBody());
        assertEquals("https://api.example.com/data", result.getUrl());
    }

    @Test
    void postWithExplicitMethodAndData() {
        ParsedRequest result = parser.parse("curl -X POST -d '{\"key\":\"value\"}' https://api.example.com/data");
        assertEquals("POST", result.getMethod());
        assertEquals("{\"key\":\"value\"}", result.getBody());
        assertEquals("https://api.example.com/data", result.getUrl());
    }

    @Test
    void headersExtraction() {
        ParsedRequest result = parser.parse(
                "curl -H 'Content-Type: application/json' -H 'Authorization: Bearer token' https://api.example.com");
        assertEquals("application/json", result.getHeaders().get("Content-Type"));
        assertEquals("Bearer token", result.getHeaders().get("Authorization"));
        assertEquals("https://api.example.com", result.getUrl());
    }

    @Test
    void basicAuth() {
        ParsedRequest result = parser.parse("curl -u admin:password https://api.example.com");
        assertEquals("admin:password", result.getBasicAuth());
        assertEquals("https://api.example.com", result.getUrl());
    }

    @Test
    void emptyCommandThrowsCurlParseException() {
        assertThrows(CurlParseException.class, () -> parser.parse(""));
    }

    @Test
    void nullCommandThrowsCurlParseException() {
        assertThrows(CurlParseException.class, () -> parser.parse(null));
    }

    @Test
    void missingUrlThrowsCurlParseException() {
        assertThrows(CurlParseException.class, () -> parser.parse("curl -X GET"));
    }

    @Test
    void securityPipeCharacterRejected() {
        assertThrows(CurlParseException.class, () ->
                parser.parse("curl https://example.com | cat"));
    }

    @Test
    void securityBacktickRejected() {
        assertThrows(CurlParseException.class, () ->
                parser.parse("curl https://example.com/`whoami`"));
    }

    @Test
    void securitySemicolonRejected() {
        assertThrows(CurlParseException.class, () ->
                parser.parse("curl https://example.com; rm -rf /"));
    }

    @Test
    void securityCommandSubstitutionRejected() {
        assertThrows(CurlParseException.class, () ->
                parser.parse("curl https://example.com/$(whoami)"));
    }
}
