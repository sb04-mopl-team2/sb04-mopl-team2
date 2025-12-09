package com.codeit.mopl.domain.notification.template;

import com.codeit.mopl.config.betterKorean.BetterKorean;
import com.codeit.mopl.domain.notification.template.context.DirectMessageContext;
import com.codeit.mopl.domain.notification.template.context.FollowCreatedContext;
import com.codeit.mopl.domain.notification.template.context.PlaylistContentAddedContext;
import com.codeit.mopl.domain.notification.template.context.PlaylistCreatedContext;
import com.codeit.mopl.domain.notification.template.context.PlaylistSubscribedContext;
import com.codeit.mopl.domain.notification.template.context.RoleChangedContext;
import com.codeit.mopl.domain.notification.template.context.WatchingSessionStartedContext;

public enum NotificationTemplate {

  PLAYLIST_CREATED {
    @Override
    public NotificationMessage build(PlaylistCreatedContext ctx) {
      String josa = BetterKorean.with(ctx.playlistTitle()).get_을_를();

      String title = "새 플레이리스트가 추가됐어요";
      String content = "%s님이 새로운 플레이리스트 %s%s 만들었어요!"
          .formatted(ctx.username(), ctx.playlistTitle(), josa);

      return new NotificationMessage(title, content);
    }
  },

  WATCHING_SESSION_STARTED {
    @Override
    public NotificationMessage build(WatchingSessionStartedContext ctx) {
      String josa = BetterKorean.with(ctx.contentTitle()).get_을_를();

      String title = "%s님이 시청 중이에요".formatted(ctx.username());
      String content = "%s님이 %s%s 보고 있어요!"
          .formatted(ctx.username(), ctx.contentTitle(), josa);

      return new NotificationMessage(title, content);
    }
  },

  PLAYLIST_CONTENT_ADDED {
    @Override
    public NotificationMessage build(PlaylistContentAddedContext ctx) {
      String title = "구독한 플레이리스트에 새로운 콘텐츠 추가";
      String content = "구독 중인 '%s' 플레이리스트에 '%s' 콘텐츠가 추가되었어요."
          .formatted(ctx.playlistTitle(), ctx.contentTitle());

      return new NotificationMessage(title, content);
    }
  },

  PLAYLIST_SUBSCRIBED {
    @Override
    public NotificationMessage build(PlaylistSubscribedContext ctx) {
      String title = "플레이리스트에 새로운 구독자 알림";
      String content = "%s님이 '%s' 플레이리스트를 구독했어요."
          .formatted(ctx.username(), ctx.playlistTitle());

      return new NotificationMessage(title, content);
    }
  },

  FOLLOW_CREATED {
    @Override
    public NotificationMessage build(FollowCreatedContext ctx) {
      String title = "새로운 팔로워가 생겼어요";
      String content = "%s님이 회원님을 팔로우하기 시작했어요!"
          .formatted(ctx.username());

      return new NotificationMessage(title, content);
    }
  },

  ROLE_CHANGED {
    @Override
    public NotificationMessage build(RoleChangedContext ctx) {
      String title = "내 권한이 변경되었어요.";
      String josa = BetterKorean.with(ctx.afterRole()).get_으로_로_with();

      String content = "내 권한이 '%s'에서 '%s'%s 변경되었어요."
          .formatted(ctx.beforeRole(), ctx.afterRole(), josa);

      return new NotificationMessage(title, content);
    }
  },

  DM_CREATED {
    @Override
    public NotificationMessage build(DirectMessageContext ctx) {
      String title = "[DM] " + ctx.username();
      String content = ctx.content();

      return new NotificationMessage(title, content);
    }
  };

  public NotificationMessage build(PlaylistCreatedContext ctx) {
    throw unsupported();
  }

  public NotificationMessage build(WatchingSessionStartedContext ctx) {
    throw unsupported();
  }

  public NotificationMessage build(PlaylistContentAddedContext ctx) {
    throw unsupported();
  }

  public NotificationMessage build(PlaylistSubscribedContext ctx) {
    throw unsupported();
  }

  public NotificationMessage build(FollowCreatedContext ctx) {
    throw unsupported();
  }

  public NotificationMessage build(RoleChangedContext ctx) {
    throw unsupported();
  }

  public NotificationMessage build(DirectMessageContext ctx) {
    throw unsupported();
  }

  protected UnsupportedOperationException unsupported() {
    return new UnsupportedOperationException("지원하지 않는 NotificationTemplate 호출 방식입니다.");
  }
}
