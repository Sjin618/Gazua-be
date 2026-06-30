package com.acc.gazua.domain.Payment.service;

import com.acc.gazua.domain.Payment.dto.PaymentPrepareRequest;
import com.acc.gazua.domain.Payment.dto.PaymentPrepareResponse;
import com.acc.gazua.domain.Payment.dto.PaymentVerifyRequest;
import com.acc.gazua.global.security.details.CustomUserDetails;

public interface PaymentService {

    PaymentPrepareResponse preparePayment(CustomUserDetails customUserDetails,
                                          PaymentPrepareRequest request);

    void verifyPayment(PaymentVerifyRequest request);
}
