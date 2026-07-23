package com.curlrunner.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for tokenizing cURL command strings.
 * Splits a cURL command into tokens respecting quoted strings (single and double quotes).
 */
public class CurlTokenizer {

    private CurlTokenizer() {
        // Utility class - prevent instantiation
    }

    /**
     * Tokenizes a cURL command string into individual tokens, respecting quoted strings.
     *
     * <p>Key behaviors:
     * <ul>
     *   <li>Strings inside single quotes are treated as a single token</li>
     *   <li>Strings inside double quotes are treated as a single token</li>
     *   <li>Outer quotes are removed from the final token</li>
     *   <li>Spaces inside quotes do NOT separate tokens</li>
     *   <li>Multiple consecutive spaces outside quotes are collapsed</li>
     *   <li>Newline and tab characters are treated as whitespace separators</li>
     * </ul>
     *
     * @param input the cURL command string to tokenize
     * @return a list of tokens extracted from the input
     */
    public static List<String> tokenize(String input) {
        // Pre-process: remove backslash line continuations (\ followed by newline)
        // This handles both Unix (\n) and Windows (\r\n) line endings
        String processed = input.replaceAll("\\\\\\s*\\r?\\n\\s*", " ");
        // Also handle case where \ is followed by whitespace without newline (pasted as literal)
        // e.g., "POST \--url" where the \ was a line continuation that lost its newline
        // We detect this as a \ immediately before a - which starts a flag
        processed = processed.replaceAll("\\\\(--?)", "$1");

        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < processed.length(); i++) {
            char c = processed.charAt(i);

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
            } else if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
            } else if (c == '\\' && !inSingleQuote && !inDoubleQuote) {
                // Backslash outside quotes: check if it's a line continuation
                if (i + 1 < processed.length()) {
                    char next = processed.charAt(i + 1);
                    if (isWhitespace(next)) {
                        // Skip the backslash (line continuation that lost its newline)
                        continue;
                    }
                }
                // Standalone backslash at end of input — ignore it
                if (i == processed.length() - 1) {
                    continue;
                }
                current.append(c);
            } else if (isWhitespace(c) && !inSingleQuote && !inDoubleQuote) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            tokens.add(current.toString());
        }

        return tokens;
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }
}
