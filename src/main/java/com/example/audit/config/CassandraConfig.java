package com.example.audit.config;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.util.Map;

@Configuration
public class CassandraConfig {
  @Value("${cassandra.host}")
    private String cassandraHost;

  @Value("${cassandra.port}")
    private int cassandraPort;

  @Value("${cassandra.keyspace}")
    private String keyspace;

  @Value("${cassandra.table}")
    private String table;

  @Value("${cassandra.datacenter}")
    private String datacenter;


  @Bean
  public CqlSession cqlSession(CqlSessionBuilder sessionBuilder) {
    InetSocketAddress address = InetSocketAddress.createUnresolved(cassandraHost, cassandraPort);
    sessionBuilder = sessionBuilder.addContactPoint(address);
    sessionBuilder.withKeyspace((CqlIdentifier) null);

    CqlSession session = sessionBuilder.build();

    SimpleStatement statement = SchemaBuilder.createKeyspace(keyspace)
        .ifNotExists()
        .withNetworkTopologyStrategy(Map.of(datacenter, 1))
        .build();
    session.execute(statement);

    session.execute(String.format("""
      CREATE TABLE IF NOT EXISTS %s.%s (
        user_id UUID,
        event_time TIMESTAMP,
        event_type TEXT,
        event_details TEXT,
        PRIMARY KEY ((user_id), event_time)
      ) WITH CLUSTERING ORDER BY (event_time DESC)
        AND default_time_to_live = 31536000;
      """, keyspace, table));

    return sessionBuilder
      .withKeyspace(keyspace)
      .build();
  }
}