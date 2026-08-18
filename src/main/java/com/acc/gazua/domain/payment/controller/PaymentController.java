package com.acc.gazua.domain.payment.controller;

import com.acc.gazua.domain.payment.dto.PaymentPrepareRequest;
import com.acc.gazua.domain.payment.dto.PaymentPrepareResponse;
import com.acc.gazua.domain.payment.dto.PaymentVerifyRequest;
import com.acc.gazua.domain.payment.service.PaymentService;
import com.acc.gazua.domain.reservation.service.ReservationFacade;
import com.acc.gazua.global.dto.ApiResponse;
import com.acc.gazua.global.security.details.CustomUserDetails;
import io.portone.sdk.server.errors.WebhookVerificationException;
import io.portone.sdk.server.webhook.WebhookVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class PaymentController {
    private final PaymentService paymentService;
    private final WebhookVerifier webhookVerifier;
    private final ObjectMapper objectMapper;

    //결제 준비
    //결제하기 클릭했을 시
    //사용자 ID, 예약하려는 숙소 정보, 결제 금액 정보
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentPrepareResponse>> preparePayment(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                                                              @RequestBody PaymentPrepareRequest request){
        return ResponseEntity.ok(ApiResponse.success(paymentService.preparePayment(customUserDetails, request),
                "결제하기 클릭 성공"));
    }

    //결제 검증
    @PostMapping("/verify")
    public ResponseEntity<Void> verifyPayment(@RequestBody PaymentVerifyRequest request){
        paymentService.verifyPayment(request);
        return ResponseEntity.ok(null);
    }

    //결제 환불

    //결제 관련 웹훅
    @PostMapping("/webhooks")
    public ResponseEntity<Void> portOneWebhooks(@RequestBody String rawBody,
                                                @RequestHeader("webhook-id") String webhookId,
                                                @RequestHeader("webhook-timestamp") String webhookTimestamp,
                                                @RequestHeader("webhook-signature") String webhookSignature){

        try{
            webhookVerifier.verify(rawBody, webhookId, webhookSignature, webhookTimestamp);
        }catch(WebhookVerificationException e){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        JsonNode jsonNode = objectMapper.readTree(rawBody);
        String type = jsonNode.get("type").asString();
        String paymentId = jsonNode.at("/data/paymentId").asString();

        switch(type){
            case "Transaction.PAID":
        }

        return ResponseEntity.ok(null);
    }

}
