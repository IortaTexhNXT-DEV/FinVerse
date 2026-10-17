package com.iortatechnxt.brokerverse.workflow.service;

import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.workflow.domain.StageCountRow;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCase;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCaseRepository;
import com.iortatechnxt.brokerverse.workflow.domain.WorkflowStage;
import com.iortatechnxt.brokerverse.workflow.domain.WorkflowStageRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "My Work": the queues of the stages the current user's team works (by the stage's owner
 * permission), with open, overdue and assigned-to-me counts (BRNB.096/115: Processing sees every
 * Marketing submission in one place; stalled items are easy to spot).
 */
@Service
@Transactional(readOnly = true)
public class WorkQueueService {

  private final WorkCaseRepository cases;
  private final WorkflowStageRepository stages;
  private final UserDirectory users;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param cases cases
   * @param stages stages
   * @param users user directory
   * @param currentUser current user
   * @param clock clock
   */
  public WorkQueueService(
      WorkCaseRepository cases,
      WorkflowStageRepository stages,
      UserDirectory users,
      CurrentUser currentUser,
      Clock clock) {
    this.cases = cases;
    this.stages = stages;
    this.users = users;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Queue stages the current user works.
   *
   * @return stages, by workflow and order
   */
  public List<WorkflowStage> myStages() {
    return stages
        .findByOwnerPermissionIsNotNullAndTerminalFalseOrderByWorkflowCodeAscSortOrderAsc()
        .stream()
        .filter(s -> currentUser.hasAuthority(s.getOwnerPermission()))
        .toList();
  }

  /**
   * Items of the queues the current user works.
   *
   * @param query filters
   * @param pageable page
   * @return cases, oldest due first
   */
  public Page<WorkCase> queue(QueueQuery query, Pageable pageable) {
    List<WorkflowStage> mine = myStages();
    Instant now = clock.instant();
    String me = currentUser.username();
    Specification<WorkCase> spec =
        (root, q, cb) -> {
          List<Predicate> where = new ArrayList<>();
          where.add(cb.equal(root.get("companyId"), query.companyId()));
          where.add(cb.isFalse(root.get("closed")));
          where.add(inStages(root, cb, mine));
          addFilters(query, root, cb, where, now, me);
          if (q != null) {
            q.orderBy(
                cb.asc(cb.coalesce(root.get("dueAt"), root.get("stageEnteredAt"))),
                cb.asc(root.get("id")));
          }
          return cb.and(where.toArray(Predicate[]::new));
        };
    return cases.findAll(spec, pageable);
  }

  private static Predicate inStages(
      Root<WorkCase> root, CriteriaBuilder cb, List<WorkflowStage> mine) {
    if (mine.isEmpty()) {
      return cb.disjunction();
    }
    return cb.or(
        mine.stream()
            .map(
                s ->
                    cb.and(
                        cb.equal(root.get("workflowCode"), s.getWorkflowCode()),
                        cb.equal(root.get("stageCode"), s.getStageCode())))
            .toArray(Predicate[]::new));
  }

  private static void addFilters(
      QueueQuery query,
      Root<WorkCase> root,
      CriteriaBuilder cb,
      List<Predicate> where,
      Instant now,
      String me) {
    if (query.workflowCode() != null) {
      where.add(cb.equal(root.get("workflowCode"), query.workflowCode()));
    }
    if (query.stageCode() != null) {
      where.add(cb.equal(root.get("stageCode"), query.stageCode()));
    }
    if (query.scope() == QueueQuery.Scope.MINE) {
      where.add(cb.equal(cb.lower(root.get("assignee")), me.toLowerCase(Locale.ROOT)));
    } else if (query.scope() == QueueQuery.Scope.UNASSIGNED) {
      where.add(cb.isNull(root.get("assignee")));
    }
    if (query.overdueOnly()) {
      where.add(cb.lessThan(root.get("dueAt"), now));
    }
    if (query.text() != null && !query.text().isBlank()) {
      String like = "%" + query.text().toLowerCase(Locale.ROOT).trim() + "%";
      where.add(
          cb.or(
              cb.like(cb.lower(root.get("reference")), like),
              cb.like(cb.lower(root.get("title")), like)));
    }
  }

  /**
   * Counts per stage for the current user's queues (My Work tiles).
   *
   * @param companyId company
   * @return counts, one per queue stage the user works (zero when empty)
   */
  public List<QueueCount> counts(Long companyId) {
    Map<String, StageCountRow> byStage = new HashMap<>();
    cases
        .openCounts(companyId, clock.instant(), currentUser.username())
        .forEach(r -> byStage.put(r.key(), r));
    return myStages().stream()
        .map(
            s -> {
              StageCountRow r = byStage.get(s.getWorkflowCode() + ":" + s.getStageCode());
              return r == null
                  ? new QueueCount(s.getWorkflowCode(), s.getStageCode(), s.getName(), 0, 0, 0)
                  : new QueueCount(
                      s.getWorkflowCode(),
                      s.getStageCode(),
                      s.getName(),
                      r.open(),
                      orZero(r.overdue()),
                      orZero(r.mine()));
            })
        .toList();
  }

  private static long orZero(Long value) {
    return value == null ? 0 : value;
  }

  /**
   * Users who may be assigned a case: holders of its stage's owner permission.
   *
   * @param stage stage
   * @return user names
   */
  public List<String> eligibleUsers(WorkflowStage stage) {
    return stage.getOwnerPermission() == null
        ? List.of()
        : users.usersWithPermission(stage.getOwnerPermission());
  }

  /**
   * Open cases past their due time (SLA alert check).
   *
   * @return cases
   */
  public List<WorkCase> overdue() {
    return cases.findByClosedFalseAndDueAtBefore(clock.instant());
  }
}
