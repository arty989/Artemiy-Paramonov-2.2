package com.example.audit.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.servererrors.InvalidQueryException;
import com.example.audit.dto.AuditMessageDto;
import com.example.audit.enums.Action;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.cassandra.CassandraContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class UserAuditServiceTest {

  @Container
  @ServiceConnection
  private static final CassandraContainer cassandraContainer = new CassandraContainer("cassandra:5.0.3")
      .waitingFor(Wait.forListeningPort()) ;

  @Autowired
  private CqlSession session;

  @Value("${cassandra.keyspace}")
  private String keyspace;

  @Value("${cassandra.table}")
  private String table;

  @Autowired
  private UserAuditService userAuditService;

  @BeforeEach
  void clearContainer() {
    session.execute(String.format("TRUNCATE %s.%s", keyspace, table));
  }

  @Test
  void testInsertAndSelectUserAction() {
    Long userId = 1L;
    Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

    AuditMessageDto insertAuditMessageDto = AuditMessageDto.builder()
      .userId(userId)
      .eventTime(now)
      .eventType(Action.INSERT)
      .eventDetails("Insert first row")
    .build();

    userAuditService.insertUserAction(insertAuditMessageDto);

    List<AuditMessageDto> actions = userAuditService.selectUserActions(userId);

    assertThat(actions).isNotNull();
    assertThat(actions).hasSize(1);
    assertThat(actions.get(0)).isEqualTo(insertAuditMessageDto);
  }

  @Test
  void testShouldFailWhenUserIdToSelectIsNull() {
    assertThatThrownBy(() ->
        userAuditService.selectUserActions(null)
    ).isInstanceOf(InvalidQueryException.class);
  }

  @Test
  void testShouldReturnEmptyWhenUserIdToSelectDoesNotExist() {
    Long userId = 2L;

    List<AuditMessageDto> actions = userAuditService.selectUserActions(userId);

    assertThat(actions).isNotNull();
    assertThat(actions).isEmpty();
  }
}