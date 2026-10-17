package com.iortatechnxt.brokerverse.nbadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestAction;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestEvent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestReturnService;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

/**
 * Lifecycle of a group-profile change (BRD 3.002; maker-checker): the maker's draft, its submission
 * to any checker, the return for correction with the correction remark, the approval by another
 * user and the history of every step. The maker never decides the change.
 */
@IntegrationTest
class UserAccessLifecycleIT {

  private static final String MAKER = "requestor";
  private static final String CHECKER = "uamapprover";

  @Autowired private AccessRequestService requests;
  @Autowired private AccessRequestReturnService returns;
  @Autowired private AsUser as;

  @Test
  void aDraftIsSubmittedReturnedCorrectedAndApprovedByAnotherUser() {
    AccessRequestContent proposal = AccessRequestIT.newProfile("Reports for the officers");
    AccessRequest draft = as.run(MAKER, () -> requests.create(proposal, true));
    assertThat(draft.getStatus()).isEqualTo(AccessRequestStatus.DRAFT);
    assertThatThrownBy(() -> as.run(CHECKER, () -> requests.view(draft.getId())))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> as.run("badmin", () -> requests.edit(draft.getId(), proposal)))
        .extracting("code")
        .isEqualTo("ACCESS_NOT_REQUESTER");

    AccessRequest pending = as.run(MAKER, () -> requests.submit(draft.getId(), "Please"));
    assertThat(pending.getStatus()).isEqualTo(AccessRequestStatus.PENDING);
    assertThat(pending.getAssignedApprover()).isNull();
    assertThatThrownBy(() -> as.run(MAKER, () -> requests.approve(draft.getId(), null)))
        .isInstanceOf(RuntimeException.class);

    AccessRequest returned =
        as.run(CHECKER, () -> returns.returnRequest(draft.getId(), "Name the reports"));
    assertThat(returned.getStatus()).isEqualTo(AccessRequestStatus.RETURNED);
    assertThatThrownBy(() -> as.run(MAKER, () -> requests.submit(draft.getId(), " ")))
        .extracting("code")
        .isEqualTo("ACCESS_CORRECTION_REMARKS");
    as.run(
        MAKER,
        () -> requests.edit(draft.getId(), proposal.withJustification("Production reports")));
    as.run(MAKER, () -> requests.submit(draft.getId(), "Reports named"));

    AccessRequest decided =
        as.run(CHECKER, () -> requests.approve(draft.getId(), "Agreed")).request();
    assertThat(decided.getDecidedBy()).isEqualTo(CHECKER);
    assertThat(decided.getStatus())
        .isIn(AccessRequestStatus.APPROVED, AccessRequestStatus.FOR_IMPLEMENTATION);
    assertThat(as.run(MAKER, () -> requests.history(draft.getId())))
        .extracting(AccessRequestEvent::getAction)
        .startsWith(
            AccessRequestAction.SAVE,
            AccessRequestAction.SUBMIT,
            AccessRequestAction.RETURN,
            AccessRequestAction.EDIT,
            AccessRequestAction.RESUBMIT,
            AccessRequestAction.APPROVE);
  }

  @Test
  void theMakerCancelsAPendingChange() {
    AccessRequest pending =
        as.run(MAKER, () -> requests.submit(AccessRequestIT.newProfile("Short-lived")));
    AccessRequest cancelled =
        as.run(MAKER, () -> returns.cancel(pending.getId(), "No longer needed"));
    assertThat(cancelled.getStatus()).isEqualTo(AccessRequestStatus.CANCELLED);
    assertThat(cancelled.getCancelReason()).isEqualTo("No longer needed");
  }
}
