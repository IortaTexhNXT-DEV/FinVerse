package com.iortatechnxt.brokerverse.renewal;

import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.ADMIN;
import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.AO;
import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.PO;
import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.PROC_TL;
import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.TL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.account.domain.AccountRepository;
import com.iortatechnxt.brokerverse.account.domain.AccountStatus;
import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import com.iortatechnxt.brokerverse.account.service.AccountLifecycleService;
import com.iortatechnxt.brokerverse.account.service.AccountService;
import com.iortatechnxt.brokerverse.attachment.domain.AttachmentTarget;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadOptions;
import com.iortatechnxt.brokerverse.attachment.service.DocumentService.UploadedFile;
import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.booking.domain.BookingSource;
import com.iortatechnxt.brokerverse.booking.service.BookingOptions;
import com.iortatechnxt.brokerverse.booking.service.BookingService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.renewal.acceptance.service.AcceptanceService;
import com.iortatechnxt.brokerverse.renewal.candidate.service.AccountHistoryService;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateQueryService;
import com.iortatechnxt.brokerverse.renewal.domain.AcceptanceMethod;
import com.iortatechnxt.brokerverse.renewal.domain.CheckOutcome;
import com.iortatechnxt.brokerverse.renewal.domain.ClosedAs;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerBatch;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerResponse;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerResponseCode;
import com.iortatechnxt.brokerverse.renewal.domain.LetterStatus;
import com.iortatechnxt.brokerverse.renewal.domain.OverrideKind;
import com.iortatechnxt.brokerverse.renewal.domain.RaNotice;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalLetter;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.insurer.service.InsurerBatchService;
import com.iortatechnxt.brokerverse.renewal.insurer.service.RenewalInsurerResponseService;
import com.iortatechnxt.brokerverse.renewal.letter.service.LetterService;
import com.iortatechnxt.brokerverse.renewal.letter.service.LetterSweeps;
import com.iortatechnxt.brokerverse.renewal.marketing.service.OverrideService;
import com.iortatechnxt.brokerverse.renewal.marketing.service.RenewalAssignmentService;
import com.iortatechnxt.brokerverse.renewal.marketing.service.RenewalDispositionService;
import com.iortatechnxt.brokerverse.renewal.marketing.service.ReviewService;
import com.iortatechnxt.brokerverse.renewal.processing.service.ProcessingService;
import com.iortatechnxt.brokerverse.renewal.service.BatchOutcome;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Renewal wave R1-D: the Renewal Advice generated (locking Marketing) and sent protected, the
 * acceptance with its evidence, the fast track, booking of the renewal account closing the renewal
 * RENEWED, the closing letter of a Not for Renewal, and the expiry sweep.
 */
@IntegrationTest
class RenewalLettersIT {

  @Autowired private RenewalFixtures fx;
  @Autowired private RenewalAssignmentService assignments;
  @Autowired private AccountHistoryService history;
  @Autowired private RenewalDispositionService dispositions;
  @Autowired private ReviewService review;
  @Autowired private OverrideService overrides;
  @Autowired private ProcessingService processing;
  @Autowired private InsurerBatchService batches;
  @Autowired private RenewalInsurerResponseService responses;
  @Autowired private LetterService letters;

  @Autowired
  private com.iortatechnxt.brokerverse.renewal.letter.service.LetterActions letterActions;

  @Autowired private LetterSweeps sweeps;
  @Autowired private AcceptanceService acceptances;
  @Autowired private CandidateQueryService queries;
  @Autowired private AccountRepository accounts;
  @Autowired private AccountService accountService;
  @Autowired private AccountLifecycleService lifecycle;
  @Autowired private BookingService bookings;
  @Autowired private DocumentService documents;
  @Autowired private TransactionTemplate tx;
  @Autowired private AsUser as;
  @Autowired private com.iortatechnxt.brokerverse.renewal.channel.service.ChannelService channels;

  @Autowired
  private com.iortatechnxt.brokerverse.renewal.channel.domain.ChannelMessageRepository
      channelMessages;

  @Autowired private com.iortatechnxt.brokerverse.system.service.SystemParameterService parameters;
  @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;

  private RenewalCandidate raReady(PaymentArrangement arrangement) {
    RenewalCandidate c = fx.extracted(fx.book("MTR10", "RETAIL", arrangement));
    List<String> ref = List.of(c.getRenewalRef());
    as.run(TL, () -> fx.initiate(c));
    as.run(TL, () -> assignments.assign(fx.company(), ref, AO, null));
    as.run(AO, () -> history.open(fx.company(), c.getRenewalRef()));
    as.run(
        AO,
        () ->
            dispositions.save(
                fx.company(),
                c.getRenewalRef(),
                new RenewalDispositionService.Input(
                    RenewalDisposition.FOR_RENEWAL, null, null, null, null)));
    as.run(AO, () -> dispositions.push(fx.company(), ref));
    override(ref);
    assertThat(as.run(TL, () -> review.post(fx.company(), ref)).refused()).isEmpty();
    as.run(PROC_TL, () -> processing.assign(fx.company(), ref, PO));
    RenewalCandidate processed = fx.reload(c);
    String insurer = processed.getSnapshot().insurerCode();
    InsurerBatch batch =
        as.run(
            PO, () -> batches.create(fx.company(), insurer, c.getExpiryDate(), c.getExpiryDate()));
    as.run(PO, () -> batches.send(fx.company(), batch.getBatchNo()));
    as.run(
        PO,
        () ->
            responses.manual(
                fx.company(),
                c.getRenewalRef(),
                new InsurerResponse.Content(
                    InsurerResponseCode.RENEW_AS_IS, null, null, null, null, null, null, null),
                false));
    RenewalCandidate ready = fx.reload(c);
    assertThat(ready.getStage()).isEqualTo(RenewalStage.RA_READY);
    return ready;
  }

  private void override(List<String> ref) {
    as.run(
        TL,
        () ->
            overrides.override(
                fx.company(),
                ref,
                new OverrideService.Request(
                    OverrideKind.OUTSTANDING_BALANCE, null, "OTHERS", "Paid at renewal")));
  }

  private List<String> failing(RenewalCandidate c) {
    return as.run(PO, () -> queries.latestResults(fx.reload(c))).stream()
        .filter(r -> r.getOutcome() == CheckOutcome.FAIL)
        .map(r -> r.getCheckCode() + ": " + r.getMessage())
        .toList();
  }

  @Test
  void theRenewalAdviceIsGeneratedSentAcceptedAndTheRenewalAccountBookedCloseTheRenewal() {
    RenewalCandidate c = raReady(PaymentArrangement.DIRECT_TO_INSURER);
    List<String> ref = List.of(c.getRenewalRef());
    BatchOutcome generated =
        as.run(PO, () -> letters.generateRa(fx.company(), ref, RaNotice.FIRST, true));
    assertThat(generated.refused()).as(failing(c).toString()).isEmpty();
    RenewalCandidate locked = fx.reload(c);
    assertThat(locked.getStage()).isEqualTo(RenewalStage.RA_GENERATED);
    assertThat(locked.getMarketingLockedAt()).isNotNull();

    BatchOutcome sent = as.run(PO, () -> letters.sendRa(fx.company(), ref));
    assertThat(sent.refused()).isEmpty();
    assertThat(fx.reload(c).getStage()).isEqualTo(RenewalStage.RA_SENT);
    List<RenewalLetter> list = as.run(PO, () -> letters.of(fx.company(), c.getRenewalRef()));
    assertThat(list).extracting(RenewalLetter::getStatus).contains(LetterStatus.QUEUED);

    assertThatThrownBy(
            () ->
                as.run(
                    AO,
                    () ->
                        acceptances.accept(
                            fx.company(),
                            c.getRenewalRef(),
                            new AcceptanceService.Input(
                                AcceptanceMethod.SIGNED_RA, null, null, null, null, false),
                            "USER")))
        .isInstanceOf(BusinessRuleException.class);
    as.run(
        AO,
        () ->
            acceptances.accept(
                fx.company(),
                c.getRenewalRef(),
                new AcceptanceService.Input(
                    AcceptanceMethod.PAYMENT, null, "OR-" + c.getId(), null, null, true),
                "USER"));
    RenewalCandidate accepted = fx.reload(c);
    assertThat(accepted.getStage()).isEqualTo(RenewalStage.FOR_PLACEMENT_BOOKING);

    Account renewal = tx.execute(s -> accounts.findByArn(accepted.getRenewalArn()).orElseThrow());
    assertThat(renewal.getStatus()).isIn(AccountStatus.READY_FOR_PLACEMENT, AccountStatus.PLACED);
    as.run(
        PO,
        () ->
            documents.upload(
                new AttachmentTarget("Account", renewal.getId().toString()),
                List.of(
                    new UploadedFile(
                        "epolicy.pdf", "%PDF-1.4 e".getBytes(StandardCharsets.US_ASCII))),
                new UploadOptions("EPOLICY", false, null, null)));
    as.run(PO, () -> accountService.directBooking(renewal.getId(), null));
    as.run(
        PO,
        () ->
            lifecycle.recordPolicy(
                renewal.getArn(), List.of("POL-R-" + c.getId()), LocalDate.of(2026, 9, 20)));
    as.run(
        PO,
        () ->
            bookings.book(
                renewal.getArn(),
                BookingOptions.of(BookingFixtures.BOOKED_ON, null),
                BookingSource.INDIVIDUAL));
    RenewalCandidate renewed = fx.reload(c);
    assertThat(renewed.getStage()).isEqualTo(RenewalStage.RENEWED);
    assertThat(renewed.getClosedAs()).isEqualTo(ClosedAs.RENEWED);
    assertThat(renewed.getRenewedInvoiceNo()).isNotNull();
  }

  @Test
  void aNotForRenewalIsClosedWithItsLetterAndARenewalPastExpiryIsSwept() {
    RenewalCandidate c = fx.unassignedRetail();
    List<String> ref = List.of(c.getRenewalRef());
    as.run(TL, () -> assignments.assign(fx.company(), ref, AO, null));
    as.run(AO, () -> history.open(fx.company(), c.getRenewalRef()));
    as.run(
        AO,
        () ->
            dispositions.save(
                fx.company(),
                c.getRenewalRef(),
                new RenewalDispositionService.Input(
                    RenewalDisposition.NOT_FOR_RENEWAL, "UNIT_SOLD", null, null, null)));
    as.run(AO, () -> dispositions.push(fx.company(), ref));
    as.run(TL, () -> review.post(fx.company(), ref));
    assertThat(fx.reload(c).getStage()).isEqualTo(RenewalStage.LETTER_PENDING);
    BatchOutcome closed = as.run(PO, () -> letters.closingLetters(fx.company(), ref));
    assertThat(closed.refused()).isEmpty();
    RenewalCandidate after = fx.reload(c);
    assertThat(after.getStage()).isEqualTo(RenewalStage.CLOSED);
    assertThat(after.getClosedAs()).isEqualTo(ClosedAs.NOT_RENEWED);

    RenewalCandidate other = fx.unassignedRetail();
    LocalDate afterExpiry = other.getExpiryDate().plusDays(1);
    assertThat(sweeps.expired(afterExpiry)).contains(other.getId());
    as.run(
        PROC_TL,
        () -> {
          sweeps.expire(other.getId());
          return null;
        });
    RenewalCandidate swept = fx.reload(other);
    assertThat(swept.getStage()).isEqualTo(RenewalStage.CLOSED);
    assertThat(swept.getClosedAs()).isEqualTo(ClosedAs.EXPIRED_UNRENEWED);
  }

  @Test
  void generateAndSendRaHandsTheFirstNoticeToCcmUnderTheClientsFileNameAndItIsDelivered() {
    RenewalCandidate c = raReady(PaymentArrangement.DIRECT_TO_INSURER);
    List<String> ref = List.of(c.getRenewalRef());
    BatchOutcome sent =
        as.run(PO, () -> letterActions.generateAndSendRa(fx.company(), ref, RaNotice.SECOND, true));
    assertThat(sent.refused()).as(failing(c).toString()).isEmpty();
    assertThat(fx.reload(c).getStage()).isEqualTo(RenewalStage.RA_SENT);
    RenewalLetter ra = as.run(PO, () -> letters.of(fx.company(), c.getRenewalRef())).get(0);
    assertThat(ra.getNotice()).isEqualTo(RaNotice.FIRST);
    var message =
        channelMessages
            .findByCompanyIdAndDocRefOrderByIdDesc(fx.company(), ra.getLetterNo())
            .get(0);
    assertThat(message.getExternalRef()).startsWith("CCMSIM-");
    assertThat(message.getFileName())
        .matches("MTR_RA_First Notice_" + c.getRenewalRef() + "_\\d{8}\\.pdf");
    as.run(PO, () -> channels.refresh());
    as.run(PO, () -> sweeps.refreshDeliveries());
    RenewalLetter delivered = as.run(PO, () -> letters.letter(fx.company(), ra.getLetterNo()));
    assertThat(delivered.getStatus()).isEqualTo(LetterStatus.SENT);
    assertThat(as.run(PO, () -> letterActions.resend(fx.company(), ra.getLetterNo()))).isNull();
    assertThat(
            channelMessages.findByCompanyIdAndDocRefOrderByIdDesc(fx.company(), ra.getLetterNo()))
        .hasSize(2);
    assertThat(as.run(PO, () -> letters.letter(fx.company(), ra.getLetterNo())).getAttachmentId())
        .isEqualTo(ra.getAttachmentId());
  }

  @Test
  void byEmailTheRenewalAdviceIsAProtectedEmailWithItsPasswordInASeparateEmail() {
    RenewalCandidate c = raReady(PaymentArrangement.DIRECT_TO_INSURER);
    List<String> ref = List.of(c.getRenewalRef());
    as.run(ADMIN, () -> parameters.update("RNW_DELIVERY_CHANNEL", "EMAIL"));
    try {
      as.run(PO, () -> letterActions.generateAndSendRa(fx.company(), ref, RaNotice.FIRST, true));
    } finally {
      as.run(ADMIN, () -> parameters.update("RNW_DELIVERY_CHANNEL", "CCM"));
    }
    RenewalLetter ra = as.run(PO, () -> letters.of(fx.company(), c.getRenewalRef())).get(0);
    assertThat(ra.getMessageId()).isNotNull();
    Integer mails =
        jdbc.queryForObject(
            "select count(*) from msg_outbound where entity_type = 'RenewalCandidate' and entity_id = ?",
            Integer.class,
            c.getId().toString());
    assertThat(mails).isGreaterThanOrEqualTo(2);
  }
}
