package com.iortatechnxt.brokerverse.prodrecon.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconCycle;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconCycleRepository;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.ReconStatus;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconItem;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconItemRepository;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reconciliation cycles (OPERATIONS_DESIGN 7, workflow {@code OPS_RECON}): one open cycle per
 * insurer and production month, opened by the first extraction or insurer upload, moved by the
 * sending, the insurer feedback and the closing, and mirrored from its work case. The cycles board
 * (PRCID.013) shows each cycle with its item counts per status bucket.
 */
@Service
@Transactional
public class ReconCycleService {

  /** Entity type of the cycles' work cases. */
  public static final String ENTITY = "ReconCycle";

  private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");
  private static final DateTimeFormatter TITLE_MONTH = DateTimeFormatter.ofPattern("MMM yyyy");
  private static final String FEEDBACK = "feedback_uploaded";

  private final ReconCycleRepository cycles;
  private final ReconItemRepository items;
  private final WorkflowService workflow;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param cycles cycles
   * @param items items
   * @param workflow workflow engine
   * @param numbers cycle numbers
   * @param audit audit trail
   * @param clock clock
   */
  public ReconCycleService(
      ReconCycleRepository cycles,
      ReconItemRepository items,
      WorkflowService workflow,
      DocumentNumberService numbers,
      AuditTrailService audit,
      Clock clock) {
    this.cycles = cycles;
    this.items = items;
    this.workflow = workflow;
    this.numbers = numbers;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * The open cycle of an insurer and month, opened with its work case when there is none.
   *
   * @param companyId company
   * @param insurerCode insurer
   * @param month any day of the production month
   * @return the cycle
   */
  public ReconCycle openOrGet(Long companyId, String insurerCode, LocalDate month) {
    LocalDate first = month.withDayOfMonth(1);
    return cycles
        .findByCompanyIdAndInsurerCodeAndProductionMonthAndClosedFalse(
            companyId, insurerCode, first)
        .orElseGet(() -> open(companyId, insurerCode, first));
  }

  private ReconCycle open(Long companyId, String insurerCode, LocalDate month) {
    String no = numbers.next("PRC-" + insurerCode + "-" + MONTH.format(month));
    ReconCycle cycle = cycles.save(new ReconCycle(companyId, no, insurerCode, month));
    workflow.start(
        new StartCase(
            companyId,
            ReconCycle.WORKFLOW,
            new CaseRecord(
                ENTITY,
                String.valueOf(cycle.getId()),
                no,
                "Production reconciliation " + insurerCode + " " + TITLE_MONTH.format(month),
                "/prodrecon/cycles/" + cycle.getId(),
                null),
            null));
    audit.record(ENTITY, no, AuditAction.CREATE, "Cycle opened for " + insurerCode);
    return cycle;
  }

  /**
   * A cycle.
   *
   * @param id cycle
   * @return cycle
   */
  @Transactional(readOnly = true)
  public ReconCycle require(Long id) {
    return cycles.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * An open cycle.
   *
   * @param id cycle
   * @return cycle
   */
  @Transactional(readOnly = true)
  public ReconCycle requireOpen(Long id) {
    ReconCycle cycle = require(id);
    if (!cycle.isOpen()) {
      throw new BusinessRuleException(
          "RECON_CYCLE_CLOSED", "Cycle " + cycle.getCycleNo() + " is closed");
    }
    return cycle;
  }

  /**
   * The cycles board (PRCID.013).
   *
   * @param companyId company
   * @param filter insurer, month and stage, any may be null
   * @param pageable page
   * @return cycles with their counts per status
   */
  @Transactional(readOnly = true)
  public Page<CycleView> board(Long companyId, BoardFilter filter, Pageable pageable) {
    Page<ReconCycle> page =
        cycles.search(
            companyId,
            filter.insurerCode(),
            filter.month() == null ? null : filter.month().withDayOfMonth(1),
            filter.stage(),
            pageable);
    Map<Long, Map<ReconStatus, Long>> counts = counts(page.map(ReconCycle::getId).getContent());
    return page.map(c -> new CycleView(c, counts.getOrDefault(c.getId(), Map.of())));
  }

  /**
   * A cycle with its counts.
   *
   * @param id cycle
   * @return view
   */
  @Transactional(readOnly = true)
  public CycleView view(Long id) {
    ReconCycle cycle = require(id);
    return new CycleView(cycle, counts(List.of(id)).getOrDefault(id, Map.of()));
  }

  private Map<Long, Map<ReconStatus, Long>> counts(List<Long> ids) {
    Map<Long, Map<ReconStatus, Long>> out = new HashMap<>();
    if (ids.isEmpty()) {
      return out;
    }
    for (Object[] row : items.countByStatus(ids)) {
      out.computeIfAbsent((Long) row[0], k -> new EnumMap<>(ReconStatus.class))
          .put((ReconStatus) row[1], (Long) row[2]);
    }
    return out;
  }

  /**
   * Closes a cycle (reconciliation handler).
   *
   * @param id cycle
   * @param comment comment, may be null
   * @return the cycle
   */
  public ReconCycle close(Long id, String comment) {
    ReconCycle cycle = requireOpen(id);
    workflow.transition(ENTITY, String.valueOf(id), "close", TransitionNote.comment(comment));
    return cycle;
  }

  /**
   * Moves a cycle to reconciling after an insurer upload (system action).
   *
   * @param cycle cycle
   */
  void feedbackUploaded(ReconCycle cycle) {
    cycle.uploaded(clock.instant());
    workflow.systemTransition(
        ENTITY,
        String.valueOf(cycle.getId()),
        FEEDBACK,
        TransitionNote.comment("Insurer production uploaded"));
  }

  /**
   * Closes a reconciling cycle once every item is matched or confirmed for closure (system).
   *
   * @param cycle cycle
   * @return true when closed now
   */
  boolean closeWhenSettled(ReconCycle cycle) {
    if (!ReconCycle.RECONCILING.equals(cycle.getStage())) {
      return false;
    }
    List<ReconItem> all = items.findByCycleIdOrderByIdAsc(cycle.getId());
    if (all.isEmpty() || !all.stream().allMatch(ReconItem::isSettled)) {
      return false;
    }
    workflow.systemTransition(
        ENTITY,
        String.valueOf(cycle.getId()),
        "for_closure",
        TransitionNote.comment("Every item is matched or confirmed for closure"));
    return true;
  }

  /**
   * Mirrors the work case stage on the cycle.
   *
   * @param event transition
   */
  @EventListener
  public void onTransition(WorkCaseTransitioned event) {
    if (!ENTITY.equals(event.entityType())) {
      return;
    }
    cycles
        .findById(Long.valueOf(event.entityId()))
        .ifPresent(c -> c.mirrorStage(event.toStage(), clock.instant()));
  }

  /**
   * Board filters.
   *
   * @param insurerCode insurer
   * @param month production month
   * @param stage stage
   */
  public record BoardFilter(String insurerCode, LocalDate month, String stage) {}

  /**
   * A cycle with its item counts.
   *
   * @param cycle cycle
   * @param counts items per status
   */
  public record CycleView(ReconCycle cycle, Map<ReconStatus, Long> counts) {

    /** Defensive copy. */
    public CycleView {
      counts = Map.copyOf(counts);
    }
  }
}
