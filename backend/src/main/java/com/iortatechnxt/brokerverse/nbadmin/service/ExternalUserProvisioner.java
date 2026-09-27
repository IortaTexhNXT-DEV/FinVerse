package com.iortatechnxt.brokerverse.nbadmin.service;

/**
 * Port: provisions external (portal) users of approved access requests of user type EXTERNAL
 * (cross-BRD decision D7; USER_ACCESS_DESIGN section 4.4). Implemented by the {@code portal} module
 * (Employee Benefits wave E1-A): {@code create} makes the portal user INVITED and sends the
 * invitation link (no password is sent), {@code disable} / {@code enable} switch its access.
 *
 * <p>Without a {@code portal} module the default adapter ({@link NbadminPortDefaults}) is not
 * available: EXTERNAL requests are refused with {@value #NOT_AVAILABLE}. {@code nbadmin} never
 * depends on {@code portal}. Every method runs inside the request's transaction; a {@code
 * BusinessRuleException} refuses the submission or the approval.
 */
public interface ExternalUserProvisioner {

  /** Error code of the refusing default. */
  String NOT_AVAILABLE = "EXTERNAL_USERS_NOT_AVAILABLE";

  /**
   * Whether external users can be provisioned at all.
   *
   * @return false for the default adapter
   */
  boolean available();

  /**
   * Checks a request when it is submitted (for example: the party exists, the user name is free for
   * a creation, the user exists for a disable / enable). Nothing is changed.
   *
   * @param action what the request asks for
   * @param account the external user
   */
  void validate(ExternalUserAction action, ExternalUserAccount account);

  /**
   * Creates the portal user and sends the invitation (approved CREATE_USER request).
   *
   * @param account the external user, with the approver
   */
  void create(ExternalUserAccount account);

  /**
   * Disables the portal user (approved DISABLE_USER request).
   *
   * @param account the external user, with the approver
   */
  void disable(ExternalUserAccount account);

  /**
   * Enables the portal user again (approved ENABLE_USER request).
   *
   * @param account the external user, with the approver
   */
  void enable(ExternalUserAccount account);

  /** What an EXTERNAL request asks for. */
  enum ExternalUserAction {
    /** Create and invite. */
    CREATE,
    /** Disable. */
    DISABLE,
    /** Enable. */
    ENABLE
  }
}
