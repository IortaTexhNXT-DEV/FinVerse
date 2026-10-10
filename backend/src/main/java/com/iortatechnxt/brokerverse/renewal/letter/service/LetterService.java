package com.iortatechnxt.brokerverse.renewal.letter.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.security.CurrentUser;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.check.service.BlockingChecks;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateExpiry;
import com.iortatechnxt.brokerverse.renewal.domain.ClosedAs;
import com.iortatechnxt.brokerverse.renewal.domain.LetterBatch;
import com.iortatechnxt.brokerverse.renewal.domain.LetterBatchRepository;
import com.iortatechnxt.brokerverse.renewal.domain.LetterSource;
import com.iortatechnxt.brokerverse.renewal.domain.LetterStatus;
import com.iortatechnxt.brokerverse.renewal.domain.LetterType;
import com.iortatechnxt.brokerverse.renewal.domain.RaNotice;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalLetter;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalLetterRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.service.BatchOutcome;
import com.iortatechnxt.brokerverse.renewal.service.RenewalBatch;
import com.iortatechnxt.brokerverse.renewal.service.RenewalCodes;
import com.iortatechnxt.brokerverse.renewal.service.RenewalFlow;
import com.iortatechnxt.brokerverse.renewal.service.RenewalParameters;
import com.iortatechnxt.brokerverse.renewal.service.RenewalRecords;
import com.iortatechnxt.brokerverse.workflow.service.TransitionNote;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Renewal letters (FR-RN-080-082, 024): the Renewal Advice generated as first or second notice
 * (locking the Marketing side; a late RA needs the user's confirmation), sent in batch protected,
 * and the No Advice Letter or Not for Renewal letter that closes a renewal of the letter step.
 */
@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class LetterService {

  private static final String RENEWAL = "Renewal ";

  private final RenewalRecords records;
  private final RenewalLetterRepository letters;
  private final LetterBatchRepository batches;
  private final LetterWriter writer;
  private final BlockingChecks blocking;
  private final RenewalFlow flow;
  private final RenewalParameters parameters;
  private final RaNotices notices;
  private final RenewalBatch batch;
  private final DocumentNumberService numbers;
  private final CurrentUser currentUser;
  private final Clock clock;
  private final org.springframework.transaction.support.TransactionTemplate tx;

  /**
   * Creates the service.
   *
   * @param records renewals
   * @param letters letters
   * @param batches letter batches
   * @param writer letter writer
   * @param blocking blocking checks
   * @param flow workflow
   * @param parameters parameters
   * @param notices notice and due-date rules of the letters
   * @param batch batch runner
   * @param numbers document numbers
   * @param currentUser current user
   * @param clock clock
   * @param txManager transactions
   */
  @SuppressWarnings({"java:S107", "PMD.ExcessiveParameterList"}) // constructor injection
  public LetterService(
      RenewalRecords records,
      RenewalLetterRepository letters,
      LetterBatchRepository batches,
      LetterWriter writer,
      BlockingChecks blocking,
      RenewalFlow flow,
      RenewalParameters parameters,
      RaNotices notices,
      RenewalBatch batch,
      DocumentNumberService numbers,
      CurrentUser currentUser,
      Clock clock,
      org.springframework.transaction.PlatformTransactionManager txManager) {
    this.records = records;
    this.letters = letters;
    this.batches = batches;
    this.writer = writer;
    this.blocking = blocking;
    this.flow = flow;
    this.parameters = parameters;
    this.notices = notices;
    this.batch = batch;
    this.numbers = numbers;
    this.currentUser = currentUser;
    this.clock = clock;
    this.tx = new org.springframework.transaction.support.TransactionTemplate(txManager);
  }

  /**
   * Generates Renewal Advices.
   *
   * @param companyId company
   * @param refs renewals
   * @param notice FIRST (RA Ready) or SECOND (RA Sent, no acceptance)
   * @param confirmLate the user confirms RAs generated less than the minimum notice before expiry
   * @return generated and refused renewals
   */
  public BatchOutcome generateRa(
      Long companyId, List<String> refs, RaNotice notice, boolean confirmLate) {
    RaNotice n = notice == RaNotice.SECOND ? RaNotice.SECOND : RaNotice.FIRST;
    LetterBatch.Kind kind = new LetterBatch.Kind(LetterType.RA, n);
    LetterBatch run = start(companyId, LetterBatch.Action.GENERATE, kind, refs.size());
    BatchOutcome outcome =
        batch.run(refs, ref -> generateOne(records.get(companyId, ref), kind, confirmLate, run));
    finish(run, outcome);
    return outcome;
  }

  private void generateOne(
      RenewalCandidate c, LetterBatch.Kind requested, boolean confirmLate, LetterBatch run) {
    LocalDate today = BusinessClock.today(clock);
    long days = c.daysToExpiry(today);
    LetterBatch.Kind kind = notices.kindOf(c, requested);
    if (kind.notice() == RaNotice.SECOND) {
      notices.requireSecondNotice(c, today);
    } else {
      RenewalRecords.requireStage(c, RenewalStage.RA_READY);
      blocking.require(c, "have its Renewal Advice generated");
    }
    boolean late = days < parameters.raMinNoticeDays();
    if (late && !confirmLate) {
      throw new BusinessRuleException(
          "RNW_RA_LATE",
          RENEWAL
              + c.getRenewalRef()
              + " expires in "
              + days
              + " day(s): confirm the late Renewal Advice");
    }
    writer.generate(c, kind, LetterSource.USER, run.getId(), late ? currentUser.username() : null);
    c.noticed(kind.notice());
    if (kind.notice() == RaNotice.FIRST) {
      c.lockMarketing(clock.instant());
      flow.act(c, "generate_ra", TransitionNote.comment("Renewal Advice generated"));
    }
  }

  /**
   * Sends the generated Renewal Advices of renewals (first notice: the renewal becomes RA Sent /
   * Awaiting Response).
   *
   * @param companyId company
   * @param refs renewals
   * @return sent and refused renewals
   */
  public BatchOutcome sendRa(Long companyId, List<String> refs) {
    LetterBatch run =
        start(
            companyId,
            LetterBatch.Action.SEND,
            new LetterBatch.Kind(LetterType.RA, null),
            refs.size());
    BatchOutcome outcome =
        batch.runReporting(
            refs,
            ref -> {
              RenewalCandidate c = records.get(companyId, ref);
              RenewalLetter letter = pending(c, LetterType.RA);
              Optional<String> refusal = writer.send(c, letter);
              if (refusal.isEmpty()
                  && letter.getNotice() == RaNotice.FIRST
                  && c.getStage() == RenewalStage.RA_GENERATED) {
                flow.act(c, "send_ra", TransitionNote.comment("Sent to the client"));
              }
              return refusal.orElse(null);
            });
    finish(run, outcome);
    return outcome;
  }

  /**
   * Generates and sends the letter that closes a renewal of the letter step: the No Advice Letter
   * for a NAL-eligible reason, else the Not for Renewal letter.
   *
   * @param companyId company
   * @param refs renewals
   * @return sent and refused renewals
   */
  public BatchOutcome closingLetters(Long companyId, List<String> refs) {
    return batch.runReporting(
        refs,
        ref -> {
          RenewalCandidate c = records.get(companyId, ref);
          RenewalRecords.requireStage(c, RenewalStage.LETTER_PENDING);
          LetterType type = closingType(c);
          requireNoOtherClosingLetter(c, type);
          RenewalLetter letter =
              pendingOptional(c, type)
                  .orElseGet(
                      () ->
                          writer.generate(
                              c, new LetterBatch.Kind(type, null), LetterSource.USER, null, null));
          Optional<String> refusal = writer.send(c, letter);
          if (refusal.isPresent()) {
            return refusal.get();
          }
          if (type == LetterType.NFR) {
            c.getFlags().setNfrSent(true);
          }
          c.getExpiry().letterSent(type.name());
          flow.act(
              c,
              "send_letter",
              TransitionNote.comment(
                  LetterWriter.label(new LetterBatch.Kind(type, null)) + " sent"));
          c.close(ClosedAs.NOT_RENEWED, null, clock.instant());
          return null;
        });
  }

  /**
   * Generates and sends the closing letter of renewals that reached their effective expiry date
   * unrenewed (Walkthrough addendum R37-HC-07 to 12; FR-RN-082): the No Advice Letter of the
   * renewals routed to Operations (a Renewal Advice was sent), the Non-Renewal Letter (the Not for
   * Renewal Letter) of those routed to the Marketing AO. A renewal never receives both.
   *
   * @param companyId company
   * @param refs renewals
   * @return sent and refused renewals
   */
  public BatchOutcome closingLettersAtExpiry(Long companyId, List<String> refs) {
    return batch.runReporting(
        refs,
        ref -> {
          RenewalCandidate c = records.get(companyId, ref);
          LetterType type = closingTypeAtExpiry(c);
          requireNoOtherClosingLetter(c, type);
          notices.requireClosingDue(c, type);
          RenewalLetter letter =
              pendingOptional(c, type)
                  .orElseGet(
                      () ->
                          writer.generate(
                              c, new LetterBatch.Kind(type, null), LetterSource.USER, null, null));
          Optional<String> refusal = writer.send(c, letter);
          if (refusal.isEmpty()) {
            closingSent(c, type);
          }
          return refusal.orElse(null);
        });
  }

  /**
   * The closing letter a renewal unrenewed at its effective expiry is routed to, for its sender.
   */
  private LetterType closingTypeAtExpiry(RenewalCandidate c) {
    String route = c.getExpiry().getClosingRoute();
    if (route == null) {
      throw new BusinessRuleException(
          "RNW_CLOSING_NOT_DUE", RENEWAL + c.getRenewalRef() + " is not due a closing letter");
    }
    LetterType type = CandidateExpiry.NAL.equals(route) ? LetterType.NAL : LetterType.NFR;
    String permission = type == LetterType.NAL ? RenewalCodes.RA_SEND : RenewalCodes.DISPOSE;
    if (!currentUser.hasAuthority(permission)) {
      throw new BusinessRuleException(
          "RNW_CLOSING_NOT_YOURS",
          type == LetterType.NAL
              ? "The No Advice Letter of " + c.getRenewalRef() + " is sent by Operations"
              : "The Non-Renewal Letter of "
                  + c.getRenewalRef()
                  + " is sent by the Account Officer");
    }
    return type;
  }

  private static void closingSent(RenewalCandidate c, LetterType type) {
    if (type == LetterType.NFR) {
      c.getFlags().setNfrSent(true);
    }
    c.getExpiry().letterSent(type.name());
  }

  /**
   * A No Advice Letter and a Non-Renewal (Not for Renewal) Letter exclude each other (R37-HC-09,
   * 10).
   */
  private static void requireNoOtherClosingLetter(RenewalCandidate c, LetterType type) {
    String sent = c.getExpiry().getClosingLetter();
    if (sent != null) {
      throw new BusinessRuleException(
          "RNW_CLOSING_LETTER_SENT",
          RENEWAL
              + c.getRenewalRef()
              + " already has a "
              + LetterWriter.label(new LetterBatch.Kind(LetterType.valueOf(sent), null))
              + " and cannot receive a "
              + LetterWriter.label(new LetterBatch.Kind(type, null)));
    }
  }

  /**
   * The letter type that closes a renewal of the letter step.
   *
   * @param c renewal
   * @return NAL or NFR
   */
  public LetterType closingType(RenewalCandidate c) {
    boolean hasRa =
        letters.findByCandidateIdOrderByIdDesc(c.getId()).stream()
            .anyMatch(l -> l.getType() == LetterType.RA && l.getStatus() != LetterStatus.CANCELLED);
    String reason = c.getDisposition().reasonCode();
    return !hasRa && reason != null && parameters.nalReason(reason)
        ? LetterType.NAL
        : LetterType.NFR;
  }

  /**
   * The letters of a renewal, newest first.
   *
   * @param companyId company
   * @param renewalRef renewal
   * @return letters
   */
  @Transactional(readOnly = true)
  public List<RenewalLetter> of(Long companyId, String renewalRef) {
    return letters.findByCandidateIdOrderByIdDesc(records.get(companyId, renewalRef).getId());
  }

  /**
   * A letter.
   *
   * @param companyId company
   * @param letterNo letter number
   * @return letter
   */
  @Transactional(readOnly = true)
  public RenewalLetter letter(Long companyId, String letterNo) {
    RenewalLetter letter =
        letters
            .findByCompanyIdAndLetterNo(companyId, letterNo)
            .orElseThrow(() -> new ResourceNotFoundException("Letter", letterNo));
    records.requireScope(records.byId(letter.getCandidateId()));
    return letter;
  }

  private RenewalLetter pending(RenewalCandidate c, LetterType type) {
    return pendingOptional(c, type)
        .orElseThrow(
            () ->
                new BusinessRuleException(
                    "RNW_LETTER_NONE", RENEWAL + c.getRenewalRef() + " has no letter to send"));
  }

  private Optional<RenewalLetter> pendingOptional(RenewalCandidate c, LetterType type) {
    return letters.findByCandidateIdOrderByIdDesc(c.getId()).stream()
        .filter(l -> l.getType() == type)
        .filter(
            l -> l.getStatus() == LetterStatus.GENERATED || l.getStatus() == LetterStatus.FAILED)
        .findFirst();
  }

  private LetterBatch start(
      Long companyId, LetterBatch.Action action, LetterBatch.Kind kind, int n) {
    return tx.execute(
        s ->
            batches.save(
                new LetterBatch(
                    companyId,
                    numbers.next("RLB-" + BusinessClock.today(clock).getYear()),
                    action,
                    kind,
                    n,
                    clock.instant())));
  }

  private void finish(LetterBatch run, BatchOutcome outcome) {
    tx.executeWithoutResult(
        s ->
            batches
                .findById(run.getId())
                .ifPresent(
                    b ->
                        b.complete(
                            outcome.done().size(),
                            outcome.refused().size(),
                            String.join("; ", outcome.refused().values()),
                            clock.instant())));
  }
}
