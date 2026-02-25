package com.acc.gazua.global.security.auth.repository;

import com.acc.gazua.global.security.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken,Long> {

    @Query("DELETE FROM RefreshToken r where r.userId = :userId")
    void deleteByUserId(Long userId);

    @Query("DELETE FROM RefreshToken r where r.currentToken = :currentToken")
    void deleteByCurrentToken(String currentToken);

    Optional<RefreshToken> findByToken(String refreshToken);
}
