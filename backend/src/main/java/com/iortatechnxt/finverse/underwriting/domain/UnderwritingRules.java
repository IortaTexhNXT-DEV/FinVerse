package com.iortatechnxt.finverse.underwriting.domain;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.party.domain.Party;
import com.iortatechnxt.finverse.party.domain.PartyType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/** Validation rules shared by quotations, policies and open covers. */
public final class UnderwritingRules {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private UnderwritingRules() {}

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
    boolean coinsured = businessType == BusinessType.DIRECT_WITH_COINSURANCE;
    if (!coinsured && (sharePct.compareTo(HUNDRED) != 0 || coinsurer != null)) {
      throw new BusinessRuleException(
          "INVALID_SHARE", "Direct business is written at 100 % without coinsurer");
    }
    if (coinsured && (coinsurer == null || coinsurer.getPartyType() != PartyType.COINSURER)) {
      throw new BusinessRuleException(
          "COINSURER_REQUIRED", "Coinsured business needs a coinsurer party");
    }
    if (coinsured && sharePct.compareTo(HUNDRED) == 0) {
      throw new BusinessRuleException(
          "INVALID_SHARE", "Coinsured business must have a share below 100 %");
    }
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
