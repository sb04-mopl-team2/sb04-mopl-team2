package com.codeit.mopl.domain.message.directmessage.repository;

import com.codeit.mopl.domain.message.directmessage.entity.DirectMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {
}
