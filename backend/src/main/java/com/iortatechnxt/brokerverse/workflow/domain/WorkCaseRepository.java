package com.iortatechnxt.brokerverse.workflow.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Work cases. */
public interface WorkCaseRepository
    extends JpaRepository<WorkCase, Long>, JpaSpecificationExecutor<WorkCase> {

  /**
   * The case of a record.
   *
   * @param entityType entity type
   * @param entityId entity id
   * @return case
   */
  Optional<WorkCase> findByEntityTypeAndEntityId(String entityType, String entityId);

  /**
   * Open cases per workflow and stage of a company, with overdue and "assigned to me" counts.
   *
   * @param companyId company
   * @param now current time
   * @param me current user
   * @return one row per stage
   */
  @Query(
      "select new com.iortatechnxt.brokerverse.workflow.domain.StageCountRow("
          + " c.workflowCode, c.stageCode, count(c),"
          + " sum(case when c.dueAt < :now then 1 else 0 end),"
          + " sum(case when lower(c.assignee) = lower(:me) then 1 else 0 end))"
          + " from WorkCase c where c.companyId = :companyId and c.closed = false"
          + " group by c.workflowCode, c.stageCode")
  List<StageCountRow> openCounts(
      @Param("companyId") Long companyId, @Param("now") Instant now, @Param("me") String me);

  /**
   * Open cases past their due time.
   *
   * @param now current time
   * @return cases
   */
  List<WorkCase> findByClosedFalseAndDueAtBefore(Instant now);
}
