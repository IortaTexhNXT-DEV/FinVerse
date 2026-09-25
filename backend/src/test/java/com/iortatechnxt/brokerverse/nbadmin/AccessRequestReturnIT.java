package com.iortatechnxt.brokerverse.nbadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.approval.service.BulkApprovalService;
import com.iortatechnxt.brokerverse.approval.service.BulkApprovalService.Item;
import com.iortatechnxt.brokerverse.approval.service.BulkApprovalService.Outcome;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequest;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestContent;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestStatus;
import com.iortatechnxt.brokerverse.nbadmin.domain.AccessRequestType;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestReturnService;
import com.iortatechnxt.brokerverse.nbadmin.service.AccessRequestService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Returned access requests (BASAU 2.4.1, 2.6.x) and bulk approval from the inbox (BASAU 2.5.3). */
@IntegrationTest
class AccessRequestReturnIT {

  @Autowired private AccessRequestService requests;
  @Autowired private AccessRequestReturnService returns;
  @Autowired private BulkApprovalService bulk;
  @Autowired private AsUser as;

  private AccessRequest submit(AccessRequestType type, String name) {
    return as.run(
        "badmin",
        () ->
            requests.submit(
                new AccessRequestContent(
                    type,
                    name,
                    "Returned Request User",
                    name + "@example.ph",
                    Set.of("MKT_AO"),
                    null,
                    "Joined Marketing")));
  }

  @Test
  void returnedRequestIsResubmittedAndApprovedInBulk() {
    String name = "r" + System.nanoTime();
    AccessRequest created = submit(AccessRequestType.CREATE_USER, name);
    as.run("approver", () -> requests.approve(created.getId(), null));
    AccessRequest request = submit(AccessRequestType.MODIFY_ROLES, name);

    assertThatThrownBy(() -> as.run("approver", () -> returns.returnRequest(request.getId(), " ")))
        .isInstanceOf(BusinessRuleException.class);
    assertThatThrownBy(() -> as.run("badmin", () -> returns.returnRequest(request.getId(), "x")))
        .extracting("code")
        .isEqualTo("ACCESS_FOUR_EYES");
    AccessRequest returned =
        as.run(
            "approver", () -> returns.returnRequest(request.getId(), "Attach the HR memo number"));
    assertThat(returned.getStatus()).isEqualTo(AccessRequestStatus.RETURNED);
    assertThat(returned.getReturnedCount()).isEqualTo(1);
    assertThatThrownBy(() -> as.run("approver", () -> requests.approve(request.getId(), null)))
        .extracting("code")
        .isEqualTo("ACCESS_REQUEST_DECIDED");
    assertThatThrownBy(() -> as.run("approver", () -> returns.resubmit(request.getId(), "memo")))
        .extracting("code")
        .isEqualTo("ACCESS_NOT_REQUESTER");

    AccessRequest again =
        as.run("badmin", () -> returns.resubmit(request.getId(), "Joined Marketing, HR memo 12"));
    assertThat(again.getStatus()).isEqualTo(AccessRequestStatus.PENDING);
    assertThat(again.getJustification()).contains("HR memo 12");

    String other = "n" + System.nanoTime();
    AccessRequest newUser = submit(AccessRequestType.CREATE_USER, other);
    List<Outcome> outcomes =
        as.run(
            "approver",
            () ->
                bulk.approve(
                    List.of(
                        new Item("BROKING_ADMIN", "Access request", again.getRequestNo(), null),
                        new Item(
                            "BROKING_ADMIN", "Access request", newUser.getRequestNo(), null))));
    assertThat(outcomes.get(0).approved()).isTrue();
    assertThat(outcomes.get(1).approved()).isFalse();
    assertThat(outcomes.get(1).message()).contains("temporary password");
    assertThat(requests.get(again.getId()).getStatus()).isEqualTo(AccessRequestStatus.APPROVED);
  }
}
