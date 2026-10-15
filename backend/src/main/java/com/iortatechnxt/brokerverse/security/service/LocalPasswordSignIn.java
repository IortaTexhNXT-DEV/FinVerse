package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.config.ProductionSafeguards;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Whether a password held by BrokerVerse may be used to sign in. The users of the bank sign in at
 * Microsoft Entra ID; local passwords are kept only for the test accounts of the development, SIT
 * and UAT environments (seed data). A production deployment ({@code prod} profile or {@code
 * brokerverse.environment=production}) refuses every local-password sign-in, password change and
 * password reset, whatever the sign-in mode parameter says, and so does any deployment that sets
 * {@code brokerverse.security.local-password-sign-in=false}.
 */
@Component
public class LocalPasswordSignIn {

  /** Property switching local-password sign-in off outside production. */
  public static final String PROPERTY = "brokerverse.security.local-password-sign-in";

  private final boolean allowed;

  /**
   * Creates the policy.
   *
   * @param environment resolved environment
   */
  public LocalPasswordSignIn(Environment environment) {
    this.allowed = ProductionSafeguards.localPasswordSignIn(environment);
  }

  /**
   * Whether local-password sign-in is allowed in this deployment.
   *
   * @return false in production
   */
  public boolean allowed() {
    return allowed;
  }
}
