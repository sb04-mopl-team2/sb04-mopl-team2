package com.codeit.mopl.domain.follow.service;

import com.codeit.mopl.domain.follow.dto.FollowDto;
import com.codeit.mopl.domain.follow.dto.FollowRequest;
import com.codeit.mopl.domain.follow.entity.Follow;
import com.codeit.mopl.domain.follow.mapper.FollowMapper;
import com.codeit.mopl.domain.follow.repository.FollowRepository;
import com.codeit.mopl.domain.user.entity.User;
import com.codeit.mopl.domain.user.repository.UserRepository;
import com.codeit.mopl.event.event.FollowerIncreaseEvent;
import com.codeit.mopl.exception.follow.FollowDuplicateException;
import com.codeit.mopl.exception.follow.FollowSelfProhibitedException;
import com.codeit.mopl.exception.user.ErrorCode;
import com.codeit.mopl.exception.user.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final FollowMapper followMapper;
    private final UserRepository userRepository;
    //
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public FollowDto createFollow(FollowRequest request, UUID followerId) {
        UUID followeeId = request.followeeId();
        log.info("[팔로우 관리] 팔로우 생성 시작 - followerId: {}, followeeId: {}", followerId, followeeId);

        // 자기 자신 팔로우 금지
        if (followerId.equals(followeeId)) {
            throw FollowSelfProhibitedException.withFollowerIdAndFolloweeId(followerId, followeeId);
        }

        User follower = getUserById(followerId);
        User followee = getUserById(followeeId);

        // 중복 팔로우 금지
        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            throw FollowDuplicateException.withFollowerIdAndFolloweeId(followerId, followeeId);
        }

        Follow follow = new Follow(follower, followee);
        FollowDto dto = followMapper.toDto(followRepository.save(follow));
        eventPublisher.publishEvent(new FollowerIncreaseEvent(dto));
        log.info("[팔로우 관리] 팔로우 생성 완료 - id: {}", dto.id());
        return dto;
    }

    @Transactional
    public void increaseFollowerCount(FollowDto followDto) {
        log.info("[팔로우 관리] 팔로워 증가 이벤트 처리 시작 - followDto: {}", followDto);
        User followee = getUserById(followDto.followeeId());
        UUID followeeId = followee.getId();
        Long followerCount = userRepository.findFollowerCountById(followeeId);
        userRepository.increaseFollowerCount(followeeId);
        log.info("[팔로우 관리] 팔로워 증가 이벤트 처리 완료 - followeeId: {}, oldFollowerCount: {}, newFollowerCount: {}", followeeId, followerCount, followerCount+1);
    }

    private User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, Map.of("userId", userId)));
    }

    public String getFollowNotificationTitle(UUID followerId) {
        User follower = getUserById(followerId);
        return follower.getName() + "님이 나를 팔로우했어요.";
    }
}
