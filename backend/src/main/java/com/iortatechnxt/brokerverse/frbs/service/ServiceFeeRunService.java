package com.iortatechnxt.brokerverse.frbs.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.frbs.domain.FrbsEnums.RunStage;
import com.iortatechnxt.brokerverse.frbs.domain.PaidInvoice;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeItem;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeItemRepository;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeLine;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeLineRepository;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRecipient;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRun;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRunRepository;
import com.iortatechnxt.brokerverse.frbs.service.ServiceFeeCalculator.Computation;
import com.iortatechnxt.brokerverse.frbs.service.ServiceFeeCalculator.ComputedLine;
import com.iortatechnxt.brokerverse.frbs.service.ServiceFeeCalculator.ItemFee;
import com.iortatechnxt.brokerverse.workflow.domain.CaseRecord;
import com.iortatechnxt.brokerverse.workflow.service.StartCase;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service-fee runs (FRBS 2.10.0; design 7.3): the GL officer computes the fee of the invoices fully
 * paid in a period and submits it; the team lead approves it (four eyes), which accrues every line
 * and sends it to Disbursement as a payout request. A computed run can be recomputed; the workflow
 * panel returns it or cancels it.
 */
@Service
@Transactional
public class ServiceFeeRunService {

  private final ServiceFeeRunRepository runs;
  private final ServiceFeeLineRepository lines;
  private final ServiceFeeItemRepository items;
  private final ServiceFeeBase base;
  private final ServiceFeeSetupService setup;
  private final ServiceFeePayouts payouts;
  private final WorkflowService workflow;
  private final DocumentNumberService numbers;
  private final CurrentUser currentUser;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param runs runs
   * @param lines lines
   * @param items invoices
   * @param base paid invoices
   * @param setup rules and recipients
   * @param payouts accrual and payout requests
   * @param workflow workflow engine
   * @param numbers document numbers
   * @param currentUser current user
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // Spring constructor injection
  public ServiceFeeRunService(
      ServiceFeeRunRepository runs,
      ServiceFeeLineRepository lines,
      ServiceFeeItemRepository items,
      ServiceFeeBase base,
      ServiceFeeSetupService setup,
      ServiceFeePayouts payouts,
      WorkflowService workflow,
      DocumentNumberService numbers,
      CurrentUser currentUser,
      AuditTrailService audit,
      Clock clock) {
    this.runs = runs;
    this.lines = lines;
    this.items = items;
    this.base = base;
    this.setup = setup;
    this.payouts = payouts;
    this.workflow = workflow;
    this.numbers = numbers;
    this.currentUser = currentUser;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Computes a run for the invoices fully paid in a period (FRBS 2.10.0).
   *
   * @param companyId company
   * @param from first day
   * @param to last day
   * @return run in stage COMPUTED
   */
  public ServiceFeeRun compute(Long companyId, LocalDate from, LocalDate to) {
    LocalDate today = LocalDate.now(clock.withZone(ServiceFeeBase.MANILA));
    if (from == null || to == null || to.isBefore(from) || to.isAfter(today)) {
      throw new BusinessRuleException(
          "SERVICE_FEE_PERIOD", "Give a period that ends on or before today and after it starts");
    }
    Computation computation = calculate(companyId, from, to);
    ServiceFeeRun run =
        runs.save(
            new ServiceFeeRun(
                companyId, numbers.next("SFR-" + today.getYear()), from, to, clock.instant()));
    store(run, computation);
    workflow.start(
        new StartCase(
            companyId,
            ServiceFees.WORKFLOW,
            new CaseRecord(
                ServiceFees.ENTITY,
                String.valueOf(run.getId()),
                run.getRunNo(),
                "Service fee " + from + " to " + to,
                ServiceFees.link(run.getId()),
                null),
            null));
    audit.record(
        ServiceFees.ENTITY,
        run.getRunNo(),
        AuditAction.CREATE,
        "Computed "
            + computation.invoiceCount()
            + " invoice(s), fee "
            + computation.feeTotal()
            + (computation.uncovered().isEmpty()
                ? ""
                : ", " + computation.uncovered().size() + " invoice(s) without a rule"));
    return run;
  }

  /**
   * Computes a run again after a return or a rule change: its invoices are freed and the period is
   * read again.
   *
   * @param runId run
   * @return run
   */
  public ServiceFeeRun recompute(Long runId) {
    ServiceFeeRun run = require(runId);
    run.requireStage(RunStage.COMPUTED, "recomputed");
    items.deleteAll(items.findByRunIdOrderByInvoiceNoAsc(runId));
    items.flush();
    lines.deleteAll(lines.findByRunIdOrderByLineNoAsc(runId));
    lines.flush();
    Computation computation = calculate(run.getCompanyId(), run.getPeriodFrom(), run.getPeriodTo());
    store(run, computation);
    audit.record(
        ServiceFees.ENTITY,
        run.getRunNo(),
        AuditAction.UPDATE,
        "Recomputed " + computation.invoiceCount() + " invoice(s), fee " + computation.feeTotal());
    return run;
  }

  /**
   * Submits a computed run to the team lead.
   *
   * @param runId run
   * @param comment comment
   * @return run
   */
  public ServiceFeeRun submit(Long runId, String comment) {
    ServiceFeeRun run = require(runId);
    run.requireStage(RunStage.COMPUTED, "submitted");
    if (run.getInvoiceCount() == 0) {
      throw new BusinessRuleException(
          "SERVICE_FEE_EMPTY", "Run " + run.getRunNo() + " has no invoice to pay a fee on");
    }
    workflow.transition(ServiceFees.ENTITY, key(run), "submit", ServiceFees.note(comment));
    run.submitted(currentUser.username(), clock.instant());
    return run;
  }

  /**
   * Approves a run (four eyes): every line is accrued and sent to Disbursement (FRBS 2.10.0).
   *
   * @param runId run
   * @param comment comment
   * @return run
   */
  public ServiceFeeRun approve(Long runId, String comment) {
    ServiceFeeRun run = require(runId);
    run.requireStage(RunStage.FOR_APPROVAL, "approved");
    String user = currentUser.username();
    if (CurrentUser.sameUser(user, run.getSubmittedBy())
        || CurrentUser.sameUser(user, run.getCreatedBy())) {
      throw new BusinessRuleException(
          "SERVICE_FEE_FOUR_EYES", "The run is approved by someone other than its preparer");
    }
    workflow.transition(ServiceFees.ENTITY, key(run), "approve", ServiceFees.note(comment));
    run.approved(user, clock.instant());
    for (ServiceFeeLine line : lines.findByRunIdOrderByLineNoAsc(runId)) {
      payouts.accrue(run, line);
      payouts.send(run, line);
    }
    return run;
  }

  /**
   * Sends a returned line to Disbursement again.
   *
   * @param lineId line
   * @return line
   */
  public ServiceFeeLine resend(Long lineId) {
    ServiceFeeLine line =
        lines
            .findById(lineId)
            .orElseThrow(() -> new ResourceNotFoundException("Service-fee line", lineId));
    ServiceFeeRun run = require(line.getRunId());
    run.requireStage(RunStage.APPROVED, "paid again");
    payouts.send(run, line);
    return line;
  }

  private Computation calculate(Long companyId, LocalDate from, LocalDate to) {
    List<PaidInvoice> paid = base.paid(companyId, from, to);
    Map<String, ServiceFeeRecipient> recipients =
        setup.recipients(companyId).stream()
            .collect(Collectors.toMap(ServiceFeeRecipient::getSalesUnit, Function.identity()));
    Computation computation =
        ServiceFeeCalculator.compute(paid, setup.rules(), recipients, base.units(companyId));
    if (computation.lines().isEmpty()) {
      throw new BusinessRuleException(
          "SERVICE_FEE_NOTHING",
          "No invoice fully paid from "
              + from
              + " to "
              + to
              + " is left for a service fee"
              + (computation.uncovered().isEmpty()
                  ? ""
                  : " (" + computation.uncovered().size() + " paid invoice(s) have no rule)"));
    }
    return computation;
  }

  private void store(ServiceFeeRun run, Computation computation) {
    int lineNo = 0;
    for (ComputedLine c : computation.lines()) {
      lineNo++;
      ServiceFeeLine line = lines.save(new ServiceFeeLine(run.getId(), lineNo, c.draft()));
      for (ItemFee f : c.items()) {
        items.save(
            new ServiceFeeItem(
                run.getId(), line.getId(), f.invoice(), f.fee().base(), f.fee().fee()));
      }
    }
    run.computed(computation.invoiceCount(), computation.feeTotal(), clock.instant());
  }

  private ServiceFeeRun require(Long runId) {
    return runs.findById(runId)
        .orElseThrow(() -> new ResourceNotFoundException("Service-fee run", runId));
  }

  private static String key(ServiceFeeRun run) {
    return String.valueOf(run.getId());
  }
}
