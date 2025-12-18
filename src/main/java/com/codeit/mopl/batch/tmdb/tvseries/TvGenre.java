package com.codeit.mopl.batch.tmdb.tvseries;

import java.util.Arrays;
import lombok.Getter;

@Getter
public enum TvGenre {
  ACTION_ADVENTURE(10759, "액션 & 어드벤처"),
  ANIMATION(16, "애니메이션"),
  COMEDY(35, "코미디"),
  CRIME(80, "범죄"),
  DOCUMENTARY(99, "다큐멘터리"),
  DRAMA(18, "드라마"),
  FAMILY(10751, "가족"),
  KIDS(10762, "키즈"),
  MYSTERY(9648, "미스터리"),
  NEWS(10763, "뉴스"),
  REALITY(10764, "리얼리티"),
  SCI_FI_FANTASY(10765, "SF & 판타지"),
  SOAP(10766, "소프"),
  TALK(10767, "토크쇼"),
  WAR_POLITICS(10768, "전쟁 & 정치"),
  WESTERN(37, "웨스턴"),

  UNKNOWN(0, "기타"); // 매칭되지 않는 ID 대응용

  private final int id;
  private final String tag;

  TvGenre(int id, String tag) {
    this.id = id;
    this.tag = tag;
  }

  public static TvGenre fromId(int id) {
    return Arrays.stream(values())
        .filter(g -> g.id == id)
        .findFirst()
        .orElse(UNKNOWN);
  }

  @Override
  public String toString() {
    return tag;
  }
}
