package com.iortatechnxt.brokerverse.nbadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.approval.service.ApprovalInboxService;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.messaging.domain.NotificationRepository;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestAction;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestEvent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRiskFlag;
import com.iortatechnxt.brokerverse.nbadmin.domain.ExternalParty;
import com.iortatechnxt.brokerverse.nbadmin.domain.ExternalPartyKind;
import com.iortatechnxt.brokerverse.nbadmin.domain.RequestedUserData;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessDecisionService;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestReturnService;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestSearch;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestSearch.Scope;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestService;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestService.Decision;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessScheduledChanges;
import com.iortatechnxt.brokerverse.security.domain.AccessChangeLog;
import com.iortatechnxt.brokerverse.security.domain.AccessChangeLogRepository;
import com.iortatechnxt.brokerverse.security.domain.AppUser;
import com.iortatechnxt.brokerverse.security.domain.Role;
import com.iortatechnxt.brokerverse.security.service.UserAdminService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;

/**
 * User access request lifecycle (BRD 1.002-1.008, 2.002; UAM-NFR-14, 40): draft, submit to the
 * chosen approver, return, correction, approval and application; the effective-date job; the second
 * approval; cancellation.
 */
@IntegrationTest
class UserAccessLifecycleIT {

  private static final String REQUESTOR = "requestor";
  private static final String APPROVER = "uamapprover";

  @Autowired private AccessRequestService requests;
  @Autowired private AccessDecisionService decisions;
  @Autowired private AccessRequestReturnService returns;
  @Autowired private AccessScheduledChanges scheduled;
  @Autowired private ApprovalInboxService inbox;
  @Autowired private UserAdminService users;
  @Autowired private AccessChangeLogRepository changeLog;
  @Autowired private NotificationRepository notifications;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  private static AccessRequestContent enrol(String user) {
    return new AccessRequestContent(
            AccessRequestType.CREATE_USER,
            user,
            "Lifecycle User",
            user + "@example.ph",
            Set.of("MKT_AO"),
            null,
            "Joined Marketing")
        .withUserData(new RequestedUserData("W" + user, null, null, null, false));
  }

  private AppUser newUser() {
    String name = AccessRequestIT.username();
    AccessRequest r =
        as.run(REQUESTOR, () -> requests.create(enrol(name), false, List.of(APPROVER)));
    as.run(APPROVER, () -> requests.approve(r.getId(), null));
    return users.getByUsername(name);
  }

  @Test
  void draftIsSubmittedReturnedCorrectedApprovedAndApplied() {
    String name = AccessRequestIT.username();
    AccessRequest draft = as.run(REQUESTOR, () -> requests.create(enrol(name), true, null));
    assertThat(draft.getStatus()).isEqualTo(AccessRequestStatus.DRAFT);
    assertThatThrownBy(() -> as.run("badmin", () -> requests.view(draft.getId())))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> as.run("badmin", () -> requests.edit(draft.getId(), enrol(name))))
        .extracting("code")
        .isEqualTo("ACCESS_NOT_REQUESTER");
    assertThatThrownBy(
            () -> as.run(REQUESTOR, () -> requests.submit(draft.getId(), List.of(), null)))
        .extracting("code")
        .isEqualTo("ACCESS_APPROVER_REQUIRED");
    assertThatThrownBy(
            () -> as.run(REQUESTOR, () -> requests.submit(draft.getId(), List.of("ao"), null)))
        .extracting("code")
        .isEqualTo("ACCESS_APPROVER_NOT_ELIGIBLE");

    AccessRequest pending =
        as.run(REQUESTOR, () -> requests.submit(draft.getId(), List.of(APPROVER), "Please"));
    assertThat(pending.getStatus()).isEqualTo(AccessRequestStatus.PENDING);
    assertThat(pending.getAssignedApprover()).isEqualTo(APPROVER);
    assertThat(inboxOf(APPROVER)).contains(pending.getRequestNo());
    assertThat(inboxOf("approver")).doesNotContain(pending.getRequestNo());
    assertThatThrownBy(() -> as.run("approver", () -> requests.approve(draft.getId(), null)))
        .extracting("code")
        .isEqualTo("ACCESS_NOT_ASSIGNED");
    assertThat(
            as.run(
                    APPROVER,
                    () ->
                        requests.search(
                            new AccessRequestSearch(
                                Scope.ASSIGNED, null, null, name, null, null, null, null, null),
                            Pageable.ofSize(5)))
                .getContent())
        .extracting(AccessRequest::getRequestNo)
        .containsExactly(pending.getRequestNo());

    AccessRequest returned =
        as.run(APPROVER, () -> returns.returnRequest(draft.getId(), "Add the Windows ID"));
    assertThat(returned.getStatus()).isEqualTo(AccessRequestStatus.RETURNED);
    assertThatThrownBy(
            () -> as.run(REQUESTOR, () -> requests.submit(draft.getId(), List.of(APPROVER), " ")))
        .extracting("code")
        .isEqualTo("ACCESS_CORRECTION_REMARKS");
    as.run(
        REQUESTOR,
        () ->
            requests.edit(
                draft.getId(),
                enrol(name)
                    .withUserData(new RequestedUserData("WIN" + name, null, null, null, false))));
    as.run(REQUESTOR, () -> requests.submit(draft.getId(), List.of(APPROVER), "Windows ID added"));

    Decision decision = as.run(APPROVER, () -> requests.approve(draft.getId(), "Welcome"));
    assertThat(decision.request().getStatus()).isEqualTo(AccessRequestStatus.APPROVED);
    assertThat(decision.temporaryPassword()).isNotBlank();
    AppUser created = users.getByUsername(name);
    assertThat(created.getWindowsId()).isEqualTo("WIN" + name);
    assertThat(changeLog.findByRequestNoOrderById(pending.getRequestNo()))
        .extracting(AccessChangeLog::getApprovedBy)
        .containsOnly(APPROVER);
    assertThat(as.run(REQUESTOR, () -> requests.history(draft.getId())))
        .extracting(AccessRequestEvent::getAction)
        .containsExactly(
            AccessRequestAction.SAVE,
            AccessRequestAction.SUBMIT,
            AccessRequestAction.RETURN,
            AccessRequestAction.EDIT,
            AccessRequestAction.RESUBMIT,
            AccessRequestAction.APPROVE,
            AccessRequestAction.APPLY);
    assertThat(
            notifications.findAll().stream()
                .filter(n -> n.getTitle().contains(pending.getRequestNo()))
                .map(n -> n.getRecipient())
                .distinct())
        .contains(APPROVER, REQUESTOR)
        .doesNotContain("approver");
  }

  @Test
  void scheduledChangeIsAppliedByTheJobOnItsDate() {
    AppUser user = newUser();
    LocalDate tomorrow = LocalDate.now().plusDays(2);
    AccessRequest request =
        as.run(
            REQUESTOR,
            () ->
                requests.create(
                    new AccessRequestContent(
                            AccessRequestType.DISABLE_USER,
                            user.getUsername(),
                            null,
                            null,
                            null,
                            null,
                            "Leaves the company")
                        .withEffectiveFrom(tomorrow),
                    false,
                    List.of(APPROVER)));
    as.run(APPROVER, () -> requests.approve(request.getId(), null));
    assertThat(requests.get(request.getId()).getStatus()).isEqualTo(AccessRequestStatus.SCHEDULED);
    assertThat(users.getByUsername(user.getUsername()).isEnabled()).isTrue();

    scheduled.applyDue(tomorrow.minusDays(1));
    assertThat(requests.get(request.getId()).getStatus()).isEqualTo(AccessRequestStatus.SCHEDULED);
    AccessScheduledChanges.Result result = scheduled.applyDue(tomorrow);
    assertThat(result.applied()).isPositive();
    assertThat(requests.get(request.getId()).getStatus()).isEqualTo(AccessRequestStatus.APPROVED);
    assertThat(users.getByUsername(user.getUsername()).isEnabled()).isFalse();

    AccessRequest again =
        as.run(
            REQUESTOR,
            () ->
                requests.create(
                    new AccessRequestContent(
                            AccessRequestType.ENABLE_USER,
                            user.getUsername(),
                            null,
                            null,
                            null,
                            null,
                            "Back")
                        .withEffectiveFrom(tomorrow),
                    false,
                    List.of(APPROVER)));
    as.run(APPROVER, () -> requests.approve(again.getId(), null));
    jdbc.update("update sec_user set enabled = true where username = ?", user.getUsername());
    scheduled.applyDue(tomorrow);
    AccessRequest failed = requests.get(again.getId());
    assertThat(failed.getStatus()).isEqualTo(AccessRequestStatus.SCHEDULED);
    assertThat(failed.getApplyError()).contains("already active");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from alt_alert where dedup_key = ?",
                Integer.class,
                "UAM_SCHEDULED_APPLY_FAILED:" + again.getRequestNo()))
        .isEqualTo(1);
    as.run(APPROVER, () -> returns.cancel(again.getId(), "Not needed any more"));
    assertThat(requests.get(again.getId()).getStatus()).isEqualTo(AccessRequestStatus.CANCELLED);
  }

  @Test
  void privilegedChangeNeedsASecondApprovalByAnotherApprover() {
    AppUser user = newUser();
    AccessRequest request =
        as.run(
            REQUESTOR,
            () ->
                requests.create(
                    new AccessRequestContent(
                        AccessRequestType.MODIFY_USER,
                        user.getUsername(),
                        null,
                        null,
                        Set.of("MKT_AO", "SYSADMIN"),
                        null,
                        "Back-up administrator"),
                    false,
                    List.of(APPROVER)));
    assertThat(request.riskFlags()).containsExactly(AccessRiskFlag.PRIVILEGE_INCREASE);
    assertThatThrownBy(
            () -> as.run(user.getUsername(), () -> decisions.approve(request.getId(), null)))
        .extracting("code")
        .isEqualTo("ACCESS_SUBJECT_DECIDES");
    as.run(APPROVER, () -> requests.approve(request.getId(), null));
    assertThat(requests.get(request.getId()).getStatus())
        .isEqualTo(AccessRequestStatus.PENDING_SECOND);
    assertThat(inboxOf("secapprover")).contains(request.getRequestNo());
    assertThatThrownBy(() -> as.run(APPROVER, () -> decisions.secondApprove(request.getId(), null)))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> as.run(APPROVER, () -> decisions.approve(request.getId(), null)))
        .extracting("code")
        .isEqualTo("ACCESS_SECOND_APPROVAL_PENDING");

    as.run("secapprover", () -> decisions.secondApprove(request.getId(), "Checked with IT"));
    assertThat(requests.get(request.getId()).getStatus()).isEqualTo(AccessRequestStatus.APPROVED);
    assertThat(users.getByUsername(user.getUsername()).getRoles())
        .extracting(Role::getCode)
        .contains("SYSADMIN");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from alt_alert where dedup_key = ?",
                Integer.class,
                "UAM_PRIVILEGED_CHANGE:" + request.getRequestNo()))
        .isEqualTo(1);
  }

  @Test
  void requestsAreCheckedOnSubmission() {
    AppUser user = newUser();
    String name = user.getUsername();
    assertCode(
        new AccessRequestContent(
            AccessRequestType.MODIFY_USER, name, null, null, null, null, "No change"),
        "ACCESS_NOTHING_CHANGED");
    assertCode(
        new AccessRequestContent(
                AccessRequestType.MODIFY_USER, name, null, null, null, null, "Windows")
            .withUserData(new RequestedUserData(user.getWindowsId(), null, null, null, false)),
        "ACCESS_NOTHING_CHANGED");
    assertCode(enrol("ab"), "ACCESS_USERNAME");
    assertCode(enrol("abc" + name), "ACCESS_USER_ID_FORMAT");
    assertCode(
        enrol(AccessRequestIT.username()).withEffectiveFrom(LocalDate.now().minusDays(3)),
        "ACCESS_EFFECTIVE_DATE");
    assertCode(
        enrol(AccessRequestIT.username())
            .withUserData(new RequestedUserData(user.getWindowsId(), null, null, null, false)),
        "ACCESS_WINDOWS_ID_IN_USE");
    assertCode(
        new AccessRequestContent(
            AccessRequestType.ENABLE_USER, name, null, null, null, null, "Active"),
        "ACCESS_USER_ALREADY_ACTIVE");
    assertCode(
        new AccessRequestContent(
            AccessRequestType.MODIFY_USER, REQUESTOR, null, null, Set.of("TSU"), null, "Own"),
        "SELF_ROLE_CHANGE");
    assertThatThrownBy(
            () ->
                as.run(
                    REQUESTOR,
                    () ->
                        requests.create(
                            enrol(AccessRequestIT.username()),
                            false,
                            List.of(APPROVER, "approver"))))
        .extracting("code")
        .isEqualTo("ACCESS_ONE_APPROVER");
    assertThatThrownBy(
            () ->
                as.run(
                    REQUESTOR,
                    () ->
                        requests.create(
                            enrol(AccessRequestIT.username())
                                .withExternal(
                                    new ExternalParty(
                                        ExternalPartyKind.CLIENT, "C-1", "CLIENT_HR")),
                            true,
                            null)))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(
            () ->
                as.run(
                    "badmin",
                    () ->
                        requests.create(
                            enrol(AccessRequestIT.username())
                                .withExternal(
                                    new ExternalParty(
                                        ExternalPartyKind.CLIENT, "C-1", "CLIENT_HR")),
                            false,
                            null)))
        .extracting("code")
        .isEqualTo("EXTERNAL_USERS_NOT_AVAILABLE");

    AccessRequest pending =
        as.run(
            REQUESTOR,
            () ->
                requests.create(
                    new AccessRequestContent(
                        AccessRequestType.DISABLE_USER, name, null, null, null, null, "Left"),
                    false,
                    List.of(APPROVER)));
    assertThatThrownBy(() -> as.run(REQUESTOR, () -> returns.cancel(pending.getId(), " ")))
        .extracting("code")
        .isEqualTo("ACCESS_CANCEL_REASON");
    as.run(REQUESTOR, () -> returns.cancel(pending.getId(), "Wrong user"));
    assertThat(inboxOf(APPROVER)).doesNotContain(pending.getRequestNo());
    assertThatThrownBy(() -> as.run(REQUESTOR, () -> returns.cancel(pending.getId(), "Again")))
        .extracting("code")
        .isEqualTo("ACCESS_REQUEST_DECIDED");
  }

  private void assertCode(AccessRequestContent content, String code) {
    assertThatThrownBy(
            () -> as.run(REQUESTOR, () -> requests.create(content, false, List.of(APPROVER))))
        .extracting("code")
        .isEqualTo(code);
  }

  private List<String> inboxOf(String user) {
    return as.run(user, () -> inbox.inbox(null)).stream().map(PendingApproval::reference).toList();
  }
}
