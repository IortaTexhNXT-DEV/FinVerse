package com.iortatechnxt.brokerverse.migration.archive.domain;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/** Legacy archive access log. */
public interface AccessLogRepository
    extends JpaRepository<AccessLog, Long>, JpaSpecificationExecutor<AccessLog> {

  /**
   * Records exported by a user since a time.
   *
   * @param username user
   * @param since start
   * @return exported records
   */
  @Query(
      "select coalesce(sum(a.resultCount), 0) from AccessLog a where a.username = ?1"
          + " and a.action = 'EXPORT' and a.accessedAt >= ?2")
  long exportedSince(String username, Instant since);
}
