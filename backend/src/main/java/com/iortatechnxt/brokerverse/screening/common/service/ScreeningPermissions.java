package com.iortatechnxt.brokerverse.screening.common.service;

/**
 * Permission names and SpEL expressions of the screening module (design 6.1), shared by the
 * screening sub-packages so that every endpoint states its guard the same way.
 */
public final class ScreeningPermissions {

  /** Module code in the approval inbox, alerts and audit (design 8). */
  public static final String MODULE = "SCREENING";

  /** View cases, lists and the client screening tab. */
  public static final String VIEW = "SCR_VIEW";

  /** Draft and submit configuration versions (SNSRP-101-108). */
  public static final String CONFIG_MAINTAIN = "SCR_CONFIG_MAINTAIN";

  /** Approve / reject configuration versions (SNSRP-109). */
  public static final String CONFIG_APPROVE = "SCR_CONFIG_APPROVE";

  /** Add / change / deactivate list entries, upload list files (SNSRP-201, 203). */
  public static final String LIST_MAINTAIN = "SCR_LIST_MAINTAIN";

  /** Approve / reject list changes (SNSRP-204). */
  public static final String LIST_APPROVE = "SCR_LIST_APPROVE";

  /** Guard: configuration maker. */
  public static final String HAS_CONFIG_MAINTAIN = "hasAuthority('SCR_CONFIG_MAINTAIN')";

  /** Guard: configuration checker. */
  public static final String HAS_CONFIG_APPROVE = "hasAuthority('SCR_CONFIG_APPROVE')";

  /** Guard: configuration maker or checker (reading versions). */
  public static final String HAS_CONFIG_ACCESS =
      "hasAnyAuthority('SCR_CONFIG_MAINTAIN','SCR_CONFIG_APPROVE','SCR_VIEW')";

  /** Guard: list maker. */
  public static final String HAS_LIST_MAINTAIN = "hasAuthority('SCR_LIST_MAINTAIN')";

  /** Guard: list checker. */
  public static final String HAS_LIST_APPROVE = "hasAuthority('SCR_LIST_APPROVE')";

  /** Guard: reading watchlists, sources and runs. */
  public static final String HAS_LIST_ACCESS =
      "hasAnyAuthority('SCR_VIEW','SCR_LIST_MAINTAIN','SCR_LIST_APPROVE')";

  private ScreeningPermissions() {}
}
