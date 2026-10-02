package com.iortatechnxt.brokerverse.security.service.sso;

/**
 * A single sign-on that cannot complete. The code goes back to the web client (it shows a message
 * in words); the detail is written to the log and the audit trail only.
 */
public class SsoException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** The provider's answer is not valid (signature, audience, validity, state). */
  public static final String INVALID = "SSO_INVALID";

  /** The identity is not linked to an active user of BrokerVerse. */
  public static final String NOT_LINKED = "SSO_NOT_LINKED";

  /** Single sign-on is not configured or not the sign-in mode. */
  public static final String NOT_CONFIGURED = "SSO_NOT_CONFIGURED";

  /** The identity provider could not be reached or refused the sign-in. */
  public static final String PROVIDER_ERROR = "SSO_PROVIDER_ERROR";

  private final String code;

  /**
   * Creates the exception.
   *
   * @param code code for the web client
   * @param detail detail for the log
   */
  public SsoException(String code, String detail) {
    super(detail);
    this.code = code;
  }

  /**
   * Creates the exception with its cause.
   *
   * @param code code for the web client
   * @param detail detail for the log
   * @param cause cause
   */
  public SsoException(String code, String detail, Throwable cause) {
    super(detail, cause);
    this.code = code;
  }

  /**
   * Code for the web client.
   *
   * @return code
   */
  public String getCode() {
    return code;
  }
}
