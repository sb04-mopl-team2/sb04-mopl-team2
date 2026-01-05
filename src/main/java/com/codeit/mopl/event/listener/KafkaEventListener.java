package com.codeit.mopl.event.listener;

import com.codeit.mopl.event.event.*;
import com.codeit.mopl.event.sender.KafkaEventSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class KafkaEventListener {

    private final KafkaEventSender sender;

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(NotificationCreateEvent event) {
        log.info("kafka NotificationCreate Event");
        String key = Optional.ofNullable(event.notificationDto().id())
                .map(Object::toString)
                .orElse(null);

        sender.send("mopl-notification-create", key, event);
    }

//    @Async("taskExecutor")
//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
//    public void on(FollowerIncreaseEvent event) {
//        String key = event.followeeId().toString();
//        sender.send("mopl-follower-increase", key, event);
//    }
//
//    @Async("taskExecutor")
//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
//    public void on(FollowerDecreaseEvent event) {
//        String key = event.followeeId().toString();
//        sender.send("mopl-follower-decrease", key, event);
//    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserRoleUpdateEvent event) {
        String key = event.userId().toString();
        sender.send("mopl-user-role-update", key, event);
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserLogInOutEvent event) {
        log.info("kafka UserLogInOut Event");
        String key = event.userId().toString();
        sender.send("mopl-user-login-out", key, event);
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(DirectMessageCreateEvent event) {
        log.info("kafka DirectMessageCreate Event");
        String key = Optional.ofNullable(event.directMessageDto().id())
                .map(Object::toString)
                .orElse(null);
        sender.send("mopl-directMessage-create", key, event);
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PlayListCreateEvent event) {
        log.info("kafka PlayListCreate Event");
        String key = Optional.ofNullable(event.playListId())
                .map(Object::toString)
                .orElse(null);
        sender.send("mopl-playList-create", key, event);
    }

    @Async("taskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(WatchingSessionCreateEvent event) {
        log.info("kafka WatchingSessionCreate Event");
        String key = Optional.ofNullable(event.watchingSessionId())
                .map(Object::toString)
                .orElse(null);
        sender.send("mopl-watchingSession-create", key, event);
    }

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(MailSendEvent event) {
        log.info("[Kafka] MailSendEvent Event");
        String key = Optional.ofNullable(event.email())
                .map(Object::toString)
                .orElse(null);
        sender.send("mopl-mail-send", key, event);
    }
}