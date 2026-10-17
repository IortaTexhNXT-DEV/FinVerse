package com.iortatechnxt.brokerverse.disbursement.api.dto;

import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.DisbursementMode;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.LineOrigin;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PostingStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.VoucherStage;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeAccount;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherLine;
import com.iortatechnxt.brokerverse.disbursement.domain.VoucherLine.LineValues;
import com.iortatechnxt.brokerverse.disbursement.service.VoucherBulk.ItemResult;
import com.iortatechnxt.brokerverse.disbursement.service.VoucherService.Allocation;
import com.iortatechnxt.brokerverse.disbursement.service.VoucherViewService.VoucherView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Requests and responses of the voucher endpoints (DIS 2.7-2.21). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class VoucherDtos {

  private VoucherDtos() {}

  /**
   * A voucher in a list.
   *
   * @param id id
   * @param dvNo DV number
   * @param requestId request
   * @param payeeCode payee code
   * @param payeeName payee name
   * @param disbursementType type
   * @param mode mode of payment
   * @param currency currency
   * @param gross gross
   * @param ewt withholding tax
   * @param net net
   * @param stage stage
   * @param postingStatus posting status
   * @param proformaEdited entry edited
   * @param autoCreated built automatically
   * @param rootInvoiceNo root invoice
   * @param createdAt created
   * @param approvedAt approved
   */
  public record VoucherSummary(
      Long id,
      String dvNo,
      Long requestId,
      String payeeCode,
      String payeeName,
      String disbursementType,
      DisbursementMode mode,
      String currency,
      BigDecimal gross,
      BigDecimal ewt,
      BigDecimal net,
      VoucherStage stage,
      PostingStatus postingStatus,
      boolean proformaEdited,
      boolean autoCreated,
      String rootInvoiceNo,
      Instant createdAt,
      Instant approvedAt) {

    /**
     * Maps a voucher.
     *
     * @param v voucher
     * @return DTO
     */
    public static VoucherSummary from(Voucher v) {
      return new VoucherSummary(
          v.getId(),
          v.getDvNo(),
          v.getRequestId(),
          v.getPayeeCode(),
          v.getPayeeName(),
          v.getDisbursementType(),
          v.getMode(),
          v.getCurrency(),
          v.getGross(),
          v.getEwt(),
          v.getNet(),
          v.getStage(),
          v.getPostingStatus(),
          v.isProformaEdited(),
          v.isAutoCreated(),
          v.getRootInvoiceNo(),
          v.getCreatedAt(),
          v.getApprovedAt());
    }
  }

  /**
   * A line of the proforma entry.
   *
   * @param side debit or credit
   * @param accountCode account
   * @param partyCode sub-ledger party
   * @param costCenter cost centre
   * @param businessLine line of business
   * @param amount amount
   * @param component event component
   * @param origin origin (read only)
   * @param narration narration
   */
  public record LineDto(
      @NotNull BalanceSide side,
      @NotBlank @Size(max = 30) String accountCode,
      @Size(max = 30) String partyCode,
      @Size(max = 20) String costCenter,
      @Size(max = 20) String businessLine,
      @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
      @Size(max = 30) String component,
      LineOrigin origin,
      @Size(max = 250) String narration) {

    /**
     * Maps a line.
     *
     * @param l line
     * @return DTO
     */
    public static LineDto from(VoucherLine l) {
      return new LineDto(
          l.getSide(),
          l.getAccountCode(),
          l.getPartyCode(),
          l.getCostCenter(),
          l.getBusinessLine(),
          l.getAmount(),
          l.getComponent(),
          l.getOrigin(),
          l.getNarration());
    }

    /**
     * The line values (origin decided by the service).
     *
     * @return values
     */
    public LineValues values() {
      return new LineValues(
          side,
          accountCode,
          partyCode,
          costCenter,
          businessLine,
          amount,
          component,
          LineOrigin.EDITED,
          narration);
    }
  }

  /**
   * The voucher page.
   *
   * @param summary list facts
   * @param request payment request
   * @param bankAccountId paying account
   * @param bankAccount paying account label
   * @param payeeAccountId payee account
   * @param payeeAccount payee account label (masked without {@code DISB_PAYEE_VIEW_FULL})
   * @param purpose purpose
   * @param valueDate value date
   * @param costCenter cost centre
   * @param expenseAccount expense account
   * @param exchangeRate rate used at posting
   * @param lines proforma entry
   * @param missing fields still missing
   * @param journalNo approval journal
   * @param cancelJournalNo reversal journal
   * @param postingError posting error
   * @param submittedBy processor
   * @param reviewedBy checker
   * @param approvedBy approver
   * @param cancelReason cancellation reason
   * @param cancelledBy cancelled by
   * @param instrument instrument, null before approval
   * @param tags tags
   */
  public record VoucherResponse(
      VoucherSummary summary,
      RequestDtos.RequestResponse request,
      Long bankAccountId,
      String bankAccount,
      Long payeeAccountId,
      String payeeAccount,
      String purpose,
      LocalDate valueDate,
      String costCenter,
      String expenseAccount,
      BigDecimal exchangeRate,
      List<LineDto> lines,
      List<String> missing,
      String journalNo,
      String cancelJournalNo,
      String postingError,
      String submittedBy,
      String reviewedBy,
      String approvedBy,
      String cancelReason,
      String cancelledBy,
      InstrumentDtos.InstrumentResponse instrument,
      List<InstrumentDtos.TagResponse> tags) {

    /**
     * Maps the page.
     *
     * @param view voucher page
     * @param fullAccounts whether the user sees full account numbers
     * @return DTO
     */
    public static VoucherResponse from(VoucherView view, boolean fullAccounts) {
      Voucher v = view.voucher();
      PayeeAccount pa = view.payeeAccount();
      return new VoucherResponse(
          VoucherSummary.from(v),
          RequestDtos.RequestResponse.from(view.request()),
          v.getBankAccountId(),
          view.bank() == null
              ? null
              : view.bank().getCode()
                  + " - "
                  + view.bank().getBankName()
                  + " "
                  + view.bank().getAccountNo(),
          v.getPayeeAccountId(),
          pa == null
              ? null
              : pa.getBankName()
                  + " "
                  + (fullAccounts ? pa.getAccountNo() : PayeeAccount.mask(pa.getAccountNo())),
          v.getPurpose(),
          v.getValueDate(),
          v.getCostCenter(),
          v.getExpenseAccount(),
          v.getExchangeRate(),
          v.getLines().stream().map(LineDto::from).toList(),
          view.missing(),
          v.getJournalNo(),
          v.getCancelJournalNo(),
          v.getPostingError(),
          v.getSubmittedBy(),
          v.getReviewedBy(),
          v.getApprovedBy(),
          v.getCancelReason(),
          v.getCancelledBy(),
          view.instrument() == null ? null : InstrumentDtos.InstrumentResponse.from(view),
          view.tags().stream().map(InstrumentDtos.TagResponse::from).toList());
    }
  }

  /**
   * Processing terms (DIS 2.7.1-2.7.4).
   *
   * @param mode mode of payment
   * @param bankAccountId paying account
   * @param payeeAccountId payee account
   * @param ewt withholding tax
   * @param purpose purpose
   * @param valueDate value date
   * @param costCenter cost centre
   * @param expenseAccount expense account of an OTHER payment
   */
  public record TermsRequest(
      @NotNull DisbursementMode mode,
      @NotNull Long bankAccountId,
      Long payeeAccountId,
      @DecimalMin(value = "0") @Digits(integer = 17, fraction = 2) BigDecimal ewt,
      @NotBlank @Size(max = 500) String purpose,
      @NotNull LocalDate valueDate,
      @Size(max = 20) String costCenter,
      @Size(max = 30) String expenseAccount) {}

  /**
   * An edited proforma (DIS 2.7.6).
   *
   * @param lines lines
   */
  public record LinesRequest(@NotEmpty @Size(max = 100) List<@Valid LineDto> lines) {}

  /**
   * An expense allocation (DIS 2.7.10).
   *
   * @param rows rows
   */
  public record AllocationRequest(@NotEmpty @Size(max = 200) List<@Valid AllocationRow> rows) {

    /**
     * The rows as service values.
     *
     * @return allocations
     */
    public List<Allocation> allocations() {
      return rows.stream()
          .map(r -> new Allocation(r.accountCode(), r.costCenter(), r.amount(), r.narration()))
          .toList();
    }
  }

  /**
   * One allocation row.
   *
   * @param accountCode expense account
   * @param costCenter cost centre
   * @param amount amount
   * @param narration narration
   */
  public record AllocationRow(
      @NotBlank @Size(max = 30) String accountCode,
      @Size(max = 20) String costCenter,
      @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
      @Size(max = 250) String narration) {}

  /**
   * A comment.
   *
   * @param comment comment
   */
  public record CommentRequest(@Size(max = 500) String comment) {}

  /**
   * Several vouchers to approve (DIS 2.19.0).
   *
   * @param ids vouchers
   * @param comment comment
   */
  public record BulkApproveRequest(
      @NotEmpty @Size(max = 100) List<Long> ids, @Size(max = 500) String comment) {}

  /**
   * The outcome of one voucher of a bulk approval.
   *
   * @param id voucher
   * @param dvNo DV number
   * @param ok approved
   * @param message outcome
   */
  public record ItemResultDto(Long id, String dvNo, boolean ok, String message) {

    /**
     * Maps a result.
     *
     * @param r result
     * @return DTO
     */
    public static ItemResultDto from(ItemResult r) {
      return new ItemResultDto(r.id(), r.dvNo(), r.ok(), r.message());
    }
  }
}
