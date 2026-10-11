package com.iortatechnxt.brokerverse.disbursement.api.dto;

import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.EodStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.FundingStage;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.OutputKind;
import com.iortatechnxt.brokerverse.disbursement.domain.EodOutput;
import com.iortatechnxt.brokerverse.disbursement.domain.EodRun;
import com.iortatechnxt.brokerverse.disbursement.domain.FundingRequest;
import com.iortatechnxt.brokerverse.disbursement.domain.FundingRequest.FundingTerms;
import com.iortatechnxt.brokerverse.payables.domain.BankAccount;
import com.iortatechnxt.brokerverse.payables.domain.BankAccountStatus;
import com.iortatechnxt.brokerverse.payables.domain.ChequeBook;
import com.iortatechnxt.brokerverse.payables.domain.ChequeBookStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Requests and responses of the end-of-day, account funding and bank account / check series
 * endpoints (DIS 2.16, 2.17, 2.23-2.24).
 */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class OperationDtos {

  private OperationDtos() {}

  /**
   * An end-of-day run.
   *
   * @param id id
   * @param runNo run number
   * @param businessDate date
   * @param status status
   * @param vouchers vouchers
   * @param checks checks
   * @param credits DCTF credits
   * @param forms forms
   * @param reports reports
   * @param emails confirmations
   * @param message summary
   * @param runBy user
   * @param runAt time
   * @param outputs files
   */
  public record EodRunResponse(
      Long id,
      String runNo,
      LocalDate businessDate,
      EodStatus status,
      int vouchers,
      int checks,
      int credits,
      int forms,
      int reports,
      int emails,
      String message,
      String runBy,
      Instant runAt,
      List<OutputResponse> outputs) {

    /**
     * Maps a run.
     *
     * @param r run
     * @param outputs its files
     * @return DTO
     */
    public static EodRunResponse from(EodRun r, List<EodOutput> outputs) {
      return new EodRunResponse(
          r.getId(),
          r.getRunNo(),
          r.getBusinessDate(),
          r.getStatus(),
          r.getVouchers(),
          r.getChecks(),
          r.getCredits(),
          r.getForms(),
          r.getReports(),
          r.getEmails(),
          r.getMessage(),
          r.getCreatedBy(),
          r.getCreatedAt(),
          outputs.stream().map(OutputResponse::from).toList());
    }
  }

  /**
   * An output file of a run.
   *
   * @param id id
   * @param kind kind
   * @param code code
   * @param fileName file name
   * @param itemCount items
   */
  public record OutputResponse(
      Long id, OutputKind kind, String code, String fileName, int itemCount) {

    /**
     * Maps an output.
     *
     * @param o output
     * @return DTO
     */
    public static OutputResponse from(EodOutput o) {
      return new OutputResponse(
          o.getId(), o.getKind(), o.getCode(), o.getFileName(), o.getItemCount());
    }
  }

  /**
   * An end-of-day run to start.
   *
   * @param companyId company
   * @param businessDate business date
   */
  public record EodRequest(@NotNull Long companyId, @NotNull LocalDate businessDate) {}

  /**
   * A funding request.
   *
   * @param id id
   * @param fundingNo number
   * @param sourceBankAccountId source account
   * @param targetBankAccountId target account
   * @param amount amount
   * @param currency currency
   * @param purpose purpose
   * @param valueDate value date
   * @param stage stage
   * @param createdBy maker
   * @param verifiedBy verifier
   * @param firstApprover first approver
   * @param secondApprover second approver
   * @param bobReference BOB reference
   * @param journalNo journal
   */
  public record FundingResponse(
      Long id,
      String fundingNo,
      Long sourceBankAccountId,
      Long targetBankAccountId,
      BigDecimal amount,
      String currency,
      String purpose,
      LocalDate valueDate,
      FundingStage stage,
      String createdBy,
      String verifiedBy,
      String firstApprover,
      String secondApprover,
      String bobReference,
      String journalNo) {

    /**
     * Maps a request.
     *
     * @param f request
     * @return DTO
     */
    public static FundingResponse from(FundingRequest f) {
      return new FundingResponse(
          f.getId(),
          f.getFundingNo(),
          f.getSourceBankAccountId(),
          f.getTargetBankAccountId(),
          f.getAmount(),
          f.getCurrency(),
          f.getPurpose(),
          f.getValueDate(),
          f.getStage(),
          f.getCreatedBy(),
          f.getVerifiedBy(),
          f.getFirstApprover(),
          f.getSecondApprover(),
          f.getBobReference(),
          f.getJournalNo());
    }
  }

  /**
   * Funding terms.
   *
   * @param companyId company (create only)
   * @param sourceBankAccountId source account
   * @param targetBankAccountId target account
   * @param amount amount
   * @param currency currency
   * @param purpose purpose
   * @param valueDate value date
   * @param bobReference BOB reference
   */
  public record FundingRequestBody(
      Long companyId,
      @NotNull Long sourceBankAccountId,
      @NotNull Long targetBankAccountId,
      @NotNull @DecimalMin(value = "0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
      @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
      @NotBlank @Size(max = 500) String purpose,
      @NotNull LocalDate valueDate,
      @Size(max = 80) String bobReference) {

    /**
     * The terms.
     *
     * @return terms
     */
    public FundingTerms terms() {
      return new FundingTerms(
          sourceBankAccountId,
          targetBankAccountId,
          amount,
          currency,
          purpose.strip(),
          valueDate,
          bobReference);
    }
  }

  /**
   * An approval of a funding request.
   *
   * @param comment remarks
   * @param bobReference BOB reference
   */
  public record FundingApproval(
      @Size(max = 500) String comment, @Size(max = 80) String bobReference) {}

  /**
   * A BDOIR bank account with its check series (DIS 2.23-2.24).
   *
   * @param id id
   * @param code code
   * @param name name
   * @param bankName bank
   * @param accountNo number
   * @param currency currency
   * @param glAccountCode GL account
   * @param recordStatus maker-checker status
   * @param status active or inactive
   * @param requestedStatus status waiting for authorisation
   * @param maker last maintained by
   * @param remainingLeaves unused check leaves
   * @param books check books
   */
  public record BankResponse(
      Long id,
      String code,
      String name,
      String bankName,
      String accountNo,
      String currency,
      String glAccountCode,
      RecordStatus recordStatus,
      BankAccountStatus status,
      BankAccountStatus requestedStatus,
      String maker,
      long remainingLeaves,
      List<BookResponse> books) {

    /**
     * Maps an account.
     *
     * @param a account
     * @param books its check books
     * @return DTO
     */
    public static BankResponse from(BankAccount a, List<ChequeBook> books) {
      return new BankResponse(
          a.getId(),
          a.getCode(),
          a.getName(),
          a.getBankName(),
          a.getAccountNo(),
          a.getCurrency(),
          a.getGlAccountCode(),
          a.getRecordStatus(),
          a.getStatus(),
          a.getRequestedStatus(),
          a.getMaker(),
          books.stream().mapToLong(ChequeBook::remaining).sum(),
          books.stream().map(BookResponse::from).toList());
    }
  }

  /**
   * A check book.
   *
   * @param id id
   * @param firstNo first leaf
   * @param lastNo last leaf
   * @param nextNo next leaf
   * @param remaining unused leaves
   * @param receivedOn received
   * @param status status
   * @param editedBy edited by
   * @param previousRange range before the edit
   */
  public record BookResponse(
      Long id,
      long firstNo,
      long lastNo,
      long nextNo,
      long remaining,
      LocalDate receivedOn,
      ChequeBookStatus status,
      String editedBy,
      String previousRange) {

    /**
     * Maps a book.
     *
     * @param b book
     * @return DTO
     */
    public static BookResponse from(ChequeBook b) {
      return new BookResponse(
          b.getId(),
          b.getFirstNo(),
          b.getLastNo(),
          b.getNextNo(),
          b.remaining(),
          b.getReceivedOn(),
          b.getStatus(),
          b.getEditedBy(),
          b.getPreviousRange());
    }
  }

  /**
   * A requested bank account status (DIS 2.24.2).
   *
   * @param status active or inactive
   */
  public record BankStatusRequest(@NotNull BankAccountStatus status) {}

  /**
   * A check series (DIS 2.23.1-2.23.2).
   *
   * @param firstNo first leaf
   * @param lastNo last leaf
   * @param receivedOn received from the bank (new book only)
   */
  public record BookRequest(@Min(1) long firstNo, @Min(1) long lastNo, LocalDate receivedOn) {}
}
