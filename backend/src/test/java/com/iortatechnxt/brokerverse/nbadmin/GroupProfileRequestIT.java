package com.iortatechnxt.brokerverse.nbadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.approval.service.ApprovalInboxService;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.RequestedRole;
import com.iortatechnxt.brokerverse.nbadmin.domain.RolePermissionChange;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessImplementationService;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestReturnService;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestService;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessSettings;
import com.iortatechnxt.brokerverse.security.domain.AccessChangeLog;
import com.iortatechnxt.brokerverse.security.domain.AccessChangeLogRepository;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.domain.PrivilegeLevel;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.domain.RoleRepository;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Group-profile requests (BRD 3.002.1-4; FR-UA-040 to FR-UA-045): approvers in order, return
 * restarting the chain, FOR_IMPLEMENTATION and the implementation by the System Administrator.
 */
@IntegrationTest
class GroupProfileRequestIT {

  private static final String BADMIN = "badmin";
  private static final String FIRST = "uamapprover";
  private static final String SECOND = "approver";

  @Autowired private AccessRequestService requests;
  @Autowired private AccessRequestReturnService returns;
  @Autowired private AccessImplementationService implementations;
  @Autowired private ApprovalInboxService inbox;
  @Autowired private RoleRepository roles;
  @Autowired private AccessChangeLogRepository changeLog;
  @Autowired private SystemParameterService parameters;
  @Autowired private AsUser as;

  private static String code() {
    return "GP_" + ThreadLocalRandom.current().nextInt(1_000_000, 9_999_999);
  }

  private static AccessRequestContent create(String code, Set<String> permissions) {
    return AccessRequestContent.groupProfile(
        AccessRequestType.CREATE_ROLE,
        new RolePermissionChange(code, permissions, Set.of()),
        new RequestedRole("Processing Team Lead 2", "Second lead", PrivilegeLevel.STANDARD),
        "New profile for the Makati branch");
  }

  private AccessRequest submit(AccessRequestContent content, List<String> approvers) {
    return as.run(BADMIN, () -> requests.create(content, false, approvers));
  }

  @Test
  void approversDecideInOrderAndTheAdministratorImplements() {
    String code = code();
    AccessRequest request = submit(create(code, Set.of("REPORT_VIEW")), List.of(FIRST, SECOND));
    assertThat(request.getAssignedApprover()).isEqualTo(FIRST);
    assertThat(inboxOf(SECOND)).doesNotContain(request.getRequestNo());
    as.run(FIRST, () -> requests.approve(request.getId(), null));
    assertThat(requests.get(request.getId()).getAssignedApprover()).isEqualTo(SECOND);
    assertThat(inboxOf(SECOND)).contains(request.getRequestNo());

    as.run(SECOND, () -> returns.returnRequest(request.getId(), "Add AUDIT_VIEW"));
    as.run(
        BADMIN,
        () -> requests.edit(request.getId(), create(code, Set.of("REPORT_VIEW", "AUDIT_VIEW"))));
    AccessRequest again =
        as.run(BADMIN, () -> requests.submit(request.getId(), List.of(FIRST, SECOND), "Added"));
    assertThat(again.getAssignedApprover()).isEqualTo(FIRST);
    as.run(FIRST, () -> requests.approve(request.getId(), null));
    as.run(SECOND, () -> requests.approve(request.getId(), null));
    assertThat(requests.get(request.getId()).getStatus())
        .isEqualTo(AccessRequestStatus.FOR_IMPLEMENTATION);
    assertThat(roles.findByCode(code)).isEmpty();
    assertThat(inboxOf("admin")).contains(request.getRequestNo());

    assertThatThrownBy(() -> as.run(BADMIN, () -> implementations.implement(request.getId())))
        .extracting("code")
        .isEqualTo("ACCESS_IMPLEMENTER_IS_REQUESTER");
    AccessRequest done = as.run("admin", () -> implementations.implement(request.getId()));
    assertThat(done.getStatus()).isEqualTo(AccessRequestStatus.IMPLEMENTED);
    assertThat(done.getImplementedBy()).isEqualTo("admin");
    Role role = roles.findByCode(code).orElseThrow();
    assertThat(role.getPermissions())
        .containsExactlyInAnyOrder(Permission.REPORT_VIEW, Permission.AUDIT_VIEW);
    assertThat(changeLog.findByRequestNoOrderById(request.getRequestNo()))
        .extracting(AccessChangeLog::getApprovedBy)
        .containsOnly(SECOND);

    deactivateAndReactivate(code);
  }

  private void deactivateAndReactivate(String code) {
    AccessRequest off =
        submit(
            AccessRequestContent.groupProfile(
                AccessRequestType.DEACTIVATE_ROLE,
                new RolePermissionChange(code, Set.of(), Set.of()),
                null,
                "No longer used"),
            List.of(FIRST));
    as.run(FIRST, () -> requests.approve(off.getId(), null));
    as.run("admin", () -> implementations.implement(off.getId()));
    assertThat(roles.findByCode(code).orElseThrow().isActive()).isFalse();
    assertThatThrownBy(
            () ->
                submit(
                    AccessRequestContent.groupProfile(
                        AccessRequestType.DEACTIVATE_ROLE,
                        new RolePermissionChange(code, Set.of(), Set.of()),
                        null,
                        "Again"),
                    List.of(FIRST)))
        .extracting("code")
        .isEqualTo("ACCESS_ROLE_ALREADY_INACTIVE");
    AccessRequest on =
        submit(
            AccessRequestContent.groupProfile(
                AccessRequestType.REACTIVATE_ROLE,
                new RolePermissionChange(code, Set.of(), Set.of()),
                null,
                "Needed again"),
            List.of(FIRST));
    as.run(FIRST, () -> requests.approve(on.getId(), null));
    as.run("admin", () -> implementations.implement(on.getId()));
    assertThat(roles.findByCode(code).orElseThrow().isActive()).isTrue();
  }

  @Test
  void groupProfileRequestsAreChecked() {
    assertCode(create(code(), Set.of("REPORT_VIEW")), List.of(), "ACCESS_APPROVER_REQUIRED");
    assertCode(
        create(code(), Set.of("REPORT_VIEW")), List.of(FIRST, FIRST), "ACCESS_APPROVER_TWICE");
    assertCode(create(code(), Set.of()), List.of(FIRST), "ACCESS_NO_PERMISSION");
    assertCode(create("MKT_AO", Set.of("REPORT_VIEW")), List.of(FIRST), "DUPLICATE");
    assertCode(create("bad code", Set.of("REPORT_VIEW")), List.of(FIRST), "ACCESS_ROLE_CODE");
    assertCode(
        AccessRequestContent.groupProfile(
            AccessRequestType.DEACTIVATE_ROLE,
            new RolePermissionChange("SYSADMIN", Set.of(), Set.of()),
            null,
            "x"),
        List.of(FIRST),
        "ACCESS_ROLE_PROTECTED");
    assertCode(
        AccessRequestContent.groupProfile(
            AccessRequestType.REACTIVATE_ROLE,
            new RolePermissionChange("MKT_TL", Set.of(), Set.of()),
            null,
            "x"),
        List.of(FIRST),
        "ACCESS_ROLE_ALREADY_ACTIVE");
  }

  @Test
  void roleChangeAppliesAtApprovalWhenConfigured() {
    String code = code();
    AccessRequest created = submit(create(code, Set.of("REPORT_VIEW")), List.of(FIRST));
    as.run(FIRST, () -> requests.approve(created.getId(), null));
    as.run("admin", () -> implementations.implement(created.getId()));
    as.run("admin", () -> parameters.update(AccessSettings.ROLE_APPLY_ON_APPROVAL, "true"));
    try {
      AccessRequest change =
          submit(
              AccessRequestContent.rolePermissions(
                  new RolePermissionChange(code, Set.of("AUDIT_VIEW"), Set.of()), "Audit"),
              List.of(FIRST));
      as.run(FIRST, () -> requests.approve(change.getId(), null));
      assertThat(requests.get(change.getId()).getStatus()).isEqualTo(AccessRequestStatus.APPROVED);
      assertThat(roles.findByCode(code).orElseThrow().getPermissions())
          .contains(Permission.AUDIT_VIEW);
    } finally {
      as.run("admin", () -> parameters.update(AccessSettings.ROLE_APPLY_ON_APPROVAL, "false"));
    }
  }

  private void assertCode(AccessRequestContent content, List<String> approvers, String code) {
    assertThatThrownBy(() -> submit(content, approvers)).extracting("code").isEqualTo(code);
  }

  private List<String> inboxOf(String user) {
    return as.run(user, () -> inbox.inbox(null)).stream().map(PendingApproval::reference).toList();
  }
}
