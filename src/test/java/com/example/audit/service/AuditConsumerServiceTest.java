package com.example.audit.service;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.example.audit.dto.AuditMessageDto;
import com.example.audit.enums.Action;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@SpringBootTest(
  classes = {AuditConsumerService.class},
  properties = {
    "topic-to-consume-message=audit-topic",
    "spring.kafka.consumer.group-id=audit-test-group",
    "spring.kafka.consumer.auto-offset-reset=earliest"
  }
)
@Import({KafkaAutoConfiguration.class, AuditConsumerServiceTest.ObjectMapperTestConfig.class})
@Testcontainers
public class AuditConsumerServiceTest {

  @TestConfiguration
  static class ObjectMapperTestConfig {
    @Bean
    public ObjectMapper objectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
    }
  }

  @Container
  @ServiceConnection
  public static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

  @MockBean
  private UserAuditService userAuditService;

  @Autowired
  private KafkaTemplate<String, String> kafkaTemplate;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void shouldSendMessageToKafkaSuccessfully() throws JsonProcessingException, InterruptedException, ExecutionException {
    AuditMessageDto message = AuditMessageDto.builder()
      .userId(1L)
      .eventTime(Instant.now())
      .eventType(Action.INSERT)
      .eventDetails("Пустота")
    .build();

    String jsonMessage = objectMapper.writeValueAsString(message);

    kafkaTemplate.send("audit-topic", jsonMessage).get();
    await().atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> Mockito.verify(userAuditService, times(1)).insertUserAction(eq(message)));
  }

  @Test
  void shouldNotSendMessageToNonExistentTopic() throws JsonProcessingException, InterruptedException, ExecutionException {
    AuditMessageDto message = AuditMessageDto.builder()
      .userId(1L)
      .eventTime(Instant.now())
      .eventType(Action.INSERT)
      .eventDetails("Пустота")
    .build();

    String jsonMessage = objectMapper.writeValueAsString(message);

    kafkaTemplate.send("left-topic", jsonMessage).get();
    await().atMost(Duration.ofSeconds(5))
      .pollInterval(Duration.ofSeconds(1))
      .untilAsserted(() -> Mockito.verify(userAuditService, times(0)).insertUserAction(eq(message)));
  }
}