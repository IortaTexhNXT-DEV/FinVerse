package com.iortatechnxt.brokerverse.screening.cases.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Screening cases (SNSRP-303, 401-405). */
public interface ScreeningCaseRepository
    extends JpaRepository<ScreeningCase, Long>, JpaSpecificationExecutor<ScreeningCase> {

  /**
   * The open case of a client and type (FR-SS-034 R1).
   *
   * @param clientId client
   * @param caseType case type
   * @param status OPEN
   * @return the case
   */
  Optional<ScreeningCase> findByClientIdAndCaseTypeAndStatus(
      Long clientId, String caseType, CaseStatus status);

  /**
   * The cases of a client, newest first (client Screening tab).
   *
   * @param clientId client
   * @return cases
   */
  List<ScreeningCase> findByClientIdOrderByCreatedAtDesc(Long clientId);

  /**
   * Open cases with a due time in the given stages (SLA monitor, FR-SS-044).
   *
   * @param status OPEN
   * @param stages stages
   * @return cases
   */
  List<ScreeningCase> findByStatusAndStageInAndDueAtIsNotNullOrderByDueAtAsc(
      CaseStatus status, Collection<CaseStage> stages);

  /**
   * Open cases per assignee among the given users (LEAST_OPEN balancing).
   *
   * @param users candidate users (lower case)
   * @return rows of user and count
   */
  @Query(
      "select lower(c.assignee), count(c) from ScreeningCase c"
          + " where c.status = com.iortatechnxt.brokerverse.screening.cases.domain.CaseStatus.OPEN"
          + " and lower(c.assignee) in :users group by lower(c.assignee)")
  List<Object[]> openCountsOf(@Param("users") Collection<String> users);

  /**
   * The last time each of the given users received a new case (ROUND_ROBIN balancing).
   *
   * @param users candidate users (lower case)
   * @return rows of user and time
   */
  @Query(
      "select lower(e.toValue), max(e.occurredAt) from CaseEvent e"
          + " where e.event = com.iortatechnxt.brokerverse.screening.cases.domain.CaseEventType"
          + ".ASSIGNED and lower(e.toValue) in :users group by lower(e.toValue)")
  List<Object[]> lastAssignedOf(@Param("users") Collection<String> users);
}
