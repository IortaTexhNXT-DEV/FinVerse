package com.iortatechnxt.brokerverse.messaging.service;

import com.iortatechnxt.brokerverse.common.util.EmailAddresses;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage.RecordLink;
import com.iortatechnxt.brokerverse.system.domain.JobRun;
import com.iortatechnxt.brokerverse.system.service.JobFailureListener;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * E-mails every failed batch run to the addresses of {@code JOB_FAILURE_RECIPIENTS} (UAM-NFR-24,
 * 25; FR-UA-071), besides the in-app alert JOB_FAILURE: the job, the time and the error. Invalid
 * addresses in the parameter are skipped (and logged); an empty parameter sends nothing.
 */
@Component
public class JobFailureMailer implements JobFailureListener {

  /** Parameter holding the recipients (comma separated). */
  public static final String RECIPIENTS = "JOB_FAILURE_RECIPIENTS";

  /** Purpose code of the e-mail. */
  public static final String PURPOSE = "JOB_FAILURE";

  private static final Logger LOG = LoggerFactory.getLogger(JobFailureMailer.class);
  private static final ZoneId MANILA = ZoneId.of("Asia/Manila");
  private static final DateTimeFormatter WHEN =
      DateTimeFormatter.ofPattern("d MMM yyyy HH:mm:ss", Locale.ENGLISH);

  private final SystemParameterService parameters;
  private final MessageService messages;

  /**
   * Creates the listener.
   *
   * @param parameters business parameters (recipients)
   * @param messages e-mail outbox
   */
  public JobFailureMailer(SystemParameterService parameters, MessageService messages) {
    this.parameters = parameters;
    this.messages = messages;
  }

  @Override
  public void onFailure(JobRun run) {
    List<String> recipients =
        parameters.items(RECIPIENTS).stream().filter(JobFailureMailer::valid).distinct().toList();
    if (recipients.isEmpty()) {
      return;
    }
    String started =
        run.getStartedAt() == null ? "-" : WHEN.format(run.getStartedAt().atZone(MANILA));
    String body =
        "The batch job "
            + run.getJobName()
            + " failed.\n\nRun: "
            + run.getId()
            + "\nStarted: "
            + started
            + " (Philippine time)\nTrigger: "
            + run.getTrigger()
            + "\nError: "
            + (run.getMessage() == null ? "-" : run.getMessage())
            + "\n\nOpen Administration > Scheduled Jobs to see the run history and run the job"
            + " again.";
    messages.queueEmail(
        new OutboundEmail(
            null,
            PURPOSE,
            recipients,
            List.of(),
            "BrokerVerse: batch job " + run.getJobName() + " failed",
            body,
            List.of(),
            null,
            new RecordLink("JobRun", String.valueOf(run.getId()), run.getJobName())));
  }

  private static boolean valid(String address) {
    if (EmailAddresses.isValid(address)) {
      return true;
    }
    LOG.warn("{} is not a valid e-mail address in {}; skipped", address, RECIPIENTS);
    return false;
  }
}
