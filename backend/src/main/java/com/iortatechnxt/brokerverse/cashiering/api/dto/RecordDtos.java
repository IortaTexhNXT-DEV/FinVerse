package com.iortatechnxt.brokerverse.cashiering.api.dto;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordAccount;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.EntryType;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.ReinstatementType;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordCodes.TenderType;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordParty;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordReason;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordTender;
import com.iortatechnxt.brokerverse.cashiering.domain.RecordTender.RecordCheck;
import com.iortatechnxt.brokerverse.cashiering.service.RecordValidation.Draft;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Requests and responses of BDOI's creation, cancellation and reinstatement records. */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class RecordDtos {

  private RecordDtos() {}

  /**
   * An AR or OR creation record as entered (FRS.CSH.02.01.02, 02.02.02).
   *
   * @param companyId company
   * @param receiptKind AR or OR
   * @param receiptType AR type or OR type
   * @param branchId receipting branch
   * @param entryType client, insurer or other
   * @param clientCode client code
   * @param clientName client name
   * @param insurerCode insurer code
   * @param insurerName insurer name
   * @param insurerBank bank of the insurer
   * @param payorName payor name
   * @param tenderType cash, check or direct credit
   * @param currency PHP or USD
   * @param bankAccount Post to Bank Account
   * @param amount paid amount
   * @param vat VAT of an OR
   * @param wtax withholding tax of an OR
   * @param certificateRef BIR 2307 certificate reference
   * @param checkNo check number
   * @param checkDate check date
   * @param checkBank bank of the check
   * @param receiptDate receipt issuance date
   * @param remarks remarks
   * @param otherIncomeRef SOA or fee invoice of an other income payment
   * @param accounts accounts with their paid amounts
   */
  public record RecordRequest(
      @NotNull Long companyId,
      @NotNull ReceiptKind receiptKind,
      String receiptType,
      Long branchId,
      EntryType entryType,
      String clientCode,
      String clientName,
      String insurerCode,
      String insurerName,
      String insurerBank,
      String payorName,
      TenderType tenderType,
      String currency,
      String bankAccount,
      BigDecimal amount,
      BigDecimal vat,
      BigDecimal wtax,
      String certificateRef,
      String checkNo,
      LocalDate checkDate,
      String checkBank,
      LocalDate receiptDate,
      String remarks,
      String otherIncomeRef,
      List<AccountLine> accounts) {

    /**
     * The record as the services read it.
     *
     * @return draft
     */
    public Draft draft() {
      return new Draft(
          receiptKind,
          blankToNull(receiptType),
          branchId,
          new RecordParty(
              entryType,
              blankToNull(clientCode),
              blankToNull(clientName),
              blankToNull(insurerCode),
              blankToNull(insurerName),
              blankToNull(insurerBank),
              trimmed(payorName)),
          new RecordTender(
              tenderType,
              blankToNull(currency),
              blankToNull(bankAccount),
              amount,
              vat,
              wtax,
              blankToNull(certificateRef),
              tenderType == TenderType.CHECK
                  ? new RecordCheck(blankToNull(checkNo), checkDate, blankToNull(checkBank))
                  : null,
              receiptDate,
              trimmed(remarks),
              blankToNull(otherIncomeRef)),
          accounts == null
              ? List.of()
              : accounts.stream()
                  .filter(a -> a.reference() != null && !a.reference().isBlank())
                  .map(a -> new RecordAccount(a.reference().strip(), a.amount()))
                  .toList());
    }
  }

  /**
   * An account of a record with its amount.
   *
   * @param reference account, invoice, ARN, policy or PN number
   * @param amount amount
   */
  public record AccountLine(String reference, BigDecimal amount) {}

  /**
   * Cancellation records to save (FRS.CSH.03.01.05).
   *
   * @param companyId company
   * @param receiptIds issued ARs only or ORs only
   * @param reasonCode reason
   * @param reasonText text of Others
   */
  public record CancellationRequest(
      @NotNull Long companyId,
      @NotEmpty List<Long> receiptIds,
      String reasonCode,
      @Size(max = 250) String reasonText) {

    /**
     * The reason as the services read it.
     *
     * @return reason
     */
    public RecordReason reason() {
      return new RecordReason(
          blankToNull(reasonCode), trimmed(reasonText), null, null, null, null, null);
    }
  }

  /**
   * Reinstatement records to save (FRS.CSH.04.01.06).
   *
   * @param companyId company
   * @param receiptIds issued ARs only or ORs only
   * @param type full or partial
   * @param accounts accounts of a partial reinstatement
   * @param reasonCode reason
   * @param remarks remarks or the text of Others (100 characters)
   * @param invoiceNo invoice number (CSHID.005)
   * @param accountOfficer Account Officer
   * @param unitHead Unit Head
   * @param teamLeader Team Leader
   */
  public record ReinstatementRequest(
      Long companyId,
      List<Long> receiptIds,
      ReinstatementType type,
      List<String> accounts,
      String reasonCode,
      String remarks,
      String invoiceNo,
      String accountOfficer,
      String unitHead,
      String teamLeader) {

    /**
     * The reason as the services read it.
     *
     * @return reason
     */
    public RecordReason reason() {
      return new RecordReason(
          blankToNull(reasonCode),
          trimmed(remarks),
          type,
          blankToNull(invoiceNo),
          blankToNull(accountOfficer),
          blankToNull(unitHead),
          blankToNull(teamLeader));
    }

    /**
     * Accounts selected.
     *
     * @return accounts, never null
     */
    public List<String> chosen() {
      return accounts == null ? List.of() : accounts;
    }
  }

  /**
   * Records to post (FRS.CSH.02.05.04).
   *
   * @param ids records For Posting
   */
  public record PostRequest(@NotEmpty List<Long> ids) {}

  /**
   * A return to the creator.
   *
   * @param reason reason
   */
  public record ReturnRequest(String reason) {}

  /**
   * A record on a screen or list.
   *
   * @param id id
   * @param recordNo record number
   * @param recordKind creation, cancellation or reinstatement
   * @param receiptKind AR or OR
   * @param receiptType type code
   * @param receiptTypeLabel type as the list names it
   * @param stage status code
   * @param statusLabel status as BDOI's FRS names it
   * @param editable whether the creator may edit it
   * @param branchId receipting branch
   * @param branchName its name
   * @param party payor
   * @param tender money
   * @param bankAccountName name of the bank account
   * @param reason reason of a cancellation or reinstatement
   * @param reasonLabel reason as its list names it
   * @param receiptId receipt issued, cancelled or reinstated
   * @param receiptNo its number
   * @param accounts accounts with their amounts
   * @param accountsText the accounts in one text
   * @param total total of the record
   * @param returnReason reason of the last return
   * @param createdBy creator's name
   * @param createdByUser creator's user name (to hide the Post action)
   * @param createdAt time saved
   * @param submittedAt time submitted
   * @param postedBy Approver/Poster's name
   * @param postedAt time posted
   * @param postingResult reason a posting failed
   */
  public record RecordResponse(
      Long id,
      String recordNo,
      String recordKind,
      String receiptKind,
      String receiptType,
      String receiptTypeLabel,
      String stage,
      String statusLabel,
      boolean editable,
      Long branchId,
      String branchName,
      RecordParty party,
      RecordTender tender,
      String bankAccountName,
      RecordReason reason,
      String reasonLabel,
      Long receiptId,
      String receiptNo,
      List<RecordAccount> accounts,
      String accountsText,
      BigDecimal total,
      String returnReason,
      String createdBy,
      String createdByUser,
      Instant createdAt,
      Instant submittedAt,
      String postedBy,
      Instant postedAt,
      String postingResult) {

    /** Defensive copy. */
    public RecordResponse {
      accounts = List.copyOf(accounts);
    }
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.strip();
  }

  private static String trimmed(String value) {
    return value == null ? null : value.strip();
  }
}
