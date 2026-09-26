package com.iortatechnxt.brokerverse.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.messaging.service.JobFailureMailer;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.messaging.service.OutboundEmail;
import com.iortatechnxt.brokerverse.system.domain.JobRun;
import com.iortatechnxt.brokerverse.system.domain.JobTrigger;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** The batch failure e-mail to JOB_FAILURE_RECIPIENTS (UAM-NFR-25; FR-UA-071). */
class JobFailureMailerTest {

  private final SystemParameterService parameters = mock(SystemParameterService.class);
  private final MessageService messages = mock(MessageService.class);
  private final JobFailureMailer mailer = new JobFailureMailer(parameters, messages);

  private static JobRun failedRun() {
    return new JobRun(
        "UAM_EFFECTIVE_CHANGES", JobTrigger.SCHEDULED, "SYSTEM", Instant.parse("2026-09-25T16:05:00Z"));
  }

  @Test
  void aFailedRunIsMailedToTheValidRecipients() {
    when(parameters.items(JobFailureMailer.RECIPIENTS))
        .thenReturn(List.of("ops@bdo-insure.ph", "not-an-address", "ops@bdo-insure.ph"));
    mailer.onFailure(failedRun());
    ArgumentCaptor<OutboundEmail> mail = ArgumentCaptor.forClass(OutboundEmail.class);
    verify(messages).queueEmail(mail.capture());
    assertThat(mail.getValue().to()).containsExactly("ops@bdo-insure.ph");
    assertThat(mail.getValue().purpose()).isEqualTo(JobFailureMailer.PURPOSE);
    assertThat(mail.getValue().subject()).contains("UAM_EFFECTIVE_CHANGES");
    assertThat(mail.getValue().body()).contains("26 Sep 2026 00:05:00");
  }

  @Test
  void nothingIsSentWithoutRecipients() {
    when(parameters.items(JobFailureMailer.RECIPIENTS)).thenReturn(List.of());
    mailer.onFailure(failedRun());
    verify(messages, never()).queueEmail(any());
  }
}
