package com.iortatechnxt.finverse.payables.service;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.security.CurrentUser;
import com.iortatechnxt.finverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.finverse.currency.service.CurrencyService;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.payables.domain.ApprovableDocument;
import com.iortatechnxt.finverse.security.service.UserDirectory;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Shared helpers of the payables module: document numbers, base currency conversion and the checker
 * controls (segregation of duties and authorization limit) applied to every approval.
 */
@Component
public class PayablesSupport {

  /** Source module recorded on journals and open items. */
  public static final String MODULE = "PAYABLES";

  private final DocumentNumberService numbers;
  private final OrganizationService organization;
  private final CurrencyService currencies;
  private final UserDirectory users;
  private final CurrentUser currentUser;

  /**
   * Creates the helper.
   *
   * @param numbers document numbering
   * @param organization organization service
   * @param currencies currency service
   * @param users user directory
   * @param currentUser current user
   */
  public PayablesSupport(
      DocumentNumberService numbers,
      OrganizationService organization,
      CurrencyService currencies,
      UserDirectory users,
      CurrentUser currentUser) {
    this.numbers = numbers;
    this.organization = organization;
    this.currencies = currencies;
    this.users = users;
    this.currentUser = currentUser;
  }

  /**
   * Allocates a document number such as {@code PV-HO-2026-000001}.
   *
   * @param prefix document prefix (SI, PV, PCD, PCR)
   * @param branchId branch
   * @param date document date (gives the year)
   * @return number
   */
  public String nextNumber(String prefix, Long branchId, LocalDate date) {
    String branch = organization.requireActiveBranch(branchId).getCode();
    return numbers.next(prefix + "-" + branch + "-" + date.getYear());
  }

  /**
   * Converts an amount to the company base currency at the SPOT rate of a date.
   *
   * @param companyId company
   * @param currency currency
   * @param amount amount
   * @param date date
   * @return base amount
   */
  public BigDecimal toBase(Long companyId, String currency, BigDecimal amount, LocalDate date) {
    String base = organization.getCompany(companyId).getBaseCurrency();
    return currencies.toBase(base, currency, amount, date);
  }

  /**
   * Returns the current user after checking that they may approve the document: not its maker and
   * within their authorization limit.
   *
   * @param document document to approve
   * @param baseAmount amount in base currency
   * @return checker user name
   */
  public String checker(ApprovableDocument document, BigDecimal baseAmount) {
    String user = currentUser.username();
    document.requireChecker(user);
    users
        .authorizationLimit(user)
        .filter(limit -> baseAmount.compareTo(limit) > 0)
        .ifPresent(
            limit -> {
              throw new BusinessRuleException(
                  "AUTHORIZATION_LIMIT_EXCEEDED",
                  "Amount " + baseAmount + " exceeds your authorization limit " + limit);
            });
    return user;
  }

  /**
   * Current user name.
   *
   * @return user
   */
  public String user() {
    return currentUser.username();
  }
}
