package com.example.audit.dto;

import com.example.audit.enums.Action;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
@EqualsAndHashCode
public class ActionDto {
  @NonNull
  private UUID userId;

  @NonNull
  private Instant eventTime;

  @NonNull
  private Action eventType;

  private String eventDetails;
}
