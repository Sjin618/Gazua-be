package com.acc.gazua.domain.payment.controller;

import com.acc.gazua.domain.payment.service.PaymentSyncResult;
import com.acc.gazua.domain.payment.service.PaymentSyncService;
import io.portone.sdk.server.errors.WebhookVerificationException;
import io.portone.sdk.server.webhook.WebhookVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class PaymentController {
    private final PaymentSyncService paymentSyncService;
    private final WebhookVerifier webhookVerifier;
    private final ObjectMapper objectMapper;


    //결제 환불

    //결제 관련 웹훅
    @PostMapping("/webhooks")
    public ResponseEntity<Void> portOneWebhooks(@RequestBody String rawBody,
                                                @RequestHeader(value = "webhook-id", required = false) String webhookId,
                                                @RequestHeader(value = "webhook-timestamp", required = false) String webhookTimestamp,
                                                @RequestHeader(value = "webhook-signature", required = false) String webhookSignature) {
        // 헤더 누락: 스캐너/헬스체크일 수 있으므로 400 대신 401
        if (webhookId == null || webhookTimestamp == null || webhookSignature == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            webhookVerifier.verify(rawBody, webhookId, webhookSignature, webhookTimestamp);
        } catch (WebhookVerificationException e) {
            log.warn("웹훅 서명 검증 실패 - webhookId={}", webhookId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String type;
        String paymentId;
        try{
            JsonNode root = objectMapper.readTree(rawBody);
            JsonNode typeNode = root.get("type");
            JsonNode paymentIdNode = root.at("/data/paymentId");

            type = (typeNode == null || typeNode.isNull()) ? "" : typeNode.asString();
            paymentId = paymentIdNode.isMissingNode() ? null : paymentIdNode.asString();
        }catch (Exception e){
            //재시도해도 동일하게 실패하므로 200 응답
            log.error("웹훅 본문 파싱 실패 - webhookId={}, body={}", webhookId, rawBody, e);
            return ResponseEntity.ok().build();
        }

        if (paymentId == null || paymentId.isBlank()) {
            //재시도해도 동일하게 실패하므로 200 응답
            log.info("paymentId 없는 웹훅 수신 - webhookId={}, type={}", webhookId, type);
            return ResponseEntity.ok().build();
        }

        PaymentSyncResult result = paymentSyncService.syncPayment(paymentId);

        log.info("웹훅 처리 - webhookId={}, type={}, paymentId={}, result={}", webhookId, type, paymentId, result);

        //PortOne 재시도가 필요한 경우에만 5xx로 응답
        if (result == PaymentSyncResult.PORTONE_UNAVAILABLE) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        return ResponseEntity.ok().build();
    }

}
