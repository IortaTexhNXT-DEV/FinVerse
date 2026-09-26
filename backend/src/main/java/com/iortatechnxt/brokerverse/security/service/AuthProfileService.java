package com.iortatechnxt.brokerverse.security.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.FieldValidationException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.util.EmailAddresses;
import com.iortatechnxt.brokerverse.security.domain.AccessChange;
import com.iortatechnxt.brokerverse.security.domain.AccessChangeActivity;
import com.iortatechnxt.brokerverse.security.domain.AccessSubjectType;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The signed-in user's own contact details (UQ17; FR-UA-005): e-mail and mobile number, changed on
 * My Profile without an access request. Each changed attribute is written to the access change log
 * (activity MODIFY_USER, done by the user, no request) and to the audit trail. Everything else of
 * the user record changes only through an access request.
 */
@Service
@Transactional
public class AuthProfileService {

  private static final String ENTITY = "AppUser";
  private static final String EMAIL = "email";
  private static final String MOBILE = "mobileNo";
  private static final int MAX_EMAIL = 120;
  private static final Pattern MOBILE_NO = Pattern.compile("\\+?[0-9][0-9 ()-]{6,24}");

  private final AppUserRepository users;
  private final CurrentUser currentUser;
  private final AccessChangeRecorder changes;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param users users
   * @param currentUser current user
   * @param changes access change log
   * @param audit audit trail
   */
  public AuthProfileService(
      AppUserRepository users,
      CurrentUser currentUser,
      AccessChangeRecorder changes,
      AuditTrailService audit) {
    this.users = users;
    this.currentUser = currentUser;
    this.changes = changes;
    this.audit = audit;
  }

  /**
   * The signed-in user.
   *
   * @return user
   */
  @Transactional(readOnly = true)
  public AppUser me() {
    return users
        .findByUsernameIgnoreCase(currentUser.username())
        .orElseThrow(() -> new BusinessRuleException("UNKNOWN_USER", "Unknown user"));
  }

  /**
   * Changes the signed-in user's e-mail address and mobile number; blank clears the mobile number.
   *
   * @param email e-mail address (required)
   * @param mobileNo mobile number, may be blank
   * @return the user
   */
  public AppUser updateContact(String email, String mobileNo) {
    String newEmail = email == null ? "" : email.trim();
    String newMobile = mobileNo == null || mobileNo.isBlank() ? null : mobileNo.trim();
    Map<String, String> errors = new LinkedHashMap<>();
    if (newEmail.isEmpty()) {
      errors.put(EMAIL, "Enter your e-mail address");
    } else if (newEmail.length() > MAX_EMAIL || !EmailAddresses.isValid(newEmail)) {
      errors.put(EMAIL, newEmail + " is not a valid e-mail address");
    }
    if (newMobile != null && !MOBILE_NO.matcher(newMobile).matches()) {
      errors.put(MOBILE, "Enter the mobile number with digits only, for example +63 917 123 4567");
    }
    if (!errors.isEmpty()) {
      throw new FieldValidationException("PROFILE_INVALID", "Check the highlighted fields", errors);
    }
    AppUser user = me();
    int changed =
        change(user, EMAIL, user.getEmail(), newEmail, user::setEmail)
            + change(user, MOBILE, user.getMobileNo(), newMobile, user::setMobileNo);
    if (changed > 0) {
      audit.record(ENTITY, user.getUsername(), AuditAction.UPDATE, "Updated own contact details");
    }
    return user;
  }

  private int change(
      AppUser user, String attribute, String before, String after, Consumer<String> setter) {
    if (Objects.equals(before, after)) {
      return 0;
    }
    setter.accept(after);
    changes.record(
        new AccessChange(
            AccessSubjectType.USER,
            user.getUsername(),
            AccessChangeActivity.MODIFY_USER,
            attribute,
            before,
            after),
        ChangeAuthority.DIRECT);
    return 1;
  }
}
