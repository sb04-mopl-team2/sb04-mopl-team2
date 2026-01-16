package com.codeit.mopl.outbox.repository;

import com.codeit.mopl.outbox.entity.OutBoxEvent;
import com.codeit.mopl.outbox.entity.OutBoxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface OutBoxEventRepository extends JpaRepository<OutBoxEvent, UUID>,
        CustomOutBoxEventRepository {

    /**
     * 멀티 인스턴스 확장 시 SELECT ... FOR UPDATE SKIP LOCKED 적용 필요
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT e FROM OutBoxEvent e
                WHERE e.outBoxStatus = 'FAILED'
                ORDER BY e.createdAt ASC
            """)
    List<OutBoxEvent> findFailedTargets(Pageable pageable);

    List<OutBoxEvent> findByOutBoxStatusOrderByCreatedAtAsc(OutBoxStatus outBoxStatus, Pageable pageable);
}
