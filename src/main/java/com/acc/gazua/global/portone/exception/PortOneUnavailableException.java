package com.acc.gazua.global.portone.exception;

public class PortOneUnavailableException extends RuntimeException{
    public PortOneUnavailableException(String message) {
        super(message);
    }

    public PortOneUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
