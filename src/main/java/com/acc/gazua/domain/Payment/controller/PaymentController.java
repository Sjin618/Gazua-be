package com.acc.gazua.domain.Payment.controller;

import com.acc.gazua.domain.Payment.dto.PaymentPrepareRequest;
import com.acc.gazua.domain.Payment.dto.PaymentPrepareResponse;
import com.acc.gazua.domain.Payment.dto.PaymentVerifyRequest;
import com.acc.gazua.domain.Payment.service.PaymentService;
import com.acc.gazua.global.dto.ApiResponse;
import com.acc.gazua.global.security.details.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payment")
public class PaymentController {
    private final PaymentService paymentService;

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
    public ResponseEntity<Void> verifyPayment(@AuthenticationPrincipal CustomUserDetails customUserDetails,
                                              @RequestBody PaymentVerifyRequest request){
        paymentService.verifyPayment(request);
        return ResponseEntity.ok(null);
    }

    //결제 환불

}
