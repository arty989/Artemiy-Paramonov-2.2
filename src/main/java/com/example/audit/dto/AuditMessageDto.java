package com.example.audit.dto;

import com.example.audit.enums.Action;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class AuditMessageDto {
  @NonNull
  private Long userId;

  @NonNull
  private Instant eventTime;

  @NonNull
  private Action eventType;

  private String eventDetails;
}

