package com.iortatechnxt.brokerverse.submitted.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Lists a submitted policy is on (BRIDSP-12, 14): Free First Year auto loans, BDO / SM Group
 * employee accounts and No Touch accounts. The disposition rules read them.
 *
 * @param ffy Free First Year
 * @param employeeAccount BDO / SM Group employee account
 * @param noTouch No Touch account
 */
@Embeddable
public record SbmMarks(
    @Column(name = "ffy", nullable = false) boolean ffy,
    @Column(name = "employee_account", nullable = false) boolean employeeAccount,
    @Column(name = "no_touch", nullable = false) boolean noTouch) {

  /** On no list. */
  public static final SbmMarks NONE = new SbmMarks(false, false, false);
}
