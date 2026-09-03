package com.acc.gazua.global.portone.exception;

public class PortOneRequestException extends RuntimeException{
    public PortOneRequestException(String message) {
        super(message);
    }

    public PortOneRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
