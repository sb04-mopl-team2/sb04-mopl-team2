package com.codeit.mopl.domain.notification.dto;

import com.codeit.mopl.domain.notification.entity.SortDirection;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CursorResponseNotificationDto {

  @NotEmpty
  private List<NotificationDto> data;

  private String nextCursor;

  private UUID nextIdAfter;

  @NotNull
  private Boolean hasNext;

  @NotNull
  private Long totalCount;

  @NotNull
  private String sortBy;

  @NotNull
  private SortDirection sortDirection;

}