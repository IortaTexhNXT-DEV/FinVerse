package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequestRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums.RunStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRunRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsHandoff;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsHandoffRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceRepository;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.opsledger.service.port.OpsWorkCountSource;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Operations home counts the ledger derives by itself (BRQID.003): outstanding premium,
 * collected premium not yet remitted, holds, pending negative adjustments, locked invoices, the
 * month's production, direct payment commission outstanding, the Disbursement queue, failed
 * interface runs and open hand-offs. The Operations modules add their own queues.
 */
@Component
@Transactional(readOnly = true)
public class LedgerWorkCounts implements OpsWorkCountSource {

  private static final String INVOICES = "/operations/invoices";
  private static final Duration RECENT = Duration.ofDays(7);

  private final OpsInvoiceRepository invoices;
  private final DisbursementRequestRepository disbursements;
  private final FlowInRunRepository runs;
  private final OpsHandoffRepository handoffs;
  private final Clock clock;

  /**
   * Creates the source.
   *
   * @param invoices invoices
   * @param disbursements Disbursement queue
   * @param runs flow-in runs
   * @param handoffs hand-offs
   * @param clock clock
   */
  public LedgerWorkCounts(
      OpsInvoiceRepository invoices,
      DisbursementRequestRepository disbursements,
      FlowInRunRepository runs,
      OpsHandoffRepository handoffs,
      Clock clock) {
    this.invoices = invoices;
    this.disbursements = disbursements;
    this.runs = runs;
    this.handoffs = handoffs;
    this.clock = clock;
  }

  @Override
  public List<WorkCount> counts(Long companyId) {
    List<WorkCount> counts = new ArrayList<>(collection(companyId));
    counts.addAll(followUp(companyId));
    counts.addAll(queues(companyId));
    return counts;
  }

  private List<WorkCount> collection(Long companyId) {
    return List.of(
        new WorkCount(
            Section.CASHIERING,
            "outstanding",
            "Invoices with Outstanding Premium",
            invoices.countByCompanyIdAndPaymentStatusIn(
                companyId, List.of(PaymentStatus.UNPAID, PaymentStatus.PARTIALLY_PAID)),
            Severity.INFO,
            INVOICES + "?payment=UNPAID"),
        new WorkCount(
            Section.CASHIERING,
            "partial",
            "Partially Paid Invoices",
            invoices.countByCompanyIdAndPaymentStatusIn(
                companyId, List.of(PaymentStatus.PARTIALLY_PAID)),
            Severity.WARNING,
            INVOICES + "?payment=PARTIALLY_PAID"),
        new WorkCount(
            Section.REMITTANCE,
            "paid-unremitted",
            "Paid, Not Yet Extracted",
            invoices.countByCompanyIdAndPaymentStatusAndRemittanceStatus(
                companyId, PaymentStatus.PAID, RemittanceStatus.UNPROCESSED),
            Severity.INFO,
            INVOICES + "?payment=PAID&remittance=UNPROCESSED"),
        new WorkCount(
            Section.REMITTANCE,
            "on-hold",
            "Invoices on Hold",
            invoices.countByCompanyIdAndHoldFlagTrue(companyId),
            Severity.WARNING,
            INVOICES + "?flag=HOLD"),
        new WorkCount(
            Section.REMITTANCE,
            "locked",
            "Locked Invoices",
            invoices.countByCompanyIdAndLockOwnerIsNotNull(companyId),
            Severity.INFO,
            INVOICES + "?locked=true"));
  }

  private List<WorkCount> followUp(Long companyId) {
    LocalDate today = LocalDate.now(clock);
    return List.of(
        new WorkCount(
            Section.PRODRECON,
            "production",
            "Booked This Month",
            invoices.countByCompanyIdAndClassificationBookingDateBetween(
                companyId, today.withDayOfMonth(1), today),
            Severity.INFO,
            INVOICES),
        new WorkCount(
            Section.ADJUSTMENT,
            "pending-negative",
            "Pending Negative Adjustments",
            invoices.countByCompanyIdAndPendingNegAdjTrue(companyId),
            Severity.WARNING,
            INVOICES + "?flag=PENDING_NEG_ADJ"),
        new WorkCount(
            Section.COMMISSION,
            "dp-commission",
            "Direct Payment Commission Outstanding",
            invoices.countDirectPaymentCommissionOutstanding(companyId),
            Severity.INFO,
            INVOICES + "?dp=true"));
  }

  private List<WorkCount> queues(Long companyId) {
    return List.of(
        new WorkCount(
            Section.DISBURSEMENT,
            "to-process",
            "Payment Requests to Process",
            disbursements.countByCompanyIdAndStatusIn(
                companyId,
                List.of(
                    DisbursementRequest.Status.SENT,
                    DisbursementRequest.Status.ACKNOWLEDGED,
                    DisbursementRequest.Status.DV_ASSIGNED)),
            Severity.WARNING,
            "/operations/disbursements"),
        new WorkCount(
            Section.INTERFACES,
            "failed-runs",
            "Failed Interface Runs (7 Days)",
            runs.countByStatusInAndStartedAtGreaterThanEqual(
                List.of(RunStatus.FAILED, RunStatus.PARTIAL), clock.instant().minus(RECENT)),
            Severity.ALERT,
            "/operations/interfaces"),
        new WorkCount(
            Section.CASHIERING,
            "handoffs",
            "Hand-offs to Complete",
            handoffs.countByCompanyIdAndStatus(companyId, OpsHandoff.Status.OPEN),
            Severity.WARNING,
            "/operations/handoffs"));
  }
}
