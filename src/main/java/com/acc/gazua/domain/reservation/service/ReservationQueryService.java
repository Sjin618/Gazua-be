package com.acc.gazua.domain.reservation.service;

import com.acc.gazua.domain.reservation.dto.ReservationGetResponse;
import com.acc.gazua.domain.reservation.entity.ReservationStatus;
import com.acc.gazua.domain.reservation.exception.ReservationException;
import com.acc.gazua.domain.reservation.repository.ReservationRepository;
import com.acc.gazua.global.dto.CursorPage;
import com.acc.gazua.global.dto.CustomCursor;
import com.acc.gazua.global.dto.ErrorCode;
import com.acc.gazua.global.page.JsonCursorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
/**
 * 조회, 상세 조회에 사용되는 서비스 클래스
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReservationQueryService {
    private final ReservationRepository reservationRepository;
    private final JsonCursorUtils jsonCursorUtils;

    public CursorPage<ReservationGetResponse> getReservationList(Long userId,
                                                                 ReservationStatus status,
                                                                 String cursor,
                                                                 int size) {
        if(!status.isListable()){
            throw new ReservationException(ErrorCode.INVALID_RESERVATION_STATUS);
        }

        CustomCursor customCursor = jsonCursorUtils.deserialize(cursor);
        Long cursorId = customCursor != null ? customCursor.cursorId() : Long.MAX_VALUE;

        PageRequest pageRequest = PageRequest.of(0, size);

        Slice<ReservationGetResponse> reservationList = reservationRepository.findByReservationStatus(status, cursorId, userId, pageRequest);

        if(reservationList.isEmpty()){
            return CursorPage.empty();
        }

        List<ReservationGetResponse> list = reservationList.getContent();

        boolean hasNext = reservationList.hasNext();
        String encodedCursor = hasNext ? jsonCursorUtils.serialize(new CustomCursor(null, list.get(list.size() - 1).reservationId())) : null;

        return new CursorPage<>(list, encodedCursor, hasNext);

    }
}
