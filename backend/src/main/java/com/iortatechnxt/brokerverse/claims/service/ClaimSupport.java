package com.iortatechnxt.brokerverse.claims.service;

import com.iortatechnxt.brokerverse.claims.domain.Claim;
import com.iortatechnxt.brokerverse.claims.domain.ClaimRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.currency.domain.RateType;
import com.iortatechnxt.brokerverse.currency.service.CurrencyService;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

/**
 * Helpers shared by the claim services: claim loading, document numbers ({@code
 * <prefix>-<branch>-<year>-000001}), base-currency conversion, the current user and time, and the
 * checker's authorization limit (the same control journals and payments apply).
 */
@Component
public class ClaimSupport {

  /** Source module recorded on events, journals and open items. */
  public static final String MODULE = "CLAIMS";

  /** Entity name of claims in the audit trail and attachments. */
  public static final String CLAIM_ENTITY = "Claim";

  private final ClaimRepository claims;
  private final DocumentNumberService numbers;
  private final OrganizationService organization;
  private final CurrencyService currencies;
  private final UserDirectory users;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the helper.
   *
   * @param claims claim repository
   * @param numbers document numbering
   * @param organization companies and branches
   * @param currencies exchange rates
   * @param users authorization limits
   * @param currentUser current user
   * @param clock clock
   */
  public ClaimSupport(
      ClaimRepository claims,
      DocumentNumberService numbers,
      OrganizationService organization,
      CurrencyService currencies,
      UserDirectory users,
      CurrentUser currentUser,
      Clock clock) {
    this.claims = claims;
    this.numbers = numbers;
    this.organization = organization;
    this.currencies = currencies;
    this.users = users;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Loads a claim with its parties.
   *
   * @param id claim id
   * @return claim
   * @throws ResourceNotFoundException when unknown
   */
  public Claim claim(Long id) {
    return claims
        .findWithDetailsById(id)
        .orElseThrow(() -> new ResourceNotFoundException(CLAIM_ENTITY, id));
  }

  /**
   * Allocates a document number such as {@code CL-HO-2026-000001}.
   *
   * @param prefix series prefix (CL, CS, CR, LPO)
   * @param branchId branch
   * @param date document date (gives the year)
   * @return number
   */
  public String nextNumber(String prefix, Long branchId, LocalDate date) {
    String branch = organization.getBranch(branchId).getCode();
    return numbers.next(prefix + "-" + branch + "-" + date.getYear());
  }

  /**
   * SPOT rate of a claim's currency to the company base currency on a date.
   *
   * @param claim claim
   * @param date date
   * @return rate (1 for base-currency claims)
   */
  public BigDecimal rate(Claim claim, LocalDate date) {
    String base = organization.getCompany(claim.getCompanyId()).getBaseCurrency();
    return currencies.rateOn(base, claim.getCurrency(), RateType.SPOT, date);
  }

  /**
   * Converts a claim-currency amount to base currency at the SPOT rate of a date.
   *
   * @param claim claim
   * @param amount amount in claim currency
   * @param date date
   * @return base amount
   */
  public BigDecimal toBase(Claim claim, BigDecimal amount, LocalDate date) {
    return Money.convert(amount, rate(claim, date));
  }

  /**
   * Fails when a base-currency amount exceeds the current user's authorization limit.
   *
   * @param baseAmount amount in base currency
   * @param what document label for the message
   */
  public void requireWithinLimit(BigDecimal baseAmount, String what) {
    users
        .authorizationLimit(user())
        .filter(limit -> baseAmount.compareTo(limit) > 0)
        .ifPresent(
            limit -> {
              throw new BusinessRuleException(
                  "AUTHORIZATION_LIMIT_EXCEEDED",
                  what + ": amount " + baseAmount + " exceeds your authorization limit " + limit);
            });
  }

  /**
   * Current user name.
   *
   * @return user
   */
  public String user() {
    return currentUser.username();
  }

  /**
   * Current time.
   *
   * @return instant
   */
  public Instant now() {
    return clock.instant();
  }

  /**
   * Accounting date of an approval on a claim: the requested date (today when null), which cannot
   * precede the date the claim was reported.
   *
   * @param claim claim
   * @param requested requested date, may be null
   * @return accounting date
   */
  public LocalDate accountingDate(Claim claim, LocalDate requested) {
    LocalDate date = dateOrToday(requested);
    if (date.isBefore(claim.getLoss().getReportedDate())) {
      throw new BusinessRuleException(
          "DATE_BEFORE_NOTIFICATION",
          "The accounting date " + date + " is before the claim was reported");
    }
    return date;
  }

  /**
   * A date, or today when null.
   *
   * @param date date or null
   * @return date
   */
  public LocalDate dateOrToday(LocalDate date) {
    return date != null ? date : LocalDate.now(clock);
  }
}
