package com.iortatechnxt.brokerverse.renewal.extraction.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.domain.ApprovalStatus;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateSource;
import com.iortatechnxt.brokerverse.renewal.domain.ExtractionRun;
import com.iortatechnxt.brokerverse.renewal.domain.ExtractionTrigger;
import com.iortatechnxt.brokerverse.renewal.domain.LetterStatus;
import com.iortatechnxt.brokerverse.renewal.domain.LetterType;
import com.iortatechnxt.brokerverse.renewal.domain.RaNotice;
import com.iortatechnxt.brokerverse.renewal.domain.RaSentRequest;
import com.iortatechnxt.brokerverse.renewal.domain.RaSentRequestRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalExtractionRunRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalLetter;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalLetterRepository;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.port.LegacyPolicySource.LegacyRaSent;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The Renewal Advices already sent by hand before go-live (DMQ37, DMQ38; FR-RN-016): recorded on
 * the candidate of the migrated policy as a letter of status SENT with source LEGACY_MANUAL and the
 * tracker's date and reference, so BIBS never generates or sends it again and the second notice and
 * NRNS checkpoint count from that date. Tracker rows come from the migration (P03), from the upload
 * {@code RNW_RA_ALREADY_SENT}, or - for rows the upload rejected - as corrections prepared by one
 * member of the Renewal processing team and applied only when a second member approves them.
 */
@Service
@Transactional
public class RaAlreadySentService {

  private final RenewalCandidateRepository candidates;
  private final RenewalLetterRepository letters;
  private final RaSentRequestRepository requests;
  private final RenewalExtractionRunRepository runs;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final CurrentUser currentUser;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param candidates candidates
   * @param letters letters
   * @param requests corrections
   * @param runs extraction runs (go-live date)
   * @param numbers document numbers
   * @param audit audit trail
   * @param currentUser current user
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public RaAlreadySentService(
      RenewalCandidateRepository candidates,
      RenewalLetterRepository letters,
      RaSentRequestRepository requests,
      RenewalExtractionRunRepository runs,
      DocumentNumberService numbers,
      AuditTrailService audit,
      CurrentUser currentUser,
      Clock clock) {
    this.candidates = candidates;
    this.letters = letters;
    this.requests = requests;
    this.runs = runs;
    this.numbers = numbers;
    this.audit = audit;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  /**
   * Records the RA of a migrated header served by the migration (P03).
   *
   * @param candidateId candidate
   * @param sent the RA sent by hand
   */
  public void record(Long candidateId, LegacyRaSent sent) {
    RenewalCandidate c = candidates.findById(candidateId).orElseThrow();
    apply(
        c,
        new RaSentRequest.Row(
            c.getSourceRef(), sent.sentOn(), sent.reference(), sent.channel(), sent.recipient()));
  }

  /**
   * Checks a tracker row (upload validation and corrections).
   *
   * @param companyId company
   * @param row tracker row
   * @return the problems, empty when the row can be applied
   */
  @Transactional(readOnly = true)
  public List<String> problems(Long companyId, RaSentRequest.Row row) {
    List<String> problems = new ArrayList<>();
    Optional<RenewalCandidate> c = candidate(companyId, row.legacyRef());
    if (c.isEmpty()) {
      problems.add("No renewal candidate for legacy policy " + row.legacyRef());
    } else if (hasRa(c.get())) {
      problems.add(
          "The Renewal Advice of legacy policy " + row.legacyRef() + " is already recorded");
    }
    LocalDate goLive = goLiveDate(companyId);
    if (row.raDate() == null || row.raDate().isAfter(goLive)) {
      problems.add("Enter the date the Renewal Advice was sent (before go-live)");
    }
    return problems;
  }

  /**
   * Applies a valid tracker row.
   *
   * @param companyId company
   * @param row tracker row
   * @return letter number
   */
  public String applyRow(Long companyId, RaSentRequest.Row row) {
    List<String> problems = problems(companyId, row);
    if (!problems.isEmpty()) {
      throw new BusinessRuleException("RNW_RA_SENT_INVALID", String.join("; ", problems));
    }
    return apply(candidate(companyId, row.legacyRef()).orElseThrow(), row).getLetterNo();
  }

  /**
   * Prepares the correction of a rejected row (maker).
   *
   * @param companyId company
   * @param row corrected row
   * @param source upload job and row corrected, may be null
   * @param correction what was corrected
   * @return the correction, waiting for a checker
   */
  public RaSentRequest prepare(
      Long companyId, RaSentRequest.Row row, RaSentRequest.Source source, String correction) {
    if (correction == null || correction.isBlank()) {
      throw new BusinessRuleException("RNW_RA_SENT_CORRECTION", "Describe the correction");
    }
    List<String> problems = problems(companyId, row);
    if (!problems.isEmpty()) {
      throw new BusinessRuleException("RNW_RA_SENT_INVALID", String.join("; ", problems));
    }
    return requests.save(new RaSentRequest(companyId, row, source, correction.strip()));
  }

  /**
   * Decides a correction (checker, another member of the team); an approved row is applied.
   *
   * @param id correction
   * @param approve approve or reject
   * @param remarks remarks
   * @return the correction
   */
  public RaSentRequest decide(Long id, boolean approve, String remarks) {
    RaSentRequest request =
        requests.findById(id).orElseThrow(() -> new ResourceNotFoundException("Correction", id));
    request.decide(currentUser.username(), approve, remarks, clock.instant());
    if (approve) {
      RenewalCandidate c =
          candidate(request.getCompanyId(), request.getLegacyRef())
              .orElseThrow(() -> new ResourceNotFoundException("Renewal", request.getLegacyRef()));
      apply(c, request.row());
      request.appliedTo(c.getId());
    }
    return request;
  }

  /**
   * Corrections of a company in a status.
   *
   * @param companyId company
   * @param status status
   * @return corrections
   */
  @Transactional(readOnly = true)
  public List<RaSentRequest> corrections(Long companyId, ApprovalStatus status) {
    return requests.findByCompanyIdAndStatusOrderByIdAsc(companyId, status);
  }

  private RenewalLetter apply(RenewalCandidate c, RaSentRequest.Row row) {
    RenewalLetter letter =
        letters.save(
            RenewalLetter.legacySent(
                c,
                numbers.next("RA-" + BusinessClock.today(clock).getYear()),
                new RenewalLetter.Legacy(row.raRef(), row.raDate(), row.channel(), row.recipient()),
                clock.instant()));
    c.noticed(RaNotice.FIRST);
    audit.record(
        RenewalCodes.ENTITY,
        c.getRenewalRef(),
        AuditAction.UPDATE,
        "Renewal Advice sent by hand on "
            + row.raDate()
            + (row.raRef() == null ? "" : " (" + row.raRef() + ")")
            + " recorded; not sent again");
    return letter;
  }

  private boolean hasRa(RenewalCandidate c) {
    return letters.findByCandidateIdOrderByIdDesc(c.getId()).stream()
        .anyMatch(l -> l.getType() == LetterType.RA && l.getStatus() != LetterStatus.CANCELLED);
  }

  private Optional<RenewalCandidate> candidate(Long companyId, String legacyRef) {
    return legacyRef == null
        ? Optional.empty()
        : candidates.findByCompanyIdAndSourceAndSourceRef(
            companyId, CandidateSource.LEGACY, legacyRef.strip());
  }

  private LocalDate goLiveDate(Long companyId) {
    return runs.findByCompanyIdAndTrigger(companyId, ExtractionTrigger.GOLIVE).stream()
        .map(ExtractionRun::getExpiryFrom)
        .filter(d -> d != null)
        .min(LocalDate::compareTo)
        .orElse(BusinessClock.today(clock));
  }
}
