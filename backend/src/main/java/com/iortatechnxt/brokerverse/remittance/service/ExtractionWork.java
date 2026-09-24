package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.LedgerSearch;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine;
import com.iortatechnxt.brokerverse.remittance.domain.EarlyIncentiveRule;
import com.iortatechnxt.brokerverse.remittance.domain.ExtractionRun;
import com.iortatechnxt.brokerverse.remittance.domain.InvoiceTag;
import com.iortatechnxt.brokerverse.remittance.domain.InvoiceTagRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatchRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.ExtractionTag;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RemittanceType;
import com.iortatechnxt.brokerverse.remittance.service.LedgerPositions.HoldingPeriod;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceRules.Decision;
import com.iortatechnxt.brokerverse.remittance.service.RemittanceRules.Facts;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The work of one extraction run inside the caller's transaction (RMTID.001/003/004/006/007/
 * 014/017/020/022/023/031): examines the paid invoices of the scope, tags each one, and groups the
 * extracted invoices by insurer, remittance type and currency into new batches that enter Process
 * Remittance at REVIEW_IN_PROCESS (RMTID.009). Extracted invoices move to remittance status
 * REVIEW_IN_PROCESS and are locked by remittance (RMTID.040).
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class ExtractionWork {

  /** Entity type of batches in the workflow. */
  public static final String BATCH_ENTITY = "RemittanceBatch";

  /** Workflow of batches. */
  public static final String WORKFLOW = "OPS_REMITTANCE";

  private static final String OVER_DTIP_ALERT = "REMIT_PAIDAR_OVER_DTIP";
  private static final String PHP = "PHP";
  private static final int PAGE = 200;
  private static final Set<RemittanceStatus> ELIGIBLE =
      Set.of(
          RemittanceStatus.UNPROCESSED,
          RemittanceStatus.WITH_OUTSTANDING_BALANCE,
          RemittanceStatus.PARTIALLY_REMITTED);

  private final InvoiceLedgerQueryService ledger;
  private final BatchLedger batchLedger;
  private final LedgerPositions positions;
  private final IncentiveRuleService incentives;
  private final RemittanceSettings settings;
  private final InvoiceTagRepository tags;
  private final RemittanceBatchRepository batches;
  private final DocumentNumberService numbers;
  private final WorkflowService workflow;
  private final AlertService alerts;
  private final CurrentUser currentUser;

  /**
   * Creates the work.
   *
   * @param ledger ledger reads
   * @param batchLedger ledger statuses and locks of batch lines
   * @param positions positions and holding period
   * @param incentives incentive rules
   * @param settings parameters
   * @param tags extraction tags
   * @param batches batches
   * @param numbers batch numbers
   * @param workflow Process Remittance workflow
   * @param alerts alerts
   * @param currentUser current user
   */
  public ExtractionWork(
      InvoiceLedgerQueryService ledger,
      BatchLedger batchLedger,
      LedgerPositions positions,
      IncentiveRuleService incentives,
      RemittanceSettings settings,
      InvoiceTagRepository tags,
      RemittanceBatchRepository batches,
      DocumentNumberService numbers,
      WorkflowService workflow,
      AlertService alerts,
      CurrentUser currentUser) {
    this.ledger = ledger;
    this.batchLedger = batchLedger;
    this.positions = positions;
    this.incentives = incentives;
    this.settings = settings;
    this.tags = tags;
    this.batches = batches;
    this.numbers = numbers;
    this.workflow = workflow;
    this.alerts = alerts;
    this.currentUser = currentUser;
  }

  /**
   * Whether an invoice may be examined at all: receivable (not direct payment, not a return), not
   * cancelled, and in a remittance status that allows extraction (RMTID.019).
   *
   * @param invoice invoice
   * @return true when examinable
   */
  public static boolean isExaminable(OpsInvoice invoice) {
    return !invoice.isDpFlag()
        && !invoice.isCancelled()
        && invoice.getGrossPremium().signum() > 0
        && ELIGIBLE.contains(invoice.getRemittanceStatus());
  }

  /**
   * Examines the scope of a run and creates its batches.
   *
   * @param run the run (managed)
   * @param specialRequestNo special remittance request of a SPECIAL run, else null
   * @return batches created and the tag of each invoice examined
   */
  public Outcome perform(ExtractionRun run, String specialRequestNo) {
    HoldingPeriod holding =
        positions.holdingPeriod(settings.checkHoldDays(), run.getBusinessDate());
    Context context = new Context(run, specialRequestNo, holding, settings.capOverDtip());
    for (String invoiceNo : candidates(run)) {
      examine(context, ledger.require(invoiceNo));
    }
    List<RemittanceBatch> created = new ArrayList<>();
    context.groups.forEach(
        (key, lines) -> created.add(createBatch(run, key, lines, specialRequestNo)));
    return new Outcome(created, context.tagged);
  }

  /**
   * Checks now whether an invoice could be extracted (special remittance validation, MKTID.009),
   * with the same rules as a run.
   *
   * @param invoice invoice (components loaded)
   * @param businessDate date
   * @return decision
   */
  public Decision check(OpsInvoice invoice, LocalDate businessDate) {
    HoldingPeriod holding = positions.holdingPeriod(settings.checkHoldDays(), businessDate);
    LocalDate lastPaid = positions.lastPaidOn(invoice.getInvoiceNo()).orElse(null);
    return RemittanceRules.decide(facts(invoice, lastPaid, holding, settings.capOverDtip()));
  }

  private static Facts facts(
      OpsInvoice invoice, LocalDate lastPaid, HoldingPeriod holding, boolean cap) {
    String owner = invoice.getLockOwner();
    String lockedBy = owner == null || RemittanceSettings.MODULE.equals(owner) ? null : owner;
    return new Facts(
        LedgerPositions.position(invoice),
        invoice.isHoldFlag(),
        invoice.isPendingNegAdj(),
        invoice.isWrittenOff(),
        lastPaid != null && holding.holds(invoice.getBranchId(), lastPaid),
        lockedBy,
        cap);
  }

  private List<String> candidates(ExtractionRun run) {
    if (run.getInvoiceNo() != null) {
      return List.of(run.getInvoiceNo());
    }
    Set<String> found = new LinkedHashSet<>();
    collect(found, run, null, RemittanceStatus.UNPROCESSED);
    collect(found, run, PaymentStatus.PARTIALLY_PAID, RemittanceStatus.WITH_OUTSTANDING_BALANCE);
    collect(found, run, null, RemittanceStatus.PARTIALLY_REMITTED);
    return new ArrayList<>(found);
  }

  private void collect(
      Set<String> found, ExtractionRun run, PaymentStatus payment, RemittanceStatus status) {
    int page = 0;
    Page<OpsInvoice> result;
    do {
      result =
          ledger.search(
              new LedgerSearch(
                  run.getCompanyId(),
                  null,
                  run.getInsurerCode(),
                  null,
                  payment,
                  status,
                  null,
                  null,
                  false,
                  null,
                  null),
              PageRequest.of(page++, PAGE, Sort.by("id")));
      result.forEach(i -> found.add(i.getInvoiceNo()));
    } while (result.hasNext());
  }

  private void examine(Context ctx, OpsInvoice invoice) {
    if (isExaminable(invoice)) {
      examineEligible(ctx, invoice);
    } else if (ctx.single) {
      tag(ctx, invoice, null, notDue("Remittance status " + invoice.getRemittanceStatus()));
    }
  }

  private void examineEligible(Context ctx, OpsInvoice invoice) {
    EarlyIncentiveRule incentive =
        ctx.special
            ? null
            : incentives.earlyIncentive(invoice, ctx.run.getBusinessDate()).orElse(null);
    RemittanceType type = ctx.special ? RemittanceType.SPECIAL : typeOf(invoice, incentive);
    if (ctx.run.getRemittanceType() != null && ctx.run.getRemittanceType() != type) {
      return;
    }
    LocalDate lastPaid = positions.lastPaidOn(invoice.getInvoiceNo()).orElse(null);
    Decision decision = decide(ctx, invoice, lastPaid);
    ctx.run.count(decision.tag());
    record(ctx, new Examined(invoice, type, incentive, lastPaid), decision);
  }

  private Decision decide(Context ctx, OpsInvoice invoice, LocalDate lastPaid) {
    Decision decision = RemittanceRules.decide(facts(invoice, lastPaid, ctx.holding, ctx.cap));
    if (decision.overDtip()) {
      raiseOverDtip(invoice, decision);
    }
    return decision;
  }

  private void record(Context ctx, Examined e, Decision decision) {
    OpsInvoice invoice = e.invoice();
    if (decision.tag() == ExtractionTag.EXTRACTED) {
      BigDecimal rate = e.incentive() == null ? null : e.incentive().getRate();
      RemittanceRules.Line amounts =
          RemittanceRules.amounts(LedgerPositions.position(invoice), decision.remittable(), rate);
      BatchLine line =
          new BatchLine(
              LedgerPositions.facts(invoice, e.lastPaid()),
              invoice.getRemittanceStatus(),
              amounts.basicPremium(),
              amounts.amounts());
      InvoiceTag t = tag(ctx, invoice, e.type(), decision);
      ctx.groups
          .computeIfAbsent(
              new GroupKey(invoice.getInsurerCode(), e.type(), invoice.getCurrency()),
              k -> new ArrayList<>())
          .add(new Pending(line, t));
    } else if (ctx.single || decision.tag() == ExtractionTag.UNEXTRACTED_DUE) {
      tag(ctx, invoice, e.type(), decision);
    } else {
      ctx.tagged.put(invoice.getInvoiceNo(), decision.tag());
    }
  }

  private static Decision notDue(String remarks) {
    return new Decision(
        ExtractionTag.UNEXTRACTED_NOT_DUE, List.of(), remarks, BigDecimal.ZERO, false);
  }

  private static RemittanceType typeOf(OpsInvoice invoice, EarlyIncentiveRule incentive) {
    if (incentive != null) {
      return RemittanceType.WITH_INCENTIVES;
    }
    return PHP.equals(invoice.getCurrency())
        ? RemittanceType.NORMAL_PHP
        : RemittanceType.NORMAL_USD;
  }

  private InvoiceTag tag(Context ctx, OpsInvoice invoice, RemittanceType type, Decision decision) {
    ctx.tagged.put(invoice.getInvoiceNo(), decision.tag());
    var p = LedgerPositions.position(invoice);
    return tags.save(
        new InvoiceTag(
            ctx.run.getId(),
            new InvoiceTag.Facts(
                invoice.getCompanyId(), invoice.getInvoiceNo(), invoice.getInsurerCode(), type),
            new InvoiceTag.Outcome(
                decision.tag(),
                decision.reasons().isEmpty() ? null : String.join(",", decision.reasons()),
                decision.remarks()),
            new InvoiceTag.Money(
                p.paidAr().subtract(p.dtip().remitted()),
                p.dtip().balance(),
                decision.remittable())));
  }

  private void raiseOverDtip(OpsInvoice invoice, Decision decision) {
    alerts.raise(
        OVER_DTIP_ALERT,
        new AlertFacts(
            invoice.getCompanyId(),
            invoice.getBranchId(),
            "OpsInvoice",
            invoice.getInvoiceNo(),
            "Paid AR of "
                + invoice.getInvoiceNo()
                + " is above its DTIP balance"
                + (decision.reasons().contains(RemittanceRules.PAID_AR_OVER_DTIP)
                    ? " - excluded (RMTID.014)"
                    : " - capped at the DTIP (RMTID.014)"),
            decision.remittable(),
            OVER_DTIP_ALERT + ":" + invoice.getInvoiceNo()));
  }

  private RemittanceBatch createBatch(
      ExtractionRun run, GroupKey key, List<Pending> lines, String specialRequestNo) {
    String number =
        numbers.next("RMB-" + key.insurerCode() + "-" + run.getBusinessDate().getYear());
    RemittanceBatch batch =
        new RemittanceBatch(
            new RemittanceBatch.Header(
                run.getCompanyId(),
                number,
                key.insurerCode(),
                key.type(),
                key.currency(),
                run.getId(),
                specialRequestNo),
            specialRequestNo == null ? currentUser.optionalUsername().orElse(null) : null);
    lines.forEach(p -> batch.add(p.line()));
    RemittanceBatch saved = batches.save(batch);
    workflow.start(
        new StartCase(
            run.getCompanyId(),
            WORKFLOW,
            new CaseRecord(
                BATCH_ENTITY,
                saved.getId().toString(),
                number,
                key.insurerCode()
                    + " - "
                    + BatchDocuments.typeLabel(key.type())
                    + " - "
                    + lines.size()
                    + " account(s)",
                "/remittance/batches/" + saved.getId(),
                null),
            null));
    for (Pending p : lines) {
      batchLedger.take(p.line().getInvoiceNo(), number);
      p.tag().inBatch(number);
    }
    run.batchCreated();
    return saved;
  }

  /** State of one run. */
  private static final class Context {
    private final ExtractionRun run;
    private final boolean special;
    private final boolean single;
    private final HoldingPeriod holding;
    private final boolean cap;
    private final Map<GroupKey, List<Pending>> groups = new LinkedHashMap<>();
    private final Map<String, ExtractionTag> tagged = new LinkedHashMap<>();

    Context(ExtractionRun run, String specialRequestNo, HoldingPeriod holding, boolean cap) {
      this.run = run;
      this.special = specialRequestNo != null;
      this.single = run.getInvoiceNo() != null;
      this.holding = holding;
      this.cap = cap;
    }
  }

  private record Examined(
      OpsInvoice invoice, RemittanceType type, EarlyIncentiveRule incentive, LocalDate lastPaid) {}

  private record GroupKey(String insurerCode, RemittanceType type, String currency) {}

  private record Pending(BatchLine line, InvoiceTag tag) {}

  /**
   * What a run produced.
   *
   * @param batches batches created
   * @param tagged tag of every invoice examined
   */
  public record Outcome(List<RemittanceBatch> batches, Map<String, ExtractionTag> tagged) {

    /** Defensive copies. */
    public Outcome {
      batches = List.copyOf(batches);
      tagged = Map.copyOf(tagged);
    }
  }
}
