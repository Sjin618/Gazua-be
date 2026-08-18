package com.acc.gazua.global.portone;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PortOneClient {

    @Value("${PORTONE_API_SECRET}")
    private String apiSecret;

    private final RestTemplate restTemplate;

    private final String REFUND_URL = "https://api.portone.io/payments/{paymentId}/cancel";
    private final String PAYMENT_INFO_URL = "https://api.portone.io/payments/{paymentId}";

    private HttpHeaders getHeader(){
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "PortOne " + apiSecret);

        return headers;
    }

    //portOne 결제 단건 조회
    public PortOnePaymentInfo getPaymentAmount(String paymentId){
        HttpEntity<Void> entity = new HttpEntity<>(getHeader());

        try {
            ResponseEntity<PortOnePaymentResponse> response = restTemplate.exchange(
                    PAYMENT_INFO_URL,
                    HttpMethod.GET,
                    entity,
                    PortOnePaymentResponse.class,
                    paymentId
            );
            PortOnePaymentResponse payment = response.getBody();
            if (payment == null) {
                throw new IllegalStateException("포트원에서 결제 정보를 찾을 수 없습니다.");
            }

            if ("PAID".equals(payment.status())) {
                throw new IllegalStateException("결제가 완료되지 않은 상태입니다: " + payment.status());
            }

            return new PortOnePaymentInfo(payment.amount().total(), payment.id());
        } catch (HttpClientErrorException.NotFound e) {
            throw new IllegalStateException("포트원에서 결제 정보를 찾을 수 없습니다.", e);
        } catch (HttpClientErrorException e) {
            //throw new PortOneRefundException(e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException();
        }
    }

    //환불
    public void refundPayment(String paymentId, Long amount, String reason) {
        RefundPaymentRequest refundPaymentRequest = new RefundPaymentRequest(amount, reason);

        restTemplate.exchange(
                REFUND_URL,
                HttpMethod.POST,
                new HttpEntity<>(refundPaymentRequest,getHeader()),
                RefundPaymentRequest.class,
                paymentId
        );
    }


    //id가 paymentId
    public record PortOnePaymentInfo(long paidTotal,String paymentId){}
    public record PortOnePaymentResponse(String status, String id,PaymentAmount amount){}
    public record PaymentAmount(long total) {}

    public record RefundPaymentRequest(Long amount, String reason){}
}
