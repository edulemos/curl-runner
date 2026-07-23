package com.curlrunner.parser;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CurlTokenizerTest {

    @Test
    void simpleTokenizationWithoutQuotes() {
        List<String> tokens = CurlTokenizer.tokenize("curl -X GET https://example.com");
        assertEquals(List.of("curl", "-X", "GET", "https://example.com"), tokens);
    }

    @Test
    void tokensWithSingleQuotesPreserveSpaces() {
        List<String> tokens = CurlTokenizer.tokenize("curl -H 'Content-Type: application/json'");
        assertEquals(List.of("curl", "-H", "Content-Type: application/json"), tokens);
    }

    @Test
    void tokensWithDoubleQuotesPreserveSpaces() {
        List<String> tokens = CurlTokenizer.tokenize("curl -d \"hello world\"");
        assertEquals(List.of("curl", "-d", "hello world"), tokens);
    }

    @Test
    void multipleConsecutiveSpacesCollapsed() {
        List<String> tokens = CurlTokenizer.tokenize("curl   -X   POST   https://example.com");
        assertEquals(List.of("curl", "-X", "POST", "https://example.com"), tokens);
    }

    @Test
    void newlinesAndTabsTreatedAsWhitespace() {
        List<String> tokens = CurlTokenizer.tokenize("curl\t-X\nGET\r\nhttps://example.com");
        assertEquals(List.of("curl", "-X", "GET", "https://example.com"), tokens);
    }

    @Test
    void emptyStringReturnsEmptyList() {
        List<String> tokens = CurlTokenizer.tokenize("");
        assertTrue(tokens.isEmpty());
    }
}
