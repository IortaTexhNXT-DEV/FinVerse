package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.nbadmin.domain.ExternalPartyKind;

/**
 * An external (portal) user as an access request of user type EXTERNAL describes it (decision D7;
 * USER_ACCESS_DESIGN section 4.4).
 *
 * @param requestNo access request number (for the provisioner's own audit)
 * @param username portal user name (login)
 * @param fullName full name (create)
 * @param email e-mail the invitation is sent to (create)
 * @param partyKind insurer or client
 * @param partyCode code of the insurer or client
 * @param portalRole portal role (INSURER_USER, CLIENT_HR)
 * @param approvedBy approver of the request; null while the request is only validated
 */
public record ExternalUserAccount(
    String requestNo,
    String username,
    String fullName,
    String email,
    ExternalPartyKind partyKind,
    String partyCode,
    String portalRole,
    String approvedBy) {}
