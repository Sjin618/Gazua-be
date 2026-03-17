package com.acc.gazua.domain.like.repository;

import com.acc.gazua.domain.like.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface LikeRepository extends JpaRepository<Like,Long> {

    boolean existsByAccommodationIdAndUserId(Long accId,Long userId);

    @Modifying
    @Query("DELETE FROM Like l WHERE l.accommodation.id = :accId and l.user.id = :userId")
    void directDelete(Long accId,Long userId);
}
