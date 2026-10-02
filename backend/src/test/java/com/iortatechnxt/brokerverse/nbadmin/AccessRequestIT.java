package com.iortatechnxt.brokerverse.nbadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.approval.service.ApprovalInboxService;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessMatrix;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessMatrixService;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestService;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestService.Decision;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

@IntegrationTest
class AccessRequestIT {

  @Autowired private AccessRequestService requests;
  @Autowired private AccessMatrixService matrix;
  @Autowired private UserAdminService users;
  @Autowired private ApprovalInboxService inbox;
  @Autowired private PasswordEncoder encoder;
  @Autowired private AsUser as;

  /** A new user ID in the BDOI format (USER_ID_PATTERN a999999999). */
  static String username() {
    return String.format("u%09d", Math.floorMod(System.nanoTime(), 1_000_000_000L));
  }

  private AccessRequest submit(String user, AccessRequestContent content) {
    return as.run(user, () -> requests.submit(content));
  }

  @Test
  void createdUserIsAppliedOnApprovalWithATemporaryPassword() {
    String name = username();
    AccessRequest request =
        submit(
            "badmin",
            new AccessRequestContent(
                AccessRequestType.CREATE_USER,
                name,
                "New Account Officer",
                name + "@example.ph",
                Set.of("MKT_AO"),
                null,
                "Joined Marketing"));
    assertThat(request.getStatus()).isEqualTo(AccessRequestStatus.PENDING);
    assertThat(request.getRequestNo()).startsWith("AR-");
    assertThat(as.run("approver", () -> inbox.inbox(null)))
        .extracting(PendingApproval::reference)
        .contains(request.getRequestNo());
    assertThat(as.run("badmin", () -> inbox.inbox(null)))
        .extracting(PendingApproval::reference)
        .doesNotContain(request.getRequestNo());
    assertThatThrownBy(
            () ->
                submit(
                    "badmin",
                    new AccessRequestContent(
                        AccessRequestType.CREATE_USER,
                        name,
                        "Twice",
                        null,
                        Set.of("MKT_AO"),
                        null,
                        "x")))
        .extracting("code")
        .isEqualTo("ACCESS_REQUEST_PENDING");

    Decision decision = as.run("approver", () -> requests.approve(request.getId(), "welcome"));
    assertThat(decision.request().getStatus()).isEqualTo(AccessRequestStatus.APPROVED);
    assertThat(decision.request().getDecidedBy()).isEqualTo("approver");
    assertThat(decision.temporaryPassword()).hasSize(14);
    assertThat(decision.toString()).doesNotContain(decision.temporaryPassword());
    AppUser created = users.getByUsername(name);
    assertThat(encoder.matches(decision.temporaryPassword(), created.getPasswordHash())).isTrue();
    assertThat(created.getRoles()).extracting(Role::getCode).containsExactly("MKT_AO");
    assertThatThrownBy(() -> as.run("approver", () -> requests.approve(request.getId(), null)))
        .extracting("code")
        .isEqualTo("ACCESS_REQUEST_DECIDED");

    modifyDisableAndEnable(name);
  }

  private void modifyDisableAndEnable(String name) {
    AccessRequest roles =
        submit(
            "badmin",
            new AccessRequestContent(
                AccessRequestType.MODIFY_ROLES,
                name,
                null,
                null,
                Set.of("MKT_AO", "TSU"),
                null,
                "Covers TSU"));
    as.run("approver", () -> requests.approve(roles.getId(), null));
    assertThat(
            users.getByUsername(name).getRoles().stream()
                .map(Role::getCode)
                .collect(Collectors.toSet()))
        .containsExactlyInAnyOrder("MKT_AO", "TSU");

    AccessRequest disable =
        submit(
            "badmin",
            new AccessRequestContent(
                AccessRequestType.DISABLE_USER, name, null, null, null, null, "Left"));
    as.run("approver", () -> requests.approve(disable.getId(), null));
    assertThat(users.getByUsername(name).isEnabled()).isFalse();

    AccessRequest enable =
        submit(
            "badmin",
            new AccessRequestContent(
                AccessRequestType.ENABLE_USER, name, null, null, null, null, "Back"));
    assertThatThrownBy(() -> as.run("approver", () -> requests.reject(enable.getId(), " ")))
        .extracting("code")
        .isEqualTo("ACCESS_REJECT_REASON");
    Decision rejected = as.run("approver", () -> requests.reject(enable.getId(), "Not yet"));
    assertThat(rejected.request().getStatus()).isEqualTo(AccessRequestStatus.REJECTED);
    assertThat(rejected.temporaryPassword()).isNull();
    assertThat(users.getByUsername(name).isEnabled()).isFalse();
    assertThat(
            as.run(
                "approver",
                () ->
                    requests.search(
                        AccessRequestStatus.REJECTED,
                        AccessRequestType.ENABLE_USER,
                        name,
                        Pageable.ofSize(5))))
        .extracting(AccessRequest::getId)
        .containsExactly(enable.getId());
  }

  @Test
  void requestsAreValidatedAndDecidedUnderFourEyes() {
    assertThatThrownBy(
            () ->
                submit(
                    "badmin",
                    new AccessRequestContent(
                        AccessRequestType.CREATE_USER,
                        "mkttl",
                        "Dup",
                        null,
                        Set.of("MKT_AO"),
                        null,
                        "x")))
        .extracting("code")
        .isEqualTo("ACCESS_USER_EXISTS");
    assertThatThrownBy(
            () ->
                submit(
                    "badmin",
                    new AccessRequestContent(
                        AccessRequestType.MODIFY_ROLES,
                        username(),
                        null,
                        null,
                        Set.of("MKT_AO"),
                        null,
                        "x")))
        .extracting("code")
        .isEqualTo("ACCESS_UNKNOWN_USER");
    assertThatThrownBy(
            () ->
                submit(
                    "badmin",
                    new AccessRequestContent(
                        AccessRequestType.CREATE_USER,
                        username(),
                        "N",
                        null,
                        Set.of("NOPE"),
                        null,
                        "x")))
        .extracting("code")
        .isEqualTo("ACCESS_UNKNOWN_ROLE");
    assertThatThrownBy(
            () ->
                submit(
                    "badmin",
                    new AccessRequestContent(
                        AccessRequestType.CREATE_USER,
                        username(),
                        " ",
                        null,
                        Set.of("TSU"),
                        null,
                        "x")))
        .extracting("code")
        .isEqualTo("ACCESS_FULL_NAME");
    assertThatThrownBy(
            () ->
                submit(
                    "badmin",
                    new AccessRequestContent(
                        AccessRequestType.MODIFY_ROLES, "ao2", null, null, Set.of(), null, "x")))
        .extracting("code")
        .isEqualTo("ACCESS_ROLES");
    assertThatThrownBy(
            () ->
                submit(
                    "badmin",
                    new AccessRequestContent(
                        AccessRequestType.DISABLE_USER, "x", null, null, null, null, "x")))
        .extracting("code")
        .isEqualTo("ACCESS_USERNAME");

    AccessRequest own =
        submit(
            "badmin",
            new AccessRequestContent(
                AccessRequestType.CREATE_USER,
                username(),
                "Self Made",
                null,
                Set.of("TSU"),
                null,
                "x"));
    assertThatThrownBy(() -> as.run("badmin", () -> requests.approve(own.getId(), null)))
        .extracting("code")
        .isEqualTo("ACCESS_FOUR_EYES");
    as.run("uamapprover", () -> requests.reject(own.getId(), "Withdrawn"));
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
