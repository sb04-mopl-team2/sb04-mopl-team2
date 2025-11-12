package com.codeit.mopl.domain.notification.dto;

import com.codeit.mopl.domain.notification.entity.Level;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {

  @NotNull
  private UUID id;

  @NotNull
  private Instant createdAt;

  @NotNull
  private UUID receiverId;

  @NotBlank
  private String title;

  @NotBlank
  private String content;

  @NotNull
  private Level level;
}
