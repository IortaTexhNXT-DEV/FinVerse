package com.iortatechnxt.brokerverse.migration.cutover.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.migration.cutover.domain.CutoverPlan;
import com.iortatechnxt.brokerverse.migration.cutover.domain.CutoverPlanRepository;
import com.iortatechnxt.brokerverse.migration.cutover.domain.CutoverTask;
import com.iortatechnxt.brokerverse.migration.cutover.domain.CutoverTaskRepository;
import com.iortatechnxt.brokerverse.migration.cutover.domain.GonogoCriterion;
import com.iortatechnxt.brokerverse.migration.cutover.domain.GonogoCriterionRepository;
import com.iortatechnxt.brokerverse.migration.cutover.domain.GonogoDecision;
import com.iortatechnxt.brokerverse.migration.cutover.domain.GonogoDecisionRepository;
import com.iortatechnxt.brokerverse.migration.signoff.domain.Gate;
import com.iortatechnxt.brokerverse.migration.signoff.domain.MigSignoff;
import com.iortatechnxt.brokerverse.migration.signoff.service.GateRecorder;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The cutover plans of the migration (DATA_MIGRATION_DESIGN 17.1-17.5): mock runs, the dress
 * rehearsal and the production cut-over, each with the runbook task list (planned relative to the
 * go-live date T), the twelve go / no-go criteria - seven measured by the system, five recorded by
 * the Data Migration Lead - and the decision of the go / no-go board (gate G7 for the production
 * plan).
 */
@Service
@Transactional
public class CutoverService {

  private static final String ENTITY = "MigCutoverPlan";
  private static final String PRODUCTION_SCOPE = "ALL";

  private final CutoverPlanRepository plans;
  private final CutoverTaskRepository tasks;
  private final GonogoCriterionRepository criteria;
  private final GonogoDecisionRepository decisions;
  private final GonogoMeasures measures;
  private final GateRecorder gates;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param plans plans
   * @param tasks tasks
   * @param criteria go / no-go criteria
   * @param decisions go / no-go decisions
   * @param measures measured criteria
   * @param gates sign-offs (G7)
   * @param numbers plan numbers
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators
  public CutoverService(
      CutoverPlanRepository plans,
      CutoverTaskRepository tasks,
      GonogoCriterionRepository criteria,
      GonogoDecisionRepository decisions,
      GonogoMeasures measures,
      GateRecorder gates,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.plans = plans;
    this.tasks = tasks;
    this.criteria = criteria;
    this.decisions = decisions;
    this.measures = measures;
    this.gates = gates;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * The plans of a company.
   *
   * @param companyId company
   * @return plans by go-live date
   */
  @Transactional(readOnly = true)
  public List<CutoverPlan> plans(Long companyId) {
    return plans.findByCompanyIdOrderByGoLiveDateAscIdAsc(companyId);
  }

  /**
   * A plan with its tasks, criteria and decisions.
   *
   * @param planNo plan number
   * @return the plan
   */
  @Transactional(readOnly = true)
  public PlanView view(String planNo) {
    CutoverPlan plan = plan(planNo);
    return new PlanView(
        plan,
        tasks.findByPlanIdOrderBySeqAsc(plan.getId()),
        criteria.findByPlanIdOrderByCriterionNoAsc(plan.getId()),
        decisions.findByPlanIdOrderByIdDesc(plan.getId()));
  }

  private CutoverPlan plan(String planNo) {
    return plans
        .findByPlanNo(planNo)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, planNo));
  }

  /**
   * Creates a plan with the standard runbook and the go / no-go criteria.
   *
   * @param companyId company
   * @param data name, kind, environment, go-live date and freeze window
   * @return the plan
   */
  public CutoverPlan create(Long companyId, CutoverPlan.Data data) {
    if (data.goLiveDate() == null) {
      throw new BusinessRuleException("MIG_GOLIVE_REQUIRED", "Give the go-live date of the plan");
    }
    CutoverPlan plan =
        plans.save(
            new CutoverPlan(
                companyId, numbers.next("MCP-" + BusinessClock.today(clock).getYear()), data));
    for (CutoverTask.Data t : Runbook.tasks(data.goLiveDate())) {
      tasks.save(new CutoverTask(plan.getId(), t, currentUser.username(), clock.instant()));
    }
    for (Runbook.Criterion c : Runbook.CRITERIA) {
      criteria.save(
          new GonogoCriterion(plan.getId(), c.no(), c.name(), c.threshold(), c.measure()));
    }
    audit.record(ENTITY, plan.getPlanNo(), AuditAction.CREATE, data.kind() + " " + data.name());
    return plan;
  }

  /**
   * Records the progress of a task.
   *
   * @param planNo plan
   * @param seq task sequence
   * @param status new status
   * @param note remarks
   * @return the task
   */
  public CutoverTask progress(String planNo, int seq, CutoverTask.Status status, String note) {
    CutoverPlan plan = plan(planNo);
    List<CutoverTask> all = tasks.findByPlanIdOrderBySeqAsc(plan.getId());
    CutoverTask task =
        all.stream()
            .filter(t -> t.getSeq() == seq)
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Cutover task", planNo + "/" + seq));
    if (status == CutoverTask.Status.IN_PROGRESS || status == CutoverTask.Status.DONE) {
      requireDependencies(all, task);
    }
    if (status == CutoverTask.Status.BLOCKED && (note == null || note.isBlank())) {
      throw new BusinessRuleException("MIG_REASON_REQUIRED", "Give the reason of the block");
    }
    task.progress(
        status,
        LocalDateTime.ofInstant(clock.instant(), BusinessClock.zone()),
        note,
        currentUser.username(),
        clock.instant());
    if (plan.getStatus() == CutoverPlan.Status.PLANNED) {
      plan.mark(CutoverPlan.Status.IN_PROGRESS);
    }
    return task;
  }

  /**
   * Measures the automatic go / no-go criteria of a plan.
   *
   * @param planNo plan
   * @return the criteria
   */
  public List<GonogoCriterion> measure(String planNo) {
    CutoverPlan plan = plan(planNo);
    List<GonogoCriterion> all = criteria.findByPlanIdOrderByCriterionNoAsc(plan.getId());
    for (GonogoCriterion c : all) {
      if (!c.manual()) {
        GonogoMeasures.Result r = measures.measure(plan.getCompanyId(), c.getMeasure());
        c.measured(r.value(), r.met(), null, currentUser.username(), clock.instant());
      }
    }
    return all;
  }

  /**
   * Records a manual criterion (smoke test, rollback point, hypercare, TB signed, renewal).
   *
   * @param planNo plan
   * @param criterionNo criterion
   * @param met threshold met
   * @param note evidence
   * @return the criterion
   */
  public GonogoCriterion record(String planNo, int criterionNo, boolean met, String note) {
    CutoverPlan plan = plan(planNo);
    GonogoCriterion c =
        criteria.findByPlanIdOrderByCriterionNoAsc(plan.getId()).stream()
            .filter(x -> x.getCriterionNo() == criterionNo)
            .findFirst()
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Go / no-go criterion", String.valueOf(criterionNo)));
    if (!c.manual()) {
      throw new BusinessRuleException(
          "MIG_CRITERION_MEASURED", "Criterion " + criterionNo + " is measured by the system");
    }
    if (note == null || note.isBlank()) {
      throw new BusinessRuleException(
          "MIG_EVIDENCE_REQUIRED", "Give the evidence of the criterion");
    }
    c.measured(met ? "Met" : "Not met", met, note, currentUser.username(), clock.instant());
    return c;
  }

  /**
   * Records the decision of the go / no-go board (gate G7 of a production plan).
   *
   * @param planNo plan
   * @param go GO or NO-GO
   * @param comment comment (the reason of a NO-GO)
   * @return the decision
   */
  public GonogoDecision decide(String planNo, boolean go, String comment) {
    CutoverPlan plan = plan(planNo);
    List<GonogoCriterion> all = criteria.findByPlanIdOrderByCriterionNoAsc(plan.getId());
    int met = (int) all.stream().filter(GonogoCriterion::isMetTrue).count();
    requireComment(go, all.size() - met, comment);
    GonogoDecision d =
        decisions.save(
            new GonogoDecision(
                plan.getId(),
                go,
                comment,
                met,
                all.size(),
                currentUser.username(),
                clock.instant()));
    plan.mark(go ? CutoverPlan.Status.GO : CutoverPlan.Status.NO_GO);
    if (plan.getKind() == CutoverPlan.Kind.PRODUCTION) {
      gates.record(
          plan.getCompanyId(),
          new MigSignoff.Scope(PRODUCTION_SCOPE, null, plan.getId()),
          Gate.G7,
          new MigSignoff.Signer("MIGRATION_GONOGO", currentUser.username()),
          go,
          comment);
    }
    audit.record(
        ENTITY,
        plan.getPlanNo(),
        AuditAction.AUTHORIZE,
        (go ? "GO" : "NO-GO") + " " + met + "/" + all.size());
    return d;
  }

  private static void requireDependencies(List<CutoverTask> all, CutoverTask task) {
    for (Integer dep : task.dependencies()) {
      boolean open = all.stream().anyMatch(t -> t.getSeq() == dep && !t.finished());
      if (open) {
        throw new BusinessRuleException(
            "MIG_TASK_DEPENDENCY",
            "Task " + dep + " must be finished before task " + task.getSeq());
      }
    }
  }

  private static void requireComment(boolean go, int unmet, String comment) {
    boolean blank = comment == null || comment.isBlank();
    if (go && unmet > 0 && blank) {
      throw new BusinessRuleException(
          "MIG_GONOGO_JUSTIFY", unmet + " criteria are not met; give the justification of the GO");
    }
    if (!go && blank) {
      throw new BusinessRuleException("MIG_REASON_REQUIRED", "Give the reason of the NO-GO");
    }
  }

  /**
   * A plan with its tasks, criteria and decisions.
   *
   * @param plan plan
   * @param tasks tasks by sequence
   * @param criteria criteria by number
   * @param decisions decisions, newest first
   */
  public record PlanView(
      CutoverPlan plan,
      List<CutoverTask> tasks,
      List<GonogoCriterion> criteria,
      List<GonogoDecision> decisions) {

    /** Defensive copies. */
    public PlanView {
      tasks = List.copyOf(tasks);
      criteria = List.copyOf(criteria);
      decisions = List.copyOf(decisions);
    }
  }
}
