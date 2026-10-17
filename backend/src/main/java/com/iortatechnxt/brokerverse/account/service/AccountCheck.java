package com.iortatechnxt.brokerverse.account.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Readiness of an account for submission and validation, as shown on the wizard's review step.
 *
 * @param fieldErrors missing minimum fields by field path (BRNB.003)
 * @param missingDocuments mandatory document types not yet uploaded
 * @param duplicates live accounts insuring the same risk, with their ARN (BRNB.051)
 * @param premiumRated whether the premium has been computed
 * @param tsuRequired whether a TSU routing rule applies (BRNB.098)
 * @param tsuRule matching rule code
 * @param tsuReason explanation of the TSU decision
 * @param tsuCleared whether TSU has cleared the account
 */
public record AccountCheck(
    Map<String, String> fieldErrors,
    List<String> missingDocuments,
    List<DuplicateFinding> duplicates,
    boolean premiumRated,
    boolean tsuRequired,
    String tsuRule,
    String tsuReason,
    boolean tsuCleared) {

  /** Defensive copies. */
  public AccountCheck {
    fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    missingDocuments = List.copyOf(missingDocuments);
    duplicates = List.copyOf(duplicates);
  }

  /**
   * Whether Marketing may submit the account (TSU clearance is checked by Processing).
   *
   * @return true when complete, documented, rated and free of duplicates
   */
  public boolean readyToSubmit() {
    return fieldErrors.isEmpty()
        && missingDocuments.isEmpty()
        && duplicates.isEmpty()
        && premiumRated;
  }
}
