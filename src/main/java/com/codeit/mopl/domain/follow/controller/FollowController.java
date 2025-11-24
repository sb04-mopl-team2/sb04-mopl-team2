package com.codeit.mopl.domain.follow.controller;

import com.codeit.mopl.domain.follow.dto.FollowDto;
import com.codeit.mopl.domain.follow.dto.FollowRequest;
import com.codeit.mopl.domain.follow.service.FollowService;
import com.codeit.mopl.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/follows")
@Slf4j
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping
    public ResponseEntity<FollowDto> follow(@Valid @RequestBody FollowRequest request,
                                            @AuthenticationPrincipal CustomUserDetails follower) {
        log.info("[팔로우 관리] 팔로우 요청 시작 - followeeId: {}", request.followeeId());
        UUID followerId = follower.getUser().id();
        FollowDto dto = followService.createFollow(request, followerId);
        log.info("[팔로우 관리] 팔로우 요청 응답 - id: {}, followeeId: {}, followerId: {}", dto.id(), dto.followeeId(), dto.followerId());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/followed-by-me")
    public ResponseEntity<Boolean> isFollowedByMe(@RequestParam("followeeId") UUID followeeId,
                                                  @AuthenticationPrincipal CustomUserDetails follower) {
        UUID followerId = follower.getUser().id();
        log.info("[팔로우 관리] 특정 유저를 내가 팔로우하는지 여부 조회 요청 시작 - followerId: {}, followeeId: {} ", followerId, followeeId);
        boolean isFollowed = followService.isFollowedByMe(followerId, followeeId);
        log.info("[팔로우 관리] 특정 유저를 내가 팔로우하는지 여부 조회 요청 응답 - followerId: {}, followeeId: {}, isFollowed: {}", followerId, followeeId, isFollowed);
        return ResponseEntity.status(HttpStatus.OK).body(isFollowed);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> getFollowerCount(@RequestParam("followeeId") UUID followeeId,
                                                 @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID requesterId = userDetails.getUser().id();
        log.info("[팔로우 관리] 팔로워 수 조회 요청 시작 - userId: {}, followeeId: {}", requesterId, followeeId);
        long followerCount = followService.getFollowerCount(followeeId);
        log.info("[팔로우 관리] 팔로워 수 조회 요청 응답 - userId: {}, followeeId: {}, followerCount: {}", requesterId, followeeId, followerCount);
        return ResponseEntity.status(HttpStatus.OK).body(followerCount);
    }

    @DeleteMapping("/{followId}")
    public ResponseEntity<Void> deleteFollow(@PathVariable("followId") UUID followId,
                                             @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID requesterId = userDetails.getUser().id();
        log.info("[팔로우 관리] 팔로우 삭제 요청 시작 - userId: {}, followId: {}", requesterId, followId);
        followService.deleteFollow(followId, requesterId);
        log.info("[팔로우 관리] 팔로우 삭제 요청 응답 - userId: {}, followId: {}", requesterId, followId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
