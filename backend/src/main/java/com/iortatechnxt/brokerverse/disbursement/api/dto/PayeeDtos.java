package com.iortatechnxt.brokerverse.disbursement.api.dto;

import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.DisbursementMode;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeRequestSource;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeRequestStatus;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeSource;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.PayeeStage;
import com.iortatechnxt.brokerverse.disbursement.domain.Payee;
import com.iortatechnxt.brokerverse.disbursement.domain.Payee.PayeeDetails;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeAccount;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeAccount.AccountDetails;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** Requests and responses of the payee endpoints (DIS 2.2.0-2.2.8). */
@SuppressWarnings("PMD.MissingStaticMethodInNonInstantiatableClass") // holder of nested types
public final class PayeeDtos {

  private PayeeDtos() {}

  /**
   * A payee in a list.
   *
   * @param id id
   * @param payeeCode code
   * @param payeeClass class
   * @param name name
   * @param defaultMode default mode
   * @param currency currency
   * @param source source
   * @param stage stage
   * @param accountNo primary account (masked without {@code DISB_PAYEE_VIEW_FULL})
   * @param used used by a voucher
   */
  public record PayeeSummary(
      Long id,
      String payeeCode,
      String payeeClass,
      String name,
      DisbursementMode defaultMode,
      String currency,
      PayeeSource source,
      PayeeStage stage,
      String accountNo,
      boolean used) {

    /**
     * Maps a payee.
     *
     * @param p payee
     * @param full full account numbers
     * @return DTO
     */
    public static PayeeSummary from(Payee p, boolean full) {
      return new PayeeSummary(
          p.getId(),
          p.getPayeeCode(),
          p.getPayeeClass(),
          p.getName(),
          p.getDefaultMode(),
          p.getCurrency(),
          p.getSource(),
          p.getStage(),
          p.primaryAccount().map(a -> number(a, full)).orElse(null),
          p.isUsed());
    }
  }

  private static String number(PayeeAccount a, boolean full) {
    return full ? a.getAccountNo() : PayeeAccount.mask(a.getAccountNo());
  }

  /**
   * A payee with its bank accounts.
   *
   * @param summary list facts
   * @param address address
   * @param email e-mail
   * @param tin TIN
   * @param allowedModes allowed modes
   * @param disbursementTypes disbursement types
   * @param defaultCostCenter default cost centre
   * @param remarks remarks
   * @param accounts bank accounts
   * @param createdBy maker
   * @param updatedBy last maintained by
   */
  public record PayeeResponse(
      PayeeSummary summary,
      String address,
      String email,
      String tin,
      List<DisbursementMode> allowedModes,
      List<String> disbursementTypes,
      String defaultCostCenter,
      String remarks,
      List<AccountResponse> accounts,
      String createdBy,
      String updatedBy) {

    /**
     * Maps a payee.
     *
     * @param p payee with accounts
     * @param full full account numbers
     * @return DTO
     */
    public static PayeeResponse from(Payee p, boolean full) {
      return new PayeeResponse(
          PayeeSummary.from(p, full),
          p.getAddress(),
          p.getEmail(),
          p.getTin(),
          p.getAllowedModes(),
          p.getDisbursementTypes(),
          p.getDefaultCostCenter(),
          p.getRemarks(),
          p.getAccounts().stream().map(a -> AccountResponse.from(a, full)).toList(),
          p.getCreatedBy(),
          p.getUpdatedBy());
    }
  }

  /**
   * A bank account of a payee.
   *
   * @param id id
   * @param bankName bank
   * @param bankBranch branch
   * @param accountNo number (masked without {@code DISB_PAYEE_VIEW_FULL})
   * @param accountName name
   * @param currency currency
   * @param mode mode
   * @param primary primary
   * @param active active
   */
  public record AccountResponse(
      Long id,
      String bankName,
      String bankBranch,
      String accountNo,
      String accountName,
      String currency,
      DisbursementMode mode,
      boolean primary,
      boolean active) {

    /**
     * Maps an account.
     *
     * @param a account
     * @param full full number
     * @return DTO
     */
    public static AccountResponse from(PayeeAccount a, boolean full) {
      return new AccountResponse(
          a.getId(),
          a.getBankName(),
          a.getBankBranch(),
          number(a, full),
          a.getAccountName(),
          a.getCurrency(),
          a.getMode(),
          a.isPrimaryAccount(),
          a.isActive());
    }
  }

  /**
   * Payee details to save.
   *
   * @param companyId company (create only)
   * @param payeeCode party code (create only)
   * @param payeeClass class (LOV {@code PAYEE_CLASS})
   * @param name name
   * @param address address
   * @param email e-mail
   * @param tin TIN
   * @param defaultMode default mode
   * @param allowedModes allowed modes
   * @param disbursementTypes disbursement types
   * @param currency currency
   * @param defaultCostCenter default cost centre
   * @param remarks remarks
   * @param accounts bank accounts (create only)
   */
  public record PayeeRequestBody(
      Long companyId,
      @Size(max = 30) String payeeCode,
      @NotBlank @Size(max = 20) String payeeClass,
      @NotBlank @Size(max = 250) String name,
      @Size(max = 500) String address,
      @Email @Size(max = 200) String email,
      @Size(max = 30) String tin,
      @NotNull DisbursementMode defaultMode,
      @NotEmpty List<DisbursementMode> allowedModes,
      List<@Size(max = 30) String> disbursementTypes,
      @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
      @Size(max = 20) String defaultCostCenter,
      @Size(max = 500) String remarks,
      List<@Valid AccountRequest> accounts) {

    /**
     * The details.
     *
     * @return details
     */
    public PayeeDetails details() {
      return new PayeeDetails(
          payeeClass,
          name.strip(),
          address,
          email,
          tin,
          defaultMode,
          allowedModes,
          disbursementTypes,
          currency,
          defaultCostCenter,
          remarks);
    }

    /**
     * The bank accounts.
     *
     * @return accounts
     */
    public List<AccountDetails> accountDetails() {
      return accounts == null ? List.of() : accounts.stream().map(AccountRequest::details).toList();
    }
  }

  /**
   * A bank account to add.
   *
   * @param bankName bank
   * @param bankBranch branch
   * @param accountNo number
   * @param accountName name
   * @param currency currency
   * @param mode mode served (CTA, TT, ONLINE_BANKING)
   * @param primary primary account
   */
  public record AccountRequest(
      @NotBlank @Size(max = 120) String bankName,
      @Size(max = 120) String bankBranch,
      @NotBlank @Size(max = 40) String accountNo,
      @NotBlank @Size(max = 250) String accountName,
      @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
      @NotNull DisbursementMode mode,
      boolean primary) {

    /**
     * The details.
     *
     * @return details
     */
    public AccountDetails details() {
      return new AccountDetails(
          bankName, bankBranch, accountNo.strip(), accountName, currency, mode, primary);
    }
  }

  /**
   * A payee maintenance request.
   *
   * @param id id
   * @param source source
   * @param payeeCode code
   * @param payeeName name
   * @param details details
   * @param sourceRef source reference
   * @param status status
   * @param payeeId payee maintained
   * @param createdAt raised
   * @param closedBy closed by
   */
  public record PayeeRequestResponse(
      Long id,
      PayeeRequestSource source,
      String payeeCode,
      String payeeName,
      String details,
      String sourceRef,
      PayeeRequestStatus status,
      Long payeeId,
      Instant createdAt,
      String closedBy) {

    /**
     * Maps a request.
     *
     * @param r request
     * @return DTO
     */
    public static PayeeRequestResponse from(PayeeRequest r) {
      return new PayeeRequestResponse(
          r.getId(),
          r.getSource(),
          r.getPayeeCode(),
          r.getPayeeName(),
          r.getDetails(),
          r.getSourceRef(),
          r.getStatus(),
          r.getPayeeId(),
          r.getCreatedAt(),
          r.getClosedBy());
    }
  }
}
