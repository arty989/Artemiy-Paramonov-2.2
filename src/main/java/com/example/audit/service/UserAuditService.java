package com.example.audit.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.example.audit.dto.ActionDto;
import com.example.audit.enums.Action;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserAuditService {

  @Autowired
  private CqlSession session;

  private PreparedStatement insertAuditPreparedStmt;
  private PreparedStatement selectAuditPreparedStmt;

  @PostConstruct
  private void prepareStatements() {
    insertAuditPreparedStmt = session.prepare(
    "INSERT INTO my_keyspace.user_audit (user_id, event_time, event_type, event_details) " +
        "VALUES (?, ?, ?, ?)"
    );
    selectAuditPreparedStmt = session.prepare(
    "SELECT * FROM my_keyspace.user_audit WHERE user_id = ? ORDER BY event_time DESC");
  }

  public void insertUserAction(ActionDto action) {
    BoundStatement insertAuditBoundStmt = insertAuditPreparedStmt.bind(
        action.getUserId(),
        action.getEventTime(),
        action.getEventType().toString(),
        action.getEventDetails()
    );
    session.execute(insertAuditBoundStmt);
  }

  public List<ActionDto> selectUserActions(UUID user_id) {
    BoundStatement selectAuditBoundStmt = selectAuditPreparedStmt.bind(
      user_id
    );

    ResultSet resultSet = session.execute(selectAuditBoundStmt);

    return resultSet.all().stream()
      .map(this::mapRowToActionDto)
      .toList();
  }

  private ActionDto mapRowToActionDto(Row row) {
    return ActionDto.builder()
      .userId(row.getUuid("user_id"))
      .eventTime(row.getInstant("event_time"))
      .eventType(Action.valueOf(row.getString("event_type")))
      .eventDetails(row.getString("event_details"))
      .build();
  }
}