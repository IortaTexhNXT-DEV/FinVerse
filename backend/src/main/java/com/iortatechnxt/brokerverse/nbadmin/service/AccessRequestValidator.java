package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestRepository;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks an access request (FR-UA-010): light checks (formats only) when a draft is saved, full
 * checks on submission and again at approval by {@link GroupProfileRequestValidator}. Users are
 * maintained in UIDM-ISC: a request about a user is refused ({@code ACCESS_USERS_BY_UIDM}).
 */
@Component
@Transactional(readOnly = true)
public class AccessRequestValidator {

  private final GroupProfileRequestValidator groupProfiles;
  private final AccessRequestRepository requests;

  /**
   * Creates the validator.
   *
   * @param groupProfiles group-profile checks
   * @param requests requests (one open request per group profile)
   */
  public AccessRequestValidator(
      GroupProfileRequestValidator groupProfiles, AccessRequestRepository requests) {
    this.groupProfiles = groupProfiles;
    this.requests = requests;
  }

  /**
   * Full checks of a request about to be submitted (the one-step submission of PMADD05 too).
   *
   * @param c request content
   * @return normalised content
   */
  public AccessRequestContent validate(AccessRequestContent c) {
    AccessRequestContent clean = check(c);
    requireNoOpenRequest(clean, null);
    return clean;
  }

  /**
   * Full checks on submission of a saved request: as {@link #validate} but the request itself does
   * not count as another open request.
   *
   * @param c request content
   * @param requestId the request
   * @return normalised content
   */
  public AccessRequestContent validateSubmission(AccessRequestContent c, Long requestId) {
    AccessRequestContent clean = check(c);
    requireNoOpenRequest(clean, requestId);
    return clean;
  }

  /**
   * Checks the change again at approval (FR-UA-031 R3): the user or role may have changed since
   * submission.
   *
   * @param c request content
   * @return normalised content
   */
  public AccessRequestContent recheck(AccessRequestContent c) {
    return check(c);
  }

  /**
   * Light checks of a draft: the subject is named in a valid format (BRD 1.002.1.2).
   *
   * @param c request content
   * @return content with trimmed remarks
   */
  public AccessRequestContent validateDraft(AccessRequestContent c) {
    if (c.type() == null) {
      throw new BusinessRuleException("ACCESS_TYPE", "Choose the type of request");
    }
    requireGroupProfile(c);
    if (c.roleCode() == null || c.roleCode().isBlank()) {
      throw new BusinessRuleException("ACCESS_ROLE", "Select the role to change");
    }
    return c.withJustification(trimmed(c.justification()));
  }

  private AccessRequestContent check(AccessRequestContent c) {
    if (c.justification() == null || c.justification().isBlank()) {
      throw new BusinessRuleException("ACCESS_JUSTIFICATION", "Enter the justification");
    }
    requireGroupProfile(c);
    return groupProfiles.validate(c.withJustification(c.justification().trim()));
  }

  private static void requireGroupProfile(AccessRequestContent c) {
    if (!c.type().isGroupProfile()) {
      throw new BusinessRuleException(
          AccessRequestPermissions.USERS_BY_UIDM,
          "Users are maintained in UIDM-ISC; only group-profile changes are requested here");
    }
  }

  private void requireNoOpenRequest(AccessRequestContent clean, Long requestId) {
    Long self = requestId == null ? Long.valueOf(-1L) : requestId;
    if (requests.existsByRoleCodeAndStatusInAndIdNot(
        clean.roleCode(), AccessRequestStatus.OPEN, self)) {
      throw new BusinessRuleException(
          "ACCESS_REQUEST_PENDING",
          "A request for role " + clean.roleCode() + " is already waiting for approval");
    }
  }

  private static String trimmed(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
