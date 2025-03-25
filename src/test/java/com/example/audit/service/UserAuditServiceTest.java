package com.example.audit.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.servererrors.InvalidQueryException;
import com.example.audit.enums.Action;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.cassandra.CassandraContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

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

  @Autowired
  private UserAuditService userAuditService;

  @BeforeEach
  void clearContainer() {
    session.execute("TRUNCATE my_keyspace.user_audit");
  }

  @Test
  void testInsertAndSelectUserAction() {
    UUID userId = UUID.randomUUID();
    Action action = Action.INSERT;
    String eventDetails = "Insert first row";

    userAuditService.insertUserAction(userId, action, eventDetails);

    List<Row> actions = userAuditService.selectUserActions(userId);

    assertThat(actions).isNotNull();
    assertThat(actions).hasSize(1);
    assertThat(actions.get(0).getUuid("user_id")).isEqualTo(userId);
    assertThat(actions.get(0).getString("event_type")).isEqualTo("INSERT");
    assertThat(actions.get(0).getString("event_details")).isEqualTo("Insert first row");
  }

  @Test
  void testShouldFailWhenUserIdToInsertIsNull() {
    assertThatThrownBy(() ->
        userAuditService.insertUserAction(null, Action.INSERT, "Insert first row")
    ).isInstanceOf(InvalidQueryException.class);
  }

  @Test
  void testShouldFailWhenUserIdToSelectIsNull() {
    assertThatThrownBy(() ->
        userAuditService.insertUserAction(null, Action.INSERT, "Insert first row")
    ).isInstanceOf(InvalidQueryException.class);
  }

  @Test
  void testShouldReturnEmptyWhenUserIdToSelectDoesNotExist() {
    UUID userId = UUID.randomUUID();

    List<Row> actions = userAuditService.selectUserActions(userId);

    assertThat(actions).isNotNull();
    assertThat(actions).isEmpty();
  }
}