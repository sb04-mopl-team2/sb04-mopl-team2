package com.codeit.mopl.sse;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SseMessage {
  private UUID eventId;      // 고유 이벤트 ID
  private UUID receiverId;   // 어떤 사용자에게 보낸 이벤트인지
  private String eventName;  // 이벤트 이름 (예: "notifications.created")
  private Object data;       // 전송한 실제 데이터 (ex: NotificationDto)
  private Instant createdAt; // 생성 시각
}
