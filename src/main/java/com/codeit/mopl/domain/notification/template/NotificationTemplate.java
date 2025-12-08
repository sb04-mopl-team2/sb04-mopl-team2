package com.codeit.mopl.domain.notification.template;

import com.codeit.mopl.betterKorean.BetterKorean;

public enum NotificationTemplate {

  PLAYLIST_CREATED {
    @Override
    public String title(NotificationContext c) {
      return "새 플레이리스트가 추가됐어요";
    }

    @Override
    public String content(NotificationContext c) {
      String josa = BetterKorean.with(c.target()).get_을_를();
      return c.username() + "님이 새로운 플레이리스트 "
          + c.target() + josa
          + " 만들었어요!";
    }
  },

  WATCHING_SESSION_STARTED {
    @Override
    public String title(NotificationContext c) {
      return c.username() + "님이 시청 중이에요";
    }

    @Override
    public String content(NotificationContext c) {
      String josa = BetterKorean.with(c.target()).get_을_를();
      return c.username() + "님이 "
          + c.target() + josa
          + " 보고 있어요!";
    }
  },

  SUBSCRIBED_PLAYLIST_CONTENT_ADDED {
    @Override
    public String title(NotificationContext c) {
      return "구독한 플레이리스트에 새 콘텐츠가 생겼어요";
    }

    @Override
    public String content(NotificationContext c) {
      return c.playlist() + " 플레이리스트에 "
          + c.content() + " 콘텐츠가 추가되었어요!";
    }
  },

  PLAYLIST_SUBSCRIBED {
    @Override
    public String title(NotificationContext c) {
      return "플레이리스트에 새 구독자가 생겼어요";
    }

    @Override
    public String content(NotificationContext c) {
      return c.username() + "님이 "
          + c.playlist()
          + " 플레이리스트를 구독했어요!";
    }
  },

  FOLLOW_CREATED {
    @Override
    public String title(NotificationContext c) {
      return "새로운 팔로워가 생겼어요";
    }

    @Override
    public String content(NotificationContext c) {
      return c.username() + "님이 회원님을 팔로우하기 시작했어요!";
    }
  },

  DM_CREATED {
    @Override
    public String title(NotificationContext c) {
      return "[DM] " + c.username();
    }

    @Override
    public String content(NotificationContext c) {
      return c.content();
    }
  };

  public abstract String title(NotificationContext context);
  public abstract String content(NotificationContext context);
}
