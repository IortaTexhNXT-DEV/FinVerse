package com.iortatechnxt.brokerverse.acsl.service;

import com.iortatechnxt.brokerverse.acsl.domain.Correction;
import com.iortatechnxt.brokerverse.acsl.domain.CorrectionLine;
import com.iortatechnxt.brokerverse.acsl.domain.CorrectionStage;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.journal.api.dto.JournalLineRequest;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.journal.domain.JournalType;
import com.iortatechnxt.brokerverse.journal.service.SystemJournalRequest;
import com.iortatechnxt.brokerverse.journal.service.SystemJournalService;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.service.port.InvoiceCorrectionSink;
import com.iortatechnxt.brokerverse.opsledger.service.port.InvoiceCorrectionSink.CorrectionRequest;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Approval and posting of a correction entry (ACSL 2.11.0-2.15.0, 2.9.1): the approver, who is not
 * its maker, approves it and it posts at once as a system journal with the correction's lines
 * (source {@code ACSL}, reference {@code ACS:<correction>}); party lines on control accounts record
 * and match their open items ({@link CorrectionOpenItems}) and lines on an invoice component record
 * a signed {@code CORRECTION} movement on the invoice ledger through {@link InvoiceCorrectionSink}.
 *
 * <p>The journal takes the type of the journal it corrects (or of the invoice's booking journal),
 * so control accounts accept it exactly as they accepted the original (system journals are not
 * manual postings); a correction of no journal and no invoice posts as an ADJUSTMENT journal under
 * the manual-posting controls.
 */
@Service
@Transactional
public class CorrectionPosting {

  private static final int MAX_REFERENCE = 60;

  private final CorrectionService corrections;
  private final CorrectionJournals journals;
  private final SystemJournalService systemJournals;
  private final CorrectionOpenItems openItems;
  private final InvoiceCorrectionSink sink;
  private final WorkflowService workflow;
  private final AcslNotifier notifier;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param corrections corrections
   * @param journals journals (type of the corrected journal)
   * @param systemJournals system journal posting
   * @param openItems open items of the party lines
   * @param sink invoice ledger corrections (opsledger port)
   * @param workflow workflow engine
   * @param notifier notifications
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  public CorrectionPosting(
      CorrectionService corrections,
      CorrectionJournals journals,
      SystemJournalService systemJournals,
      CorrectionOpenItems openItems,
      InvoiceCorrectionSink sink,
      WorkflowService workflow,
      AcslNotifier notifier,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.corrections = corrections;
    this.journals = journals;
    this.systemJournals = systemJournals;
    this.openItems = openItems;
    this.sink = sink;
    this.workflow = workflow;
    this.notifier = notifier;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Approves and posts a correction (ACSL 2.11.0, 2.15.0).
   *
   * @param id correction
   * @param comment approver's comment (ACSL 2.11.1)
   * @return the posted correction
   */
  public Correction approve(Long id, String comment) {
    Correction c = corrections.get(id);
    Acsl.requireStage(c.getCorrectionNo(), c.getStage(), CorrectionStage.FOR_APPROVAL);
    String user = currentUser.username();
    if (Stream.of(c.getCreatedBy(), c.getSubmittedBy())
        .anyMatch(maker -> CurrentUser.sameUser(user, maker))) {
      throw new BusinessRuleException(
          Acsl.FOUR_EYES, "The approver of a correction is not its maker");
    }
    workflow.transition(
        Acsl.CORRECTION_ENTITY, String.valueOf(c.getId()), "approve", Acsl.note(comment));
    LocalDate today = LocalDate.now(clock);
    JournalBatch batch = systemJournals.post(journal(c, today));
    int items = openItems.record(c, batch, today);
    int movements = ledgerMovements(c, batch, today);
    c.posted(user, clock.instant(), batch.getBatchNo(), items, movements);
    audit.record(
        Acsl.CORRECTION_ENTITY,
        c.getCorrectionNo(),
        AuditAction.POST,
        "Approved and posted as " + batch.getBatchNo());
    notifier.user(
        c.getSubmittedBy(),
        c.getCorrectionNo() + " posted",
        "Journal " + batch.getBatchNo(),
        Acsl.correctionLink(c.getId()),
        Acsl.CORRECTION_ENTITY,
        c.getId());
    return c;
  }

  private SystemJournalRequest journal(Correction c, LocalDate today) {
    JournalType type =
        journals
            .sourceBatch(c.getCompanyId(), c.getOriginalBatchNo(), c.getInvoiceNo())
            .map(JournalBatch::getJournalType)
            .orElse(JournalType.ADJUSTMENT);
    String reference = c.getInvoiceNo() == null ? c.getCorrectionNo() : c.getInvoiceNo();
    List<JournalLineRequest> lines = new ArrayList<>();
    for (CorrectionLine l : c.getLines()) {
      lines.add(
          new JournalLineRequest(
              l.getAccountCode(),
              l.getSide(),
              l.getAmount(),
              c.getCurrency(),
              null,
              null,
              l.getCostCenter(),
              l.getBusinessLine(),
              l.getPartyCode(),
              truncate(l.getInvoiceNo() == null ? reference : l.getInvoiceNo()),
              l.getNarration()));
    }
    return new SystemJournalRequest(
        c.getCompanyId(),
        c.getBranchId(),
        type,
        today,
        c.getCurrency(),
        "ACSL correction " + c.getCorrectionNo() + ": " + truncate(c.getDescription()),
        truncate(reference),
        Acsl.MODULE,
        "ACS:" + c.getCorrectionNo(),
        lines);
  }

  private int ledgerMovements(Correction c, JournalBatch batch, LocalDate today) {
    Map<String, Map<LedgerComponent, BigDecimal>> perInvoice = new LinkedHashMap<>();
    for (CorrectionLine l : c.getLines()) {
      if (l.getInvoiceNo() != null && l.getComponent() != null) {
        LedgerComponent component = LedgerComponent.valueOf(l.getComponent());
        perInvoice
            .computeIfAbsent(l.getInvoiceNo(), k -> new EnumMap<>(LedgerComponent.class))
            .merge(
                component,
                CorrectionLineRules.ledgerChange(component, l.debitAmount()),
                BigDecimal::add);
      }
    }
    int movements = 0;
    for (Map.Entry<String, Map<LedgerComponent, BigDecimal>> e : perInvoice.entrySet()) {
      Map<LedgerComponent, BigDecimal> changes = new EnumMap<>(LedgerComponent.class);
      e.getValue().forEach((k, v) -> putNonZero(changes, k, v));
      if (!changes.isEmpty()) {
        movements +=
            sink.record(
                    new CorrectionRequest(
                        e.getKey(),
                        changes,
                        Acsl.MODULE,
                        c.getCorrectionNo(),
                        today,
                        batch.getBatchNo(),
                        truncate(c.getDescription())))
                .movements();
      }
    }
    return movements;
  }

  private static void putNonZero(
      Map<LedgerComponent, BigDecimal> changes, LedgerComponent key, BigDecimal value) {
    if (value.signum() != 0) {
      changes.put(key, value);
    }
  }

  private static String truncate(String text) {
    return text == null || text.length() <= MAX_REFERENCE ? text : text.substring(0, MAX_REFERENCE);
  }
}
