package com.iortatechnxt.brokerverse.screening.cases.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEvent;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEvent.EventFacts;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEventRepository;
import com.iortatechnxt.brokerverse.screening.cases.domain.CaseEventType;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCase;
import com.iortatechnxt.brokerverse.screening.cases.domain.ScreeningCaseRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Writes the insert-only case timeline (SNSRP-401, 404, 405, 701-704, 903): every step of a case
 * with the actor and the time. A refused submission is recorded in its own transaction so that the
 * VALIDATION_FAILED event stays although the submission is rolled back (FR-SS-060).
 */
@Component
@Transactional
public class CaseTimeline {

  private final CaseEventRepository events;
  private final ScreeningCaseRepository cases;
  private final CurrentUser currentUser;
  private final TransactionTemplate aside;
  private final Clock clock;

  /**
   * Creates the timeline writer.
   *
   * @param events events
   * @param cases cases
   * @param currentUser current user
   * @param txManager transactions
   * @param clock clock
   */
  public CaseTimeline(
      CaseEventRepository events,
      ScreeningCaseRepository cases,
      CurrentUser currentUser,
      PlatformTransactionManager txManager,
      Clock clock) {
    this.events = events;
    this.cases = cases;
    this.currentUser = currentUser;
    this.aside = new TransactionTemplate(txManager);
    this.aside.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    this.clock = clock;
  }

  /**
   * Records an event in the current transaction.
   *
   * @param c the case
   * @param type the event
   * @param facts stages, values, reason and remarks
   * @return the event
   */
  public CaseEvent record(ScreeningCase c, CaseEventType type, EventFacts facts) {
    return events.save(new CaseEvent(c, type, facts, currentUser.username(), clock.instant()));
  }

  /**
   * Records an event in a transaction of its own, kept when the caller's transaction rolls back.
   *
   * @param caseId the case
   * @param type the event
   * @param facts stages, values, reason and remarks
   */
  public void recordAside(Long caseId, CaseEventType type, EventFacts facts) {
    String actor = currentUser.username();
    aside.executeWithoutResult(
        s ->
            cases
                .findById(caseId)
                .ifPresent(
                    c -> events.save(new CaseEvent(c, type, facts, actor, clock.instant()))));
  }

  /**
   * The timeline of a case, oldest first.
   *
   * @param caseId the case
   * @return events
   */
  @Transactional(readOnly = true)
  public List<CaseEvent> of(Long caseId) {
    return events.findByCaseIdOrderByIdAsc(caseId);
  }
}
