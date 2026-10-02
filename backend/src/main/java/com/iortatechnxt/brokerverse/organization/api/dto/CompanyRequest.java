package com.iortatechnxt.brokerverse.organization.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Create / update company request.
 *
 * @param code company code (ignored on update)
 * @param name legal name
 * @param baseCurrency ISO currency (ignored on update)
 * @param taxId tax identification number
 * @param address registered address
 * @param fiscalYearStartMonth 1-12
 * @param backValueDays allowed back-dated days for journals
 * @param forwardValueDays allowed forward-dated days for journals
 * @param retainedEarningsAccount account code receiving year-end profit/loss
 * @param profile client profile (short name, group, logo, head-office and bank codes), may be null
 */
public record CompanyRequest(
    @NotBlank @Size(max = 10) @Pattern(regexp = "[A-Z0-9]+") String code,
    @NotBlank @Size(max = 150) String name,
    @NotBlank @Pattern(regexp = "[A-Z]{3}") String baseCurrency,
    @Size(max = 30) String taxId,
    @Size(max = 300) String address,
    @Min(1) @Max(12) int fiscalYearStartMonth,
    @Min(0) @Max(366) int backValueDays,
    @Min(0) @Max(366) int forwardValueDays,
    @Size(max = 30) String retainedEarningsAccount,
    @Valid ClientProfileRequest profile) {

  /**
   * A request without the client profile (the short name is then the company code).
   *
   * @param code company code
   * @param name legal name
   * @param baseCurrency ISO currency
   * @param taxId tax identification number
   * @param address registered address
   * @param fiscalYearStartMonth 1-12
   * @param backValueDays allowed back-dated days
   * @param forwardValueDays allowed forward-dated days
   * @param retainedEarningsAccount retained earnings account code
   */
  public CompanyRequest(
      String code,
      String name,
      String baseCurrency,
      String taxId,
      String address,
      int fiscalYearStartMonth,
      int backValueDays,
      int forwardValueDays,
      String retainedEarningsAccount) {
    this(
        code,
        name,
        baseCurrency,
        taxId,
        address,
        fiscalYearStartMonth,
        backValueDays,
        forwardValueDays,
        retainedEarningsAccount,
        null);
  }

  /**
   * The client profile of a company.
   *
   * @param shortName short name in texts and labels (blank = the company code)
   * @param groupName group the company belongs to, used in labels of group concepts
   * @param logoRef logo printed on documents (theme pack logo or file store reference)
   * @param headOfficeCode code of the head office in files and for records without a branch
   * @param defaultBankCode bank account code proposed by default
   */
  public record ClientProfileRequest(
      @Size(max = 30) String shortName,
      @Size(max = 60) String groupName,
      @Size(max = 300) String logoRef,
      @Size(max = 10) @Pattern(regexp = "[A-Z0-9]*") String headOfficeCode,
      @Size(max = 30) String defaultBankCode) {}
}
