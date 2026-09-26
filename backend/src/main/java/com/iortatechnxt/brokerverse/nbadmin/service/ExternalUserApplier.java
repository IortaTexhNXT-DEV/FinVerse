package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.ExternalParty;
import com.iortatechnxt.brokerverse.security.domain.AccessChange;
import com.iortatechnxt.brokerverse.security.domain.AccessChangeActivity;
import com.iortatechnxt.brokerverse.security.domain.AccessSubjectType;
import com.iortatechnxt.brokerverse.security.service.AccessChangeRecorder;
import com.iortatechnxt.brokerverse.security.service.ChangeAuthority;
import org.springframework.stereotype.Component;

/**
 * Applies an approved request of user type EXTERNAL (decision D7; USER_ACCESS_DESIGN section 4.4):
 * the port {@link ExternalUserProvisioner} creates, disables or enables the portal user, and the
 * access change log records it with subject type EXTERNAL_USER, the request number and approver.
 */
@Component
public class ExternalUserApplier {

  private final ExternalUserProvisioner provisioner;
  private final AccessChangeRecorder changes;

  /**
   * Creates the applier.
   *
   * @param provisioner external user port
   * @param changes access change log
   */
  public ExternalUserApplier(ExternalUserProvisioner provisioner, AccessChangeRecorder changes) {
    this.provisioner = provisioner;
    this.changes = changes;
  }

  /**
   * Provisions the external user of an approved request.
   *
   * @param r approved EXTERNAL request
   */
  public void apply(AccessRequest r) {
    ExternalParty party = r.getExternal();
    ExternalUserAccount account =
        new ExternalUserAccount(
            r.getRequestNo(),
            r.getUsername(),
            r.getFullName(),
            r.getEmail(),
            party.kind(),
            party.code(),
            party.portalRole(),
            r.getDecidedBy());
    AccessChangeActivity activity;
    switch (AccessRequestValidator.action(r.getRequestType())) {
      case DISABLE -> {
        provisioner.disable(account);
        activity = AccessChangeActivity.DISABLE_USER;
      }
      case ENABLE -> {
        provisioner.enable(account);
        activity = AccessChangeActivity.ENABLE_USER;
      }
      default -> {
        provisioner.create(account);
        activity = AccessChangeActivity.CREATE_USER;
      }
    }
    changes.record(
        new AccessChange(
            AccessSubjectType.EXTERNAL_USER,
            r.getUsername(),
            activity,
            "party",
            null,
            party.kind() + " " + party.code() + " " + party.portalRole()),
        ChangeAuthority.request(r.getRequestNo(), r.getDecidedBy()));
  }
}
