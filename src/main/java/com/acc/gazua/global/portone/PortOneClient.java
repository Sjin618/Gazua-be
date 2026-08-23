package com.acc.gazua.global.portone;

import com.acc.gazua.global.portone.exception.PortOneUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
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

            if (!"PAID".equals(payment.status())) {
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

    public PortOnePaymentSnapshot getPayment(String paymentId){
        try{
            ResponseEntity<PortOnePaymentResponse> response = restTemplate.exchange(
                    PAYMENT_INFO_URL,
                    HttpMethod.GET,
                    new HttpEntity<>(getHeader()),
                    PortOnePaymentResponse.class,
                    paymentId
            );

            PortOnePaymentResponse body = response.getBody();
            if(body == null){
                throw new PortOneUnavailableException("포트원 응답 본문이 비어있습니다.");
            }

            return new PortOnePaymentSnapshot(
                    body.id(),
                    body.status(),
                    body.amount() == null ? 0L : body.amount().total(),
                    body.amount() == null ? 0L : body.amount().cancelled(),
                    body.paidAt(),
                    body.cancelledAt(),
                    body.method() == null ? null : body.method().type(),
                    body.channel() == null ? null : body.channel().pgProvider()
            );
        }catch (HttpClientErrorException e){
            // 4xx — 우리 요청이 잘못됨 (시크릿 오류 등). 재시도 무의미, 즉시 인지해야 함
            log.error("포트원 요청 오류 - paymentId={}, status={}, body={}",
                    paymentId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("포트원 요청 오류", e);
        }catch(HttpServerErrorException | ResourceAccessException e){
            // 5xx / 타임아웃 / 커넥션 실패 — 재시도 가능
            throw new PortOneUnavailableException("포트원 응답 실패: " + paymentId, e);
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

    public record PortOnePaymentSnapshot(
            String paymentId,
            String status,
            long paidTotal,
            long cancelledTotal,
            Instant paidAt,
            Instant cancelledAt,
            String payMethod,
            String pgProvider
    ){
        public boolean isPaid(){
            return "PAID".equals(status);}
        public boolean isFullyCancelled(){
            return "CANCELLED".equals(status);}
        public boolean isPartialCancelled(){
            return "PARTIAL_CANCELLED".equals(status);
        }
        public boolean isFailed(){
            return "FAILED".equals(status);}
    }

    //id가 paymentId
    public record PortOnePaymentInfo(long paidTotal,String paymentId){}
    public record PortOnePaymentResponse(
            String status,
            String id,
            PaymentAmount amount,
            Instant paidAt,
            Instant cancelledAt,
            PaymentMethod method,
            PaymentChannel channel){}
    public record PaymentAmount(long total, long cancelled) {}
    public record PaymentMethod(String type){}
    public record PaymentChannel(String pgProvider){}

    public record RefundPaymentRequest(Long amount, String reason){}
}
