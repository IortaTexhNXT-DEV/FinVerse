package com.iortatechnxt.brokerverse.opsledger.service.port;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Port: issue a Head Office Official Receipt for BDOI income (CSHID.002/006/007). Implemented by
 * cashiering; called by remittance (commission OR per settlement batch, incentive OR) and
 * commission (DP commission OR). The default adapter, while cashiering is not installed, records a
 * hand-off and returns {@link Status#DEFERRED} without a number: the OR is then issued by hand.
 */
public interface ReceiptIssuer {

  /**
   * Issues an OR, idempotent on (source module, source reference).
   *
   * @param request payee, OR type, lines and source
   * @return the OR number, or a deferral
   */
  IssuedReceipt issueOfficialReceipt(ReceiptRequest request);

  /** Outcome of an OR request. */
  enum Status {
    /** OR issued and posted. */
    ISSUED,
    /** Not issued now (no cashiering module): handed over for manual issuance. */
    DEFERRED
  }

  /**
   * An OR to issue.
   *
   * @param companyId company
   * @param orType OR type (LOV {@code OR_TYPE}, e.g. COMMISSION, INCENTIVE)
   * @param payee payee party code and name
   * @param currency currency
   * @param receiptDate receipt date
   * @param lines one line per invoice or commission item (CSHID.007 consolidation per insurer and
   *     certificate)
   * @param source module and reference (idempotency) with the certificate and remarks
   */
  record ReceiptRequest(
      Long companyId,
      String orType,
      Payee payee,
      String currency,
      LocalDate receiptDate,
      List<ReceiptLine> lines,
      Source source) {

    /** Defensive copy. */
    public ReceiptRequest {
      lines = List.copyOf(lines);
    }

    /**
     * Total gross of the lines.
     *
     * @return sum of the gross amounts
     */
    public BigDecimal gross() {
      return lines.stream().map(ReceiptLine::gross).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
  }

  /**
   * The payee of an OR.
   *
   * @param partyCode party code (insurer, client)
   * @param name name printed on the OR
   */
  record Payee(String partyCode, String name) {}

  /**
   * Where an OR request comes from.
   *
   * @param module source module (e.g. REMITTANCE)
   * @param reference source reference (e.g. settlement batch number)
   * @param certificateRef BIR certificate of the withholding tax, may be null
   * @param remarks remarks, may be null
   */
  record Source(String module, String reference, String certificateRef, String remarks) {}

  /**
   * One OR line.
   *
   * @param invoiceNo invoice, may be null for non-invoice income
   * @param insurerCode insurer of the commission, may be null
   * @param gross gross amount
   * @param vat output VAT
   * @param wtax withholding tax
   * @param description description
   */
  record ReceiptLine(
      String invoiceNo,
      String insurerCode,
      BigDecimal gross,
      BigDecimal vat,
      BigDecimal wtax,
      String description) {

    /**
     * Net amount received.
     *
     * @return gross + VAT - withholding tax
     */
    public BigDecimal net() {
      return gross.add(vat).subtract(wtax);
    }
  }

  /**
   * Result of an OR request.
   *
   * @param status issued or deferred
   * @param receiptNo OR number, null when deferred
   * @param journalBatchNo GL journal, null when deferred
   * @param message what happened
   */
  record IssuedReceipt(Status status, String receiptNo, String journalBatchNo, String message) {}
}
