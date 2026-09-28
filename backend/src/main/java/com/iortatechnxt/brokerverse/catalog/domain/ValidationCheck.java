package com.iortatechnxt.brokerverse.catalog.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * One check of the validation checkpoint of a package version (PMADD06), run when the validator
 * releases or returns it.
 *
 * @param seq order on the checklist
 * @param code check code (HIERARCHY, INSURER_TERMS, SHARES, RATES, DATES, TEST_PREMIUM)
 * @param label what is checked, as the validator reads it
 * @param result passed, failed or not applicable
 * @param detail the figures compared ("3 of 3 insurers have terms for 2 included coverages")
 */
@Embeddable
public record ValidationCheck(
    @Column(nullable = false) int seq,
    @Column(nullable = false, length = 30) String code,
    @Column(nullable = false, length = 200) String label,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) Result result,
    @Column(length = 500) String detail) {

  /** Result of a check. */
  public enum Result {
    /** The version meets the check. */
    PASSED,
    /** The version does not meet the check. */
    FAILED,
    /** The check does not apply to this version (e.g. no co-insurance shares). */
    NOT_APPLICABLE
  }
}
