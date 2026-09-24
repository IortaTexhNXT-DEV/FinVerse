package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.RemittanceStatus;
import com.iortatechnxt.brokerverse.opsledger.service.BookRates;
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
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Posts an approved batch inside the approval transaction (OPERATIONS_DESIGN 5 rows 12-13,
 * RMTID.010/011/019/023, CSHID.007):
 *
 * <ol>
 *   <li>per invoice line, event {@code OPS_REMITTANCE} ({@code RMB:<batch>:<invoice>}): DTIP (paid
 *       AR) and CWT (WTAX) against the commission receivable (commission and VAT) and the net due
 *       to the insurer for disbursement, at the BOOK rate; and the REMITTED ledger movement on
 *       DTIP, commission, VAT and WTAX with remittance status APPROVED;
 *   <li>for a With Incentives batch, event {@code OPS_REMIT_INCENTIVE} ({@code RMB:<batch>:INC});
 *   <li>the payment request to Disbursement through {@link DisbursementGateway} (type REMITTANCE,
 *       amount payable = net due less the incentive);
 *   <li>the commission OR (CSHID.007) and the incentive OR through {@link ReceiptIssuer} (the
 *       default adapter hands them over to Cashiering until the cashiering module is installed).
 * </ol>
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class BatchPosting {

  /** Remittance event. */
  public static final String REMITTANCE_EVENT = "OPS_REMITTANCE";

  /** Incentive event. */
  public static final String INCENTIVE_EVENT = "OPS_REMIT_INCENTIVE";

  private static final String DUE_FOR_DISBURSEMENT = "DUE_FOR_DISBURSEMENT";
  private static final String PREFIX = "RMB:";

  private final AccountingEventPublisher accounting;
  private final BookRates rates;
  private final InvoiceLedgerQueryService ledger;
  private final InvoiceLedgerService writer;
  private final DisbursementGateway disbursement;
  private final ReceiptIssuer receipts;
  private final BatchDocuments documents;
  private final Clock clock;

  /**
   * Creates the posting.
   *
   * @param accounting accounting engine
   * @param rates BOOK rates
   * @param ledger ledger reads
   * @param writer ledger movements and statuses
   * @param disbursement Disbursement port
   * @param receipts OR port
   * @param documents insurer names
   * @param clock clock
   */
  public BatchPosting(
      AccountingEventPublisher accounting,
      BookRates rates,
      InvoiceLedgerQueryService ledger,
      InvoiceLedgerService writer,
      DisbursementGateway disbursement,
      ReceiptIssuer receipts,
      BatchDocuments documents,
      Clock clock) {
    this.accounting = accounting;
    this.rates = rates;
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
    for (BatchLine line : batch.included()) {
      OpsInvoice invoice = ledger.require(line.getInvoiceNo());
      branchId = branchId == null ? invoice.getBranchId() : branchId;
      postLine(batch, line, invoice, today);
    }
    RemittanceAmounts totals = batch.getTotals();
    if (totals.incentiveTotal().signum() > 0) {
      postIncentive(batch, branchId, today);
    }
    String insurerName = documents.insurerName(batch);
    DisbursementTicket ticket =
        disbursement.send(
            batch.getCompanyId(),
            new DisbursementRequest.Spec(
                DisbursementRequest.Type.REMITTANCE,
                RemittanceSettings.MODULE,
                batch.getBatchNo(),
                batch.getInsurerCode(),
                insurerName,
                batch.getCurrency(),
                totals.payable(),
                "Remittance "
                    + batch.getBatchNo()
                    + " to "
                    + insurerName
                    + " ("
                    + batch.getLineCount()
                    + " account(s))",
                batch.getBatchNo()));
    batch.sentToDisbursement(ticket.requestNo(), ticket.status().name(), totals.payable());
    issueReceipts(batch, insurerName, today);
  }

  private void postLine(
      RemittanceBatch batch, BatchLine line, OpsInvoice invoice, LocalDate today) {
    RemittanceAmounts a = line.getAmounts();
    Map<String, BigDecimal> amounts = new LinkedHashMap<>();
    amounts.put("DTIP", a.dtip());
    amounts.put("CWT", a.wtax());
    amounts.put("COMMISSION_RECEIVABLE", a.commissionReceivable());
    amounts.put(DUE_FOR_DISBURSEMENT, a.netDue());
    String journal =
        accounting
            .publish(
                rates.price(
                    event(
                        REMITTANCE_EVENT,
                        new EventKey(
                            batch,
                            invoice.getBranchId(),
                            today,
                            PREFIX + batch.getBatchNo() + ":" + line.getInvoiceNo()),
                        invoice,
                        amounts)))
            .getBatchNo();
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
            PREFIX + batch.getBatchNo(),
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

  private void postIncentive(RemittanceBatch batch, Long branchId, LocalDate today) {
    RemittanceAmounts t = batch.getTotals();
    Map<String, BigDecimal> amounts = new LinkedHashMap<>();
    amounts.put(DUE_FOR_DISBURSEMENT, t.incentiveTotal());
    amounts.put("INCENTIVE_INCOME", t.incentive());
    amounts.put("OUTPUT_VAT", t.incentiveVat());
    accounting.publish(
        rates.price(
            event(
                INCENTIVE_EVENT,
                new EventKey(batch, branchId, today, PREFIX + batch.getBatchNo() + ":INC"),
                null,
                amounts)));
  }

  private static BusinessEvent event(
      String type, EventKey key, OpsInvoice invoice, Map<String, BigDecimal> amounts) {
    RemittanceBatch batch = key.batch();
    return new BusinessEvent(
        type,
        batch.getCompanyId(),
        key.branchId(),
        key.valueDate(),
        batch.getCurrency(),
        RemittanceSettings.MODULE,
        key.sourceRef(),
        batch.getBatchNo(),
        batch.getInsurerCode(),
        invoice == null ? null : invoice.getClassification().productLine(),
        invoice == null ? null : invoice.getClassification().costCenter(),
        (invoice == null
                ? "Early remittance incentive "
                : "Remittance " + invoice.getInvoiceNo() + " ")
            + batch.getBatchNo(),
        amounts,
        null);
  }

  private void issueReceipts(RemittanceBatch batch, String insurerName, LocalDate today) {
    List<String> messages = new ArrayList<>();
    if (batch.getTotals().commission().signum() > 0) {
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
    if (batch.getTotals().incentive().signum() > 0) {
      IssuedReceipt or =
          issue(
              batch,
              insurerName,
              today,
              "INCENTIVE",
              a -> new BigDecimal[] {a.incentive(), a.incentiveVat(), BigDecimal.ZERO});
      batch.incentiveReceipt(or.status().name(), or.receiptNo());
      messages.add("Incentive OR: " + or.message());
    }
    batch.receiptMessage(messages.isEmpty() ? null : String.join("; ", messages));
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
    return receipts.issueOfficialReceipt(
        new ReceiptIssuer.ReceiptRequest(
            batch.getCompanyId(),
            orType,
            new ReceiptIssuer.Payee(batch.getInsurerCode(), insurerName),
            batch.getCurrency(),
            today,
            lines,
            new ReceiptIssuer.Source(
                RemittanceSettings.MODULE,
                batch.getBatchNo() + ":" + orType,
                null,
                orType + " of remittance batch " + batch.getBatchNo())));
  }

  private record EventKey(
      RemittanceBatch batch, Long branchId, LocalDate valueDate, String sourceRef) {}
}
