package org.poo.exceptions;

public class UnauthorizedCardAccessException extends Exception {
    public UnauthorizedCardAccessException(final String message) {
        super(message);
    }
}
