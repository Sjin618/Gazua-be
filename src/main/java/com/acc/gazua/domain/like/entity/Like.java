package com.acc.gazua.domain.like.entity;

import com.acc.gazua.domain.accommodation.entity.Accommodation;
import com.acc.gazua.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "likes")
public class Like {
    @Id @GeneratedValue
    @Column(name = "likes_id")
    private Long id;

    @ManyToOne
    private User user; //좋아요를 누른 사용자 ID

    @ManyToOne
    @JoinColumn(name = "accommodation_id")
    private Accommodation accommodation; //좋아요 한 숙소 ID
}
