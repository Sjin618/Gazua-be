package com.acc.gazua.global.portone.exception;


public class PortOnePaymentNotFoundException extends RuntimeException{
    public PortOnePaymentNotFoundException(String paymentId){
        super("포트원에 존재하지 않는 결제 건입니다: " + paymentId);
    }
}
