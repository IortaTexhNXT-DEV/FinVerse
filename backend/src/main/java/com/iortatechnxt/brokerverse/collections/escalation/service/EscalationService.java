package com.iortatechnxt.brokerverse.collections.escalation.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.collections.escalation.domain.Escalation;
import com.iortatechnxt.brokerverse.collections.escalation.domain.Escalation.Header;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.Kind;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.Stage;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.TargetLevel;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationItem;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationRepository;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationRule;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationCandidates.Candidate;
import com.iortatechnxt.brokerverse.collections.installment.service.LedgerBalances;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.domain.WorkCase;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import com.iortatechnxt.brokerverse.workflow.service.WorkCaseTransitioned;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Escalations of collection accounts (BRCLXN.049/050/055): raised by a rule (job {@code
 * CLX_ESCALATION} or a broken promise) or by a user for the invoices of one account, routed in the
 * workflow {@code CLX_ESCALATION} to the team lead ({@code route}) or the unit / section head
 * ({@code route_to_head}), assigned to the designated user when there is one, and notified ({@code
 * CLX_ESCALATED}). The business actions escalate further, resolve and resubmit run here;
 * acknowledge and return to the handler are generic workflow actions. An escalation whose invoices
 * are collected is closed automatically ({@code auto_close}).
 */
@Service
@Transactional
public class EscalationService {

  /** Entity type in the workflow and the audit trail. */
  public static final String ENTITY = "CollectionEscalation";

  /** The workflow. */
  public static final String WORKFLOW = "CLX_ESCALATION";

  /** Stages of an open escalation. */
  public static final Set<Stage> OPEN =
      Set.copyOf(Arrays.stream(Stage.values()).filter(Stage::isOpen).toList());

  private static final Set<String> BUSINESS_ACTIONS =
      Set.of("escalate_further", "resolve", "resubmit");

  private final EscalationRepository escalations;
  private final LedgerBalances ledger;
  private final WorkflowService workflow;
  private final EscalationNotices notices;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param escalations escalations
   * @param ledger ledger reads
   * @param workflow workflow CLX_ESCALATION
   * @param notices notifications and target checks
   * @param numbers escalation numbers
   * @param audit audit trail
   * @param clock clock
   */
  public EscalationService(
      EscalationRepository escalations,
      LedgerBalances ledger,
      WorkflowService workflow,
      EscalationNotices notices,
      DocumentNumberService numbers,
      AuditTrailService audit,
      Clock clock) {
    this.escalations = escalations;
    this.ledger = ledger;
    this.workflow = workflow;
    this.notices = notices;
    this.numbers = numbers;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * An escalation with its invoices.
   *
   * @param id escalation
   * @return escalation
   */
  @Transactional(readOnly = true)
  public Escalation get(Long id) {
    return escalations
        .findWithItemsById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Escalations of a company.
   *
   * @param companyId company
   * @param stages stages (all when empty)
   * @param q number, account, client, assured or invoice
   * @param pageable page
   * @return escalations
   */
  @Transactional(readOnly = true)
  public Page<Escalation> search(
      Long companyId, Collection<Stage> stages, String q, Pageable pageable) {
    return escalations.search(
        companyId,
        stages == null || stages.isEmpty() ? List.of(Stage.values()) : stages,
        "%" + (q == null ? "" : q.strip().toLowerCase(Locale.ROOT)) + "%",
        pageable);
  }

  /**
   * Escalations of a collection account, newest first.
   *
   * @param invoiceNo invoice
   * @return escalations
   */
  @Transactional(readOnly = true)
  public List<Escalation> forInvoice(String invoiceNo) {
    List<Escalation> list = escalations.forInvoice(invoiceNo);
    list.forEach(Escalation::loadItems);
    return list;
  }

  /**
   * A manual escalation of invoices of one account (BRCLXN.050).
   *
   * @param request target, reason and remarks
   * @param invoices the account's invoices, validated
   * @param bulkRef bulk reference when several were escalated at once, may be null
   * @return the escalation, routed
   */
  public Escalation raiseManual(
      ManualEscalation request, List<Candidate> invoices, String bulkRef) {
    notices.requireTarget(request.targetLevel(), request.targetUsername());
    notices.requireReason(request.reasonCode());
    Candidate lead = invoices.get(0);
    Header header =
        new Header(
            request.companyId(),
            nextNumber(),
            Kind.MANUAL,
            null,
            lead.arn(),
            lead.clientCode(),
            lead.assuredName(),
            request.targetLevel(),
            blankToNull(request.targetUsername()),
            request.reasonCode(),
            request.remarks(),
            lead.currency(),
            notices.defaultSlaHours(request.targetLevel()),
            null,
            bulkRef);
    Escalation escalation = open(header, invoices, false);
    notices.escalated(escalation, invoices, true);
    return escalation;
  }

  /**
   * An automatic escalation of an account by a rule, unless the rule already escalated it this
   * month or its escalation is still open (idempotent key rule + invoice + month).
   *
   * @param rule the rule met
   * @param account the account
   * @param asOf business date
   * @return the escalation, or nothing when already escalated
   */
  public Optional<Escalation> raiseForRule(EscalationRule rule, Candidate account, LocalDate asOf) {
    String key = rule.getCode() + ":" + account.invoiceNo() + ":" + YearMonth.from(asOf);
    if (escalations.existsByDedupKey(key)
        || escalations.existsForRule(rule.getCode(), account.invoiceNo(), OPEN)) {
      return Optional.empty();
    }
    Header header =
        new Header(
            rule.getCompanyId(),
            nextNumber(),
            Kind.AUTO,
            rule.getCode(),
            account.arn(),
            account.clientCode(),
            account.assuredName(),
            rule.getTargetLevel(),
            rule.getTargetUsername(),
            rule.getReasonCode(),
            rule.getName(),
            account.currency(),
            rule.getSlaHours(),
            key,
            null);
    Escalation escalation = open(header, List.of(account), true);
    if (rule.isNotify()) {
      notices.escalated(escalation, List.of(account), false);
    }
    return Optional.of(escalation);
  }

  private Escalation open(Header header, List<Candidate> invoices, boolean automatic) {
    LocalDate today = LocalDate.now(clock);
    List<EscalationItem.Facts> facts =
        invoices.stream()
            .map(
                c ->
                    new EscalationItem.Facts(
                        c.invoiceNo(),
                        c.policyNo(),
                        c.balance(),
                        (int) RuleMatcher.days(c.dates().booking(), today)))
            .toList();
    Escalation escalation = escalations.save(Escalation.raise(header, facts, clock.instant()));
    String id = String.valueOf(escalation.getId());
    workflow.start(
        new StartCase(
            escalation.getCompanyId(),
            WORKFLOW,
            new CaseRecord(
                ENTITY,
                id,
                escalation.getEscalationNo(),
                escalation.getAssuredName() + " - " + escalation.getArn(),
                "/collections/escalations/" + id,
                escalation.getTargetLevel().name()),
            null));
    String action = escalation.getTargetLevel().isHead() ? "route_to_head" : "route";
    TransitionNote note = new TransitionNote(escalation.getReasonCode(), escalation.getRemarks());
    WorkCase routed =
        automatic
            ? workflow.systemTransition(ENTITY, id, action, note)
            : workflow.transition(ENTITY, id, action, note);
    if (escalation.getTargetUsername() != null) {
      routed.assignTo(escalation.getTargetUsername());
    }
    audit.record(
        ENTITY,
        escalation.getEscalationNo(),
        AuditAction.CREATE,
        (automatic ? "Escalated by rule " + escalation.getRuleCode() : "Escalated manually")
            + " to "
            + escalation.getTargetLevel()
            + (escalation.getTargetUsername() == null ? "" : " " + escalation.getTargetUsername())
            + ": "
            + facts.stream().map(EscalationItem.Facts::invoiceNo).toList());
    return escalation;
  }

  /**
   * A business action of the escalation's workflow: escalate further (with a reason), resolve (with
   * the resolution) or resubmit.
   *
   * @param id escalation
   * @param action action code
   * @param note reason and comment
   * @return the escalation
   */
  public Escalation act(Long id, String action, TransitionNote note) {
    if (!BUSINESS_ACTIONS.contains(action)) {
      throw new BusinessRuleException(
          "CLX_ESCALATION_ACTION", "'" + action + "' is not an escalation action");
    }
    if ("resolve".equals(action) && (note == null || isBlank(note.comment()))) {
      throw new BusinessRuleException(
          "CLX_RESOLUTION_REQUIRED", "Describe how the escalation was resolved");
    }
    Escalation escalation = get(id);
    workflow.transition(ENTITY, String.valueOf(id), action, note);
    return escalation;
  }

  /**
   * Closes an open escalation whose invoices are all collected (BRCLXN.049 "auto_close").
   *
   * @param id escalation
   * @return true when closed
   */
  public boolean autoCloseIfCollected(Long id) {
    Escalation escalation = get(id);
    if (!escalation.getStatus().isOpen() || escalation.getStatus() == Stage.RAISED) {
      return false;
    }
    boolean collected =
        escalation.getItems().stream()
            .allMatch(
                i -> ledger.outstanding(i.getInvoiceNo()).map(ledger::collected).orElse(true));
    if (collected) {
      workflow.systemTransition(
          ENTITY,
          String.valueOf(id),
          "auto_close",
          TransitionNote.comment("Account collected: nothing left above the threshold"));
    }
    return collected;
  }

  /**
   * Mirrors the stage of the workflow on the escalation, for every transition including the generic
   * ones run from the workflow panel.
   *
   * @param event transition
   */
  @EventListener
  public void on(WorkCaseTransitioned event) {
    if (!ENTITY.equals(event.entityType())) {
      return;
    }
    escalations
        .findById(Long.valueOf(event.entityId()))
        .ifPresent(
            e -> {
              e.markStage(Stage.valueOf(event.toStage()), event.comment(), clock.instant());
              audit.record(
                  ENTITY,
                  e.getEscalationNo(),
                  AuditAction.UPDATE,
                  event.action() + ": " + event.fromStage() + " -> " + event.toStage());
            });
  }

  private String nextNumber() {
    return numbers.next("ESC-" + LocalDate.now(clock).getYear());
  }

  private static boolean isBlank(String text) {
    return text == null || text.isBlank();
  }

  private static String blankToNull(String text) {
    return isBlank(text) ? null : text.strip();
  }

  /**
   * A manual escalation request (BRCLXN.050).
   *
   * @param companyId company
   * @param invoiceNos invoices to escalate (one case per account)
   * @param targetLevel TL, UH, SECTION_HEAD or USER
   * @param targetUsername designated user, required for USER
   * @param reasonCode reason (LOV CLX_ESCALATION_REASON)
   * @param remarks remarks
   */
  public record ManualEscalation(
      Long companyId,
      List<String> invoiceNos,
      TargetLevel targetLevel,
      String targetUsername,
      String reasonCode,
      String remarks) {

    /** Defensive copy. */
    public ManualEscalation {
      invoiceNos = invoiceNos == null ? List.of() : List.copyOf(invoiceNos);
    }
  }
}
