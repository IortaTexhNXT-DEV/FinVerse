package com.iortatechnxt.brokerverse.tax.api.dto;

import com.iortatechnxt.brokerverse.tax.domain.ReturnStatus;
import com.iortatechnxt.brokerverse.tax.domain.TaxRemittance;
import com.iortatechnxt.brokerverse.tax.domain.TaxReturn;
import com.iortatechnxt.brokerverse.tax.domain.TaxReturnLine;
import com.iortatechnxt.brokerverse.tax.domain.WorksheetKind;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Tax return with its lines and remittance.
 *
 * @param id id
 * @param companyId company
 * @param returnNo return number
 * @param formCode form
 * @param worksheet worksheet
 * @param periodStart period start
 * @param periodEnd period end
 * @param periodLabel period label
 * @param dueDate due date
 * @param taxBase tax base
 * @param taxDue tax due
 * @param taxCredits credits
 * @param amountPayable amount payable
 * @param excessCredit excess credit carried over
 * @param status status
 * @param overdue past due and not paid (today)
 * @param preparedBy preparer
 * @param preparedAt preparation time
 * @param filedBy filer
 * @param filedOn filing date
 * @param filingReference filing reference
 * @param statusReason cancellation reason
 * @param lines return lines (detail only)
 * @param remittance remittance, null until paid
 */
public record TaxReturnResponse(
    Long id,
    Long companyId,
    String returnNo,
    String formCode,
    WorksheetKind worksheet,
    LocalDate periodStart,
    LocalDate periodEnd,
    String periodLabel,
    LocalDate dueDate,
    BigDecimal taxBase,
    BigDecimal taxDue,
    BigDecimal taxCredits,
    BigDecimal amountPayable,
    BigDecimal excessCredit,
    ReturnStatus status,
    boolean overdue,
    String preparedBy,
    Instant preparedAt,
    String filedBy,
    LocalDate filedOn,
    String filingReference,
    String statusReason,
    List<Line> lines,
    Remittance remittance) {

  /**
   * Maps a return for a list (no lines, no remittance).
   *
   * @param r return
   * @param today reference date for the overdue flag
   * @return response
   */
  public static TaxReturnResponse summary(TaxReturn r, LocalDate today) {
    return of(r, today, List.of(), null);
  }

  /**
   * Maps a return with its lines and remittance.
   *
   * @param r return (lines loaded)
   * @param today reference date for the overdue flag
   * @param remittance remittance, null until paid
   * @return response
   */
  public static TaxReturnResponse detail(TaxReturn r, LocalDate today, TaxRemittance remittance) {
    return of(
        r,
        today,
        r.getLines().stream().map(Line::from).toList(),
        remittance == null ? null : Remittance.from(remittance));
  }

  private static TaxReturnResponse of(
      TaxReturn r, LocalDate today, List<Line> lines, Remittance remittance) {
    return new TaxReturnResponse(
        r.getId(),
        r.getCompanyId(),
        r.getReturnNo(),
        r.getFormCode(),
        r.getWorksheet(),
        r.getPeriodStart(),
        r.getPeriodEnd(),
        r.period().label(),
        r.getDueDate(),
        r.getTaxBase(),
        r.getTaxDue(),
        r.getTaxCredits(),
        r.getAmountPayable(),
        r.getExcessCredit(),
        r.getStatus(),
        r.isOverdueOn(today),
        r.getPreparedBy(),
        r.getPreparedAt(),
        r.getFiledBy(),
        r.getFiledOn(),
        r.getFilingReference(),
        r.getStatusReason(),
        lines,
        remittance);
  }

  /**
   * Return line.
   *
   * @param lineNo line number
   * @param lineCode line code
   * @param description description
   * @param baseAmount tax base
   * @param amount amount
   */
  public record Line(
      int lineNo, String lineCode, String description, BigDecimal baseAmount, BigDecimal amount) {

    static Line from(TaxReturnLine l) {
      return new Line(
          l.getLineNo(), l.getLineCode(), l.getDescription(), l.getBaseAmount(), l.getAmount());
    }
  }

  /**
   * Remittance of the return.
   *
   * @param paidOn payment date
   * @param amount amount paid
   * @param payableCleared tax payable cleared
   * @param creditApplied credits applied
   * @param bankAccountCode bank account
   * @param paymentReference payment reference
   * @param journalBatchNo journal
   * @param late paid after the due date
   */
  public record Remittance(
      LocalDate paidOn,
      BigDecimal amount,
      BigDecimal payableCleared,
      BigDecimal creditApplied,
      String bankAccountCode,
      String paymentReference,
      String journalBatchNo,
      boolean late) {

    static Remittance from(TaxRemittance r) {
      return new Remittance(
          r.getPaidOn(),
          r.getAmount(),
          r.getPayableCleared(),
          r.getCreditApplied(),
          r.getBankAccountCode(),
          r.getPaymentReference(),
          r.getJournalBatchNo(),
          r.isLate());
    }
  }
}
