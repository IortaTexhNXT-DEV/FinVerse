package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestRepository;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.ExternalParty;
import com.iortatechnxt.brokerverse.nbadmin.service.ExternalUserProvisioner.ExternalUserAction;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks an access request (FR-UA-010): light checks (formats only) when a draft is saved, full
 * checks on submission and again at approval. Internal users are checked by {@link
 * UserRequestValidator}, group profiles by {@link GroupProfileRequestValidator}, external (portal)
 * users by the port {@link ExternalUserProvisioner} (decision D7).
 */
@Component
@Transactional(readOnly = true)
public class AccessRequestValidator {

  private static final Set<AccessRequestType> EXTERNAL_TYPES =
      EnumSet.of(
          AccessRequestType.CREATE_USER,
          AccessRequestType.DISABLE_USER,
          AccessRequestType.ENABLE_USER);

  private final UserRequestValidator users;
  private final GroupProfileRequestValidator groupProfiles;
  private final ExternalUserProvisioner externalUsers;
  private final AccessRequestRepository requests;
  private final Clock clock;

  /**
   * Creates the validator.
   *
   * @param users internal user checks
   * @param groupProfiles group-profile checks
   * @param externalUsers external (portal) users (port)
   * @param requests requests (one open request per user or role)
   * @param clock clock
   */
  public AccessRequestValidator(
      UserRequestValidator users,
      GroupProfileRequestValidator groupProfiles,
      ExternalUserProvisioner externalUsers,
      AccessRequestRepository requests,
      Clock clock) {
    this.users = users;
    this.groupProfiles = groupProfiles;
    this.externalUsers = externalUsers;
    this.requests = requests;
    this.clock = clock;
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
    if (c.type().isGroupProfile()) {
      if (c.roleCode() == null || c.roleCode().isBlank()) {
        throw new BusinessRuleException("ACCESS_ROLE", "Select the role to change");
      }
    } else {
      UserRequestValidator.requireUsername(c.username());
    }
    requireEffectiveDate(c);
    return c.withJustification(trimmed(c.justification()));
  }

  private AccessRequestContent check(AccessRequestContent c) {
    if (c.justification() == null || c.justification().isBlank()) {
      throw new BusinessRuleException("ACCESS_JUSTIFICATION", "Enter the justification");
    }
    requireEffectiveDate(c);
    AccessRequestContent trimmedContent = c.withJustification(c.justification().trim());
    if (c.type().isGroupProfile()) {
      return groupProfiles.validate(trimmedContent);
    }
    if (c.external() != null) {
      return external(trimmedContent);
    }
    return users.validate(trimmedContent);
  }

  private AccessRequestContent external(AccessRequestContent c) {
    ExternalParty party = c.external();
    if (!EXTERNAL_TYPES.contains(c.type())) {
      throw new BusinessRuleException(
          "ACCESS_EXTERNAL_TYPE", "External users are created, disabled or enabled only");
    }
    if (party.kind() == null || party.code() == null) {
      throw new BusinessRuleException(
          "ACCESS_EXTERNAL_PARTY", "Select the insurer or client of the external user");
    }
    String username = UserRequestValidator.requireUsername(c.username());
    externalUsers.validate(
        action(c.type()),
        new ExternalUserAccount(
            null,
            username,
            c.fullName(),
            c.email(),
            party.kind(),
            party.code(),
            party.portalRole(),
            null));
    return c;
  }

  /**
   * The provisioner action of an external request.
   *
   * @param type CREATE_USER, DISABLE_USER or ENABLE_USER
   * @return action
   */
  static ExternalUserAction action(AccessRequestType type) {
    return switch (type) {
      case DISABLE_USER -> ExternalUserAction.DISABLE;
      case ENABLE_USER -> ExternalUserAction.ENABLE;
      default -> ExternalUserAction.CREATE;
    };
  }

  private void requireEffectiveDate(AccessRequestContent c) {
    if (c.effectiveFrom() == null) {
      return;
    }
    if (c.type().isGroupProfile()) {
      throw new BusinessRuleException(
          "ACCESS_EFFECTIVE_DATE_USER_ONLY",
          "An effective date applies to user requests; group profiles are implemented");
    }
    if (c.effectiveFrom().isBefore(LocalDate.now(clock))) {
      throw new BusinessRuleException(
          "ACCESS_EFFECTIVE_DATE", "The effective date cannot be before today");
    }
  }

  private void requireNoOpenRequest(AccessRequestContent clean, Long requestId) {
    Long self = requestId == null ? Long.valueOf(-1L) : requestId;
    boolean groupProfile = clean.type().isGroupProfile();
    boolean open =
        groupProfile
            ? requests.existsByRoleCodeAndStatusInAndIdNot(
                clean.roleCode(), AccessRequestStatus.OPEN, self)
            : requests.existsByUsernameIgnoreCaseAndStatusInAndIdNot(
                clean.username(), AccessRequestStatus.OPEN, self);
    if (open) {
      String subject = groupProfile ? "role " + clean.roleCode() : "user " + clean.username();
      throw new BusinessRuleException(
          "ACCESS_REQUEST_PENDING",
          "A request for " + subject + " is already waiting for approval");
    }
  }

  private static String trimmed(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
