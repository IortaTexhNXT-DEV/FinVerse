package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.adjustment.domain.AmountInput;
import com.iortatechnxt.brokerverse.adjustment.domain.Computation;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestTerms;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Date and sign checks of an endorsement request (ADJID.004 negative "invalid date range"): the
 * effective date lies within the invoice's cover, a change of period keeps the term, an extension
 * of the period has a valid range; and whether the request reduces the invoice.
 */
final class RequestChecks {

  private static final String NF_PERIOD_CHANGE = "NF_PERIOD_CHANGE";
  private static final Set<String> PERIOD_TYPES = Set.of(NF_PERIOD_CHANGE, "NF_PERIOD_EXTENSION");

  private RequestChecks() {}

  /**
   * Refuses dates outside the cover or an invalid new period.
   *
   * @param invoice invoice of the request
   * @param t terms
   */
  static void requireDates(OpsInvoice invoice, RequestTerms t) {
    LocalDate inception = invoice.getClassification().inceptionDate();
    LocalDate expiry = invoice.getClassification().expiryDate();
    if (t.effectiveDate().isBefore(inception) || !t.effectiveDate().isBefore(expiry)) {
      throw new BusinessRuleException(
          "ADJ_EFFECTIVE_DATE_OUTSIDE_TERM",
          "The effective date must be within the cover " + inception + " to " + expiry);
    }
    if (PERIOD_TYPES.contains(t.endorsementType()) && !validPeriod(t, inception, expiry)) {
      throw new BusinessRuleException(
          "ADJ_PERIOD_INVALID",
          "Enter a valid new period (a change of period keeps the term of "
              + inception
              + " to "
              + expiry
              + ")");
    }
  }

  private static boolean validPeriod(RequestTerms t, LocalDate inception, LocalDate expiry) {
    if (t.newPeriodFrom() == null
        || t.newPeriodTo() == null
        || !t.newPeriodTo().isAfter(t.newPeriodFrom())) {
      return false;
    }
    return !NF_PERIOD_CHANGE.equals(t.endorsementType())
        || ChronoUnit.DAYS.between(t.newPeriodFrom(), t.newPeriodTo())
            == ChronoUnit.DAYS.between(inception, expiry);
  }

  /**
   * Whether a request reduces the invoice (sets the pending negative adjustment).
   *
   * @param c computation
   * @param t terms
   * @param a amounts
   * @return true for cancellations, write-offs and decreases
   */
  static boolean negative(Computation c, RequestTerms t, AmountInput a) {
    return switch (c) {
      case NONE -> false;
      case SUM_INSURED -> t.sumInsuredChange().signum() < 0;
      case AMOUNTS -> amountSign(a) < 0;
      default -> true;
    };
  }

  private static int amountSign(AmountInput a) {
    BigDecimal premium =
        Stream.of(a.basic(), a.dst(), a.premiumTaxVat(), a.lgt(), a.fst(), a.other())
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (premium.signum() != 0) {
      return premium.signum();
    }
    return a.commission() == null ? 0 : a.commission().signum();
  }
}
