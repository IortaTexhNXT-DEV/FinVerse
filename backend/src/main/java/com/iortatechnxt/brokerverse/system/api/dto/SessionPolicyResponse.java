package com.iortatechnxt.brokerverse.system.api.dto;

/**
 * Session policy applied by the web client (BRNB.040; BDOI FRS FRUM.001.03 and FRUM.001.04).
 *
 * @param timeoutMinutes inactivity timeout
 * @param warningSeconds how long before the inactivity sign-out the warning dialog appears (the
 *     warning shows after SESSION_IDLE_WARNING_MINUTES of inactivity)
 * @param expiryWarningMinutes how long before the system-triggered (absolute) sign-out at the token
 *     expiry the client warns the user
 * @param idleWarningMinutes minutes of inactivity after which the warning shows (the x minutes of
 *     BDOI's warning text)
 * @param bdoiDialog whether the warning uses BDOI's text and the buttons Stay Logged In and Log Out
 *     (SESSION_BDOI_DIALOG)
 * @param timeoutPage whether the inactivity sign-out opens the page "Your session timed out" with
 *     the Log In button (SESSION_TIMEOUT_PAGE)
 */
public record SessionPolicyResponse(
    int timeoutMinutes,
    int warningSeconds,
    int expiryWarningMinutes,
    int idleWarningMinutes,
    boolean bdoiDialog,
    boolean timeoutPage) {}
