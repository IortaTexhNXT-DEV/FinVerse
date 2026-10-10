package com.iortatechnxt.brokerverse.cashiering.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.ApplicationRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.CashReceiptRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptActionRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptRecordRepository;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.security.service.UserDirectory;
import com.iortatechnxt.brokerverse.workflow.service.WorkflowService;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * A reinstatement the setting does not allow names the setting as the users read it, not its key.
 */
class ReinstatementSettingMessageTest {

  private final CashieringDecisions decisions = mock(CashieringDecisions.class);

  @Test
  void aCancelledReceiptNamesTheSettingInWords() {
    when(decisions.reinstatesCancelledReceipts()).thenReturn(false);
    ReceiptActionService service =
        new ReceiptActionService(
            mock(ReceiptActionRepository.class),
            mock(CashReceiptRepository.class),
            mock(ReceiptReversalService.class),
            mock(LovService.class),
            mock(WorkflowService.class),
            mock(DocumentNumberService.class),
            mock(NotificationService.class),
            mock(AuditTrailService.class),
            mock(CurrentUser.class),
            Clock.systemUTC(),
            mock(UserDirectory.class),
            decisions);
    assertThatThrownBy(() -> service.requestReinstatement(1L, null))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("(setting What Reinstate does)")
        .hasMessageNotContaining("CASH_");
  }

  @Test
  void anIssuedReceiptNamesTheSettingInWords() {
    when(decisions.reinstatesIssuedReceipts()).thenReturn(false);
    ReversalRecordService service =
        new ReversalRecordService(
            mock(ReceiptRecordRepository.class),
            mock(CashReceiptRepository.class),
            mock(ApplicationRepository.class),
            mock(ReversalChecks.class),
            mock(RecordNumbers.class),
            decisions,
            mock(RecordWording.class),
            mock(AuditTrailService.class));
    assertThatThrownBy(() -> service.reinstate(List.of(1L), null, List.of()))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("(setting What Reinstate does)")
        .hasMessageNotContaining("CASH_");
  }
}
