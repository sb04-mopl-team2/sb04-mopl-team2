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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
