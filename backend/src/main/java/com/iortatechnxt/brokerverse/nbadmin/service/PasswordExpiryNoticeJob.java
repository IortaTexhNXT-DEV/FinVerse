package com.iortatechnxt.brokerverse.nbadmin.service;

import com.iortatechnxt.brokerverse.security.service.AuthPasswordService;
import com.iortatechnxt.brokerverse.security.service.AuthPasswordService.PasswordExpiry;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Job {@code PASSWORD_EXPIRY_NOTICE} (UAM-NFR-36; FR-UA-005; USER_ACCESS_DESIGN section 8): LOCAL
 * sign-in only, tells the users whose password expires within {@value #NOTICE_DAYS} days, in the
 * app and by e-mail. Daily at 06:00 PHT ({@code brokerverse.jobs.password-expiry-notice-cron}).
 * Each user is told in a transaction of its own, so one failure does not stop the others.
 */
@Component
public class PasswordExpiryNoticeJob implements ManagedJob {

  /** Job name in the job monitor. */
  public static final String JOB_NAME = "PASSWORD_EXPIRY_NOTICE";

  /** Days before the expiry the notice is sent. */
  public static final int NOTICE_DAYS = 7;

  private static final Logger LOG = LoggerFactory.getLogger(PasswordExpiryNoticeJob.class);

  private final AuthPasswordService passwords;
  private final PasswordNoticeMailer mailer;
  private final TransactionTemplate transaction;
  private final String cron;

  /**
   * Creates the job.
   *
   * @param passwords passwords that expire soon
   * @param mailer notices
   * @param transactionManager transaction manager (a transaction per user)
   * @param cron schedule
   */
  public PasswordExpiryNoticeJob(
      AuthPasswordService passwords,
      PasswordNoticeMailer mailer,
      PlatformTransactionManager transactionManager,
      @Value("${brokerverse.jobs.password-expiry-notice-cron:0 0 22 * * *}") String cron) {
    this.passwords = passwords;
    this.mailer = mailer;
    this.transaction = new TransactionTemplate(transactionManager);
    this.cron = cron;
  }

  @Override
  public String name() {
    return JOB_NAME;
  }

  @Override
  public String description() {
    return "Tells users whose password expires within 7 days (LOCAL sign-in)";
  }

  @Override
  public String cron() {
    return cron;
  }

  @Override
  public JobOutcome execute(LocalDate businessDate) {
    List<PasswordExpiry> expiring = passwords.expiringWithin(NOTICE_DAYS);
    int told = 0;
    for (PasswordExpiry expiry : expiring) {
      try {
        if (Boolean.TRUE.equals(transaction.execute(s -> mailer.notifyExpiry(expiry)))) {
          told++;
        }
      } catch (RuntimeException ex) {
        LOG.warn("Password expiry notice to {} failed", expiry.username(), ex);
      }
    }
    return new JobOutcome(
        told, told + " of " + expiring.size() + " user(s) told that their password expires soon");
  }
}
