package com.codeit.mopl.domain.follow.service;

import com.codeit.mopl.domain.follow.dto.FollowDto;
import com.codeit.mopl.domain.follow.dto.FollowRequest;
import com.codeit.mopl.domain.follow.entity.Follow;
import com.codeit.mopl.domain.follow.mapper.FollowMapper;
import com.codeit.mopl.domain.follow.repository.FollowRepository;
import com.codeit.mopl.domain.notification.entity.Level;
import com.codeit.mopl.domain.notification.service.NotificationService;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.event.entity.EventType;
import com.codeit.mopl.event.entity.ProcessedEvent;
import com.codeit.mopl.event.event.FollowerDecreaseEvent;
import com.codeit.mopl.event.event.FollowerIncreaseEvent;
import com.codeit.mopl.event.repository.ProcessedEventRepository;
import com.codeit.mopl.exception.follow.*;
import com.codeit.mopl.exception.user.UserErrorCode;
import com.codeit.mopl.exception.user.UserIdIsNullException;
import com.codeit.mopl.exception.user.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final FollowMapper followMapper;
    //
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ProcessedEventRepository processedEventRepository;

    @Transactional
    public FollowDto createFollow(FollowRequest request, UUID followerId) {
        UUID followeeId = request.followeeId();
        log.info("[팔로우 관리] 팔로우 생성 시작: followerId = {}, followeeId = {}", followerId, followeeId);

        // 자기 자신 팔로우 금지
        if (followerId.equals(followeeId)) {
            throw FollowSelfProhibitedException.withFollowerIdAndFolloweeId(followerId, followeeId);
        }

        // 중복 팔로우 금지
        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            throw FollowDuplicateException.withFollowerIdAndFolloweeId(followerId, followeeId);
        }

        User follower = getUserById(followerId);
        User followee = getUserById(followeeId);

        Follow follow = new Follow(follower, followee);
        FollowDto dto = followMapper.toDto(followRepository.save(follow));
        eventPublisher.publishEvent(new FollowerIncreaseEvent(follow.getId(), followeeId));

        // 알람 발행
        String title = getFollowNotificationTitle(follower.getName());
        notificationService.createNotification(followeeId, title, "", Level.INFO);
        log.info("[팔로우 관리] 팔로우 생성 완료: id = {}", dto.id());
        return dto;
    }

    @Transactional
    public void processFollowerIncrease(UUID followId, UUID followeeId) {
        log.info("[팔로우 관리] 팔로워 증가 이벤트 처리 시작: followId = {}, followeeId = {}", followId, followeeId);
        // 이미 처리된 이벤트면 early return
        if(checkAndMarkAsProcessed(followId, EventType.FOLLOWER_INCREASE)) {
            return;
        }
        User followee = getUserById(followeeId);
        followee.increaseFollowerCount();
        log.info("[팔로우 관리] 팔로워 증가 이벤트 처리 완료: followId = {}, followeeId = {}", followId, followeeId);
    }

    @Transactional(readOnly = true)
    public boolean isFollowedByMe(UUID followerId, UUID followeeId) {
        log.info("[팔로우 관리] 특정 유저를 내가 팔로우하는지 여부 조회 시작: followerId = {}, followeeId = {}", followerId, followeeId);
        if (!userRepository.existsById(followeeId)) {
            throw new UserNotFoundException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", followeeId));
        }
        boolean isFollowed = followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId);
        log.info("[팔로우 관리] 특정 유저를 내가 팔로우하는지 여부 조회 완료: followerId = {}, followeeId = {}, isFollowed = {}", followerId, followeeId, isFollowed);
        return isFollowed;
    }

    @Transactional(readOnly = true)
    public long getFollowerCount(UUID followeeId) {
        log.info("[팔로우 관리] 팔로워 수 조회 시작: followeeId = {}", followeeId);
        User followee = getUserById(followeeId);
        long followerCount = followee.getFollowerCount();
        log.info("[팔로우 관리] 팔로워 수 조회 완료: followeeId = {}, followerCount = {}", followeeId, followerCount);
        return followerCount;
    }

    @Transactional
    public void deleteFollow(UUID followId, UUID requesterId) {
        log.info("[팔로우 관리] 팔로우 삭제 시작: followId = {}, requesterId = {}", followId, requesterId);
        Follow follow = followRepository.findById(followId)
                .orElseThrow(() -> FollowNotFoundException.withId(followId));

        // 팔로우한 본인이 아니면 팔로우 삭제 불가능
        UUID followerId = follow.getFollower().getId();
        if (!followerId.equals(requesterId)) {
            throw FollowDeleteForbiddenException.withIds(followId, followerId, requesterId);
        }

        followRepository.delete(follow);
        UUID followeeId = follow.getFollowee().getId();
        eventPublisher.publishEvent(new FollowerDecreaseEvent(follow.getId(), followeeId));
        log.info("[팔로우 관리] 팔로우 삭제 완료: followId = {}, followeeId = {}", followId, followeeId);
    }

    @Transactional
    public void processFollowerDecrease(UUID followId, UUID followeeId) {
        log.info("[팔로우 관리] 팔로워 감소 이벤트 처리 시작: followId = {}, followeeId = {}", followId, followeeId);
        // 이미 처리된 이벤트면 early return
        if (checkAndMarkAsProcessed(followId, EventType.FOLLOWER_DECREASE)) {
            return;
        }
        User followee = getUserById(followeeId);
        long followerCount = followee.getFollowerCount();
        detectFollowerCountIsNegative(followeeId, followerCount);
        followee.decreaseFollowerCount();
        log.info("[팔로우 관리] 팔로워 감소 이벤트 처리 완료: followId = {}, followeeId = {}", followId, followeeId);
    }

    private void detectFollowerCountIsNegative(UUID followeeId, long followerCount) {
        if (followerCount <= 0) {
            log.error("[팔로우 관리] 팔로우 감소 중단 - 팔로워 수가 0이하 입니다: followeeId = {}, followerCount = {}", followeeId, followerCount);
            throw FollowerCountCannotBeNegativeException.withFolloweeIdAndFollowerCount(followeeId, followerCount);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected boolean checkAndMarkAsProcessed(UUID followId, EventType eventType) {
        if (followId == null) {
            throw FollowIdIsNullException.withDetails();
        }
        ProcessedEvent processedEvent = new ProcessedEvent(followId, eventType);
        try {
            processedEventRepository.save(processedEvent);
            return false;
        } catch (DataIntegrityViolationException e) {
            log.warn("[팔로우 관리] 이벤트 처리 중단 - 이미 처리된 이벤트입니다: eventId = {}, eventType = {}", followId, eventType);
            return true;
        }
    }

    private User getUserById(UUID userId) {
        if (userId == null) {
            throw new UserIdIsNullException(UserErrorCode.USER_ID_IS_NULL, Map.of("userId", "null"));
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(UserErrorCode.USER_NOT_FOUND, Map.of("userId", userId)));
    }

    private String getFollowNotificationTitle(String followerName) {
        return followerName + "님이 나를 팔로우했어요.";
    }
}
