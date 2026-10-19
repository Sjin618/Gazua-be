package com.acc.gazua.domain.policy.service;

import com.acc.gazua.domain.accommodation.entity.Accommodation;
import com.acc.gazua.domain.accommodation.entity.AccommodationType;
import com.acc.gazua.domain.policy.entity.CancellationPolicyType;
import com.acc.gazua.domain.room.entity.Room;
import org.springframework.stereotype.Component;

@Component
public class CancellationPolicyResolver {

    public CancellationPolicyType resolve(Room room) {
        return resolve(room.getAccommodation());
    }

    public CancellationPolicyType resolve(Accommodation accommodation) {
        if (accommodation.getCancellationPolicy() != null) {
            return accommodation.getCancellationPolicy();
        }
        return defaultOf(accommodation.getType());
    }

    public CancellationPolicyType getOrDefault(CancellationPolicyType type,AccommodationType accommodationType) {
        if(type != null) return type;

        return defaultOf(accommodationType);
    }

    //숙소가 정잭을 지정하지 않았을 때의 폴백
    private CancellationPolicyType defaultOf(AccommodationType type) {
        if (type == null) {
            return CancellationPolicyType.MODERATE;
        }
        return switch(type){
            case PENSION, CAMPING, HANOK -> CancellationPolicyType.STRICT;
            case HOTEL,MOTEL -> CancellationPolicyType.MODERATE;
            case GUEST_HOUSE -> CancellationPolicyType.FLEXIBLE;
        };
    }
}
