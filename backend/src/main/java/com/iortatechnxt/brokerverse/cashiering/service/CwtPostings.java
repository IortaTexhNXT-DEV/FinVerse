package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.Application;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ApplicationSource;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentMode;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptSource;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.UnappliedOrigin;
import com.iortatechnxt.brokerverse.cashiering.domain.CwtBatch;
import com.iortatechnxt.brokerverse.cashiering.domain.CwtTag;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt.ReceiptTender;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied.UnappliedSpec;
import com.iortatechnxt.brokerverse.cashiering.service.ApplicationService.ApplyOptions;
import com.iortatechnxt.brokerverse.cashiering.service.CashReceiptService.ArIssue;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.domain.DisbursementRequest;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest;
import com.iortatechnxt.brokerverse.opsledger.service.MovementRequest.DocumentRefs;
import com.iortatechnxt.brokerverse.opsledger.service.port.DisbursementGateway;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Postings of the BIR 2307 flow (OPERATIONS_DESIGN section 5 rows 3-4): the cash path AR and its
 * application (CSHID.026), the reclass of the 2% portion from PR to PR2307 at validation ({@code
 * OPS_CWT_RECLASS}, ledger {@code CWT_RECLASS}), the payment request to Disbursement and the DTIP
 * offset at release to the insurer ({@code OPS_CWT_DTIP_OFFSET}, PR2307 and DTIP settled).
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class CwtPostings {

  private static final String CWT = "CWT:";

  private final InvoiceLedgerQueryService ledgerQuery;
  private final InvoiceLedgerService ledger;
  private final CashieringPosting posting;
  private final CashReceiptService receipts;
  private final ApplicationService applier;
  private final UnappliedService unapplied;
  private final DisbursementGateway disbursement;
  private final Clock clock;

  /**
   * Creates the postings.
   *
   * @param ledgerQuery invoice ledger reads
   * @param ledger invoice ledger writes
   * @param posting accounting events
   * @param receipts receipts
   * @param applier application engine
   * @param unapplied unapplied workbench
   * @param disbursement Disbursement gateway
   * @param clock clock
   */
  public CwtPostings(
      InvoiceLedgerQueryService ledgerQuery,
      InvoiceLedgerService ledger,
      CashieringPosting posting,
      CashReceiptService receipts,
      ApplicationService applier,
      UnappliedService unapplied,
      DisbursementGateway disbursement,
      Clock clock) {
    this.ledgerQuery = ledgerQuery;
    this.ledger = ledger;
    this.posting = posting;
    this.receipts = receipts;
    this.applier = applier;
    this.unapplied = unapplied;
    this.disbursement = disbursement;
    this.clock = clock;
  }

  /**
   * Cash path: AR for the 2% and its application beyond the 98% cap.
   *
   * @param tag tag
   * @param branchId receiving branch
   * @return AR number
   */
  public String cashPath(CwtTag tag, Long branchId) {
    OpsInvoice invoice = invoice(tag);
    LocalDate today = LocalDate.now(clock);
    Receipt ar =
        receipts.issueAr(
            new ArIssue(
                tag.getCompanyId(),
                branchId,
                "OTC",
                today,
                tag.getClientCode(),
                invoice.getPayorName() != null ? invoice.getPayorName() : invoice.getAssuredName(),
                invoice.getAssuredName(),
                invoice.getClassification().salesUnit(),
                invoice.getCurrency(),
                tag.getAmount(),
                new ReceiptTender(
                    PaymentMode.CASH,
                    null,
                    null,
                    null,
                    null,
                    ReceiptSource.CWT,
                    CashieringSettings.MODULE,
                    CWT + tag.getReference(),
                    "BIR 2307 cash path")));
    BigDecimal applied =
        applier
            .apply(
                invoice,
                tag.getAmount(),
                new Application.Origin(ar.getId(), null, ApplicationSource.CWT, tag.getReference()),
                new ApplyOptions(today, false, ar.getReceiptNo(), true))
            .map(Application::getAmount)
            .orElse(BigDecimal.ZERO);
    BigDecimal left = tag.getAmount().subtract(applied);
    if (left.signum() > 0) {
      unapplied.create(
          tag.getCompanyId(),
          ar.getBranchId(),
          new UnappliedSpec(
              UnappliedOrigin.EXCESS,
              ar.getId(),
              null,
              tag.getInvoiceNo(),
              tag.getClientCode(),
              ar.getPayorName(),
              ar.getSalesUnit(),
              ar.getCurrency(),
              left,
              null,
              CashieringSettings.MODULE,
              CWT + tag.getReference(),
              "2307 cash above the balance"));
    }
    return ar.getReceiptNo();
  }

  /**
   * Reclassifies the 2% portion from the PR to PR2307 (CSHID.027).
   *
   * @param tag tag
   * @return journal batch
   */
  public String reclass(CwtTag tag) {
    OpsInvoice invoice = invoice(tag);
    Map<LedgerComponent, BigDecimal> taken = new EnumMap<>(LedgerComponent.class);
    BigDecimal left = tag.getAmount();
    List<LedgerComponent> order = new ArrayList<>(LedgerComponent.applicationHierarchy());
    Collections.reverse(order);
    for (LedgerComponent c : order) {
      BigDecimal take = invoice.component(c).getBalance().max(BigDecimal.ZERO).min(left);
      if (take.signum() > 0) {
        taken.put(c, take);
        left = left.subtract(take);
      }
    }
    if (left.signum() > 0) {
      throw new BusinessRuleException(
          "CWT_AMOUNT_MISMATCH",
          tag.getReference() + ": the invoice has less premium outstanding than the 2307 amount");
    }
    Map<String, BigDecimal> amounts = new LinkedHashMap<>();
    amounts.put("PR2307", tag.getAmount());
    amounts.putAll(CashieringPosting.prAmounts(taken, false));
    String ref = CWT + tag.getReference();
    String batch =
        posting.publish(
            ApplicationService.context(
                invoice, LocalDate.now(clock), "BIR 2307 reclass " + tag.getReference()),
            CashieringPosting.CWT_RECLASS,
            ref,
            amounts);
    Map<LedgerComponent, BigDecimal> movement = new EnumMap<>(LedgerComponent.class);
    taken.forEach((c, v) -> movement.put(c, v.negate()));
    movement.put(LedgerComponent.PR2307, tag.getAmount());
    record(invoice, MovementType.CWT_RECLASS, ref, movement, batch);
    return batch;
  }

  /**
   * Sends the batch to Disbursement.
   *
   * @param batch batch
   * @param anyInvoiceNo an invoice of the batch (currency)
   * @return Disbursement request number
   */
  public String route(CwtBatch batch, String anyInvoiceNo) {
    OpsInvoice invoice = ledgerQuery.require(anyInvoiceNo);
    return disbursement
        .send(
            batch.getCompanyId(),
            new DisbursementRequest.Spec(
                DisbursementRequest.Type.CWT2307,
                CashieringSettings.MODULE,
                batch.getBatchNo(),
                batch.getInsurerCode(),
                batch.getInsurerCode(),
                invoice.getCurrency(),
                batch.getTotalAmount(),
                "BIR 2307 certificates "
                    + batch.getBatchNo()
                    + " ("
                    + batch.getTagCount()
                    + ") to release",
                null))
        .requestNo();
  }

  /**
   * Settles PR2307 against the premium due to the insurer (DBMID.001).
   *
   * @param tag tag
   * @return journal batch
   */
  public String dtipOffset(CwtTag tag) {
    OpsInvoice invoice = invoice(tag);
    String ref = CWT + tag.getReference() + ":DTIP";
    String batch =
        posting.publish(
            ApplicationService.context(
                invoice, LocalDate.now(clock), "BIR 2307 released " + tag.getReference()),
            CashieringPosting.CWT_DTIP_OFFSET,
            ref,
            Map.of("DTIP", tag.getAmount(), "PR2307", tag.getAmount()),
            Map.of("DTIP", tag.getInsurerCode(), "PR2307", tag.getClientCode()));
    Map<LedgerComponent, BigDecimal> movement = new EnumMap<>(LedgerComponent.class);
    movement.put(LedgerComponent.DTIP, tag.getAmount());
    movement.put(LedgerComponent.PR2307, tag.getAmount());
    record(invoice, MovementType.REMITTED, ref, movement, batch);
    return batch;
  }

  private void record(
      OpsInvoice invoice,
      MovementType type,
      String ref,
      Map<LedgerComponent, BigDecimal> amounts,
      String batch) {
    ledger.post(
        new MovementRequest(
            invoice.getInvoiceNo(),
            type,
            CashieringSettings.MODULE,
            ref,
            LocalDate.now(clock),
            amounts,
            new DocumentRefs(null, null, null, batch),
            "BIR 2307"));
  }

  private OpsInvoice invoice(CwtTag tag) {
    OpsInvoice invoice = ledgerQuery.require(tag.getInvoiceNo());
    invoice.loadCollections();
    return invoice;
  }
}
