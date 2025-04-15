package com.example.audit.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.example.audit.dto.AuditMessageDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class AuditConsumerService {
  private ObjectMapper objectMapper;
  private UserAuditService userAuditService;

  @KafkaListener(topics = {"${topic-to-consume-message}"})
  public void consumeMessage(String message) throws JsonMappingException, JsonProcessingException {
    AuditMessageDto userAction = objectMapper.readValue(message, AuditMessageDto.class);
    log.info("Получено сообщение {}", message);

    userAuditService.insertUserAction(userAction);
  }
}