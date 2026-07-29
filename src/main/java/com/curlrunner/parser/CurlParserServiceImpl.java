package com.curlrunner.parser;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of CurlParserService that parses cURL command strings
 * into structured ParsedRequest objects.
 */
@Service
public class CurlParserServiceImpl implements CurlParserService {

    /**
     * Validates the raw cURL command string for shell injection characters.
     * Rejects commands containing pipes, redirections, backticks, command substitution,
     * semicolons, double ampersands, or double pipes.
     *
     * @param curlCommand the raw cURL command string to validate
     * @throws CurlParseException if shell injection characters are detected
     */
    private void validateSecurity(String curlCommand) throws CurlParseException {
        if (curlCommand.contains("||")) {
            throw new CurlParseException(
                "cURL command rejected: contains double pipe '||' (shell injection risk)");
        }
        if (curlCommand.contains("&&")) {
            throw new CurlParseException(
                "cURL command rejected: contains '&&' operator (shell injection risk)");
        }
        if (curlCommand.contains("$(")) {
            throw new CurlParseException(
                "cURL command rejected: contains command substitution '$(' (shell injection risk)");
        }
        if (curlCommand.contains("|")) {
            throw new CurlParseException(
                "cURL command rejected: contains pipe '|' character (shell injection risk)");
        }
        if (curlCommand.contains(">")) {
            throw new CurlParseException(
                "cURL command rejected: contains output redirection '>' (shell injection risk)");
        }
        if (curlCommand.contains("<")) {
            throw new CurlParseException(
                "cURL command rejected: contains input redirection '<' (shell injection risk)");
        }
        if (curlCommand.contains("`")) {
            throw new CurlParseException(
                "cURL command rejected: contains backtick '`' (shell injection risk)");
        }
        if (curlCommand.contains(";")) {
            throw new CurlParseException(
                "cURL command rejected: contains semicolon ';' (shell injection risk)");
        }
    }

    @Override
    public ParsedRequest parse(String curlCommand) throws CurlParseException {
        if (curlCommand == null || curlCommand.isBlank()) {
            throw new CurlParseException("cURL command cannot be empty");
        }

        validateSecurity(curlCommand);

        List<String> tokens = CurlTokenizer.tokenize(curlCommand);

        if (tokens.isEmpty() || !tokens.get(0).equalsIgnoreCase("curl")) {
            throw new CurlParseException("Command must start with 'curl'");
        }

        ParsedRequest request = new ParsedRequest();
        boolean explicitMethod = false;

        int i = 1; // skip "curl"
        while (i < tokens.size()) {
            String token = tokens.get(i);
            switch (token) {
                case "-X":
                case "--request":
                    i++;
                    if (i >= tokens.size()) {
                        throw new CurlParseException("Flag " + token + " requires a value");
                    }
                    request.setMethod(tokens.get(i).toUpperCase());
                    explicitMethod = true;
                    break;

                case "-H":
                case "--header":
                    i++;
                    if (i >= tokens.size()) {
                        throw new CurlParseException("Flag " + token + " requires a value");
                    }
                    String headerStr = tokens.get(i);
                    String[] parts = headerStr.split(":", 2);
                    if (parts.length == 2) {
                        request.addHeader(parts[0].trim(), parts[1].trim());
                    }
                    break;

                case "-d":
                case "--data":
                case "--data-raw":
                    i++;
                    if (i >= tokens.size()) {
                        throw new CurlParseException("Flag " + token + " requires a value");
                    }
                    request.setBody(tokens.get(i));
                    if (!explicitMethod) {
                        request.setMethod("POST");
                    }
                    break;

                case "-u":
                case "--user":
                    i++;
                    if (i >= tokens.size()) {
                        throw new CurlParseException("Flag " + token + " requires a value");
                    }
                    request.setBasicAuth(tokens.get(i));
                    break;

                case "--url":
                    i++;
                    if (i >= tokens.size()) {
                        throw new CurlParseException("Flag " + token + " requires a value");
                    }
                    request.setUrl(tokens.get(i));
                    break;

                default:
                    if (!token.startsWith("-")) {
                        request.setUrl(token);
                    }
                    break;
            }
            i++;
        }

        if (request.getUrl() == null) {
            throw new CurlParseException("URL not found in cURL command");
        }

        return request;
    }
}
