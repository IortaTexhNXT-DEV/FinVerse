package com.iortatechnxt.brokerverse.identity.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;

/**
 * A provisioning event or a directory request that a rule refuses; the event is kept as refused.
 */
public class IdentityRefused extends BusinessRuleException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates the refusal.
   *
   * @param code error code
   * @param message message
   */
  public IdentityRefused(String code, String message) {
    super(code, message);
  }

  /**
   * Creates the refusal with its cause.
   *
   * @param code error code
   * @param message message
   * @param cause cause
   */
  public IdentityRefused(String code, String message, Throwable cause) {
    super(code, message, cause);
  }
}
