package com.codeit.mopl.domain.notification.entity;

import com.codeit.mopl.domain.base.DeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Setter
@Entity
@Table(name = "notifications") // 테이블 이름 지정 (옵션)
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Notification extends DeletableEntity {

  // 유저의 id를 외래키로 사용하도록 추후 변경해야함
  @Column(nullable = false, columnDefinition = "uuid")
  private UUID receiverId;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Level level;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.UNREAD; // 기본값
}
