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
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
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
    LocalDate today = BusinessClock.today(clock);
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
    return reclass(
        invoice(tag),
        tag.getAmount(),
        CWT + tag.getReference(),
        "BIR 2307 reclass " + tag.getReference());
  }

  /**
   * Reclassifies an amount of the premium receivable of an invoice to PR 2307.
   *
   * @param invoice invoice (collections loaded)
   * @param amount amount
   * @param ref source reference
   * @param narration narration
   * @return journal batch
   */
  public String reclass(OpsInvoice invoice, BigDecimal amount, String ref, String narration) {
    Map<LedgerComponent, BigDecimal> taken = new EnumMap<>(LedgerComponent.class);
    BigDecimal left = amount;
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
          ref + ": the invoice has less premium outstanding than the 2307 amount");
    }
    Map<String, BigDecimal> amounts = new LinkedHashMap<>();
    amounts.put("PR2307", amount);
    amounts.putAll(CashieringPosting.prAmounts(taken, false));
    String batch =
        posting.publish(
            ApplicationService.context(invoice, BusinessClock.today(clock), narration),
            CashieringPosting.CWT_RECLASS,
            ref,
            invoice.getLegacy().ledgerContext().route(amounts));
    Map<LedgerComponent, BigDecimal> movement = new EnumMap<>(LedgerComponent.class);
    taken.forEach((c, v) -> movement.put(c, v.negate()));
    movement.put(LedgerComponent.PR2307, amount);
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
   * Releases the 2307 certificate to the insurer (DBMID.001): PR2307 offset against DTIP.
   *
   * @param tag tag
   * @return journal batch
   */
  public String dtipOffset(CwtTag tag) {
    return dtipOffset(
        invoice(tag),
        tag.getAmount(),
        CWT + tag.getReference() + ":DTIP",
        "BIR 2307 released " + tag.getReference());
  }

  /**
   * Offsets an amount of PR 2307 of an invoice against its due to insurer.
   *
   * @param invoice invoice
   * @param amount amount
   * @param ref source reference
   * @param narration narration
   * @return journal batch
   */
  public String dtipOffset(OpsInvoice invoice, BigDecimal amount, String ref, String narration) {
    String batch =
        posting.publish(
            ApplicationService.context(invoice, BusinessClock.today(clock), narration),
            CashieringPosting.CWT_DTIP_OFFSET,
            ref,
            invoice.getLegacy().ledgerContext().route(Map.of("DTIP", amount, "PR2307", amount)),
            invoice
                .getLegacy()
                .ledgerContext()
                .routeParties(
                    Map.of("DTIP", invoice.getInsurerCode(), "PR2307", invoice.getClientCode())));
    Map<LedgerComponent, BigDecimal> movement = new EnumMap<>(LedgerComponent.class);
    movement.put(LedgerComponent.DTIP, amount);
    movement.put(LedgerComponent.PR2307, amount);
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
            BusinessClock.today(clock),
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
