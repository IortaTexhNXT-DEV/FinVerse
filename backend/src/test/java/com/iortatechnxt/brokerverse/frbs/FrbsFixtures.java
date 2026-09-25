package com.iortatechnxt.brokerverse.frbs;

import com.iortatechnxt.brokerverse.accounting.domain.CostCenterRuleValues;
import com.iortatechnxt.brokerverse.accounting.service.CostCenterRuleService;
import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeLine;
import com.iortatechnxt.brokerverse.frbs.domain.ServiceFeeRun;
import com.iortatechnxt.brokerverse.frbs.service.ServiceFeeBase;
import com.iortatechnxt.brokerverse.frbs.service.ServiceFeeQueryService;
import com.iortatechnxt.brokerverse.frbs.service.ServiceFeeRunService;
import com.iortatechnxt.brokerverse.opsledger.OpsLedgerFixtures;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceComponent;
import com.iortatechnxt.brokerverse.opsledger.service.DisbursementQueueService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest;
import com.iortatechnxt.brokerverse.support.AsUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Service-fee test data: booked motor invoices (CBG) paid in full by Cashiering today, a catch-all
 * cost-centre rule for the accrual (account 5614 needs one), runs computed by the GL officer
 * ({@code glofficer}) and approved by the GL team lead ({@code gltl}), and the in-app Disbursement
 * queue worked by {@code disb}.
 */
@Component
public class FrbsFixtures {

  /** GL officer: computes, submits, tags. */
  public static final String OFFICER = "glofficer";

  /** GL team lead: approves, configures. */
  public static final String LEAD = "gltl";

  private final OpsLedgerFixtures ops;
  private final InvoiceLedgerQueryService ledger;
  private final InvoiceLedgerService ledgerWriter;
  private final CostCenterRuleService costCenters;
  private final ServiceFeeRunService runs;
  private final ServiceFeeQueryService queries;
  private final DisbursementQueueService queue;
  private final TransactionTemplate tx;
  private final AsUser as;

  FrbsFixtures(
      OpsLedgerFixtures ops,
      InvoiceLedgerQueryService ledger,
      InvoiceLedgerService ledgerWriter,
      CostCenterRuleService costCenters,
      ServiceFeeRunService runs,
      ServiceFeeQueryService queries,
      DisbursementQueueService queue,
      TransactionTemplate tx,
      AsUser as) {
    this.ops = ops;
    this.ledger = ledger;
    this.ledgerWriter = ledgerWriter;
    this.costCenters = costCenters;
    this.runs = runs;
    this.queries = queries;
    this.queue = queue;
    this.tx = tx;
    this.as = as;
  }

  /** The demo company. */
  public Long company() {
    return ops.company();
  }

  /** Today in Manila (the business day of the payment status change). */
  public static LocalDate today() {
    return LocalDate.now(ServiceFeeBase.MANILA);
  }

  /** A booked motor invoice paid in full today; returns its number. */
  public String paidInvoice() {
    String invoiceNo = ops.motorInvoice().getInvoiceNo();
    as.run(
        "cashier",
        () ->
            tx.execute(
                s ->
                    ledgerWriter.post(
                        new MovementRequest(
                            invoiceNo,
                            MovementType.APPLIED,
                            "CASHIERING",
                            "SFT:" + BookingFixtures.token(),
                            today(),
                            balances(ledger.require(invoiceNo)),
                            new MovementRequest.DocumentRefs(
                                "AR-SF-" + invoiceNo, null, null, null),
                            "Paid in full"))));
    return invoiceNo;
  }

  private static Map<LedgerComponent, BigDecimal> balances(OpsInvoice invoice) {
    Map<LedgerComponent, BigDecimal> amounts = new EnumMap<>(LedgerComponent.class);
    for (OpsInvoiceComponent c : invoice.getComponents()) {
      if (c.getComponent().isPremiumReceivable() && c.getBalance().signum() > 0) {
        amounts.put(c.getComponent(), c.getBalance());
      }
    }
    return amounts;
  }

  /** A catch-all cost-centre rule for the service-fee accrual (FRBS 3.1.1). */
  public void costCentreRule() {
    as.run(
        LEAD,
        () -> {
          boolean present =
              costCenters.list(company()).stream()
                  .anyMatch(r -> "FRBS_SERVICE_FEE_ACCRUE".equals(r.getEventType()));
          if (!present) {
            costCenters.create(
                company(),
                new CostCenterRuleValues(
                    9000,
                    "FRBS",
                    "FRBS_SERVICE_FEE_ACCRUE",
                    null,
                    null,
                    null,
                    "NB-CBG-M",
                    "Service fee accrual (test)",
                    true));
          }
          return Boolean.TRUE;
        });
  }

  /** A paid invoice and a run computed for today. */
  public ServiceFeeRun computedRun() {
    paidInvoice();
    costCentreRule();
    return as.run(OFFICER, () -> runs.compute(company(), today(), today()));
  }

  /** A run computed, submitted and approved (accrued and sent to Disbursement). */
  public ServiceFeeRun approvedRun() {
    ServiceFeeRun run = computedRun();
    as.run(OFFICER, () -> runs.submit(run.getId(), "Please approve"));
    return as.run(LEAD, () -> runs.approve(run.getId(), "Approved"));
  }

  /** The lines of a run with a fee. */
  public List<ServiceFeeLine> paidLines(ServiceFeeRun run) {
    return queries.lines(run.getId()).stream().filter(l -> l.getFee().signum() > 0).toList();
  }

  /** The gateway request of a line. */
  public DisbursementRequest request(ServiceFeeRun run, ServiceFeeLine line) {
    return queue.find("FRBS", line.sourceRef(run.getRunNo())).orElseThrow();
  }

  /** Disbursement pays a line. */
  public void pay(ServiceFeeRun run, ServiceFeeLine line) {
    Long id = request(run, line).getId();
    as.run(
        "disb",
        () -> {
          queue.acknowledge(id);
          queue.assignDv(id, "DV-SF-" + BookingFixtures.token());
          return queue.markPaid(id);
        });
  }

  /** Disbursement returns a line. */
  public void returnLine(ServiceFeeRun run, ServiceFeeLine line) {
    Long id = request(run, line).getId();
    as.run("disb", () -> queue.returnToSource(id, "Payee unknown"));
  }
}
