package com.codeit.mopl.domain.message.directmessage.repository;

import com.codeit.mopl.domain.message.directmessage.entity.DirectMessage;
import java.time.Instant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, UUID> {

    boolean existsByConversationIdAndReceiverIdAndIsReadFalse(UUID conversationId, UUID receiverId);

    @Query("""
    SELECT dm FROM DirectMessage dm
    WHERE dm.conversation.id = :conversationId
    ORDER BY dm.createdAt DESC, dm.id DESC
""")
    List<DirectMessage> findFirstPage(
            @Param("conversationId") UUID conversationId,
            Pageable pageable
    );

    @Query("""
       SELECT dm FROM DirectMessage dm
       WHERE dm.conversation.id = :conversationId
       AND (
           :cursor IS NULL 
           OR dm.createdAt < :cursor
           OR (dm.createdAt = :cursor 
               AND (:idAfter IS NULL OR dm.id < :idAfter)
        )
       )
       ORDER BY dm.createdAt DESC, dm.id DESC
""")
    List<DirectMessage>findMessagesBefore(
            @Param("conversationId") UUID conversationId,
            @Param("cursor") Instant cursor,
            @Param("idAfter") UUID idAfter,
            Pageable pageable
            );

    @Query("""
        SELECT dm FROM DirectMessage dm
        WHERE dm.conversation.id = :conversationId
        AND (
            :cursor IS NULL 
            OR dm.createdAt > :cursor
            OR (dm.createdAt = :cursor 
                AND (:idAfter IS NULL OR dm.id > :idAfter)
         )
        )
        ORDER BY dm.createdAt ASC, dm.id ASC
""")
    List<DirectMessage> findMessagesAfter(
            @Param("conversationId") UUID conversationId,
            @Param("cursor") Instant cursor,
            @Param("idAfter") UUID idAfter,
            Pageable pageable
            );

    long countAllByConversationId(UUID conversationId);
}
