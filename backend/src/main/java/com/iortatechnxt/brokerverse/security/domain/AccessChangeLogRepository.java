package com.iortatechnxt.brokerverse.security.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence of {@link AccessChangeLog} (insert and read only). */
public interface AccessChangeLogRepository extends JpaRepository<AccessChangeLog, Long> {

  /**
   * Changes of one subject, newest first.
   *
   * @param subjectType user or role
   * @param subject user name or role code
   * @return change rows
   */
  List<AccessChangeLog> findBySubjectTypeAndSubjectIgnoreCaseOrderByIdDesc(
      AccessSubjectType subjectType, String subject);

  /**
   * Changes applied for one access request.
   *
   * @param requestNo request number
   * @return change rows in order
   */
  List<AccessChangeLog> findByRequestNoOrderById(String requestNo);
}
