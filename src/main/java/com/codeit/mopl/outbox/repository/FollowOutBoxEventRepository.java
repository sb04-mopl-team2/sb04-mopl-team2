package com.codeit.mopl.outbox.repository;

import com.codeit.mopl.outbox.entity.FollowOutBoxEvent;
import com.codeit.mopl.outbox.entity.OutBoxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface FollowOutBoxEventRepository extends JpaRepository<FollowOutBoxEvent, UUID> {

    /**
     * 멀티 인스턴스 확장 시 SELECT ... FOR UPDATE SKIP LOCKED 적용 필요
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT e FROM FollowOutBoxEvent e
                WHERE e.outBoxStatus IN ('REQUESTED', 'FAILED')
                ORDER BY e.createdAt ASC
            """)
    List<FollowOutBoxEvent> findPublishTargets(Pageable pageable);

    List<FollowOutBoxEvent> findByOutBoxStatusOrderByCreatedAtAsc(OutBoxStatus outBoxStatus, Pageable pageable);
}
