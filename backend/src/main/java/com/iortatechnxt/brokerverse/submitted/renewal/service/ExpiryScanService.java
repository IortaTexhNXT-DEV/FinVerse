package com.iortatechnxt.brokerverse.submitted.renewal.service;

import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyStatus;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRenewal;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRenewalRepository;
import com.iortatechnxt.brokerverse.submitted.service.SbmParameters;
import com.iortatechnxt.brokerverse.submitted.service.SubmittedCodes;
import com.iortatechnxt.brokerverse.submitted.service.port.RenewalHandOff;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The expiry scan (BRIDSP-23; FRS FR-SP-060; job {@code SBM_EXPIRY_SCAN}): the For Renewal records
 * whose expiry falls within the lead days of their segment are handed to Renewal with the insurer
 * of the insurer rules, except the segments renewed by hand; a record no insurer rule fits stays
 * For Renewal in the fallout with reason NO_INSURER. The hand-offs still pending are offered to
 * the Renewal module again (replay once it is connected).
 */
@Service
@Transactional
public class ExpiryScanService {

  private final SbmPolicyRepository policies;
  private final SbmRenewalRepository renewals;
  private final HandOffService handOffs;
  private final SbmParameters parameters;
  private final LovService lovs;
  private final NotificationService notifications;

  /**
   * Creates the service.
   *
   * @param policies masterlist
   * @param renewals hand-offs
   * @param handOffs hand-off
   * @param parameters parameters
   * @param lovs lists of values (segments)
   * @param notifications notifications
   */
  public ExpiryScanService(
      SbmPolicyRepository policies,
      SbmRenewalRepository renewals,
      HandOffService handOffs,
      SbmParameters parameters,
      LovService lovs,
      NotificationService notifications) {
    this.policies = policies;
    this.renewals = renewals;
    this.handOffs = handOffs;
    this.parameters = parameters;
    this.lovs = lovs;
    this.notifications = notifications;
  }

  /**
   * Scans a company.
   *
   * @param companyId company
   * @param today business date
   * @return what the scan did
   */
  public Scan scan(Long companyId, LocalDate today) {
    int handed = 0;
    int noInsurer = 0;
    List<String> manual = parameters.manualSegments();
    for (LovValue segment : lovs.activeValues(SubmittedCodes.LOV_SEGMENT, today)) {
      if (manual.contains(segment.getCode())) {
        continue;
      }
      LocalDate until = today.plusDays(parameters.leadDays(segment.getCode()));
      for (SbmPolicy p : policies.expiringUntil(companyId, SbmPolicyStatus.FOR_RENEWAL, until)) {
        if (!segment.getCode().equals(p.getSegment())
            || renewals.findByPolicyId(p.getId()).isPresent()) {
          continue;
        }
        Optional<String> insurer = handOffs.assignInsurer(p);
        if (insurer.isEmpty()) {
          p.fallout("NO_INSURER", p.getLastRunNo());
          noInsurer++;
          continue;
        }
        notifyNear(p);
        handOffs.handOff(p, insurer.get(), false);
        handed++;
      }
    }
    int replayed = replay(companyId);
    return new Scan(handed, noInsurer, replayed);
  }

  private int replay(Long companyId) {
    int replayed = 0;
    for (SbmRenewal r :
        renewals.findByCompanyIdAndHandoffStatusInAndOutcome(
            companyId, List.of(SbmRenewal.PENDING), SbmRenewal.IN_PROGRESS)) {
      SbmPolicy p = policies.findById(r.getPolicyId()).orElse(null);
      if (p != null
          && handOffs.offer(p, r).outcome() == RenewalHandOff.Outcome.HANDED_OFF) {
        replayed++;
      }
    }
    return replayed;
  }

  private void notifyNear(SbmPolicy p) {
    Notice notice =
        new Notice(
            "Submitted policy " + p.getSbmNo() + " nears expiry",
            p.getAssured().assuredName(),
            SubmittedCodes.link(p.getId()),
            SubmittedCodes.ENTITY,
            p.getId().toString());
    for (String user : new String[] {p.getHandlerUsername(), p.getAoUsername()}) {
      if (user != null) {
        notifications.notifyUser(user, notice, "SBM_EXPIRY_NEAR");
      }
    }
  }

  /**
   * What a scan did.
   *
   * @param handedOff records handed to Renewal
   * @param noInsurer records without an insurer rule
   * @param replayed pending hand-offs taken by the Renewal module
   */
  public record Scan(int handedOff, int noInsurer, int replayed) {}
}
