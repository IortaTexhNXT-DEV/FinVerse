package com.iortatechnxt.brokerverse.cashiering.api.dto;

import com.iortatechnxt.brokerverse.cashiering.domain.Application;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentMode;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptAction;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptLine;
import com.iortatechnxt.brokerverse.opsledger.domain.LedgerComponent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Request and response records of the receipts (CSHID.001-005/010/014). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class ReceiptDtos {

  private ReceiptDtos() {}

  /**
   * A receipt in a list.
   *
   * @param id id
   * @param receiptNo AR or OR number
   * @param kind AR or OR
   * @param receiptClass class or OR type
   * @param branchId branch
   * @param receiptDate date
   * @param payorCode payor code
   * @param payorName payor
   * @param assuredName assured
   * @param currency currency
   * @param amount amount
   * @param appliedAmount applied
   * @param unappliedAmount not applied
   * @param mode mode of payment
   * @param source source
   * @param status status
   * @param printedCount prints
   */
  public record ReceiptSummaryResponse(
      Long id,
      String receiptNo,
      String kind,
      String receiptClass,
      Long branchId,
      LocalDate receiptDate,
      String payorCode,
      String payorName,
      String assuredName,
      String currency,
      BigDecimal amount,
      BigDecimal appliedAmount,
      BigDecimal unappliedAmount,
      String mode,
      String source,
      String status,
      int printedCount) {

    /**
     * Maps a receipt.
     *
     * @param r receipt
     * @return response
     */
    public static ReceiptSummaryResponse from(Receipt r) {
      return new ReceiptSummaryResponse(
          r.getId(),
          r.getReceiptNo(),
          r.getKind().name(),
          r.getReceiptClass(),
          r.getBranchId(),
          r.getReceiptDate(),
          r.getPayorCode(),
          r.getPayorName(),
          r.getAssuredName(),
          r.getCurrency(),
          r.getAmount(),
          r.getAppliedAmount(),
          r.unappliedAmount(),
          r.getMode().name(),
          r.getSource().name(),
          r.getStatus().name(),
          r.getPrintedCount());
    }
  }

  /**
   * A receipt with its lines, applications and actions.
   *
   * @param summary list facts
   * @param bookRate BOOK rate
   * @param baseAmount amount in the base currency
   * @param gross gross (OR)
   * @param vat VAT (OR)
   * @param wtax withholding tax (OR)
   * @param checkNo check number
   * @param checkBank bank
   * @param checkDate check date
   * @param certificateRef certificate
   * @param sourceModule requesting module
   * @param sourceRef requesting reference
   * @param reinstatedAmount amount reinstated
   * @param journalBatchNo journal
   * @param remarks remarks
   * @param salesUnit marketing unit
   * @param lastPrintedAt last print
   * @param createdBy issued by
   * @param createdAt issued at
   * @param lines OR lines
   * @param applications applications
   * @param actions cancellations and reinstatements
   */
  public record ReceiptResponse(
      ReceiptSummaryResponse summary,
      BigDecimal bookRate,
      BigDecimal baseAmount,
      BigDecimal gross,
      BigDecimal vat,
      BigDecimal wtax,
      String checkNo,
      String checkBank,
      LocalDate checkDate,
      String certificateRef,
      String sourceModule,
      String sourceRef,
      BigDecimal reinstatedAmount,
      String journalBatchNo,
      String remarks,
      String salesUnit,
      Instant lastPrintedAt,
      String createdBy,
      Instant createdAt,
      List<LineResponse> lines,
      List<ApplicationResponse> applications,
      List<ActionResponse> actions) {

    /**
     * Maps a receipt.
     *
     * @param r receipt (lines loaded)
     * @param applications its applications
     * @param actions its actions
     * @return response
     */
    public static ReceiptResponse from(
        Receipt r, List<Application> applications, List<ReceiptAction> actions) {
      return new ReceiptResponse(
          ReceiptSummaryResponse.from(r),
          r.getBookRate(),
          r.getBaseAmount(),
          r.getGross(),
          r.getVat(),
          r.getWtax(),
          r.getCheckNo(),
          r.getCheckBank(),
          r.getCheckDate(),
          r.getCertificateRef(),
          r.getSourceModule(),
          r.getSourceRef(),
          r.getReinstatedAmount(),
          r.getJournalBatchNo(),
          r.getRemarks(),
          r.getSalesUnit(),
          r.getLastPrintedAt(),
          r.getCreatedBy(),
          r.getCreatedAt(),
          r.getLines().stream().map(LineResponse::from).toList(),
          applications.stream().map(ApplicationResponse::from).toList(),
          actions.stream().map(ActionResponse::from).toList());
    }
  }

  /**
   * An OR line.
   *
   * @param invoiceNo invoice
   * @param insurerCode insurer
   * @param gross gross
   * @param vat VAT
   * @param wtax withholding tax
   * @param net net
   * @param description description
   */
  public record LineResponse(
      String invoiceNo,
      String insurerCode,
      BigDecimal gross,
      BigDecimal vat,
      BigDecimal wtax,
      BigDecimal net,
      String description) {

    /**
     * Maps a line.
     *
     * @param l line
     * @return response
     */
    public static LineResponse from(ReceiptLine l) {
      return new LineResponse(
          l.getInvoiceNo(),
          l.getInsurerCode(),
          l.getGross(),
          l.getVat(),
          l.getWtax(),
          l.getNet(),
          l.getDescription());
    }
  }

  /**
   * An application.
   *
   * @param id id
   * @param reference APP- reference
   * @param invoiceNo invoice
   * @param arn account
   * @param source source
   * @param amount amount
   * @param allocation amount per component
   * @param realizedCommission commission realized
   * @param realizedVat VAT realized
   * @param valueDate value date
   * @param status ACTIVE or REVERSED
   * @param reversalReason reversal reason
   * @param journalBatchNo journal
   */
  public record ApplicationResponse(
      Long id,
      String reference,
      String invoiceNo,
      String arn,
      String source,
      BigDecimal amount,
      Map<LedgerComponent, BigDecimal> allocation,
      BigDecimal realizedCommission,
      BigDecimal realizedVat,
      LocalDate valueDate,
      String status,
      String reversalReason,
      String journalBatchNo) {

    /**
     * Maps an application.
     *
     * @param a application
     * @return response
     */
    public static ApplicationResponse from(Application a) {
      return new ApplicationResponse(
          a.getId(),
          a.reference(),
          a.getInvoiceNo(),
          a.getArn(),
          a.getSource().name(),
          a.getAmount(),
          a.allocation(),
          a.getRealizedCommission(),
          a.getRealizedVat(),
          a.getValueDate(),
          a.getStatus(),
          a.getReversalReason(),
          a.getJournalBatchNo());
    }
  }

  /**
   * A cancellation or reinstatement.
   *
   * @param id id
   * @param receiptId receipt
   * @param transactionNo CAN- / RIN- number
   * @param action action
   * @param reasonCode reason
   * @param reasonText reason text
   * @param amount amount
   * @param invoiceNo invoice (reinstatement)
   * @param documentNo AR / OR number (reinstatement)
   * @param payorName payor (reinstatement)
   * @param accountOfficer account officer
   * @param unitHead unit head
   * @param teamLeader team leader
   * @param stage workflow stage
   * @param requestedBy requester
   * @param requestedAt requested
   * @param approvedBy approver
   * @param approvedAt approved
   * @param journalBatchNo journal
   */
  public record ActionResponse(
      Long id,
      Long receiptId,
      String transactionNo,
      String action,
      String reasonCode,
      String reasonText,
      BigDecimal amount,
      String invoiceNo,
      String documentNo,
      String payorName,
      String accountOfficer,
      String unitHead,
      String teamLeader,
      String stage,
      String requestedBy,
      Instant requestedAt,
      String approvedBy,
      Instant approvedAt,
      String journalBatchNo) {

    /**
     * Maps an action.
     *
     * @param a action
     * @return response
     */
    public static ActionResponse from(ReceiptAction a) {
      return new ActionResponse(
          a.getId(),
          a.getReceiptId(),
          a.getTransactionNo(),
          a.getAction().name(),
          a.getReasonCode(),
          a.getReasonText(),
          a.getAmount(),
          a.getInvoiceNo(),
          a.getDocumentNo(),
          a.getPayorName(),
          a.getAccountOfficer(),
          a.getUnitHead(),
          a.getTeamLeader(),
          a.getStage(),
          a.getCreatedBy(),
          a.getCreatedAt(),
          a.getApprovedBy(),
          a.getApprovedAt(),
          a.getJournalBatchNo());
    }
  }

  /**
   * An AR to issue without matching (non-premium, CSHID.021).
   *
   * @param companyId company
   * @param branchId branch
   * @param arClass AR class
   * @param receiptDate date
   * @param payorCode payor (insurer) code
   * @param payorName payor name
   * @param currency currency
   * @param amount amount
   * @param mode mode of payment
   * @param checkNo check number
   * @param checkBank bank
   * @param remarks remarks
   */
  public record ArRequest(
      @NotNull Long companyId,
      @NotNull Long branchId,
      @NotBlank String arClass,
      @NotNull LocalDate receiptDate,
      @Size(max = 30) String payorCode,
      @NotBlank @Size(max = 250) String payorName,
      @NotBlank @Size(min = 3, max = 3) String currency,
      @NotNull @DecimalMin("0.01") BigDecimal amount,
      @NotNull PaymentMode mode,
      @Size(max = 40) String checkNo,
      @Size(max = 60) String checkBank,
      @Size(max = 250) String remarks) {}

  /**
   * An OR line to issue.
   *
   * @param invoiceNo invoice
   * @param insurerCode insurer
   * @param gross gross
   * @param vat VAT
   * @param wtax withholding tax
   * @param description description
   */
  public record OrLineRequest(
      @Size(max = 40) String invoiceNo,
      @Size(max = 30) String insurerCode,
      @NotNull @DecimalMin("0.00") BigDecimal gross,
      @NotNull @DecimalMin("0.00") BigDecimal vat,
      @NotNull @DecimalMin("0.00") BigDecimal wtax,
      @Size(max = 250) String description) {}

  /**
   * An OR to issue (Head Office).
   *
   * @param companyId company
   * @param orType OR type
   * @param receiptDate date
   * @param payorCode payor code
   * @param payorName payor name
   * @param currency currency
   * @param mode mode of payment
   * @param checkNo check number
   * @param checkBank bank
   * @param certificateRef certificate
   * @param remarks remarks
   * @param lines lines
   */
  public record OrRequest(
      @NotNull Long companyId,
      @NotBlank String orType,
      @NotNull LocalDate receiptDate,
      @Size(max = 30) String payorCode,
      @NotBlank @Size(max = 250) String payorName,
      @NotBlank @Size(min = 3, max = 3) String currency,
      @NotNull PaymentMode mode,
      @Size(max = 40) String checkNo,
      @Size(max = 60) String checkBank,
      @Size(max = 60) String certificateRef,
      @Size(max = 250) String remarks,
      @NotEmpty List<@Valid OrLineRequest> lines) {}

  /**
   * A cancellation request.
   *
   * @param reasonCode reason (LOV RECEIPT_CANCEL_REASON)
   * @param reasonText text for "Others"
   */
  public record CancelRequest(@NotBlank String reasonCode, @Size(max = 250) String reasonText) {}

  /**
   * A reinstatement request.
   *
   * @param full full reinstatement
   * @param amount partial amount
   * @param reasonCode reason (LOV REINSTATEMENT_REASON)
   * @param reasonText text for "Others"
   * @param invoiceNo invoice
   * @param documentNo AR / OR number
   * @param payorName payor
   * @param accountOfficer account officer
   * @param unitHead unit head
   * @param teamLeader team leader
   */
  public record ReinstateRequest(
      boolean full,
      BigDecimal amount,
      @NotBlank String reasonCode,
      @Size(max = 250) String reasonText,
      @Size(max = 40) String invoiceNo,
      @Size(max = 40) String documentNo,
      @Size(max = 250) String payorName,
      @Size(max = 50) String accountOfficer,
      @Size(max = 100) String unitHead,
      @Size(max = 100) String teamLeader) {}
}
