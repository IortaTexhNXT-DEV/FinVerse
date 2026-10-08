package com.iortatechnxt.brokerverse.acsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.acsl.domain.AcslCaseRepository;
import com.iortatechnxt.brokerverse.acsl.service.AcslNotifier;
import com.iortatechnxt.brokerverse.acsl.service.CaseService;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.workflow.service.WorkAssignmentService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowViewService;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/**
 * The ACSL users a case or a correction may be assigned to are offered by name in the assign
 * dialogs: they are the users who may process ACSL work.
 */
class AcslAssigneesTest {

  @Test
  void theAssigneesAreTheUsersWhoProcessAcslWork() {
    UserDirectory users = mock(UserDirectory.class);
    when(users.usersWithPermission(anyString())).thenReturn(List.of());
    when(users.usersWithPermission("ACSL_PROCESS")).thenReturn(List.of("acsl", "acsltl"));
    CaseService cases =
        new CaseService(
            mock(AcslCaseRepository.class),
            mock(InvoiceLedgerQueryService.class),
            mock(WorkflowService.class),
            mock(WorkflowViewService.class),
            mock(WorkAssignmentService.class),
            users,
            mock(DocumentNumberService.class),
            mock(ApplicationEventPublisher.class),
            mock(AcslNotifier.class),
            mock(AuditTrailService.class),
            mock(CurrentUser.class),
            Clock.systemUTC());
    assertThat(cases.processors()).containsExactly("acsl", "acsltl");
  }
}
