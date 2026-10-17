package com.iortatechnxt.brokerverse.security.service;

/** How the first factor of a sign-in was given (stored on the session, V1180). */
public final class SignInMethod {

  /** Password held by BrokerVerse or checked by the directory. */
  public static final String PASSWORD = "PASSWORD";

  /** OpenID Connect identity provider. */
  public static final String OIDC = "OIDC";

  /** SAML 2.0 identity provider. */
  public static final String SAML = "SAML";

  private SignInMethod() {}
}
