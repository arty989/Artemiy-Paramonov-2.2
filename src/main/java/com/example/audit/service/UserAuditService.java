package com.example.audit.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.example.audit.dto.AuditMessageDto;
import com.example.audit.enums.Action;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserAuditService {

    @Autowired
    private CqlSession session;

    @Value("${cassandra.keyspace}")
    private String keyspace;

    @Value("${cassandra.table}")
    private String table;

    private PreparedStatement insertAuditPreparedStmt;
    private PreparedStatement selectAuditPreparedStmt;

    @PostConstruct
    private void prepareStatements() {
        insertAuditPreparedStmt = session.prepare(
            String.format("INSERT INTO %s.%s (user_id, event_time, event_type, event_details) " +
                "VALUES (?, ?, ?, ?)", keyspace, table)
        );
        selectAuditPreparedStmt = session.prepare(
            String.format("SELECT * FROM %s.%s WHERE user_id = ? ORDER BY event_time DESC", keyspace, table)
        );
    }

  public void insertUserAction(AuditMessageDto action) {
    BoundStatement insertAuditBoundStmt = insertAuditPreparedStmt.bind(
        action.getUserId(),
        action.getEventTime(),
        action.getEventType().toString(),
        action.getEventDetails()
    );
    session.execute(insertAuditBoundStmt);
  }

  public List<AuditMessageDto> selectUserActions(Long userId) {
    BoundStatement selectAuditBoundStmt = selectAuditPreparedStmt.bind(
      userId
    );

    ResultSet resultSet = session.execute(selectAuditBoundStmt);

    return resultSet.all().stream()
      .map(this::mapRowToActionDto)
      .toList();
  }

  private AuditMessageDto mapRowToActionDto(Row row) {
    return AuditMessageDto.builder()
      .userId(row.getLong("user_id"))
      .eventTime(row.getInstant("event_time"))
      .eventType(Action.valueOf(row.getString("event_type")))
      .eventDetails(row.getString("event_details"))
      .build();
  }
}