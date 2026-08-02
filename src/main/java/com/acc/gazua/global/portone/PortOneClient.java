package com.acc.gazua.global.portone;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PortOneClient {

    @Value("${PORTONE_API_SECRET}")
    private String apiSecret;

    private final RestTemplate restTemplate;

    private HttpHeaders getHeader(){
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "PortOne " + apiSecret);

        return headers;
    }

    //portOne 결제 단건 조회
    public PortOnePaymentInfo getPaymentAmount(String paymentId){
        String url = "https://api.portone.io/payments/" + paymentId;

        HttpEntity<String> entity = new HttpEntity<>(getHeader());

        ResponseEntity<Map> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                Map.class
        );
        Map paymentData = response.getBody();
        if(paymentData != null && paymentData.get("amount") != null){
            Map<String, Object> amount = (Map<String, Object>) paymentData.get("amount");
            int paidTotal = (int) amount.get("total");
            String merchantId = (String) paymentData.get("merchantId");
            return new PortOnePaymentInfo(paidTotal, merchantId);
        }
        throw new IllegalStateException("포트원에서 결제 정보를 찾을 수 없습니다.");
    }

    //환불
    public void refundPayment(String paymentId, String reason) {
        String url = "https://api.portone.io/payments/" + paymentId + "/cancel";

        Map<String, String> body = Map.of("reason", reason);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, getHeader());

        restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                Map.class
        );
    }


    public record PortOnePaymentInfo(int paidTotal,String merchantId){}
}
