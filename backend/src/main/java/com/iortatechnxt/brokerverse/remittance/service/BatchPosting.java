package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest.DocumentRefs;
import com.iortatechnxt.brokerverse.opsledger.service.port.DisbursementGateway;
import com.iortatechnxt.brokerverse.opsledger.service.port.DisbursementGateway.DisbursementTicket;
import com.iortatechnxt.brokerverse.opsledger.service.port.ReceiptIssuer;
import com.iortatechnxt.brokerverse.opsledger.service.port.ReceiptIssuer.IssuedReceipt;
import com.iortatechnxt.brokerverse.opsledger.service.port.ReceiptIssuer.ReceiptLine;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceAmounts;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.service.RemittancePostings.Posting;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Posts an approved batch inside the approval transaction (OPERATIONS_DESIGN 5 rows 12-13,
 * RMTID.010/011/019/023, CSHID.007; ACCOUNTING_DISBURSEMENT_DESIGN 6 rows 15-17):
 *
 * <ol>
 *   <li>per invoice line, event {@code OPS_REMITTANCE} ({@code RMB:<ref>:<invoice>}): DTIP (paid
 *       AR) and CWT (WTAX) against the commission receivable (commission and VAT) and the net due
 *       to the insurer for disbursement, at the BOOK rate; and the REMITTED ledger movement on
 *       DTIP, commission, VAT and WTAX with remittance status APPROVED;
 *   <li>the early incentive with its service invoice and the CPC2 incentive ({@link
 *       BatchIncentives}, DIS 3.29.1/3.29.2);
 *   <li>the insurer's confirmed deductions, capped at the amount payable ({@link DeductionPosting},
 *       ACSL 2.9.2);
 *   <li>the payment request to Disbursement through {@link DisbursementGateway} (type REMITTANCE,
 *       routed straight to the approver with its RFP number, DIS 3.25.0), amount = net due less the
 *       incentives and deductions; when the deductions took everything, the batch is settled
 *       without a request;
 *   <li>the commission OR (CSHID.007) and the incentive OR, with the early-incentive withholding
 *       tax, through {@link ReceiptIssuer}, once per batch.
 * </ol>
 *
 * <p>{@code <ref>} is the batch number, or {@code <batch>/R<n>} when the batch is sent again after
 * a cancelled DV (DIS 2.20.0).
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class BatchPosting {

  /** Remittance event. */
  public static final String REMITTANCE_EVENT = RemittancePostings.REMITTANCE_EVENT;

  /** Incentive event. */
  public static final String INCENTIVE_EVENT = RemittancePostings.INCENTIVE_EVENT;

  private static final String INSURER = "INSURER";
  private static final String INCENTIVE = "INCENTIVE";

  private final RemittancePostings postings;
  private final BatchIncentives incentives;
  private final DeductionPosting deductions;
  private final BatchRemittance remittance;
  private final InvoiceLedgerQueryService ledger;
  private final InvoiceLedgerService writer;
  private final DisbursementGateway disbursement;
  private final ReceiptIssuer receipts;
  private final BatchDocuments documents;
  private final Clock clock;

  /**
   * Creates the posting.
   *
   * @param postings accounting events
   * @param incentives incentives and early-incentive SI
   * @param deductions insurer deductions
   * @param remittance settlement without payment
   * @param ledger ledger reads
   * @param writer ledger movements and statuses
   * @param disbursement Disbursement port
   * @param receipts OR port
   * @param documents insurer names
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public BatchPosting(
      RemittancePostings postings,
      BatchIncentives incentives,
      DeductionPosting deductions,
      BatchRemittance remittance,
      InvoiceLedgerQueryService ledger,
      InvoiceLedgerService writer,
      DisbursementGateway disbursement,
      ReceiptIssuer receipts,
      BatchDocuments documents,
      Clock clock) {
    this.postings = postings;
    this.incentives = incentives;
    this.deductions = deductions;
    this.remittance = remittance;
    this.ledger = ledger;
    this.writer = writer;
    this.disbursement = disbursement;
    this.receipts = receipts;
    this.documents = documents;
    this.clock = clock;
  }

  /**
   * Posts the batch and pushes it to Disbursement.
   *
   * @param batch approved batch with its lines
   */
  public void post(RemittanceBatch batch) {
    LocalDate today = LocalDate.now(clock);
    Long branchId = null;
    Set<String> roots = new LinkedHashSet<>();
    for (BatchLine line : batch.included()) {
      OpsInvoice invoice = ledger.require(line.getInvoiceNo());
      branchId = branchId == null ? invoice.getBranchId() : branchId;
      roots.add(invoice.getRootInvoiceNo());
      postLine(batch, line, invoice, today);
    }
    incentives.post(batch, branchId, today);
    deductions.consume(batch, branchId, today);
    String insurerName = documents.insurerName(batch);
    if (batch.amountDue().signum() > 0) {
      send(batch, insurerName, roots.size() == 1 ? roots.iterator().next() : null);
    } else {
      batch.sentToDisbursement(null, "NOT_REQUIRED", batch.amountDue());
      remittance.remitted(batch, "Deductions");
    }
    issueReceipts(batch, insurerName, today);
  }

  private void send(RemittanceBatch batch, String insurerName, String rootInvoiceNo) {
    String ref = batch.cycleReference();
    DisbursementTicket ticket =
        disbursement.send(
            batch.getCompanyId(),
            new DisbursementRequest.Spec(
                    DisbursementRequest.Type.REMITTANCE,
                    RemittanceSettings.MODULE,
                    ref,
                    batch.getInsurerCode(),
                    insurerName,
                    batch.getCurrency(),
                    batch.amountDue(),
                    "Remittance "
                        + ref
                        + " to "
                        + insurerName
                        + " ("
                        + batch.getLineCount()
                        + " account(s))",
                    batch.getBatchNo())
                .routed(ref, INSURER, "REMITTANCE", rootInvoiceNo, true)
                .withReferences(List.of(), List.of(RemittancePostings.movementRef(batch))));
    batch.sentToDisbursement(ticket.requestNo(), ticket.status().name(), batch.amountDue());
  }

  private void postLine(
      RemittanceBatch batch, BatchLine line, OpsInvoice invoice, LocalDate today) {
    RemittanceAmounts a = line.getAmounts();
    String journal =
        postings.publish(
            new Posting(
                RemittancePostings.REMITTANCE_EVENT,
                batch,
                invoice.getBranchId(),
                today,
                RemittancePostings.sourceRef(batch, line.getInvoiceNo()),
                invoice,
                RemittancePostings.lineAmounts(a, 1),
                "Remittance " + invoice.getInvoiceNo() + " " + batch.getBatchNo()));
    Map<LedgerComponent, BigDecimal> moved = new EnumMap<>(LedgerComponent.class);
    moved.put(LedgerComponent.DTIP, a.dtip());
    moved.put(LedgerComponent.COMMISSION, a.commission());
    moved.put(LedgerComponent.COMMISSION_VAT, a.commissionVat());
    moved.put(LedgerComponent.WTAX, a.wtax());
    writer.post(
        new MovementRequest(
            line.getInvoiceNo(),
            MovementType.REMITTED,
            RemittanceSettings.MODULE,
            RemittancePostings.movementRef(batch),
            today,
            moved,
            new DocumentRefs(null, null, batch.getBatchNo(), journal),
            "Remitted to " + batch.getInsurerCode() + " in " + batch.getBatchNo()));
    writer.setRemittanceStatus(
        line.getInvoiceNo(),
        RemittanceStatus.APPROVED,
        RemittanceSettings.MODULE,
        "Batch " + batch.getBatchNo() + " approved");
    line.posted(journal);
  }

  private void issueReceipts(RemittanceBatch batch, String insurerName, LocalDate today) {
    List<String> messages = new ArrayList<>();
    if (batch.getTotals().commission().signum() > 0 && batch.getCommissionOrStatus() == null) {
      IssuedReceipt or =
          issue(
              batch,
              insurerName,
              today,
              "COMMISSION",
              a -> new BigDecimal[] {a.commission(), a.commissionVat(), a.wtax()});
      batch.commissionReceipt(or.status().name(), or.receiptNo());
      messages.add("Commission OR: " + or.message());
    }
    if (batch.getTotals().incentive().signum() > 0 && batch.getIncentiveOrStatus() == null) {
      IssuedReceipt or =
          issue(
              batch,
              insurerName,
              today,
              INCENTIVE,
              a ->
                  new BigDecimal[] {
                    a.incentive(), a.incentiveVat(), incentives.wtaxOn(a.incentive())
                  });
      batch.incentiveReceipt(or.status().name(), or.receiptNo());
      messages.add("Incentive OR: " + or.message());
    }
    if (!messages.isEmpty()) {
      batch.receiptMessage(String.join("; ", messages));
    }
  }

  private IssuedReceipt issue(
      RemittanceBatch batch,
      String insurerName,
      LocalDate today,
      String orType,
      Function<RemittanceAmounts, BigDecimal[]> parts) {
    List<ReceiptLine> lines =
        batch.included().stream()
            .map(
                l -> {
                  BigDecimal[] p = parts.apply(l.getAmounts());
                  return new ReceiptLine(
                      l.getInvoiceNo(),
                      batch.getInsurerCode(),
                      p[0],
                      p[1],
                      p[2],
                      orType + " " + l.getInvoiceNo());
                })
            .filter(l -> l.gross().signum() > 0)
            .toList();
    String si = INCENTIVE.equals(orType) ? batch.getSettlement().getEarlySiNo() : null;
    String remarks =
        orType
            + " of remittance batch "
            + batch.getBatchNo()
            + (si == null ? "" : " (service invoice " + si + ")");
    return receipts.issueOfficialReceipt(
        new ReceiptIssuer.ReceiptRequest(
            batch.getCompanyId(),
            orType,
            new ReceiptIssuer.Payee(batch.getInsurerCode(), insurerName),
            batch.getCurrency(),
            today,
            lines,
            new ReceiptIssuer.Source(
                RemittanceSettings.MODULE, batch.getBatchNo() + ":" + orType, si, remarks)));
  }
}
