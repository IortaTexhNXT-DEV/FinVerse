package com.iortatechnxt.brokerverse.catalog.api.dto;

import com.iortatechnxt.brokerverse.catalog.domain.InsurerBankAccount;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerBankAccount.AccountPurpose;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerBankAccount.BankAccountDetails;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerContact;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerContact.ContactDetails;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerContact.ContactRole;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile.RemittanceCycle;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile.RemittanceMode;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile.RemittanceTerms;
import com.iortatechnxt.brokerverse.common.domain.RecordStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Requests and responses of the full insurer page (G08). */
public final class InsurerPageDtos {

  private InsurerPageDtos() {}

  /**
   * Payment terms and remittance details.
   *
   * @param paymentTerms payment terms agreed with the insurer
   * @param cycle remittance cycle
   * @param mode remittance mode
   * @param email mailbox of the remittance advices
   * @param currency remittance currency
   */
  public record RemittanceRequest(
      @Size(max = 300) String paymentTerms,
      RemittanceCycle cycle,
      RemittanceMode mode,
      @Email @Size(max = 120) String email,
      @Pattern(regexp = "[A-Z]{3}") String currency) {

    /**
     * The terms.
     *
     * @return terms
     */
    public RemittanceTerms terms() {
      return new RemittanceTerms(
          blankToNull(paymentTerms), cycle, mode, blankToNull(email), currency);
    }
  }

  /**
   * New or changed contact person.
   *
   * @param role what the contact handles
   * @param name name
   * @param position position
   * @param email e-mail
   * @param phone phone
   */
  public record ContactRequest(
      @NotNull ContactRole role,
      @NotBlank @Size(max = 150) String name,
      @Size(max = 80) String position,
      @Email @Size(max = 120) String email,
      @Size(max = 40) String phone) {

    /**
     * The contact.
     *
     * @return details
     */
    public ContactDetails details() {
      return new ContactDetails(
          role, name.trim(), blankToNull(position), blankToNull(email), blankToNull(phone));
    }
  }

  /**
   * New or changed bank account.
   *
   * @param purpose use of the account
   * @param bankName bank
   * @param bankBranch bank branch
   * @param accountNo account number
   * @param accountName account name
   * @param currency currency
   * @param primaryAccount primary account for its purpose
   */
  public record BankAccountRequest(
      @NotNull AccountPurpose purpose,
      @NotBlank @Size(max = 120) String bankName,
      @Size(max = 120) String bankBranch,
      @NotBlank @Size(max = 40) @Pattern(regexp = "[0-9A-Za-z\\-]+") String accountNo,
      @NotBlank @Size(max = 250) String accountName,
      @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
      boolean primaryAccount) {

    /**
     * The account.
     *
     * @return details
     */
    public BankAccountDetails details() {
      return new BankAccountDetails(
          purpose,
          bankName.trim(),
          blankToNull(bankBranch),
          accountNo.trim(),
          accountName.trim(),
          currency,
          primaryAccount);
    }
  }

  /**
   * A contact person.
   *
   * @param id id
   * @param insurerId insurer profile
   * @param role what the contact handles
   * @param name name
   * @param position position
   * @param email e-mail
   * @param phone phone
   * @param recordStatus maker-checker status
   * @param maker last maintainer
   * @param authorizedBy checker
   */
  public record ContactResponse(
      Long id,
      Long insurerId,
      ContactRole role,
      String name,
      String position,
      String email,
      String phone,
      RecordStatus recordStatus,
      String maker,
      String authorizedBy) {

    /**
     * Maps an entity.
     *
     * @param e entity
     * @return response
     */
    public static ContactResponse from(InsurerContact e) {
      return new ContactResponse(
          e.getId(),
          e.getInsurerId(),
          e.getRole(),
          e.getName(),
          e.getPosition(),
          e.getEmail(),
          e.getPhone(),
          e.getRecordStatus(),
          e.getMaker(),
          e.getAuthorizedBy());
    }
  }

  /**
   * A bank account.
   *
   * @param id id
   * @param insurerId insurer profile
   * @param purpose use of the account
   * @param bankName bank
   * @param bankBranch bank branch
   * @param accountNo account number
   * @param accountName account name
   * @param currency currency
   * @param primaryAccount primary account for its purpose
   * @param recordStatus maker-checker status
   * @param maker last maintainer
   * @param authorizedBy checker
   */
  public record BankAccountResponse(
      Long id,
      Long insurerId,
      AccountPurpose purpose,
      String bankName,
      String bankBranch,
      String accountNo,
      String accountName,
      String currency,
      boolean primaryAccount,
      RecordStatus recordStatus,
      String maker,
      String authorizedBy) {

    /**
     * Maps an entity.
     *
     * @param e entity
     * @return response
     */
    public static BankAccountResponse from(InsurerBankAccount e) {
      return new BankAccountResponse(
          e.getId(),
          e.getInsurerId(),
          e.getPurpose(),
          e.getBankName(),
          e.getBankBranch(),
          e.getAccountNo(),
          e.getAccountName(),
          e.getCurrency(),
          e.isPrimaryAccount(),
          e.getRecordStatus(),
          e.getMaker(),
          e.getAuthorizedBy());
    }
  }

  static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
