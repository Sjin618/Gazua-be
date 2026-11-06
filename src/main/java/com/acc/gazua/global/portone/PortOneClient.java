package com.acc.gazua.global.portone;

import com.acc.gazua.global.portone.exception.PortOneConflictException;
import com.acc.gazua.global.portone.exception.PortOnePaymentNotFoundException;
import com.acc.gazua.global.portone.exception.PortOneRequestException;
import com.acc.gazua.global.portone.exception.PortOneUnavailableException;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
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
        headers.setContentType(MediaType.APPLICATION_JSON);

        return headers;
    }

    public PortOnePaymentSnapshot getPayment(String paymentId){
        try {
            ResponseEntity<PortOnePaymentResponse> response = restTemplate.exchange(
                    PAYMENT_INFO_URL,
                    HttpMethod.GET,
                    new HttpEntity<>(getHeader()),
                    PortOnePaymentResponse.class,
                    paymentId
            );

            PortOnePaymentResponse body = response.getBody();
            if (body == null) {
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
                    body.channel() == null ? null : body.channel().pgProvider(),
                    body.cancellations() == null ? List.of() : body.cancellations()
            );
        }catch(HttpClientErrorException.NotFound e){
            // 404 - 아직 결제가 진행이 안됨 재시도 대상이 아님
            throw new PortOnePaymentNotFoundException(paymentId);
        }catch (HttpClientErrorException e){
            // 4xx — 우리 요청이 잘못됨 (시크릿 오류 등). 재시도 무의미, 즉시 인지해야 함
            log.error("포트원 요청 오류 - paymentId={}, status={}, body={}",
                    paymentId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new PortOneRequestException("포트원 요청 오류", e);
        }catch(HttpServerErrorException | ResourceAccessException e){
            // 5xx / 타임아웃 / 커넥션 실패 — 재시도 가능
            throw new PortOneUnavailableException("포트원 응답 실패: " + paymentId, e);
        }
    }



    //환불
    public PortOneCancellation refundPayment(String paymentId, Long amount, String reason, Long currentCancellableAmount) {
        try {
            ResponseEntity<CancelPaymentResponse> response =
                    restTemplate.exchange(
                    REFUND_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(new RefundPaymentRequest(amount, reason, currentCancellableAmount), getHeader()),
                    CancelPaymentResponse.class,
                    paymentId
            );

            CancelPaymentResponse body = response.getBody();
            if (body == null || body.cancellation == null) {
                //PG 는 환불을 처리했을 수 있음 - 결과를 모르는 것으로 보고 재동기화에 맡긴다

                throw new PortOneUnavailableException("포트원 취소 응답 본문이 비어있습니다 : " + paymentId);
            }

            return body.cancellation();
        }catch(HttpClientErrorException.Conflict e){
            //이미 취소된 결제 - 멱등 처리, 이후 syncPayment가 상태를 맞춘다
            log.info("포트원 취소 거부(409) - paymentId={}, body={}", paymentId, e.getResponseBodyAsString());
            throw new PortOneConflictException("포트원 취소 거부: " + paymentId, e);
        }catch(HttpClientErrorException e){
            // 4xx — 우리 요청이 잘못됨 (시크릿 오류 등). 재시도 무의미, 즉시 인지해야 함
            log.error("포트원 취소 요청 오류 - paymentId={}, status={}, body={}",
                    paymentId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new PortOneRequestException("포트원 취소 요청 오류", e);
        }catch(HttpServerErrorException | ResourceAccessException e){
            // 5xx / 타임아웃 / 커넥션 실패 — 재시도 가능
            throw new PortOneUnavailableException("포트원 취소 응답 실패: " + paymentId, e);
        }
    }

    public record PortOnePaymentSnapshot(
            String paymentId,
            String status,
            long paidTotal,
            long refundTotal,
            Instant paidAt,
            Instant cancelledAt,
            String payMethod,
            String pgProvider,
            List<PortOneCancellation> cancellations
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
    public record PortOnePaymentResponse(
            String status,
            String id,
            PaymentAmount amount,
            Instant paidAt,
            Instant cancelledAt,
            PaymentMethod method,
            PaymentChannel channel,
            List<PortOneCancellation> cancellations){}
    public record PaymentAmount(long total, long cancelled) {}
    public record PaymentMethod(String type){}
    public record PaymentChannel(String pgProvider){}

    public record CancelPaymentResponse(PortOneCancellation cancellation){}

    //PortOne 취소 내역 - status : REQUESTED(PG 접수) / SUCCEEDED / FAILED
    public record PortOneCancellation(
            String status,
            String id, //cancellationId
            String pgCancellationId,
            long totalAmount,
            String reason,
            Instant requestedAt,
            Instant cancelledAt,
            String receiptUrl
    ){
        public boolean isSucceeded(){return "SUCCEEDED".equals(status);}
        public boolean isRequested(){return "REQUESTED".equals(status);}
        public boolean isFailed(){return "FAILED".equals(status);}
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RefundPaymentRequest(Long amount, String reason, Long currentCancellableAmount){}
}
