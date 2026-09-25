package com.iortatechnxt.brokerverse.payrequest.api.dto;

import com.iortatechnxt.brokerverse.payrequest.domain.ExpenseValues;
import com.iortatechnxt.brokerverse.payrequest.domain.RefundLineValues;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestContent;
import com.iortatechnxt.brokerverse.payrequest.service.LiquidationService;
import com.iortatechnxt.brokerverse.payrequest.service.RequestDrafts;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;

/** Request bodies of the refund and cash-advance request endpoints (MKT 1.2-2.26, Appendix D). */
public interface PayRequestInputs {

  /**
   * Request-date range of the work list search, bound from the query parameters {@code from} and
   * {@code to}.
   *
   * @param from first request date
   * @param to last request date
   */
  record RequestDates(
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {}

  /**
   * One account of a Refund Request Form.
   *
   * @param arNo AR number
   * @param clientCode client number
   * @param assuredName assured or client name
   * @param invoiceNo invoice, may be blank
   * @param amount amount
   * @param reasonCode reason (REFUND_REASON)
   * @param branchUnit branch or unit
   * @param categoryA category A
   * @param categoryB category B
   * @param accountName account or check name
   */
  record RefundLineInput(
      @NotBlank @Size(max = 40) String arNo,
      @NotBlank @Size(max = 30) String clientCode,
      @NotBlank @Size(max = 250) String assuredName,
      @Size(max = 40) String invoiceNo,
      @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
      @NotBlank @Size(max = 40) String reasonCode,
      @Size(max = 60) String branchUnit,
      @Size(max = 40) String categoryA,
      @Size(max = 40) String categoryB,
      @Size(max = 250) String accountName) {

    /**
     * The line values.
     *
     * @return values
     */
    public RefundLineValues values() {
      return new RefundLineValues(
          arNo,
          clientCode,
          assuredName,
          invoiceNo,
          amount,
          reasonCode,
          branchUnit,
          categoryA,
          categoryB,
          accountName);
    }
  }

  /**
   * A Refund Request Form.
   *
   * @param segment segment
   * @param referenceText reference, e.g. "2025_331 Refund"
   * @param requestingUnit requesting unit
   * @param purpose purpose or remarks
   * @param currency currency
   * @param paymentMode payment mode (PRQ_PAYMENT_MODE)
   * @param accountNo BDO account number (credit to account)
   * @param accountName account or check name
   * @param lines accounts
   */
  record RefundInput(
      @Size(max = 40) String segment,
      @Size(max = 80) String referenceText,
      @Size(max = 60) String requestingUnit,
      @Size(max = 500) String purpose,
      @Size(max = 3) String currency,
      @NotBlank @Size(max = 20) String paymentMode,
      @Size(max = 40) String accountNo,
      @Size(max = 250) String accountName,
      @NotEmpty @Size(max = 50) List<@Valid RefundLineInput> lines) {

    /**
     * The form.
     *
     * @return draft
     */
    public RequestDrafts.Refund draft() {
      return new RequestDrafts.Refund(
          new RequestContent(segment, referenceText, requestingUnit, null, purpose, currency),
          new RequestDrafts.Payout(paymentMode, accountNo, accountName),
          lines.stream().map(RefundLineInput::values).toList());
    }
  }

  /**
   * A Request for Payment of a cash advance.
   *
   * @param segment segment
   * @param referenceText reference
   * @param requestingUnit requesting unit
   * @param rfpType RFP type (PRQ_RFP_TYPE)
   * @param purpose purpose
   * @param currency currency
   * @param employeeNo employee number
   * @param employeeName employee name
   * @param paymentMode payment mode
   * @param accountNo BDO account number (credit to account)
   * @param accountName account or check name
   * @param amount amount
   */
  record CashAdvanceInput(
      @Size(max = 40) String segment,
      @Size(max = 80) String referenceText,
      @Size(max = 60) String requestingUnit,
      @Size(max = 30) String rfpType,
      @NotBlank @Size(max = 500) String purpose,
      @Size(max = 3) String currency,
      @NotBlank @Size(max = 30) String employeeNo,
      @NotBlank @Size(max = 250) String employeeName,
      @NotBlank @Size(max = 20) String paymentMode,
      @Size(max = 40) String accountNo,
      @Size(max = 250) String accountName,
      @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount) {

    /**
     * The form.
     *
     * @return draft
     */
    public RequestDrafts.CashAdvance draft() {
      return new RequestDrafts.CashAdvance(
          new RequestContent(segment, referenceText, requestingUnit, rfpType, purpose, currency),
          employeeNo,
          employeeName,
          new RequestDrafts.Payout(paymentMode, accountNo, accountName),
          amount);
    }
  }

  /**
   * A check cancellation.
   *
   * @param targetRequestNo paid request
   * @param checkNo check number
   * @param reasonCode reason (DISB_CANCEL_REASON)
   * @param remarks remarks
   */
  record CheckCancellationInput(
      @NotBlank @Size(max = 30) String targetRequestNo,
      @Size(max = 40) String checkNo,
      @NotBlank @Size(max = 40) String reasonCode,
      @Size(max = 500) String remarks) {

    /**
     * The form.
     *
     * @return draft
     */
    public RequestDrafts.CheckCancellation draft() {
      return new RequestDrafts.CheckCancellation(targetRequestNo, checkNo, reasonCode, remarks);
    }
  }

  /**
   * A comment given with an action.
   *
   * @param comment comment, may be blank
   */
  record CommentInput(@Size(max = 500) String comment) {}

  /**
   * Assignment of a refund to a preparer.
   *
   * @param username preparer
   * @param comment comment
   */
  record AssignInput(@NotBlank @Size(max = 50) String username, @Size(max = 500) String comment) {}

  /**
   * Result of a handed-over validation.
   *
   * @param confirmed true when confirmed
   * @param newArNo new AR number (Cashiering)
   * @param remarks remarks
   */
  record ValidationResultInput(
      boolean confirmed, @Size(max = 40) String newArNo, @Size(max = 500) String remarks) {}

  /**
   * One fieldwork day of a liquidation.
   *
   * @param fieldworkDate date
   * @param particulars particulars
   * @param perDiem per diem
   * @param representation representation
   * @param transport transportation
   * @param lodging lodging
   * @param others others
   */
  record ExpenseInput(
      @NotNull LocalDate fieldworkDate,
      @NotBlank @Size(max = 250) String particulars,
      @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal perDiem,
      @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal representation,
      @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal transport,
      @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal lodging,
      @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal others) {

    /**
     * The day's values.
     *
     * @return values
     */
    public ExpenseValues values() {
      return new ExpenseValues(
          fieldworkDate, particulars, perDiem, representation, transport, lodging, others);
    }
  }

  /**
   * A liquidation form.
   *
   * @param jobLevel job level
   * @param costCenter cost centre
   * @param remarks remarks
   * @param days fieldwork days
   */
  record LiquidationInput(
      @Size(max = 40) String jobLevel,
      @Size(max = 20) String costCenter,
      @Size(max = 500) String remarks,
      @Size(max = 60) List<@Valid ExpenseInput> days) {

    /**
     * The form.
     *
     * @return draft
     */
    public LiquidationService.Draft draft() {
      return new LiquidationService.Draft(
          jobLevel,
          costCenter,
          remarks,
          days == null ? List.of() : days.stream().map(ExpenseInput::values).toList());
    }
  }

  /**
   * The account of a liquidation event role.
   *
   * @param role role (PER_DIEM ... CASH)
   * @param accountCode GL account
   */
  record LiquidationAccountInput(
      @NotBlank @Size(max = 20) String role, @NotBlank @Size(max = 30) String accountCode) {}
}
