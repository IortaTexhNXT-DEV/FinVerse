package com.iortatechnxt.brokerverse.nbadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.approval.service.ApprovalInboxService;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.RolePermissionChange;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessImplementationService;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessMatrix;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessMatrixService;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestService;
import com.iortatechnxt.brokerverse.security.api.dto.RoleRequest;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.domain.RoleRepository;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.support.Api;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.Json;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

/** Role-permission change requests and the role-to-action matrix (PMADD05). */
@IntegrationTest
class RolePermissionRequestIT {

  @Autowired private AccessRequestService requests;
  @Autowired private AccessImplementationService implementations;
  @Autowired private AccessMatrixService matrix;
  @Autowired private UserAdminService userAdmin;
  @Autowired private RoleRepository roles;
  @Autowired private ApprovalInboxService inbox;
  @Autowired private AsUser as;
  @Autowired private Api api;

  private String newRole() {
    String code = "PM_TEST_" + System.nanoTime();
    as.run(
        "admin",
        () ->
            userAdmin.createRole(
                new RoleRequest(
                    code, "Test role", Set.of(Permission.REPORT_VIEW, Permission.AUDIT_VIEW))));
    return code;
  }

  private AccessRequest submit(String role, Set<String> added, Set<String> removed) {
    return as.run(
        "badmin",
        () ->
            requests.submit(
                AccessRequestContent.rolePermissions(
                    new RolePermissionChange(role, added, removed), " Product Maintenance ")));
  }

  private Set<Permission> permissionsOf(String role) {
    return roles.findByCode(role).map(Role::getPermissions).orElseThrow();
  }

  @Test
  void approvedChangeAddsAndRemovesPermissionsOfTheRole() {
    String role = newRole();
    AccessRequest request =
        submit(role, Set.of("PRODUCT_VIEW", "REPORT_VIEW"), Set.of("AUDIT_VIEW", "PKG_ADVISORY"));
    assertThat(request.getRequestType()).isEqualTo(AccessRequestType.MODIFY_ROLE_PERMISSIONS);
    assertThat(request.getUsername()).isNull();
    assertThat(request.getJustification()).isEqualTo("Product Maintenance");
    RolePermissionChange stored = request.permissionChange();
    assertThat(stored.added()).containsExactly("PRODUCT_VIEW");
    assertThat(stored.removed()).containsExactly("AUDIT_VIEW");
    assertThat(AccessRequestService.describe(request))
        .isEqualTo(
            "Change permissions of role " + role + ": add [PRODUCT_VIEW]; remove [AUDIT_VIEW]");
    assertThat(as.run("approver", () -> inbox.inbox(null)))
        .extracting(PendingApproval::reference)
        .contains(request.getRequestNo());
    assertThat(permissionsOf(role)).contains(Permission.AUDIT_VIEW);

    assertThatThrownBy(() -> submit(role, Set.of("PKG_REQUEST"), Set.of()))
        .extracting("code")
        .isEqualTo("ACCESS_REQUEST_PENDING");
    assertThatThrownBy(() -> as.run("badmin", () -> requests.approve(request.getId(), null)))
        .extracting("code")
        .isEqualTo("ACCESS_FOUR_EYES");

    // UAM_ROLE_APPLY_ON_APPROVAL = false (UQ03): the approved change waits for the System
    // Administrator, who implements it (BRD-11 p.6).
    assertThat(
            as.run("approver", () -> requests.approve(request.getId(), "ok")).request().getStatus())
        .isEqualTo(AccessRequestStatus.FOR_IMPLEMENTATION);
    assertThat(permissionsOf(role)).contains(Permission.AUDIT_VIEW);
    as.run("admin", () -> implementations.implement(request.getId()));
    assertThat(permissionsOf(role))
        .containsExactlyInAnyOrder(Permission.REPORT_VIEW, Permission.PRODUCT_VIEW);
    assertThat(
            as.run(
                "badmin",
                () ->
                    requests.search(
                        AccessRequestStatus.IMPLEMENTED,
                        AccessRequestType.MODIFY_ROLE_PERMISSIONS,
                        role.toLowerCase(Locale.ROOT),
                        Pageable.unpaged())))
        .extracting(AccessRequest::getRequestNo)
        .containsExactly(request.getRequestNo());
  }

  @Test
  void invalidChangesAreRefused() {
    String role = newRole();
    assertThatThrownBy(() -> submit(" ", Set.of("PRODUCT_VIEW"), Set.of()))
        .extracting("code")
        .isEqualTo("ACCESS_ROLE");
    assertThatThrownBy(() -> submit("NO_SUCH_ROLE_X", Set.of("PRODUCT_VIEW"), Set.of()))
        .extracting("code")
        .isEqualTo("ACCESS_UNKNOWN_ROLE");
    assertThatThrownBy(() -> submit(role, Set.of("NOT_A_PERMISSION"), Set.of()))
        .extracting("code")
        .isEqualTo("ACCESS_UNKNOWN_PERMISSION");
    assertThatThrownBy(() -> submit(role, Set.of("PKG_REQUEST"), Set.of("PKG_REQUEST")))
        .extracting("code")
        .isEqualTo("ACCESS_PERMISSION_CONFLICT");
    assertThatThrownBy(() -> submit(role, Set.of("REPORT_VIEW"), Set.of("PKG_REQUEST")))
        .extracting("code")
        .isEqualTo("ACCESS_NO_PERMISSION_CHANGE");
    AccessRequest removal = submit(role, Set.of(), Set.of("AUDIT_VIEW"));
    assertThat(AccessRequestService.describe(removal))
        .isEqualTo("Change permissions of role " + role + ": remove [AUDIT_VIEW]");
    as.run("approver", () -> requests.reject(removal.getId(), "Keep audit access"));
    assertThat(permissionsOf(role)).contains(Permission.AUDIT_VIEW);
  }

  @Test
  void matrixShowsActionClassesByPermissionAndByAction() {
    AccessMatrix m = matrix.matrix();
    assertThat(m.permissions())
        .filteredOn(r -> r.permission().equals("PKG_REQUEST"))
        .singleElement()
        .satisfies(
            r -> {
              assertThat(r.area()).isEqualTo("PACKAGE_REQUEST");
              assertThat(r.actions()).containsExactly("CREATE", "AMEND");
              assertThat(r.roles()).contains("MKT_AO", "TSU");
            });
    assertThat(m.permissions())
        .filteredOn(r -> r.permission().equals("JOURNAL_CREATE"))
        .singleElement()
        .satisfies(r -> assertThat(r.actions()).isEmpty());

    AccessMatrix.ByAction pkg = matrix.byAction("PACKAGE_REQUEST");
    assertThat(pkg.rows())
        .extracting(AccessMatrix.ActionRow::action)
        .containsExactly("VIEW", "CREATE", "AMEND", "APPROVE");
    AccessMatrix.ActionRow approve = pkg.rows().get(3);
    assertThat(approve.permissions())
        .contains("PKG_REQUEST_APPROVE", "PKG_TSU_APPROVE", "PKG_MANCOM_SIGNOFF");
    assertThat(approve.grants().get("MKT_TL")).containsExactly("PKG_REQUEST_APPROVE");
    assertThat(approve.grants().get("MANCOM")).containsExactly("PKG_MANCOM_SIGNOFF");
    assertThat(approve.grants()).doesNotContainKey("MKT_AO");

    assertThat(matrix.byAction(null).rows())
        .extracting(AccessMatrix.ActionRow::area)
        .startsWith("PRODUCT_MAINTENANCE")
        .contains("CLIENTS", "CASHIERING", "COMMISSION");
    assertThat(as.run("badmin", matrix::exportXlsx)).isNotEmpty();
  }

  @Test
  void roleChangeIsRequestedAndReadOverHttp() throws Exception {
    String role = newRole();
    long id =
        api.read(
                api.doPost(
                        "badmin",
                        "/api/v1/nbadmin/access-requests",
                        Json.of(
                            "type",
                            "MODIFY_ROLE_PERMISSIONS",
                            "roleCode",
                            role,
                            "permissionsAdded",
                            List.of("PRODUCT_VIEW"),
                            "permissionsRemoved",
                            List.of(),
                            "justification",
                            "Read access to Product Maintenance"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.roleCode").value(role))
                    .andExpect(jsonPath("$.permissionsAdded[0]").value("PRODUCT_VIEW")))
            .get("id")
            .asLong();
    api.doGet("approver", "/api/v1/nbadmin/access-requests/" + id)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("MODIFY_ROLE_PERMISSIONS"))
        .andExpect(
            jsonPath("$.summary")
                .value("Change permissions of role " + role + ": add [PRODUCT_VIEW]"));
    api.doGet("badmin", "/api/v1/nbadmin/access-matrix/by-action?area=PRODUCT_MAINTENANCE")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rows[0].area").value("PRODUCT_MAINTENANCE"))
        .andExpect(jsonPath("$.rows[0].action").value("VIEW"));
    api.doGet("badmin", "/api/v1/nbadmin/access-matrix")
        .andExpect(
            jsonPath("$.permissions[?(@.permission == 'PRODUCT_VALIDATE')].actions[0]")
                .value("APPROVE"));
    api.doGet("ao", "/api/v1/nbadmin/access-matrix/by-action").andExpect(status().isForbidden());
  }
}
