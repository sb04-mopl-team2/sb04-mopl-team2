package com.codeit.mopl.outbox.repository;

import com.codeit.mopl.outbox.entity.OutBoxEvent;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface OutBoxEventRepository extends JpaRepository<OutBoxEvent, UUID>,
        CustomOutBoxEventRepository {

    /**
     * 멀티 인스턴스 확장 시 SELECT ... FOR UPDATE SKIP LOCKED 적용 필요
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT o FROM OutBoxEvent o
                WHERE o.outBoxStatus = 'FAILED'
                ORDER BY o.createdAt ASC
            """)
    List<OutBoxEvent> findFailedTargets(Pageable pageable);

    @Transactional
    @Modifying
    @Query(
            value = """
                    DELETE FROM outbox_events
                    WHERE id IN (
                        SELECT id
                        FROM outbox_events
                        WHERE outbox_status = 'PUBLISHED'
                        ORDER BY created_at
                        LIMIT :limitCount
                    )
                    """,
            nativeQuery = true
    )
    int deletePublishedBatch(@Param("limitCount") int limitCount);
}
