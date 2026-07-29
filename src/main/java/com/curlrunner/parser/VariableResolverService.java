package com.curlrunner.parser;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {{variable}} placeholders in a string using values from a data map.
 * Used for data-driven execution where each iteration uses different values.
 */
public class VariableResolverService {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*(\\w+)\\s*}}");

    private VariableResolverService() {
    }

    /**
     * Replaces all {{key}} placeholders in the template with values from the data map.
     * If a key is not found in the data, the placeholder is left as-is.
     * Null values are replaced with the literal string "null".
     *
     * @param template the string containing {{key}} placeholders
     * @param data     the map of key-value pairs to substitute
     * @return the resolved string with placeholders replaced
     */
    public static String resolve(String template, Map<String, Object> data) {
        if (template == null || data == null || data.isEmpty()) {
            return template;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = data.get(key);
            String replacement;
            if (value == null && data.containsKey(key)) {
                replacement = "null";
            } else if (value != null) {
                replacement = value.toString();
            } else {
                // Key not in data — leave placeholder as-is
                replacement = matcher.group(0);
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Checks if the given string contains any {{variable}} placeholders.
     */
    public static boolean hasVariables(String text) {
        if (text == null) return false;
        return VARIABLE_PATTERN.matcher(text).find();
    }
}
