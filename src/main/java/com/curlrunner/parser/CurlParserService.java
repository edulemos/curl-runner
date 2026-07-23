package com.curlrunner.parser;

/**
 * Service interface for parsing cURL command strings into structured ParsedRequest objects.
 */
public interface CurlParserService {
    ParsedRequest parse(String curlCommand) throws CurlParseException;
}
