package com.iortatechnxt.brokerverse.submitted.review.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertCheck;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.submitted.domain.SbmApprovable;
import com.iortatechnxt.brokerverse.submitted.domain.SbmDocStatus;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIaaf;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIaafRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmTor;
import com.iortatechnxt.brokerverse.submitted.domain.SbmTorRepository;
import com.iortatechnxt.brokerverse.submitted.service.SbmParameters;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Daily check of the IAAF and TOR approvals (FR-SP-041, 053): a document waiting at its level for
 * more than {@code SBM_REVIEW_SLA_DAYS} days raises {@code SBM_IAAF_SLA} or {@code SBM_TOR_SLA},
 * once per document and level.
 */
@Component
public class ReviewSlaCheck implements AlertCheck {

  private final SbmIaafRepository iaafs;
  private final SbmTorRepository tors;
  private final SbmParameters parameters;

  /**
   * Creates the check.
   *
   * @param iaafs IAAFs
   * @param tors TORs
   * @param parameters review days
   */
  public ReviewSlaCheck(SbmIaafRepository iaafs, SbmTorRepository tors, SbmParameters parameters) {
    this.iaafs = iaafs;
    this.tors = tors;
    this.parameters = parameters;
  }

  @Override
  @Transactional(readOnly = true)
  public List<AlertSignal> evaluate(LocalDate asOf) {
    LocalDate limit = asOf.minusDays(parameters.reviewSlaDays());
    List<AlertSignal> signals = new ArrayList<>();
    for (SbmIaaf i : iaafs.findByStatus(SbmDocStatus.FOR_APPROVAL)) {
      if (waiting(i, limit)) {
        signals.add(signal("SBM_IAAF_SLA", "SubmittedIaaf", i.getIaafNo(), "IAAF", i));
      }
    }
    for (SbmTor t : tors.findByStatus(SbmDocStatus.FOR_APPROVAL)) {
      if (waiting(t, limit)) {
        signals.add(signal("SBM_TOR_SLA", "SubmittedTor", t.getTorNo(), "TOR", t));
      }
    }
    return signals;
  }

  private static boolean waiting(SbmApprovable d, LocalDate limit) {
    return d.getUpdatedAt() != null && BusinessClock.dateOf(d.getUpdatedAt()).isBefore(limit);
  }

  private static AlertSignal signal(
      String code, String entity, String number, String label, SbmApprovable d) {
    return new AlertSignal(
        code,
        new AlertFacts(
            d.getCompanyId(),
            null,
            entity,
            number,
            label
                + " "
                + number
                + " has waited at approval level "
                + d.getCurrentLevel()
                + " since "
                + DisplayFormat.date(BusinessClock.dateOf(d.getUpdatedAt())),
            BigDecimal.ZERO,
            code + ":" + number + ":" + d.getCurrentLevel()));
  }
}
