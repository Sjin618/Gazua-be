package com.acc.gazua.domain.payment.service;

import com.acc.gazua.domain.payment.dto.PaymentPrepareRequest;
import com.acc.gazua.domain.payment.dto.PaymentPrepareResponse;
import com.acc.gazua.domain.payment.dto.PaymentVerifyRequest;
import com.acc.gazua.global.security.details.CustomUserDetails;

public interface PaymentService {

    PaymentPrepareResponse preparePayment(CustomUserDetails customUserDetails,
                                          PaymentPrepareRequest request);

    void verifyPayment(PaymentVerifyRequest request);
}
