package com.iortatechnxt.brokerverse.underwriting.domain;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.party.domain.Party;
import com.iortatechnxt.brokerverse.party.domain.PartyType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Validation rules shared by quotations, policies and open covers. */
public final class UnderwritingRules {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private UnderwritingRules() {}

  /**
   * Checks that a policy has at least one risk.
   *
   * @param risks risks
   */
  public static void requireRisks(Collection<?> risks) {
    if (risks.isEmpty()) {
      throw new BusinessRuleException("RISK_REQUIRED", "A policy needs at least one risk");
    }
  }

  /**
   * Checks that every risk insures a positive sum: a risk of zero (or less) covers nothing and
   * would post a policy without exposure. Endorsements do not pass through here, so NIL
   * endorsements with no change in sum insured are unaffected.
   *
   * @param risks risks of a policy or certificate
   */
  public static void requirePositiveSumsInsured(List<RiskValues> risks) {
    for (int i = 0; i < risks.size(); i++) {
      BigDecimal sum = risks.get(i).sumInsured();
      if (sum == null || sum.signum() <= 0) {
        throw new BusinessRuleException(
            "SUM_INSURED_NOT_POSITIVE",
            "Risk " + (i + 1) + ": the sum insured must be greater than zero");
      }
    }
  }

  /**
   * Underwriting year of a policy: the year its cover starts.
   *
   * @param periodFrom cover start
   * @return year
   */
  public static int underwritingYear(LocalDate periodFrom) {
    return periodFrom.getYear();
  }

  /**
   * Checks the cover period.
   *
   * @param from start
   * @param to end
   */
  public static void requirePeriod(LocalDate from, LocalDate to) {
    if (from == null || to == null || to.isBefore(from)) {
      throw new BusinessRuleException(
          "INVALID_PERIOD", "Period to must be on or after period from");
    }
  }

  /**
   * Checks that the intermediary matches the source type (none for direct business, an agent for
   * agency business, a broker for broker business).
   *
   * @param sourceType source type
   * @param intermediary intermediary party, may be null
   */
  public static void requireIntermediary(SourceType sourceType, Party intermediary) {
    Optional<PartyType> expected = sourceType.intermediaryType();
    if (expected.isEmpty()) {
      if (intermediary != null) {
        throw new BusinessRuleException(
            "UNEXPECTED_INTERMEDIARY", "Direct business has no intermediary");
      }
      return;
    }
    if (intermediary == null || intermediary.getPartyType() != expected.get()) {
      throw new BusinessRuleException(
          "INTERMEDIARY_REQUIRED", sourceType + " business needs a " + expected.get() + " party");
    }
  }

  /**
   * Checks share and coinsurer consistency with the business type.
   *
   * @param businessType business type
   * @param sharePct company share %
   * @param coinsurer coinsurer party, may be null
   */
  public static void requireCoinsurance(
      BusinessType businessType, BigDecimal sharePct, Party coinsurer) {
    if (sharePct == null || sharePct.signum() <= 0 || sharePct.compareTo(HUNDRED) > 0) {
      throw new BusinessRuleException("INVALID_SHARE", "Share % must be above 0 and at most 100");
    }
    if (businessType == BusinessType.DIRECT_WITH_COINSURANCE) {
      requireCoinsurer(sharePct, coinsurer);
    } else if (sharePct.compareTo(HUNDRED) != 0 || coinsurer != null) {
      throw new BusinessRuleException(
          "INVALID_SHARE", "Direct business is written at 100 % without coinsurer");
    }
  }

  private static void requireCoinsurer(BigDecimal sharePct, Party coinsurer) {
    if (coinsurer == null || coinsurer.getPartyType() != PartyType.COINSURER) {
      throw new BusinessRuleException(
          "COINSURER_REQUIRED", "Coinsured business needs a coinsurer party");
    }
    if (sharePct.compareTo(HUNDRED) == 0) {
      throw new BusinessRuleException(
          "INVALID_SHARE", "Coinsured business must have a share below 100 %");
    }
  }

  /**
   * Whether a policy covers a date: approved (or cancelled after the date) and within its period.
   *
   * @param status policy status
   * @param cancelledOn cancellation effective date, null when not cancelled
   * @param periodFrom period start
   * @param periodTo period end
   * @param date date
   * @return true when in force
   */
  public static boolean inForce(
      PolicyStatus status,
      LocalDate cancelledOn,
      LocalDate periodFrom,
      LocalDate periodTo,
      LocalDate date) {
    boolean live =
        status == PolicyStatus.APPROVED || cancelledOn != null && date.isBefore(cancelledOn);
    return live && !date.isBefore(periodFrom) && !date.isAfter(periodTo);
  }

  /**
   * Checks that a client party is a policyholder.
   *
   * @param customer party
   */
  public static void requireClient(Party customer) {
    if (customer == null
        || customer.getPartyType().subLedger() != PartyType.INDIVIDUAL_CLIENT.subLedger()) {
      throw new BusinessRuleException("CLIENT_REQUIRED", "The customer must be a client party");
    }
  }
}
