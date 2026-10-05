package com.acc.gazua.domain.notification.repository;

import com.acc.gazua.domain.notification.dto.NotificationGetResponse;
import com.acc.gazua.domain.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification,Long> {

    @Query("SELECT new com.acc.gazua.domain.notification.dto.NotificationGetResponse(" +
            "n.id, n.content, n.type, n.reservation.id, n.isRead, n.createdAt) " +
            "FROM Notification n " +
            "WHERE n.id < :cursorId " +
            "AND n.user.id = :userId " +
            "AND n.createdAt >= :since " +
            "ORDER BY n.id DESC")
    Slice<NotificationGetResponse> getNotifications(@Param("userId") Long userId,
                                                    @Param("since") LocalDateTime since,
                                                    @Param("cursorId") Long cursorId,
                                                    Pageable pageable);

    @Query("SELECT COUNT(n) " +
            "FROM Notification n " +
            "WHERE n.user.id = :userId " +
            "AND n.isRead = false " +
            "AND n.createdAt >= :since")
    Long getUnreadCount(@Param("userId") Long userId,
                        @Param("since") LocalDateTime since);

    @Query("SELECT n FROM Notification n WHERE n.id = :id AND n.user.id = :userId")
    Optional<Notification> findByIdAndUserId(@Param("id") Long id,
                                             @Param("userId") Long userId);


    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true " +
            "WHERE n.user.id = :userId AND n.isRead = false AND n.id <= :lastId")
    void updateReadAll(@Param("userId") Long userId,
                       @Param("lastId") Long lastId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdAt < :threshold")
    int deleteCreatedBefore(@Param("threshold") LocalDateTime threshold);
}
