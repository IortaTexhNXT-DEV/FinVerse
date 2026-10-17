package com.iortatechnxt.brokerverse.payrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.crm.service.ClientPayoutAccounts;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequestRepository;
import com.iortatechnxt.brokerverse.payrequest.domain.RefundLineRepository;
import com.iortatechnxt.brokerverse.payrequest.domain.RefundLineValues;
import com.iortatechnxt.brokerverse.payrequest.service.DisbursementLink;
import com.iortatechnxt.brokerverse.payrequest.service.PayRequestNotifier;
import com.iortatechnxt.brokerverse.payrequest.service.PayRequestRules;
import com.iortatechnxt.brokerverse.payrequest.service.PayRequestWorkflowService;
import com.iortatechnxt.brokerverse.payrequest.service.RefundValidationService;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.workflow.service.WorkAssignmentService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowViewService;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Texts of the Marketing requests as the user reads them: the refusals of an AR refunded twice name
 * the AR and the request without a requirement reference, and the preparers of a request are
 * offered from the users who may create requests.
 */
class PayRequestTextTest {

  private final RefundLineRepository lines = mock(RefundLineRepository.class);
  private final PayRequestRules rules =
      new PayRequestRules(
          lines,
          mock(InvoiceLedgerQueryService.class),
          mock(ClientService.class),
          mock(ClientPayoutAccounts.class),
          mock(LovService.class),
          mock(OrganizationService.class),
          Clock.systemUTC());

  private static RefundLineValues line(String ar) {
    return new RefundLineValues(
        ar,
        "CL-2026-000001",
        "Liza Manalo",
        null,
        new BigDecimal("1200.00"),
        "OVERPAYMENT",
        null,
        null,
        null,
        null);
  }

  @Test
  void anArTwiceOnTheRequestIsRefusedWithoutARequirementReference() {
    assertThatThrownBy(() -> rules.refundLines(1L, List.of(line("AR-1"), line("ar-1")), null))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("AR ar-1 appears twice on the request");
  }

  @Test
  void anArAlreadyRefundedNamesTheRequestWithoutARequirementReference() {
    when(lines.liveRefunds(anyList(), anyLong()))
        .thenReturn(List.<Object[]>of(new Object[] {"AR-1", "RRF-2026-000001"}));
    assertThatThrownBy(() -> rules.refundLines(1L, List.of(line("AR-1")), null))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage("AR AR-1 is already refunded by request RRF-2026-000001");
  }

  @Test
  void thePreparersAreTheUsersWhoMayCreateRequests() {
    UserDirectory users = mock(UserDirectory.class);
    when(users.usersWithPermission(anyString())).thenReturn(List.of());
    when(users.usersWithPermission("PRQ_CREATE")).thenReturn(List.of("mktao", "mktrev"));
    PayRequestWorkflowService workflow =
        new PayRequestWorkflowService(
            mock(PaymentRequestRepository.class),
            mock(RefundValidationService.class),
            mock(DisbursementLink.class),
            mock(WorkflowService.class),
            mock(WorkflowViewService.class),
            mock(WorkAssignmentService.class),
            users,
            mock(PayRequestNotifier.class),
            mock(AuditTrailService.class),
            mock(CurrentUser.class),
            Clock.systemUTC());
    assertThat(workflow.preparers()).containsExactly("mktao", "mktrev");
  }
}
