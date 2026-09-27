package com.iortatechnxt.brokerverse.payrequest.api.dto;

import com.iortatechnxt.brokerverse.payrequest.domain.CancellationTarget;
import com.iortatechnxt.brokerverse.payrequest.domain.DisbursementTrack;
import com.iortatechnxt.brokerverse.payrequest.domain.Liquidation;
import com.iortatechnxt.brokerverse.payrequest.domain.LiquidationAccount;
import com.iortatechnxt.brokerverse.payrequest.domain.LiquidationLine;
import com.iortatechnxt.brokerverse.payrequest.domain.LiquidationStatus;
import com.iortatechnxt.brokerverse.payrequest.domain.Payee;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequest;
import com.iortatechnxt.brokerverse.payrequest.domain.RefundLine;
import com.iortatechnxt.brokerverse.payrequest.domain.RefundValidation;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestContent;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestKind;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestStage;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestTrail;
import com.iortatechnxt.brokerverse.payrequest.domain.ValidationStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Response records of the refund and cash-advance request endpoints (MKT 1.3-2.26). */
public interface PayRequestViews {

  /**
   * A request in the work list (MKT 1.3.0, 1.18.0).
   *
   * @param id id
   * @param requestNo request number
   * @param kind kind
   * @param stage stage
   * @param requestDate request date
   * @param payeeName payee
   * @param payeeCode payee code
   * @param paymentMode payment mode
   * @param currency currency
   * @param amount amount
   * @param validationRequired refund of a cancelled policy
   * @param dvNo DV number
   * @param disbursementStatus gateway status
   * @param instrumentStatus instrument status
   * @param createdBy requester
   * @param createdAt created at
   */
  record RequestSummary(
      Long id,
      String requestNo,
      RequestKind kind,
      RequestStage stage,
      LocalDate requestDate,
      String payeeName,
      String payeeCode,
      String paymentMode,
      String currency,
      BigDecimal amount,
      boolean validationRequired,
      String dvNo,
      String disbursementStatus,
      String instrumentStatus,
      String createdBy,
      Instant createdAt) {

    /**
     * Maps a request.
     *
     * @param r request
     * @return summary
     */
    public static RequestSummary from(PaymentRequest r) {
      DisbursementTrack t = r.trackOrNone();
      return new RequestSummary(
          r.getId(),
          r.getRequestNo(),
          r.getKind(),
          r.getStage(),
          r.getRequestDate(),
          r.getPayee().name(),
          r.getPayee().code(),
          r.getPayee().mode(),
          r.getContent().currency(),
          r.getAmount(),
          r.isValidationRequired(),
          t.dvNo(),
          t.status(),
          t.instrumentStatus(),
          r.getCreatedBy(),
          r.getCreatedAt());
    }
  }

  /**
   * One refund account.
   *
   * @param lineNo line
   * @param arNo AR number
   * @param clientCode client
   * @param assuredName assured
   * @param invoiceNo invoice
   * @param rootInvoiceNo root invoice
   * @param amount amount
   * @param reasonCode reason
   * @param branchUnit branch or unit
   * @param categoryA category A
   * @param categoryB category B
   * @param accountName account or check name
   * @param cancelledPolicy cancelled policy (validation needed)
   */
  record LineView(
      int lineNo,
      String arNo,
      String clientCode,
      String assuredName,
      String invoiceNo,
      String rootInvoiceNo,
      BigDecimal amount,
      String reasonCode,
      String branchUnit,
      String categoryA,
      String categoryB,
      String accountName,
      boolean cancelledPolicy) {

    /**
     * Maps a line.
     *
     * @param l line
     * @return view
     */
    public static LineView from(RefundLine l) {
      return new LineView(
          l.getLineNo(),
          l.getArNo(),
          l.getClientCode(),
          l.getAssuredName(),
          l.getInvoiceNo(),
          l.getRootInvoiceNo(),
          l.getAmount(),
          l.getReasonCode(),
          l.getBranchUnit(),
          l.getCategoryA(),
          l.getCategoryB(),
          l.getAccountName(),
          l.isCancelledPolicy());
    }
  }

  /**
   * One request with its form, trail, payment track, lines and liquidation (MKT 1.5.0, 2.26.0).
   *
   * @param id id
   * @param requestNo request number
   * @param kind kind
   * @param stage stage
   * @param companyId company
   * @param requestDate request date
   * @param content header fields
   * @param payee payee and mode
   * @param amount amount
   * @param validationRequired refund of a cancelled policy
   * @param validationRound current validation round
   * @param target check of a cancellation
   * @param trail who moved it
   * @param track payment in Disbursement
   * @param payoutRecorded CA / SA added to the client record
   * @param lines refund accounts
   * @param liquidation liquidation of a cash advance, may be null
   * @param createdBy requester
   * @param createdAt created at
   */
  record RequestView(
      Long id,
      String requestNo,
      RequestKind kind,
      RequestStage stage,
      Long companyId,
      LocalDate requestDate,
      RequestContent content,
      Payee payee,
      BigDecimal amount,
      boolean validationRequired,
      int validationRound,
      CancellationTarget target,
      RequestTrail trail,
      DisbursementTrack track,
      boolean payoutRecorded,
      List<LineView> lines,
      LiquidationView liquidation,
      String createdBy,
      Instant createdAt) {

    /**
     * Maps a request.
     *
     * @param r request (lines loaded)
     * @param liquidation its liquidation, or null
     * @return view
     */
    public static RequestView from(PaymentRequest r, Liquidation liquidation) {
      return new RequestView(
          r.getId(),
          r.getRequestNo(),
          r.getKind(),
          r.getStage(),
          r.getCompanyId(),
          r.getRequestDate(),
          r.getContent(),
          r.getPayee(),
          r.getAmount(),
          r.isValidationRequired(),
          r.getValidationRound(),
          r.targetOrNone(),
          r.trailOrNone(),
          r.trackOrNone(),
          r.isPayoutRecorded(),
          r.getLines().stream().map(LineView::from).toList(),
          liquidation == null ? null : LiquidationView.from(liquidation),
          r.getCreatedBy(),
          r.getCreatedAt());
    }
  }

  /**
   * A validation task (MKT 1.11.0).
   *
   * @param id id
   * @param roundNo round
   * @param lineNo refund line
   * @param validator ACSL or CASHIERING
   * @param status status
   * @param ticketRef ACSL case, cashiering reference or hand-off
   * @param message message of the opening
   * @param newArNo new AR number
   * @param remarks result remarks
   * @param completedBy who recorded the result
   * @param completedAt when
   */
  record ValidationView(
      Long id,
      int roundNo,
      int lineNo,
      String validator,
      ValidationStatus status,
      String ticketRef,
      String message,
      String newArNo,
      String remarks,
      String completedBy,
      Instant completedAt) {

    /**
     * Maps a task.
     *
     * @param v task
     * @return view
     */
    public static ValidationView from(RefundValidation v) {
      return new ValidationView(
          v.getId(),
          v.getRoundNo(),
          v.getLineNo(),
          v.getValidator(),
          v.getStatus(),
          v.getTicketRef(),
          v.getMessage(),
          v.getNewArNo(),
          v.getRemarks(),
          v.getCompletedBy(),
          v.getCompletedAt());
    }
  }

  /**
   * A fieldwork day.
   *
   * @param lineNo line
   * @param fieldworkDate date
   * @param particulars particulars
   * @param perDiem per diem
   * @param representation representation
   * @param transport transportation
   * @param lodging lodging
   * @param others others
   * @param total total of the day
   */
  record ExpenseView(
      int lineNo,
      LocalDate fieldworkDate,
      String particulars,
      BigDecimal perDiem,
      BigDecimal representation,
      BigDecimal transport,
      BigDecimal lodging,
      BigDecimal others,
      BigDecimal total) {

    /**
     * Maps a day.
     *
     * @param d day
     * @return view
     */
    public static ExpenseView from(LiquidationLine d) {
      return new ExpenseView(
          d.getLineNo(),
          d.getFieldworkDate(),
          d.getParticulars(),
          d.getPerDiem(),
          d.getRepresentation(),
          d.getTransport(),
          d.getLodging(),
          d.getOthers(),
          d.total());
    }
  }

  /**
   * A cash-advance liquidation (Appendix D).
   *
   * @param id id
   * @param liquidationNo number
   * @param status status
   * @param jobLevel job level
   * @param costCenter cost centre
   * @param remarks remarks
   * @param totalExpenses expenses
   * @param cashAdvanced cash advanced
   * @param overShort over / (short)
   * @param submittedBy submitter
   * @param postedBy checker who posted
   * @param postedAt posted at
   * @param journalBatchNo journal
   * @param days fieldwork days
   */
  record LiquidationView(
      Long id,
      String liquidationNo,
      LiquidationStatus status,
      String jobLevel,
      String costCenter,
      String remarks,
      BigDecimal totalExpenses,
      BigDecimal cashAdvanced,
      BigDecimal overShort,
      String submittedBy,
      String postedBy,
      Instant postedAt,
      String journalBatchNo,
      List<ExpenseView> days) {

    /**
     * Maps a liquidation.
     *
     * @param l liquidation (lines loaded)
     * @return view
     */
    public static LiquidationView from(Liquidation l) {
      return new LiquidationView(
          l.getId(),
          l.getLiquidationNo(),
          l.getStatus(),
          l.getJobLevel(),
          l.getCostCenter(),
          l.getRemarks(),
          l.getTotalExpenses(),
          l.getCashAdvanced(),
          l.getOverShort(),
          l.getSubmittedBy(),
          l.getPostedBy(),
          l.getPostedAt(),
          l.getJournalBatchNo(),
          l.getLines().stream().map(ExpenseView::from).toList());
    }
  }

  /**
   * The account of a liquidation event role.
   *
   * @param role role
   * @param accountCode GL account
   */
  record AccountView(String role, String accountCode) {

    /**
     * Maps a configuration row.
     *
     * @param a row
     * @return view
     */
    public static AccountView from(LiquidationAccount a) {
      return new AccountView(a.getAccountRole(), a.getAccountCode());
    }
  }
}
