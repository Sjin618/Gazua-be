package com.acc.gazua.global.security.auth.repository;

import com.acc.gazua.global.security.auth.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken,Long> {

    //회전용 조회. 동시 요청이 같은 토큰을 함께 통과시키지 못하도록 락을 건다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RefreshToken r WHERE r.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    //로그아웃 등 락이 필요 없는 조회
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    //재사용이 감지되면 familyId 로 모든 refresh Token 폐기
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM RefreshToken r WHERE r.familyId = :familyId")
    void deleteByFamilyId(@Param("familyId") String familyId);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.familyId IN :familyIds")
    void deleteByFamilyIdIn(@Param("familyIds") Collection<String> familyIds);

    @Modifying
    @Query("DELETE FROM RefreshToken r where r.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    //만료된 토큰 정리
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiredAt < :threshold")
    int deleteExpiredBefore(@Param("threshold") LocalDateTime threshold);

    //살아있는 기기(체인) 목록을 오래된 순으로
    @Query("""
            SELECT r.familyId FROM RefreshToken r
            WHERE r.userId = :userId AND r.expiredAt > :now
            GROUP BY r.familyId
            ORDER BY MIN(r.createdAt) ASC
            """)
    List<String> findActiveFamilyIds(@Param("userId") Long userId,@Param("now") LocalDateTime now);

}
