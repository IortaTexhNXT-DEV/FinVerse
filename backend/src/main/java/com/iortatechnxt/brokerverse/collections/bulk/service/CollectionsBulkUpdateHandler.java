package com.iortatechnxt.brokerverse.collections.bulk.service;

import com.iortatechnxt.brokerverse.bulk.service.BulkColumn;
import com.iortatechnxt.brokerverse.bulk.service.BulkColumn.Type;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkImportHandler;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.collections.bulk.service.WorklistUpdates.ItemUpdate;
import com.iortatechnxt.brokerverse.collections.escalation.domain.Escalation;
import com.iortatechnxt.brokerverse.collections.escalation.domain.EscalationEnums.TargetLevel;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationCandidates.Candidate;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationNotices;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationService;
import com.iortatechnxt.brokerverse.collections.escalation.service.EscalationService.ManualEscalation;
import com.iortatechnxt.brokerverse.collections.installment.service.LedgerBalances;
import com.iortatechnxt.brokerverse.collections.promise.service.PromiseService;
import com.iortatechnxt.brokerverse.collections.promise.service.PromiseService.PromiseInput;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * {@code CLX_BULK_UPDATE} (BRCLXN.051): bulk update of collection accounts by upload, one row per
 * invoice. A row can record a promise to pay, escalate the account (BRCLXN.050) and, through the
 * worklist port, set the disposition, a collection effort and remarks. Every row is validated on
 * its own (required fields, open account, list values, escalation target) and committed in its own
 * transaction; the upload number is the bulk reference of every record, for the audit.
 */
@Component
public class CollectionsBulkUpdateHandler implements BulkImportHandler {

  /** Handler code. */
  public static final String CODE = "CLX_BULK_UPDATE";

  private static final String INVOICE = "Invoice No";
  private static final String PROMISE_DATE = "Promise Date";
  private static final String PROMISE_AMOUNT = "Promise Amount";
  private static final String PROMISED_ON = "Promised On";
  private static final String ESCALATE = "Escalate";
  private static final String LEVEL = "Escalation Level";
  private static final String TARGET = "Escalate To";
  private static final String REASON = "Escalation Reason";
  private static final String DISPOSITION = "Disposition";
  private static final String EFFORT = "Effort Code";
  private static final String REMARKS = "Remarks";

  private final LedgerBalances ledger;
  private final PromiseService promises;
  private final EscalationService escalations;
  private final WorklistUpdates worklist;
  private final LovService lovs;
  private final UserDirectory users;
  private final CurrentUser currentUser;

  /**
   * Creates the handler.
   *
   * @param ledger ledger reads
   * @param promises promises
   * @param escalations escalations
   * @param worklist worklist fields (port)
   * @param lovs escalation reasons
   * @param users escalation handlers
   * @param currentUser uploader
   */
  public CollectionsBulkUpdateHandler(
      LedgerBalances ledger,
      PromiseService promises,
      EscalationService escalations,
      WorklistUpdates worklist,
      LovService lovs,
      UserDirectory users,
      CurrentUser currentUser) {
    this.ledger = ledger;
    this.promises = promises;
    this.escalations = escalations;
    this.worklist = worklist;
    this.lovs = lovs;
    this.users = users;
    this.currentUser = currentUser;
  }

  @Override
  public String code() {
    return CODE;
  }

  @Override
  public String title() {
    return "Collections Bulk Update";
  }

  @Override
  public String permission() {
    return "CLX_BULK_UPDATE";
  }

  @Override
  public List<BulkColumn> columns() {
    return List.of(
        BulkColumn.required(INVOICE, "Invoice (collection account)", "BI-MKT-2026-000001"),
        new BulkColumn(PROMISE_DATE, "Date the client promised to pay", false, Type.DATE, ""),
        new BulkColumn(
            PROMISE_AMOUNT, "Amount promised (blank = whole outstanding)", false, Type.NUMBER, ""),
        new BulkColumn(
            PROMISED_ON, "Day the promise was made (blank = today)", false, Type.DATE, ""),
        new BulkColumn(ESCALATE, "Y to escalate the account", false, Type.YES_NO, "N"),
        BulkColumn.optional(LEVEL, "TL, UH, SECTION_HEAD or USER (blank = TL)", "TL"),
        BulkColumn.optional(TARGET, "User receiving the escalation (required for USER)", ""),
        BulkColumn.optional(REASON, "Escalation reason (CLX_ESCALATION_REASON)", "NO_COMMITMENT"),
        BulkColumn.optional(DISPOSITION, "PR disposition (CLX_PR_DISPOSITION)", ""),
        BulkColumn.optional(EFFORT, "Collection effort (CLX_EFFORT_CODE)", ""),
        BulkColumn.optional(REMARKS, "Remarks", ""));
  }

  @Override
  public String instructions() {
    return "One row per invoice. Fill the promise columns to record a promise to pay, Escalate = Y"
        + " with a reason to escalate the account, and the disposition, effort or remarks to update"
        + " the worklist. Each row is checked and committed on its own.";
  }

  @Override
  public String duplicateKey(BulkRow row) {
    return row.text(INVOICE);
  }

  @Override
  public List<String> validate(BulkRow row, BulkContext context) {
    List<String> errors = invoiceErrors(row, context);
    ItemUpdate update = update(row);
    if (row.date(PROMISE_DATE) == null && !row.yes(ESCALATE) && update.isEmpty()) {
      errors.add("Fill a promise, Escalate = Y or a worklist field");
    }
    if (row.date(PROMISE_DATE) == null && row.text(PROMISE_AMOUNT) != null) {
      errors.add("A promise amount needs the promise date");
    }
    if (row.yes(ESCALATE)) {
      errors.addAll(escalationErrors(row, context));
    }
    errors.addAll(worklist.validate(context.companyId(), update));
    return errors;
  }

  private List<String> invoiceErrors(BulkRow row, BulkContext context) {
    Optional<OpsInvoice> invoice =
        ledger.find(row.text(INVOICE)).filter(i -> i.getCompanyId().equals(context.companyId()));
    List<String> errors = new ArrayList<>();
    if (invoice.isEmpty()) {
      errors.add("Invoice " + row.text(INVOICE) + " is not in the ledger");
    } else if (invoice.get().isDpFlag()
        || invoice.get().isCancelled()
        || ledger.collected(invoice.get().premiumBalance())) {
      errors.add("Invoice " + row.text(INVOICE) + " has nothing to collect");
    }
    return errors;
  }

  private List<String> escalationErrors(BulkRow row, BulkContext context) {
    List<String> errors = new ArrayList<>();
    if (!currentUser.hasAuthority("CLX_ESCALATE")) {
      errors.add("You are not allowed to escalate accounts (CLX_ESCALATE)");
    }
    TargetLevel level = level(row);
    if (level == null) {
      errors.add(LEVEL + " must be one of " + Arrays.toString(TargetLevel.values()));
    }
    String target = row.text(TARGET);
    if (level == TargetLevel.USER && target == null) {
      errors.add("Name the user in " + TARGET + " for a USER escalation");
    }
    if (target != null && !users.usersWithPermission(EscalationNotices.HANDLER).contains(target)) {
      errors.add(target + " does not handle escalations");
    }
    boolean validReason =
        lovs.activeValues(EscalationNotices.REASON_LOV, context.businessDate()).stream()
            .map(LovValue::getCode)
            .anyMatch(c -> c.equals(row.text(REASON)));
    if (!validReason) {
      errors.add(REASON + " must be a value of " + EscalationNotices.REASON_LOV);
    }
    return errors;
  }

  @Override
  public String commit(BulkRow row, BulkContext context) {
    Long companyId = context.companyId();
    String invoiceNo = row.text(INVOICE);
    List<String> done = new ArrayList<>();
    if (row.date(PROMISE_DATE) != null) {
      promises.record(
          companyId,
          invoiceNo,
          new PromiseInput(
              row.date(PROMISED_ON),
              row.date(PROMISE_DATE),
              row.number(PROMISE_AMOUNT),
              null,
              row.text(REMARKS)),
          context.jobNo());
      done.add("promise");
    }
    if (row.yes(ESCALATE)) {
      Escalation e =
          escalations.raiseManual(
              new ManualEscalation(
                  companyId,
                  List.of(invoiceNo),
                  level(row),
                  row.text(TARGET),
                  row.text(REASON),
                  row.text(REMARKS)),
              List.of(Candidate.of(ledger.requireReceivable(companyId, invoiceNo))),
              context.jobNo());
      done.add(e.getEscalationNo());
    }
    ItemUpdate update = update(row);
    if (!update.isEmpty()) {
      done.add(worklist.apply(companyId, update, context.jobNo()));
    }
    return invoiceNo + ": " + String.join(", ", done);
  }

  private static ItemUpdate update(BulkRow row) {
    return new ItemUpdate(
        row.text(INVOICE), row.text(DISPOSITION), row.text(EFFORT), worklistRemarks(row));
  }

  /** Remarks go to the worklist only when the row sets nothing else (else they annotate it). */
  private static String worklistRemarks(BulkRow row) {
    boolean other = row.date(PROMISE_DATE) != null || row.yes(ESCALATE);
    return other ? null : row.text(REMARKS);
  }

  private static TargetLevel level(BulkRow row) {
    String text = row.text(LEVEL);
    if (text == null) {
      return TargetLevel.TL;
    }
    String name = text.strip().toUpperCase(Locale.ROOT);
    return Arrays.stream(TargetLevel.values())
        .filter(l -> l.name().equals(name))
        .findFirst()
        .orElse(null);
  }
}
