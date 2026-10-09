package com.iortatechnxt.brokerverse.renewal.letter.service;

import com.iortatechnxt.brokerverse.alert.domain.AlertFacts;
import com.iortatechnxt.brokerverse.alert.service.AlertService;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.messaging.domain.MessageStatus;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessage;
import com.iortatechnxt.brokerverse.messaging.domain.OutboundMessageRepository;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateExpiry;
import com.iortatechnxt.brokerverse.renewal.domain.ClosedAs;
import com.iortatechnxt.brokerverse.renewal.domain.LetterBatch;
import com.iortatechnxt.brokerverse.renewal.domain.LetterSource;
import com.iortatechnxt.brokerverse.renewal.domain.LetterStatus;
import com.iortatechnxt.brokerverse.renewal.domain.LetterType;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalLetter;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalLetterRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.holdcover.service.RenewalHoldCoverService;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalFlow;
import com.iortatechnxt.brokerverse.renewal.service.RenewalNotices;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The daily letter work of the renewal jobs, one renewal per transaction (FR-RN-083, 081, 082): the
 * NRNS classification with one reminder letter per renewal at the checkpoint (none while a
 * confirmed hold cover extends the cover), the expiry sweep at the effective expiry date with the
 * non-acceptance letter, the closure EXPIRED_UNRENEWED and the routing to the No Advice or
 * Non-Renewal Letter, and the delivery status of the queued letters (alert {@code
 * RNW_LETTER_FAILED}).
 */
@Service
@Transactional
public class LetterSweeps {

  /** Stages without a disposition submitted. */
  public static final Set<RenewalStage> NOT_SUBMITTED =
      EnumSet.of(
          RenewalStage.EXTRACTED,
          RenewalStage.UNASSIGNED,
          RenewalStage.TRANSFER_PENDING,
          RenewalStage.FOR_DISPOSITION,
          RenewalStage.RA_SENT);

  /** Stages closed by the expiry sweep. */
  public static final Set<RenewalStage> EXPIRABLE =
      EnumSet.of(
          RenewalStage.EXTRACTED,
          RenewalStage.UNASSIGNED,
          RenewalStage.FOR_DISPOSITION,
          RenewalStage.FOR_TL_REVIEW,
          RenewalStage.FOR_PROCESSING,
          RenewalStage.IN_PROCESSING,
          RenewalStage.WITH_INSURER,
          RenewalStage.RA_READY,
          RenewalStage.RA_GENERATED,
          RenewalStage.RA_SENT);

  private final RenewalCandidateRepository candidates;
  private final RenewalLetterRepository letters;
  private final LetterWriter writer;
  private final RenewalFlow flow;
  private final RenewalParameters parameters;
  private final RenewalNotices notices;
  private final OutboundMessageRepository messages;
  private final AlertService alerts;
  private final RenewalHoldCoverService holdCovers;
  private final Clock clock;

  /**
   * Creates the sweeps.
   *
   * @param candidates renewals
   * @param letters letters
   * @param writer letter writer
   * @param flow workflow
   * @param parameters parameters
   * @param notices notifications
   * @param messages outbound messages
   * @param alerts alerts
   * @param holdCovers hold covers of the renewals (effective expiry date)
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // constructor injection
  public LetterSweeps(
      RenewalCandidateRepository candidates,
      RenewalLetterRepository letters,
      LetterWriter writer,
      RenewalFlow flow,
      RenewalParameters parameters,
      RenewalNotices notices,
      OutboundMessageRepository messages,
      AlertService alerts,
      RenewalHoldCoverService holdCovers,
      Clock clock) {
    this.candidates = candidates;
    this.letters = letters;
    this.writer = writer;
    this.flow = flow;
    this.parameters = parameters;
    this.notices = notices;
    this.messages = messages;
    this.alerts = alerts;
    this.holdCovers = holdCovers;
    this.clock = clock;
  }

  /**
   * Ids of the renewals to classify as NRNS at a date.
   *
   * @param today business date
   * @return ids
   */
  @Transactional(readOnly = true)
  public List<Long> nrnsDue(LocalDate today) {
    int days = parameters.nrnsDays();
    return candidates.findByStageIn(NOT_SUBMITTED).stream()
        .filter(c -> !c.getFlags().isNrns() || !reminded(c))
        .filter(c -> c.daysToExpiry(today) <= days && c.daysToExpiry(today) >= 0)
        .filter(c -> !holdCovers.effectiveExpiry(c).isAfter(c.getExpiryDate()))
        .map(RenewalCandidate::getId)
        .toList();
  }

  /**
   * Flags a renewal NRNS and sends its reminder once.
   *
   * @param id renewal
   * @return true when a reminder was sent
   */
  public boolean remind(Long id) {
    RenewalCandidate c = candidates.findById(id).orElseThrow();
    c.getFlags().setNrns(true);
    if (reminded(c)) {
      return false;
    }
    RenewalLetter letter =
        writer.generate(
            c,
            new LetterBatch.Kind(LetterType.NRNS_REMINDER, null),
            LetterSource.SYSTEM,
            null,
            null);
    return writer.send(c, letter).isEmpty();
  }

  /**
   * Ids of the renewals past expiry plus the non-acceptance days.
   *
   * @param today business date
   * @return ids
   */
  @Transactional(readOnly = true)
  public List<Long> expired(LocalDate today) {
    int grace = parameters.nonAcceptanceDays();
    return candidates.findByStageIn(EXPIRABLE).stream()
        .filter(c -> closingPoint(c, grace).isBefore(today))
        .map(RenewalCandidate::getId)
        .toList();
  }

  /**
   * The day after which an unrenewed renewal closes (R37-HC-01 to 06): its effective expiry date
   * (the end of a confirmed hold cover, otherwise the policy expiry) plus the non-acceptance days
   * and the NRNS waiting days of its segment. Before it no NRNS tag and no closing letter.
   */
  private LocalDate closingPoint(RenewalCandidate c, int grace) {
    String segment = c.getSnapshot().product() == null ? null : c.getSnapshot().product().segment();
    return holdCovers
        .effectiveExpiry(c)
        .plusDays(grace)
        .plusDays(parameters.nrnsWaitingDays(segment));
  }

  /**
   * Closes an expired renewal as EXPIRED_UNRENEWED with the non-acceptance letter, tags it NRNS and
   * routes it to its closing letter (R37-HC-06 to 10): the No Advice Letter of Operations when a
   * Renewal Advice was sent, otherwise the Non-Renewal Letter of the Marketing AO.
   *
   * @param id renewal
   */
  public void expire(Long id) {
    RenewalCandidate c = candidates.findById(id).orElseThrow();
    if (!EXPIRABLE.contains(c.getStage())) {
      return;
    }
    holdCovers.refresh(c);
    c.getFlags().setNrns(true);
    String route = raSent(c) ? CandidateExpiry.NAL : CandidateExpiry.NRL;
    c.getExpiry().route(route);
    RenewalLetter letter =
        writer.generate(
            c,
            new LetterBatch.Kind(LetterType.NON_ACCEPTANCE, null),
            LetterSource.SYSTEM,
            null,
            null);
    writer.send(c, letter);
    flow.system(c, "expire", "Expired without renewal");
    c.close(ClosedAs.EXPIRED_UNRENEWED, null, clock.instant());
    List<String> owners = new ArrayList<>();
    owners.add(c.getAssignedAo());
    owners.add(c.getAssignedPo());
    notices.users(
        owners,
        RenewalCodes.EVENT_RENEWED,
        c,
        new RenewalNotices.Text(
            c.getRenewalRef() + " closed: expired without renewal",
            "The policy of "
                + c.getSnapshot().clientName()
                + " expired on "
                + DisplayFormat.date(c.effectiveExpiry())
                + (CandidateExpiry.NAL.equals(route)
                    ? "; Operations sends the No Advice Letter"
                    : "; the Account Officer sends the Non-Renewal Letter")));
  }

  private boolean raSent(RenewalCandidate c) {
    return letters.findByCandidateIdOrderByIdDesc(c.getId()).stream()
        .anyMatch(
            l ->
                l.getType() == LetterType.RA
                    && l.getSentAt() != null
                    && l.getStatus() != LetterStatus.CANCELLED);
  }

  /**
   * Updates the delivery status of the queued letters from their e-mails.
   *
   * @return number of letters failed
   */
  public int refreshDeliveries() {
    int failed = 0;
    for (RenewalLetter letter : letters.findByStatus(LetterStatus.QUEUED)) {
      OutboundMessage m =
          letter.getMessageId() == null
              ? null
              : messages.findById(letter.getMessageId()).orElse(null);
      if (m == null || m.getStatus() == MessageStatus.QUEUED) {
        continue;
      }
      boolean sent = m.getStatus() == MessageStatus.SENT;
      letter.delivered(sent, sent ? null : m.getLastError(), m.getSentAt());
      if (!sent) {
        failed++;
        alerts.raise(
            RenewalCodes.ALERT_LETTER_FAILED,
            new AlertFacts(
                letter.getCompanyId(),
                null,
                RenewalCodes.ENTITY,
                letter.getLetterNo(),
                "Letter " + letter.getLetterNo() + " was not delivered: " + m.getLastError(),
                null,
                RenewalCodes.ALERT_LETTER_FAILED + ":" + letter.getLetterNo()));
      }
    }
    return failed;
  }

  private boolean reminded(RenewalCandidate c) {
    return letters.findByCandidateIdOrderByIdDesc(c.getId()).stream()
        .anyMatch(
            l ->
                l.getType() == LetterType.NRNS_REMINDER && l.getStatus() != LetterStatus.CANCELLED);
  }
}
