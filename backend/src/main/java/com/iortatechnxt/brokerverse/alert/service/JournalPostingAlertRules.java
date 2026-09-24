package com.iortatechnxt.brokerverse.alert.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatch;
import com.iortatechnxt.brokerverse.journal.service.JournalPostingListener;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/**
 * Posting-time exception rules, evaluated in the posting transaction: LARGE_JOURNAL (total at or
 * above the threshold amount), BACK_DATED_POSTING and WEEKEND_POSTING (manual journals only).
 */
@Component
public class JournalPostingAlertRules implements JournalPostingListener {

  /** Large journal. */
  public static final String LARGE_JOURNAL = "LARGE_JOURNAL";

  /** Back-dated posting. */
  public static final String BACK_DATED = "BACK_DATED_POSTING";

  /** Posting on a non-working day. */
  public static final String WEEKEND = "WEEKEND_POSTING";

  private static final String ENTITY = "JournalBatch";

  private final AlertService alerts;
  private final OrganizationService organization;

  /**
   * Creates the rules.
   *
   * @param alerts alert service
   * @param organization organization (working days)
   */
  public JournalPostingAlertRules(AlertService alerts, OrganizationService organization) {
    this.alerts = alerts;
    this.organization = organization;
  }

  @Override
  public void onPosted(JournalBatch batch) {
    checkLargeJournal(batch);
    if (batch.getJournalType().isSystemGenerated()) {
      return;
    }
    LocalDate postedOn = LocalDate.ofInstant(batch.getPostedAt(), ZoneOffset.UTC);
    checkBackDated(batch, postedOn);
    checkNonWorkingDay(batch, postedOn);
  }

  private void checkLargeJournal(JournalBatch batch) {
    alerts
        .activeCode(LARGE_JOURNAL)
        .filter(c -> c.getThresholdAmount() != null)
        .filter(c -> batch.getTotalDebit().compareTo(c.getThresholdAmount()) >= 0)
        .ifPresent(
            c ->
                raise(
                    LARGE_JOURNAL,
                    batch,
                    "Journal "
                        + batch.getBatchNo()
                        + " of "
                        + batch.getTotalDebit()
                        + " reached the large journal threshold "
                        + c.getThresholdAmount()));
  }

  private void checkBackDated(JournalBatch batch, LocalDate postedOn) {
    long days = ChronoUnit.DAYS.between(batch.getValueDate(), postedOn);
    alerts
        .activeCode(BACK_DATED)
        .filter(c -> days > (c.getThresholdDays() == null ? 0 : c.getThresholdDays()))
        .ifPresent(
            c ->
                raise(
                    BACK_DATED,
                    batch,
                    "Journal "
                        + batch.getBatchNo()
                        + " posted on "
                        + postedOn
                        + " with value date "
                        + batch.getValueDate()
                        + " ("
                        + days
                        + " days back)"));
  }

  private void checkNonWorkingDay(JournalBatch batch, LocalDate postedOn) {
    Branch branch = organization.getBranch(batch.getBranchId());
    boolean valueDateOff = !organization.isWorkingDay(branch, batch.getValueDate());
    boolean postingDateOff = !organization.isWorkingDay(branch, postedOn);
    if (valueDateOff || postingDateOff) {
      raise(
          WEEKEND,
          batch,
          "Journal "
              + batch.getBatchNo()
              + " was "
              + (postingDateOff ? "posted on " + postedOn : "dated " + batch.getValueDate())
              + ", a non-working day of branch "
              + branch.getCode());
    }
  }

  private void raise(String code, JournalBatch batch, String message) {
    alerts.raise(
        code,
        new AlertFacts(
            batch.getCompanyId(),
            batch.getBranchId(),
            ENTITY,
            batch.getBatchNo(),
            message,
            batch.getTotalDebit(),
            code + ":" + batch.getId()));
  }
}
