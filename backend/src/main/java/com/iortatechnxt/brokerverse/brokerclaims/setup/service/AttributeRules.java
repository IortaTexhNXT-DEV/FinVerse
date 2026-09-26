package com.iortatechnxt.brokerverse.brokerclaims.setup.service;

import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimCodes;
import com.iortatechnxt.brokerverse.brokerclaims.domain.ClaimPhase;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Validation of the status and settlement type attributes proposed on Claims Setup (FR-CM-040/043),
 * with the FRS messages: a status needs a phase other than CLOSED (reached only through a
 * settlement type) and the party it waits on; follow-up days are a whole number from 1 to 365 or
 * blank (the parameter applies); a settlement type needs its outcome.
 */
final class AttributeRules {

  /** Parties a claim waits on (spec 6.1). */
  static final Set<String> WAITING_ON =
      Set.of("INSURER", "CLAIMANT", "ASSURED", "ADJUSTER", "BDOI");

  /** Outcomes of a settlement type (spec 6.2). */
  static final Set<String> OUTCOMES = Set.of("SETTLED", "CLOSED_WITHOUT_PAYMENT");

  private static final int MAX_FOLLOW_UP_DAYS = 365;

  private AttributeRules() {}

  /**
   * The attribute values of a status proposal.
   *
   * @param p proposal
   * @return attribute name to value (null = remove)
   */
  static Map<String, String> status(AttributeSetupService.StatusAttributes p) {
    String phase = upper(p.phase());
    if (phase == null || !isStatusPhase(phase)) {
      throw new BusinessRuleException("BCL_STATUS_WITHOUT_PHASE", "Set the phase of the status");
    }
    String waitingOn = upper(p.waitingOn());
    if (waitingOn == null || !WAITING_ON.contains(waitingOn)) {
      throw new BusinessRuleException(
          "BCL_WAITING_ON_REQUIRED", "Select the party the claim waits on");
    }
    Map<String, String> values = new LinkedHashMap<>();
    values.put(ClaimCodes.ATTR_PHASE, phase);
    values.put(ClaimCodes.ATTR_WAITING_ON, waitingOn);
    values.put(ClaimCodes.ATTR_FOLLOW_UP_DAYS, followUpDays(p.followUpDays()));
    values.put(
        ClaimCodes.ATTR_AWAITING_PREMIUM_REMITTANCE,
        p.awaitingPremiumRemittance() ? Boolean.TRUE.toString() : null);
    return values;
  }

  /**
   * The attribute values of a settlement type proposal.
   *
   * @param p proposal
   * @return attribute name to value
   */
  static Map<String, String> settlement(AttributeSetupService.SettlementAttributes p) {
    String outcome = upper(p.outcome());
    if (outcome == null || !OUTCOMES.contains(outcome)) {
      throw new BusinessRuleException(
          "BCL_SETTLEMENT_WITHOUT_OUTCOME", "Set the outcome of the settlement type");
    }
    Map<String, String> values = new LinkedHashMap<>();
    values.put(ClaimCodes.ATTR_OUTCOME, outcome);
    values.put(ClaimCodes.ATTR_CLOSES_CLAIM, String.valueOf(p.closesClaim()));
    values.put(
        ClaimCodes.ATTR_REQUIRES_SETTLEMENT_AMOUNT, String.valueOf(p.requiresSettlementAmount()));
    return values;
  }

  /**
   * Validates follow-up days.
   *
   * @param text entered value
   * @return the number as text, null when blank
   */
  static String followUpDays(String text) {
    if (text == null || text.isBlank()) {
      return null;
    }
    String value = text.strip();
    if (!value.matches("\\d{1,3}")) {
      throw invalidDays();
    }
    int days = Integer.parseInt(value);
    if (days < 1 || days > MAX_FOLLOW_UP_DAYS) {
      throw invalidDays();
    }
    return String.valueOf(days);
  }

  private static BusinessRuleException invalidDays() {
    return new BusinessRuleException("BCL_FOLLOW_UP_DAYS_INVALID", "Enter a whole number of days");
  }

  private static boolean isStatusPhase(String phase) {
    for (ClaimPhase p : ClaimPhase.values()) {
      if (p != ClaimPhase.CLOSED && p.name().equals(phase)) {
        return true;
      }
    }
    return false;
  }

  private static String upper(String text) {
    return text == null || text.isBlank() ? null : text.strip().toUpperCase(Locale.ROOT);
  }
}
