package com.iortatechnxt.brokerverse.csf.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** One verification check and whether the caller's answer matched (list CSF_VERIFY_CHECK). */
@Embeddable
public class CheckResult {

  @Column(name = "check_code", nullable = false, length = 40)
  private String checkCode;

  @Column(nullable = false)
  private boolean matched;

  protected CheckResult() {}

  /**
   * A check result.
   *
   * @param checkCode check (list CSF_VERIFY_CHECK)
   * @param matched whether the answer matched the client record
   */
  public CheckResult(String checkCode, boolean matched) {
    this.checkCode = checkCode;
    this.matched = matched;
  }

  public String getCheckCode() {
    return checkCode;
  }

  public boolean isMatched() {
    return matched;
  }
}
