package com.codeit.mopl.outbox.repository;

import com.codeit.mopl.outbox.entity.OutBoxEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface OutBoxEventRepository extends JpaRepository<OutBoxEvent, UUID>,
        CustomOutBoxEventRepository {

    @Query(value = """
                SELECT *
                FROM outbox_events
                WHERE outbox_status = 'FAILED'
                ORDER BY created_at ASC
                FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
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
