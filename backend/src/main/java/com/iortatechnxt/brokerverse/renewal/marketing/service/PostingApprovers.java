package com.iortatechnxt.brokerverse.renewal.marketing.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalNotices;
import com.iortatechnxt.brokerverse.renewal.service.RenewalScope;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.AppUserRepository;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The approver of a submission for posting (FRRN.016.01): the officer picks a Team Leader of the
 * renewal's unit, who then finds the account Submitted for Posting in his list; without an approver
 * every Team Leader of the unit sees it. {@value #PARAMETER} makes the choice required, optional
 * (by default) or not offered.
 */
@Component
public class PostingApprovers {

  /** Parameter: REQUIRED, OPTIONAL or NONE. */
  public static final String PARAMETER = "RNW_POSTING_APPROVER";

  private final SystemParameterService parameters;
  private final AppUserRepository users;
  private final RenewalScope scope;
  private final RenewalNotices notices;

  /**
   * Creates the component.
   *
   * @param parameters system parameters
   * @param users users
   * @param scope units of a user
   * @param notices notifications
   */
  public PostingApprovers(
      SystemParameterService parameters,
      AppUserRepository users,
      RenewalScope scope,
      RenewalNotices notices) {
    this.parameters = parameters;
    this.users = users;
    this.scope = scope;
    this.notices = notices;
  }

  /**
   * The setting.
   *
   * @return REQUIRED, OPTIONAL or NONE
   */
  public String mode() {
    return parameters.text(PARAMETER, "OPTIONAL").strip();
  }

  /**
   * The Team Leaders who may approve the posting of renewals of a unit.
   *
   * @param companyId company
   * @param unit Marketing unit of the renewal
   * @return approvers by name
   */
  public List<Approver> of(Long companyId, String unit) {
    return users.findUsernamesWithPermission(Permission.RNW_REVIEW).stream()
        .map(u -> users.findByUsernameIgnoreCase(u).filter(AppUser::isEnabled))
        .flatMap(java.util.Optional::stream)
        .filter(u -> unit == null || scope.unitsOf(companyId, u.getUsername()).contains(unit))
        .map(u -> new Approver(u.getUsername(), u.getFullName()))
        .sorted((a, b) -> a.fullName().compareTo(b.fullName()))
        .toList();
  }

  /**
   * Checks the approver chosen for a renewal.
   *
   * @param c renewal
   * @param approver user name, may be null
   * @return the approver, null for every Team Leader
   */
  public String require(RenewalCandidate c, String approver) {
    String chosen = approver == null || approver.isBlank() ? null : approver.strip();
    String mode = mode();
    if (chosen == null) {
      if ("REQUIRED".equals(mode)) {
        throw new BusinessRuleException("RNW_APPROVER_REQUIRED", "Select the approver");
      }
      return null;
    }
    if ("NONE".equals(mode)) {
      return null;
    }
    boolean valid =
        of(c.getCompanyId(), c.getOwnerUnit()).stream().anyMatch(a -> a.username().equals(chosen));
    if (!valid) {
      throw new BusinessRuleException(
          "RNW_APPROVER_INVALID", chosen + " is not a Team Leader of unit " + c.getOwnerUnit());
    }
    return chosen;
  }

  /**
   * Tells the approver that an account waits for his approval.
   *
   * @param c renewal submitted
   * @param approver approver, null for none
   */
  public void notify(RenewalCandidate c, String approver) {
    if (approver == null) {
      return;
    }
    notices.users(
        List.of(approver),
        RenewalCodes.EVENT_ASSIGNED,
        c,
        new RenewalNotices.Text(
            c.getRenewalRef() + " submitted for posting",
            "Approve the posting of the renewal of " + c.getSnapshot().clientName()));
  }

  /**
   * An approver.
   *
   * @param username user name
   * @param fullName name
   */
  public record Approver(String username, String fullName) {}
}
