package com.iortatechnxt.brokerverse.renewal.alert;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertCheck;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfile;
import com.iortatechnxt.brokerverse.catalog.domain.InsurerProfileRepository;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.renewal.domain.Bucket;
import com.iortatechnxt.brokerverse.renewal.domain.BucketHistory;
import com.iortatechnxt.brokerverse.renewal.domain.BucketHistoryRepository;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerBatch;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerBatchRepository;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerBatchStatus;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Renewal alerts of the daily alert run: renewals in the Exception bucket for longer than {@code
 * RNW_EXCEPTION_AGEING_DAYS} working days, and insurer batches past their reply date. Renewals at
 * risk raise no alert: they show with their attention flag in the renewal listing and the Account
 * Officer escalates outside the system (BRRN.036; FR-RN-102).
 */
@Component
@Transactional(readOnly = true)
public class RenewalAlertCheck implements AlertCheck {

  private static final Set<RenewalStage> BEFORE_ACCEPTANCE =
      EnumSet.of(
          RenewalStage.UNASSIGNED,
          RenewalStage.FOR_DISPOSITION,
          RenewalStage.TRANSFER_PENDING,
          RenewalStage.FOR_TL_REVIEW,
          RenewalStage.FOR_PROCESSING,
          RenewalStage.IN_PROCESSING,
          RenewalStage.WITH_INSURER,
          RenewalStage.RA_READY,
          RenewalStage.RA_GENERATED,
          RenewalStage.RA_SENT);

  private final RenewalCandidateRepository candidates;
  private final BucketHistoryRepository buckets;
  private final InsurerBatchRepository batches;
  private final RenewalParameters parameters;
  private final InsurerProfileRepository insurers;
  private final OrganizationService organization;

  /**
   * Creates the check.
   *
   * @param candidates renewals
   * @param buckets bucket history
   * @param batches insurer batches
   * @param parameters thresholds
   * @param insurers insurers (the name in the alert)
   * @param organization head office calendar (working days of the exception ageing)
   */
  public RenewalAlertCheck(
      RenewalCandidateRepository candidates,
      BucketHistoryRepository buckets,
      InsurerBatchRepository batches,
      RenewalParameters parameters,
      InsurerProfileRepository insurers,
      OrganizationService organization) {
    this.candidates = candidates;
    this.buckets = buckets;
    this.batches = batches;
    this.parameters = parameters;
    this.insurers = insurers;
    this.organization = organization;
  }

  @Override
  public List<AlertSignal> evaluate(LocalDate asOf) {
    List<AlertSignal> signals = new ArrayList<>();
    for (RenewalCandidate c : candidates.findByStageIn(BEFORE_ACCEPTANCE)) {
      ageing(c, asOf, signals);
    }
    for (InsurerBatch b :
        batches.findByStatusInAndReplyDueBefore(
            List.of(InsurerBatchStatus.SENT, InsurerBatchStatus.PARTIALLY_RESPONDED), asOf)) {
      signals.add(
          new AlertSignal(
              RenewalCodes.ALERT_INSURER_OVERDUE,
              new AlertFacts(
                  b.getCompanyId(),
                  null,
                  RenewalCodes.ENTITY_BATCH,
                  b.getBatchNo(),
                  "Insurer "
                      + insurerName(b)
                      + " has not answered batch "
                      + b.getBatchNo()
                      + " due "
                      + DisplayFormat.date(b.getReplyDue()),
                  null,
                  RenewalCodes.ALERT_INSURER_OVERDUE + ":" + b.getBatchNo())));
    }
    return signals;
  }

  private void ageing(RenewalCandidate c, LocalDate asOf, List<AlertSignal> signals) {
    if (c.getBucket() == Bucket.EXCEPTION) {
      LocalDate since =
          buckets.findByCandidateIdOrderByIdDesc(c.getId()).stream()
              .filter(h -> h.getToBucket() == Bucket.EXCEPTION)
              .map(BucketHistory::getCreatedAt)
              .map(BusinessClock::dateOf)
              .findFirst()
              .orElse(asOf);
      if (!plusWorkingDays(c.getCompanyId(), since, parameters.exceptionAgeingDays())
          .isAfter(asOf)) {
        signals.add(
            new AlertSignal(
                RenewalCodes.ALERT_EXCEPTION_AGEING,
                facts(
                    c,
                    "Renewal "
                        + c.getRenewalRef()
                        + " is an Exception since "
                        + DisplayFormat.date(since),
                    RenewalCodes.ALERT_EXCEPTION_AGEING + ":" + c.getRenewalRef() + ":" + since)));
      }
    }
  }

  /**
   * The date a number of working days after a date, on the head office calendar of the company
   * (weekends and holidays; Monday to Friday when the company has no head office), R29-05.
   */
  private LocalDate plusWorkingDays(Long companyId, LocalDate from, int days) {
    Optional<Branch> head =
        organization.listBranches(companyId).stream().filter(Branch::isHeadOffice).findFirst();
    Predicate<LocalDate> working =
        head.<Predicate<LocalDate>>map(b -> d -> organization.isWorkingDay(b, d))
            .orElse(RenewalAlertCheck::weekday);
    LocalDate date = from;
    int left = days;
    while (left > 0) {
      date = date.plusDays(1);
      if (working.test(date)) {
        left--;
      }
    }
    return date;
  }

  private static boolean weekday(LocalDate d) {
    return d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY;
  }

  private String insurerName(InsurerBatch b) {
    return insurers
        .findByCompanyIdAndPartyCode(b.getCompanyId(), b.getInsurerCode())
        .map(InsurerProfile::getName)
        .orElse(b.getInsurerCode());
  }

  private static AlertFacts facts(RenewalCandidate c, String message, String key) {
    return new AlertFacts(
        c.getCompanyId(), null, RenewalCodes.ENTITY, c.getRenewalRef(), message, null, key);
  }
}
