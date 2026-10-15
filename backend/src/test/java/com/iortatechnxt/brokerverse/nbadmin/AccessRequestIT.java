package com.iortatechnxt.brokerverse.nbadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.RequestedRole;
import com.iortatechnxt.brokerverse.nbadmin.domain.RolePermissionChange;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessImplementationService;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessMatrix;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessMatrixService;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestService;
import com.iortatechnxt.brokerverse.security.api.dto.RoleRequest;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.domain.RoleRepository;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class AccessRequestIT {

  @Autowired private AccessRequestService requests;
  @Autowired private AccessMatrixService matrix;
  @Autowired private UserAdminService users;
  @Autowired private AccessImplementationService implementations;
  @Autowired private RoleRepository roles;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  /** A new user ID in the BDOI format (USER_ID_PATTERN a999999999). */
  static String username() {
    return String.format("u%09d", Math.floorMod(System.nanoTime(), 1_000_000_000L));
  }

  /**
   * A proposal of a new group profile with a unique code (a change any test can raise).
   *
   * @param justification reason
   * @return content
   */
  static AccessRequestContent newProfile(String justification) {
    String code = "UAM_T" + Math.floorMod(System.nanoTime(), 1_000_000_000L);
    return AccessRequestContent.groupProfile(
        AccessRequestType.CREATE_ROLE,
        new RolePermissionChange(code, Set.of("REPORT_VIEW"), Set.of()),
        new RequestedRole("Test profile " + code, null, null),
        justification);
  }

  private AccessRequest submit(String user, AccessRequestContent content) {
    return as.run(user, () -> requests.submit(content));
  }

  @ParameterizedTest
  @EnumSource(
      value = AccessRequestType.class,
      names = {"CREATE_USER", "MODIFY_ROLES", "MODIFY_USER", "DISABLE_USER", "ENABLE_USER"})
  void usersAreMaintainedInUidmIscNotByRequest(AccessRequestType type) {
    assertThatThrownBy(
            () ->
                submit(
                    "badmin",
                    new AccessRequestContent(type, "ao2", "Name", null, Set.of(), null, "x")))
        .extracting("code")
        .isEqualTo("ACCESS_USERS_BY_UIDM");
  }

  @Test
  void aWithdrawnPermissionInAnApprovedChangeIsSkippedWithAnAuditNote() {
    String role = "UAM_TEST_" + System.nanoTime();
    as.run(
        "admin",
        () ->
            users.createRole(
                new RoleRequest(role, "Withdrawn test", Set.of(Permission.REPORT_VIEW))));
    AccessRequest change =
        submit(
            "badmin",
            AccessRequestContent.rolePermissions(
                new RolePermissionChange(role, Set.of("AUDIT_VIEW"), Set.of()), "Audit"));
    as.run("approver", () -> requests.approve(change.getId(), "ok"));
    // A permission withdrawn while the change waited for its implementation.
    jdbc.update(
        "update nba_access_request set permissions_added = 'AUDIT_VIEW,UAM_ENROLL' where id = ?",
        change.getId());
    as.run("admin", () -> implementations.implement(change.getId()));
    assertThat(roles.findByCode(role).orElseThrow().getPermissions())
        .containsExactlyInAnyOrder(Permission.REPORT_VIEW, Permission.AUDIT_VIEW);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from audit_log where entity_type = 'AccessRequest'"
                    + " and entity_id = ? and summary like '%UAM_ENROLL%'",
                Integer.class, change.getRequestNo()))
        .isEqualTo(1);
  }

  @Test
  void accessMatrixListsRolesAgainstPermissions() {
    AccessMatrix m = matrix.matrix();
    assertThat(m.roles())
        .extracting(AccessMatrix.RoleColumn::code)
        .contains("MKT_AO", "BUSINESS_ADMIN");
    assertThat(m.permissions())
        .filteredOn(r -> r.permission().equals("ACCESS_APPROVE"))
        .singleElement()
        .satisfies(r -> assertThat(r.roles()).contains("NB_APPROVER"));
    assertThat(as.run("badmin", matrix::exportXlsx)).isNotEmpty();
  }
}
