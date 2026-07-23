package com.curlrunner.parser;

public class CurlParseException extends RuntimeException {

    public CurlParseException(String message) {
        super(message);
    }

    public CurlParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
