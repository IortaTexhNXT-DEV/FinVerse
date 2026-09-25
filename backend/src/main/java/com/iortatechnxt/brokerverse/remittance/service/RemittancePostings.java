package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.BookRates;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceAmounts;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The accounting events of a remittance batch, shared by its approval and by the reversal of a
 * cancelled DV (OPERATIONS_DESIGN 5 rows 12-13; ACCOUNTING_DISBURSEMENT_DESIGN 6 rows 15-17):
 * source references {@code RMB:<cycle reference>:<suffix>}, the amounts of each event, and the
 * publication at the BOOK rate. The cancellation publishes the same amounts with the opposite sign
 * under {@code <reference>:CANCEL} (the engine swaps the sides of negative amounts).
 */
@Component
@Transactional(propagation = Propagation.MANDATORY)
public class RemittancePostings {

  /** Remittance event, per invoice line. */
  public static final String REMITTANCE_EVENT = "OPS_REMITTANCE";

  /** Early-incentive event, per batch. */
  public static final String INCENTIVE_EVENT = "OPS_REMIT_INCENTIVE";

  /** CPC2 event, per batch (DIS 3.29.2). */
  public static final String CPC2_EVENT = "OPS_REMIT_CPC2";

  /** Deduction event, per deduction and cycle (ACSL 2.9.2). */
  public static final String DEDUCTION_EVENT = "OPS_REMIT_DEDUCTION";

  /** Suffix of the reversal references. */
  public static final String CANCEL = ":CANCEL";

  private static final String PREFIX = "RMB:";
  private static final String DUE_FOR_DISBURSEMENT = "DUE_FOR_DISBURSEMENT";
  private static final String OUTPUT_VAT = "OUTPUT_VAT";

  private final AccountingEventPublisher accounting;
  private final BookRates rates;

  /**
   * Creates the helper.
   *
   * @param accounting accounting engine
   * @param rates BOOK rates
   */
  public RemittancePostings(AccountingEventPublisher accounting, BookRates rates) {
    this.accounting = accounting;
    this.rates = rates;
  }

  /**
   * The source reference of a posting of the batch's current cycle.
   *
   * @param batch batch
   * @param suffix invoice number, {@code INC}, {@code CPC2} or a deduction number
   * @return reference
   */
  public static String sourceRef(RemittanceBatch batch, String suffix) {
    return PREFIX + batch.cycleReference() + ":" + suffix;
  }

  /**
   * The source reference of the ledger movements of the batch's current cycle.
   *
   * @param batch batch
   * @return reference
   */
  public static String movementRef(RemittanceBatch batch) {
    return PREFIX + batch.cycleReference();
  }

  /**
   * Amounts of the remittance event of a line.
   *
   * @param a line amounts
   * @param sign 1 to post, -1 to reverse
   * @return amounts by component
   */
  public static Map<String, BigDecimal> lineAmounts(RemittanceAmounts a, int sign) {
    Map<String, BigDecimal> amounts = new LinkedHashMap<>();
    amounts.put("DTIP", signed(a.dtip(), sign));
    amounts.put("CWT", signed(a.wtax(), sign));
    amounts.put("COMMISSION_RECEIVABLE", signed(a.commissionReceivable(), sign));
    amounts.put(DUE_FOR_DISBURSEMENT, signed(a.netDue(), sign));
    return amounts;
  }

  /**
   * Amounts of the early-incentive event of a batch.
   *
   * @param t batch totals
   * @param sign 1 to post, -1 to reverse
   * @return amounts by component
   */
  public static Map<String, BigDecimal> incentiveAmounts(RemittanceAmounts t, int sign) {
    Map<String, BigDecimal> amounts = new LinkedHashMap<>();
    amounts.put(DUE_FOR_DISBURSEMENT, signed(t.incentiveTotal(), sign));
    amounts.put("INCENTIVE_INCOME", signed(t.incentive(), sign));
    amounts.put(OUTPUT_VAT, signed(t.incentiveVat(), sign));
    return amounts;
  }

  /**
   * Amounts of the CPC2 event of a batch (DIS 3.29.2).
   *
   * @param t batch totals
   * @param sign 1 to post, -1 to reverse
   * @return amounts by component
   */
  public static Map<String, BigDecimal> cpc2Amounts(RemittanceAmounts t, int sign) {
    Map<String, BigDecimal> amounts = new LinkedHashMap<>();
    amounts.put("GROSS", signed(t.cpc2Total(), sign));
    amounts.put("CPC2_INCOME", signed(t.cpc2(), sign));
    amounts.put(OUTPUT_VAT, signed(t.cpc2Vat(), sign));
    return amounts;
  }

  /**
   * Publishes an event of a batch at the BOOK rate.
   *
   * @param posting event type, key, invoice, amounts and narration
   * @return journal batch number
   */
  public String publish(Posting posting) {
    RemittanceBatch batch = posting.batch();
    OpsInvoice invoice = posting.invoice();
    BusinessEvent event =
        new BusinessEvent(
            posting.type(),
            batch.getCompanyId(),
            posting.branchId(),
            posting.valueDate(),
            batch.getCurrency(),
            RemittanceSettings.MODULE,
            posting.sourceRef(),
            batch.getBatchNo(),
            batch.getInsurerCode(),
            invoice == null ? null : invoice.getClassification().productLine(),
            invoice == null ? null : invoice.getClassification().costCenter(),
            posting.narration(),
            posting.amounts(),
            null);
    return accounting.publish(rates.price(event)).getBatchNo();
  }

  private static BigDecimal signed(BigDecimal amount, int sign) {
    return sign < 0 ? amount.negate() : amount;
  }

  /**
   * One event of a batch.
   *
   * @param type event type
   * @param batch batch
   * @param branchId branch
   * @param valueDate value date
   * @param sourceRef source reference (idempotency key)
   * @param invoice invoice of a line event, null for batch events
   * @param amounts amounts by component
   * @param narration narration
   */
  public record Posting(
      String type,
      RemittanceBatch batch,
      Long branchId,
      LocalDate valueDate,
      String sourceRef,
      OpsInvoice invoice,
      Map<String, BigDecimal> amounts,
      String narration) {}
}
