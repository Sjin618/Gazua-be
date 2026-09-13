package com.acc.gazua.global.exception;

import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.dto.ErrorResponse;
import com.acc.gazua.global.portone.exception.PortOnePaymentNotFoundException;
import com.acc.gazua.global.portone.exception.PortOneUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * 모든 컨트롤러에서 빠져나온 예외를 ErrorResponse 형태로 변환한다.
 *
 * 주의: JwtFilter 에서 던져지는 예외는 DispatcherServlet 바깥이라 여기로 오지 않는다.
 *      해당 경로는 JwtErrorHandler / JwtAuthenticationEntryPoint 가 담당한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /* ===================== 비즈니스 예외 ===================== */

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e, HttpServletRequest request) {
        ErrorCode errorCode = e.getErrorCode();
        logByStatus(errorCode, request, e);
        return toResponse(errorCode);
    }

    /* ===================== 외부 연동(PortOne) ===================== */

    @ExceptionHandler(PortOnePaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePortOnePaymentNotFound(PortOnePaymentNotFoundException e,
                                                                      HttpServletRequest request) {
        log.warn("PortOne 결제 건 없음 - {} {} : {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return toResponse(ErrorCode.PAYMENT_NOT_FOUND);
    }

    @ExceptionHandler(PortOneUnavailableException.class)
    public ResponseEntity<ErrorResponse> handlePortOneUnavailable(PortOneUnavailableException e,
                                                                  HttpServletRequest request) {
        log.error("PortOne 연동 실패 - {} {}", request.getMethod(), request.getRequestURI(), e);
        return toResponse(ErrorCode.EXTERNAL_API_ERROR);
    }

    /* ===================== 요청 검증 ===================== */

    // @Valid @RequestBody 검증 실패
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
                                                                      HttpServletRequest request) {
        List<ErrorResponse.FieldError> errors = ErrorResponse.FieldError.of(e.getBindingResult());
        log.warn("요청 본문 검증 실패 - {} {} : {}", request.getMethod(), request.getRequestURI(), errors);
        return toResponse(ErrorCode.INVALID_INPUT_VALUE, errors);
    }

    // @RequestParam / @PathVariable 에 붙은 제약(@Min, @Max ...) 위반
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException e,
                                                                       HttpServletRequest request) {
        List<ErrorResponse.FieldError> errors = new ArrayList<>();
        e.getParameterValidationResults().forEach(result -> {
            String field = result.getMethodParameter().getParameterName();
            String value = result.getArgument() == null ? "" : result.getArgument().toString();
            result.getResolvableErrors().forEach(error ->
                    errors.add(new ErrorResponse.FieldError(
                            field == null ? "" : field,
                            value,
                            resolveMessage(error))));
        });
        log.warn("요청 파라미터 검증 실패 - {} {} : {}", request.getMethod(), request.getRequestURI(), errors);
        return toResponse(ErrorCode.INVALID_INPUT_VALUE, errors);
    }

    // 클래스에 @Validated 를 붙인 경우 등
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e,
                                                                   HttpServletRequest request) {
        List<ErrorResponse.FieldError> errors = e.getConstraintViolations().stream()
                .map(violation -> new ErrorResponse.FieldError(
                        violation.getPropertyPath().toString(),
                        violation.getInvalidValue() == null ? "" : violation.getInvalidValue().toString(),
                        violation.getMessage()))
                .toList();
        log.warn("제약 조건 위반 - {} {} : {}", request.getMethod(), request.getRequestURI(), errors);
        return toResponse(ErrorCode.INVALID_INPUT_VALUE, errors);
    }

    // enum 파싱 실패, 숫자 자리에 문자 등
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e,
                                                            HttpServletRequest request) {
        String value = e.getValue() == null ? "" : e.getValue().toString();
        List<ErrorResponse.FieldError> errors = List.of(
                new ErrorResponse.FieldError(e.getName(), value, "허용되지 않은 값입니다."));
        log.warn("파라미터 타입 불일치 - {} {} : {}={}", request.getMethod(), request.getRequestURI(), e.getName(), value);
        return toResponse(ErrorCode.TYPE_MISMATCH, errors);
    }

    // 필수 파라미터 누락
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e,
                                                                HttpServletRequest request) {
        List<ErrorResponse.FieldError> errors = List.of(
                new ErrorResponse.FieldError(e.getParameterName(), "", "필수 파라미터입니다."));
        log.warn("필수 파라미터 누락 - {} {} : {}", request.getMethod(), request.getRequestURI(), e.getParameterName());
        return toResponse(ErrorCode.MISSING_PARAMETER, errors);
    }

    //필수 파라미터 Part 누락 (RequestPart)
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException e,
                                                           HttpServletRequest request) {
        log.warn("필수 파트 누락 - {} {} : {}", request.getMethod(), request.getRequestURI(), e.getRequestPartName());
        return toResponse(ErrorCode.MISSING_PARAMETER);
    }

    // 설정된 업로드 용량 초과
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException e,
                                                             HttpServletRequest request) {
        log.warn("업로드 용량 초과 - {} {}", request.getMethod(), request.getRequestURI());
        return toResponse(ErrorCode.FILE_TOO_LARGE);
    }

    // 본문이 없거나 JSON 형식이 잘못된 경우
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException e,
                                                                  HttpServletRequest request) {
        log.warn("요청 본문을 읽을 수 없음 - {} {} : {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        return toResponse(ErrorCode.INVALID_INPUT_VALUE);
    }

    /* ===================== HTTP 규약 ===================== */

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e,
                                                                  HttpServletRequest request) {
        log.warn("지원하지 않는 메서드 - {} {}", request.getMethod(), request.getRequestURI());
        return toResponse(ErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e,
                                                                     HttpServletRequest request) {
        log.warn("지원하지 않는 미디어 타입 - {} {} : {}", request.getMethod(), request.getRequestURI(), e.getContentType());
        return toResponse(ErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException e,
                                                               HttpServletRequest request) {
        log.warn("존재하지 않는 경로 - {} {}", request.getMethod(), request.getRequestURI());
        return toResponse(ErrorCode.NOT_FOUND);
    }

    /* ===================== 인가 / 영속성 ===================== */

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        log.warn("접근 권한 없음 - {} {}", request.getMethod(), request.getRequestURI());
        return toResponse(ErrorCode.FORBIDDEN);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e,
                                                                      HttpServletRequest request) {
        log.error("데이터 무결성 제약 위반 - {} {}", request.getMethod(), request.getRequestURI(), e);
        return toResponse(ErrorCode.DATA_INTEGRITY_VIOLATION);
    }

    /**
     * ResponseStatusException 등 스프링이 상태 코드를 들고 던지는 예외.
     * 아래 Exception 핸들러가 500으로 덮어버리지 않도록 상태 코드를 살려준다.
     */
    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ErrorResponse> handleErrorResponseException(ErrorResponseException e,
                                                                      HttpServletRequest request) {
        int status = e.getStatusCode().value();
        ErrorCode errorCode = status >= 500 ? ErrorCode.INTERNAL_SERVER_ERROR : ErrorCode.INVALID_INPUT_VALUE;
        if (status >= 500) {
            log.error("스프링 예외 - {} {}", request.getMethod(), request.getRequestURI(), e);
        } else {
            log.warn("스프링 예외 - {} {} : {}", request.getMethod(), request.getRequestURI(), e.getMessage());
        }
        return ResponseEntity.status(status).body(ErrorResponse.of(errorCode));
    }

    /* ===================== 최후의 보루 ===================== */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        //내부 메시지는 응답에 노출하지 않고 로그로만 남긴다
        log.error("처리되지 않은 예외 - {} {}", request.getMethod(), request.getRequestURI(), e);
        return toResponse(ErrorCode.INTERNAL_SERVER_ERROR);
    }

    /* ===================== 공통 ===================== */

    private ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }

    private ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode, List<ErrorResponse.FieldError> errors) {
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode, errors));
    }

    private void logByStatus(ErrorCode errorCode, HttpServletRequest request, Exception e) {
        if (errorCode.getStatus() >= 500) {
            log.error("[{}] {} {} - {}", errorCode.getCode(), request.getMethod(), request.getRequestURI(),
                    errorCode.getMessage(), e);
        } else {
            log.warn("[{}] {} {} - {}", errorCode.getCode(), request.getMethod(), request.getRequestURI(),
                    errorCode.getMessage());
        }
    }

    private String resolveMessage(MessageSourceResolvable error) {
        return error.getDefaultMessage() == null ? "올바르지 않은 값입니다." : error.getDefaultMessage();
    }
}
