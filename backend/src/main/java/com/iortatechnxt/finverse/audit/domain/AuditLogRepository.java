package com.iortatechnxt.finverse.audit.domain;

import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persistence and search for {@link AuditLog}. */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

  /**
   * Searches the audit trail. Null filters are ignored.
   *
   * @param username acting user filter
   * @param entityType entity type filter
   * @param entityId entity id filter
   * @param from inclusive lower bound
   * @param to exclusive upper bound
   * @param pageable paging
   * @return matching records, newest first by pageable sort
   */
  @Query(
      """
      select a from AuditLog a
      where (:username is null or lower(a.username) = lower(cast(:username as string)))
        and (:entityType is null or a.entityType = :entityType)
        and (:entityId is null or a.entityId = :entityId)
        and a.occurredAt >= :from and a.occurredAt < :to
      """)
  Page<AuditLog> search(
      @Param("username") String username,
      @Param("entityType") String entityType,
      @Param("entityId") String entityId,
      @Param("from") Instant from,
      @Param("to") Instant to,
      Pageable pageable);
}
