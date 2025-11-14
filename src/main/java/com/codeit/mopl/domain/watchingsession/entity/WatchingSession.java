package com.codeit.mopl.domain.watchingsession.entity;

import com.codeit.mopl.domain.base.DeletableEntity;
import com.codeit.mopl.domain.content.entity.Content;
import com.codeit.mopl.domain.user.entity.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "watching_sessions")
@Getter
@Setter
@NoArgsConstructor
public class WatchingSession extends DeletableEntity {

  // user has at most one active session
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false, columnDefinition = "uuid", unique = true)
  private User user;

  // many users can watch the same content
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "content_id", nullable = false, columnDefinition = "uuid")
  private Content content;
}
