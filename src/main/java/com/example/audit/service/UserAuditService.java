package com.example.audit.service;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.example.audit.enums.Action;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
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

  public void insertUserAction(UUID user_id, Action event_type, String event_details) {
    BoundStatement insertAuditBoundStmt = insertAuditPreparedStmt.bind(
        user_id,
        Instant.now(),
        event_type.toString(),
        event_details
    );
    session.execute(insertAuditBoundStmt);
  }

  public List<Row> selectUserActions(UUID user_id) {
    BoundStatement selectAuditBoundStmt = selectAuditPreparedStmt.bind(
        user_id
    );

    ResultSet resultSet = session.execute(selectAuditBoundStmt);

    return resultSet.all();
  }
}