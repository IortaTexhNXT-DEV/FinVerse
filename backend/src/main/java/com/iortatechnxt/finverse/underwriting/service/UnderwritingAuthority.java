package com.iortatechnxt.finverse.underwriting.service;

import com.iortatechnxt.finverse.approval.service.ApprovalViewer;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.common.util.Money;
import com.iortatechnxt.finverse.currency.domain.RateType;
import com.iortatechnxt.finverse.currency.service.CurrencyService;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.security.service.UserDirectory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;

/**
 * Authorization limit of underwriting approvals (policies, marine certificates, endorsements and
 * quotations), the same control as journals and payables: the approver's limit ({@link
 * UserDirectory#authorizationLimit}, none = unlimited) applies to the document's <b>gross premium
 * at 100 %</b> (before discount, loading, share and taxes; a return premium counts at its absolute
 * value), converted to the company base currency at the SPOT rate of the approval (accounting)
 * date, the rate the approval posts with.
 */
@Component
public class UnderwritingAuthority {

  /** Error code, shared with journals, payables and claims. */
  public static final String LIMIT_EXCEEDED = "AUTHORIZATION_LIMIT_EXCEEDED";

  private final UserDirectory users;
  private final CurrencyService currencies;
  private final OrganizationService organization;

  /**
   * Creates the control.
   *
   * @param users user facts (authorization limits)
   * @param currencies exchange rates
   * @param organization companies (base currency)
   */
  public UnderwritingAuthority(
      UserDirectory users, CurrencyService currencies, OrganizationService organization) {
    this.users = users;
    this.currencies = currencies;
    this.organization = organization;
  }

  /**
   * Fails when the premium exceeds the approver's authorization limit. The rate is looked up only
   * for approvers with a limit.
   *
   * @param approver approving user
   * @param document document label for the message, e.g. "Policy P-FIRE-HO-2026-000001"
   * @param premium gross premium of the document
   */
  public void requireWithinLimit(String approver, String document, Premium premium) {
    users
        .authorizationLimit(approver)
        .ifPresent(
            limit -> {
              BigDecimal base = toBase(premium, new HashMap<>());
              if (base.compareTo(limit) > 0) {
                throw new BusinessRuleException(
                    LIMIT_EXCEEDED,
                    document
                        + ": gross premium "
                        + base
                        + " exceeds your authorization limit "
                        + limit);
              }
            });
  }

  /**
   * Approval inbox filter of one viewer: true when the viewer could approve the premium. Always
   * true for the system view (alert on ageing approvals) and for users without a limit; a document
   * whose rate is missing stays visible, the approval itself reports the missing rate.
   *
   * @param viewer inbox viewer
   * @return filter on the premium of a pending document
   */
  public Predicate<Premium> inboxFilter(ApprovalViewer viewer) {
    Optional<BigDecimal> limit =
        viewer.systemView() ? Optional.empty() : users.authorizationLimit(viewer.username());
    if (limit.isEmpty()) {
      return p -> true;
    }
    Map<Long, String> baseCurrencies = new HashMap<>();
    return p -> {
      try {
        return toBase(p, baseCurrencies).compareTo(limit.get()) <= 0;
      } catch (BusinessRuleException missingRate) {
        return true;
      }
    };
  }

  private BigDecimal toBase(Premium p, Map<Long, String> baseCurrencies) {
    String base =
        baseCurrencies.computeIfAbsent(
            p.companyId(), id -> organization.getCompany(id).getBaseCurrency());
    BigDecimal rate = currencies.rateOn(base, p.currency(), RateType.SPOT, p.rateDate());
    return Money.convert(p.grossPremium().abs(), rate);
  }

  /**
   * Gross premium of a document to approve.
   *
   * @param companyId company
   * @param currency document currency
   * @param grossPremium gross premium at 100 % (null counts as zero)
   * @param rateDate approval (accounting) date whose SPOT rate converts it
   */
  public record Premium(
      Long companyId, String currency, BigDecimal grossPremium, LocalDate rateDate) {

    /**
     * Normalizes a missing premium to zero.
     *
     * @param companyId company
     * @param currency currency
     * @param grossPremium gross premium
     * @param rateDate rate date
     */
    public Premium {
      grossPremium = grossPremium == null ? BigDecimal.ZERO : grossPremium;
    }
  }
}
