package com.iortatechnxt.brokerverse.nbadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.RequestedRole;
import com.iortatechnxt.brokerverse.nbadmin.domain.RolePermissionChange;
import com.iortatechnxt.brokerverse.nbadmin.domain.SodRule;
import com.iortatechnxt.brokerverse.nbadmin.domain.SodRuleKind;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestService;
import com.iortatechnxt.brokerverse.nbadmin.service.PermissionNames;
import com.iortatechnxt.brokerverse.nbadmin.service.SodRuleService;
import com.iortatechnxt.brokerverse.security.api.dto.RoleRequest;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.domain.PrivilegeLevel;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * BDOI FRS FRUM.006.03 (conflict C16): a rule of two permissions, authorised by Information
 * Security, refuses a group-profile request that would give one profile both permissions, and a
 * user request that would give one user both through two profiles; a permission both added and
 * removed in one request is refused as before.
 */
@IntegrationTest
class PermissionCombinationIT {

  private static final String BADMIN = "badmin";
  private static final String INFOSEC = "infosec";
  private static final String REQUESTOR = "requestor";
  private static final String APPROVER = "uamapprover";

  @Autowired private SodRuleService rules;
  @Autowired private AccessRequestService requests;
  @Autowired private UserAdminService users;
  @Autowired private AsUser as;

  private String profile(Permission permission) {
    String code = "PC_" + ThreadLocalRandom.current().nextInt(1_000_000, 9_999_999);
    as.run(
        "admin",
        () -> users.createRole(new RoleRequest(code, "Profile " + code, Set.of(permission))));
    return code;
  }

  @Test
  void aRuleOfTwoPermissionsRefusesTheCombinationInAProfileAndForAUser() {
    SodRule rule =
        as.run(
            BADMIN,
            () ->
                rules.create(
                    SodRuleKind.PERMISSIONS,
                    "ACCESS_REQUEST",
                    "ACCESS_APPROVE",
                    "A requester of access does not approve access"));
    assertThat(rule.getKind()).isEqualTo(SodRuleKind.PERMISSIONS);
    assertThatThrownBy(
            () ->
                as.run(
                    BADMIN,
                    () -> rules.create(SodRuleKind.PERMISSIONS, "ACCESS_REQUEST", "NO_SUCH", "x")))
        .extracting("code")
        .isEqualTo("ACCESS_UNKNOWN_PERMISSION");
    as.run(INFOSEC, () -> rules.authorize(rule.getId()));
    try {
      String requester = profile(Permission.ACCESS_REQUEST);
      String approver = profile(Permission.ACCESS_APPROVE);
      assertThatThrownBy(
              () ->
                  as.run(
                      BADMIN,
                      () ->
                          requests.create(
                              AccessRequestContent.groupProfile(
                                  AccessRequestType.MODIFY_ROLE_PERMISSIONS,
                                  new RolePermissionChange(
                                      requester, Set.of("ACCESS_APPROVE"), Set.of()),
                                  null,
                                  "Let the requesters approve"),
                              false,
                              List.of(APPROVER))))
          .extracting("code", "message")
          .containsExactly(
              "ACCESS_PERMISSION_COMBINATION",
              "Group profile Profile "
                  + requester
                  + " may not hold both "
                  + PermissionNames.name("ACCESS_REQUEST")
                  + " and "
                  + PermissionNames.name("ACCESS_APPROVE")
                  + " (rule "
                  + rule.getRuleCode()
                  + ")");
      assertThatThrownBy(
              () ->
                  as.run(
                      BADMIN,
                      () ->
                          requests.create(
                              AccessRequestContent.groupProfile(
                                  AccessRequestType.CREATE_ROLE,
                                  new RolePermissionChange(
                                      "PC_NEW",
                                      Set.of("ACCESS_REQUEST", "ACCESS_APPROVE"),
                                      Set.of()),
                                  new RequestedRole("Both", "Both", PrivilegeLevel.STANDARD),
                                  "One profile for both"),
                              false,
                              List.of(APPROVER))))
          .extracting("code")
          .isEqualTo("ACCESS_PERMISSION_COMBINATION");
      assertThatThrownBy(
              () ->
                  as.run(
                      REQUESTOR,
                      () ->
                          requests.create(
                              new AccessRequestContent(
                                  AccessRequestType.CREATE_USER,
                                  AccessRequestIT.username(),
                                  "Combination User",
                                  "combination@example.ph",
                                  Set.of(requester, approver),
                                  null,
                                  "Joined"),
                              false,
                              List.of(APPROVER))))
          .extracting("code")
          .isEqualTo("ACCESS_PERMISSION_COMBINATION");
    } finally {
      as.run(BADMIN, () -> rules.requestDeactivation(rule.getId()));
      as.run(INFOSEC, () -> rules.authorize(rule.getId()));
    }
  }

  @Test
  void aPermissionBothAddedAndRemovedIsRefused() {
    String code = profile(Permission.REPORT_VIEW);
    assertThatThrownBy(
            () ->
                as.run(
                    BADMIN,
                    () ->
                        requests.create(
                            AccessRequestContent.groupProfile(
                                AccessRequestType.MODIFY_ROLE_PERMISSIONS,
                                new RolePermissionChange(
                                    code, Set.of("AUDIT_VIEW"), Set.of("AUDIT_VIEW")),
                                null,
                                "Undecided"),
                            false,
                            List.of(APPROVER))))
        .extracting("code")
        .isEqualTo("ACCESS_PERMISSION_CONFLICT");
  }
}
