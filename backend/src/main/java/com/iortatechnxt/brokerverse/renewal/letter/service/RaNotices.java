package com.iortatechnxt.brokerverse.renewal.letter.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.domain.LetterBatch;
import com.iortatechnxt.brokerverse.renewal.domain.LetterType;
import com.iortatechnxt.brokerverse.renewal.domain.RaNotice;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalLetter;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalLetterRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/**
 * When the letters are due (FRRN.022.01, Annex M): the notice of a Renewal Advice chosen by the
 * user or determined by the system (First Notice, then Last Notice the set days before expiry), and
 * the closing letters the set days after the effective expiry date.
 */
@Component
class RaNotices {

  private static final String RENEWAL = "Renewal ";

  private final RenewalLetterRepository letters;
  private final RenewalParameters parameters;
  private final Clock clock;

  RaNotices(RenewalLetterRepository letters, RenewalParameters parameters, Clock clock) {
    this.letters = letters;
    this.parameters = parameters;
    this.clock = clock;
  }

  /**
   * The notice of a Renewal Advice: chosen by the user, or determined by the system (FRRN.022.01,
   * Annex M) - the First Notice for an account ready for its Renewal Advice, the Last Notice for an
   * account whose Renewal Advice was sent.
   */
  LetterBatch.Kind kindOf(RenewalCandidate c, LetterBatch.Kind requested) {
    if (!parameters.raNoticeAutomatic()) {
      return requested;
    }
    return new LetterBatch.Kind(
        LetterType.RA, c.getStage() == RenewalStage.RA_SENT ? RaNotice.SECOND : RaNotice.FIRST);
  }

  /** The Last Notice is due the set days before expiry; a manual second notice after the first. */
  void requireSecondNotice(RenewalCandidate c, LocalDate today) {
    RenewalRecords.requireStage(c, RenewalStage.RA_SENT);
    if (parameters.raNoticeAutomatic()) {
      if (c.daysToExpiry(today) > parameters.raLastNoticeDays()) {
        throw new BusinessRuleException(
            "RNW_RA_SECOND",
            "The Last Notice of "
                + c.getRenewalRef()
                + " is due "
                + parameters.raLastNoticeDays()
                + " days before expiry");
      }
      return;
    }
    Instant firstSent =
        letters.findByCandidateIdOrderByIdDesc(c.getId()).stream()
            .filter(l -> l.getType() == LetterType.RA && l.getNotice() == RaNotice.FIRST)
            .map(RenewalLetter::getSentAt)
            .filter(java.util.Objects::nonNull)
            .findFirst()
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "RNW_RA_SECOND",
                        RENEWAL + c.getRenewalRef() + " has no first notice sent"));
    long since = ChronoUnit.DAYS.between(BusinessClock.dateOf(firstSent), today);
    if (since < parameters.raSecondNoticeDays()) {
      throw new BusinessRuleException(
          "RNW_RA_SECOND",
          "The second notice of "
              + c.getRenewalRef()
              + " is due "
              + parameters.raSecondNoticeDays()
              + " days after the first");
    }
  }

  /**
   * Annex M: the No Advice Letter is due the set days after the effective expiry date, the
   * Non-Renewal Letter the days of its segment (CBG or Non-CBG) after it.
   */
  void requireClosingDue(RenewalCandidate c, LetterType type) {
    String segment = c.getSnapshot().product() == null ? null : c.getSnapshot().product().segment();
    LocalDate due =
        c.effectiveExpiry()
            .plusDays(parameters.closingLetterOffset(type == LetterType.NAL, segment));
    if (BusinessClock.today(clock).isBefore(due)) {
      throw new BusinessRuleException(
          "RNW_CLOSING_NOT_DUE",
          LetterWriter.label(new LetterBatch.Kind(type, null))
              + " of "
              + c.getRenewalRef()
              + " is due on "
              + com.iortatechnxt.brokerverse.common.util.DisplayFormat.date(due));
    }
  }
}
