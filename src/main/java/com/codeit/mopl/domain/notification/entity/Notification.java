package com.codeit.mopl.domain.notification.entity;

import com.codeit.mopl.domain.base.DeletableEntity;
import com.codeit.mopl.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
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

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, columnDefinition = "uuid")
  private User user;

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

  public Notification(UUID id, Instant createdAt) {
    this.id = id;
    this.createdAt = createdAt;
  }
}
