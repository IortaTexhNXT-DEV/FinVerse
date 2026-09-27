package com.iortatechnxt.brokerverse.renewal.extraction.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSource;
import com.iortatechnxt.brokerverse.renewal.domain.ExtractionRun;
import com.iortatechnxt.brokerverse.renewal.domain.ExtractionTrigger;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalNotices;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import com.iortatechnxt.brokerverse.renewal.service.port.LegacyPolicySource;
import com.iortatechnxt.brokerverse.renewal.service.port.LegacyPolicySource.GoLiveHeaders;
import com.iortatechnxt.brokerverse.renewal.service.port.LegacyPolicySource.LegacyHeader;
import com.iortatechnxt.brokerverse.security.domain.Permission;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * The go-live take-over of the renewals (DMQ37; FR-RN-016; RENEWAL_DESIGN section 13.1): once,
 * after the migration load is signed off, every migrated in-force header expiring from go-live to
 * {@code MIG_GOLIVE_RENEWAL_TO} becomes a candidate (source LEGACY), earliest expiry first; those
 * expiring up to {@code MIG_RENEWAL_URGENT_TO} are flagged Urgent. A Renewal Advice already sent by
 * hand before go-live (P03) is recorded and never sent again. A second run creates nothing new (one
 * candidate per legacy reference). The Processing TL and the Marketing TLs are told the counts per
 * expiry month.
 */
@Service
public class GoLiveService {

  private final LegacyPolicySource legacy;
  private final ExtractionService extraction;
  private final RaAlreadySentService raSent;
  private final RenewalCandidateRepository candidates;
  private final RenewalParameters parameters;
  private final RenewalNotices notices;

  /**
   * Creates the service.
   *
   * @param legacy migrated policy headers
   * @param extraction extraction runs and candidate creation
   * @param raSent Renewal Advices already sent
   * @param candidates candidates
   * @param parameters renewal parameters
   * @param notices notifications
   */
  public GoLiveService(
      LegacyPolicySource legacy,
      ExtractionService extraction,
      RaAlreadySentService raSent,
      RenewalCandidateRepository candidates,
      RenewalParameters parameters,
      RenewalNotices notices) {
    this.legacy = legacy;
    this.extraction = extraction;
    this.raSent = raSent;
    this.candidates = candidates;
    this.parameters = parameters;
    this.notices = notices;
  }

  /**
   * Takes over the expiries of the go-live window.
   *
   * @param companyId company
   * @param goLive go-live date (first expiry of the window)
   * @return the run
   */
  public ExtractionRun takeOver(Long companyId, LocalDate goLive) {
    LocalDate to = parameters.goLiveTo();
    LocalDate urgentTo = parameters.urgentTo();
    ExtractionRun run = extraction.start(companyId, ExtractionTrigger.GOLIVE, goLive, to);
    try {
      GoLiveHeaders answer = legacy.goLiveCandidates(companyId, goLive, to);
      int created = 0;
      int existing = 0;
      int urgent = 0;
      Map<YearMonth, Integer> byMonth = new TreeMap<>();
      for (LegacyHeader header :
          answer.headers().stream()
              .sorted(Comparator.comparing(h -> h.policy().expiryDate()))
              .toList()) {
        if (candidates
            .findByCompanyIdAndSourceAndSourceRef(
                companyId, CandidateSource.LEGACY, header.legacyRef())
            .isPresent()) {
          existing++;
          continue;
        }
        boolean isUrgent = !header.policy().expiryDate().isAfter(urgentTo);
        RenewalCandidate c = extraction.createLegacy(companyId, header, run.getId(), isUrgent);
        if (header.raSent() != null) {
          raSent.record(c.getId(), header.raSent());
        }
        created++;
        urgent += isUrgent || header.urgent() ? 1 : 0;
        byMonth.merge(YearMonth.from(header.policy().expiryDate()), 1, Integer::sum);
      }
      ExtractionRun done =
          extraction.complete(
              run.getId(),
              new ExtractionRun.Counts(
                  answer.headers().size() + answer.renewedInLegacy(),
                  created,
                  existing,
                  answer.renewedInLegacy(),
                  urgent));
      notify(done, byMonth);
      return done;
    } catch (RuntimeException e) {
      extraction.fail(run.getId(), e.getMessage());
      throw new BusinessRuleException(
          "RNW_GOLIVE_FAILED", "The go-live take-over failed: " + e.getMessage(), e);
    }
  }

  private void notify(ExtractionRun run, Map<YearMonth, Integer> byMonth) {
    String months =
        byMonth.entrySet().stream()
            .map(e -> e.getKey() + ": " + e.getValue())
            .collect(Collectors.joining(", "));
    notices.holders(
        Permission.RNW_EXTRACT,
        RenewalCodes.EVENT_GOLIVE,
        new RenewalNotices.Text(
            "Go-live renewals taken over (" + run.getNewCount() + ")",
            run.getNewCount()
                + " renewals, "
                + run.getUrgentCount()
                + " urgent"
                + (months.isEmpty() ? "" : "; by expiry month " + months)),
        "/renewal/expiry?urgent=true");
  }
}
