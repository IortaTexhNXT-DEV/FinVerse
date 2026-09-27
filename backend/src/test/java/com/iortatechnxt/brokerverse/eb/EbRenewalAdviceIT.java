package com.iortatechnxt.brokerverse.eb;

import static com.iortatechnxt.brokerverse.eb.EbFixtures.AO;
import static com.iortatechnxt.brokerverse.eb.EbFixtures.hmo;
import static com.iortatechnxt.brokerverse.eb.EbFixtures.pdf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.eb.domain.EbCycle;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbCycleStage;
import com.iortatechnxt.brokerverse.eb.domain.EbDocumentTypes;
import com.iortatechnxt.brokerverse.eb.domain.EbFeedbackChannel;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbRenewalAdvice;
import com.iortatechnxt.brokerverse.eb.domain.EbRenewalAdviceRepository;
import com.iortatechnxt.brokerverse.eb.renewal.service.FeedbackService;
import com.iortatechnxt.brokerverse.eb.renewal.service.FeedbackService.FeedbackInput;
import com.iortatechnxt.brokerverse.eb.renewal.service.RenewalAdviceBatch;
import com.iortatechnxt.brokerverse.eb.renewal.service.RenewalAdviceJob;
import com.iortatechnxt.brokerverse.eb.renewal.service.RenewalAdviceService;
import com.iortatechnxt.brokerverse.eb.service.EbParameters;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The renewal advice and the client feedback (FR-EB-022, FR-EB-023; job EB_RENEWAL_ADVICE; waves
 * E1-B and E1-C): the job opens the renewal cycle and sends the protected advice, stored as a
 * RENEWAL_ADVICE document of the programme and the client; programmes that cannot receive it raise
 * EB_RA_NOT_SENT; reminders follow until feedback stops them; the AO sends it manually.
 */
@IntegrationTest
class EbRenewalAdviceIT {

  @Autowired private EbFixtures fx;
  @Autowired private RenewalAdviceJob job;
  @Autowired private RenewalAdviceService service;
  @Autowired private RenewalAdviceBatch batch;
  @Autowired private FeedbackService feedback;
  @Autowired private EbParameters parameters;
  @Autowired private EbCycleRepository cycles;
  @Autowired private EbRenewalAdviceRepository advices;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  @Test
  void theJobSendsTheAdviceInsideTheLeadTimeAndOnlyOnce() {
    LocalDate today = LocalDate.of(2031, 3, 2);
    LocalDate expiry = today.plusDays(parameters.raLeadDays() - 5);
    String arn = accountArn();
    EbProgramme p = fx.programme(true, List.of(hmo(expiry, arn)));
    EbProgramme later = fx.programme(true, List.of(hmo(expiry.plusDays(30), null)));

    JobOutcome outcome = job.execute(today);
    assertThat(outcome.message()).contains("renewal advice(s) sent");

    EbCycle cycle = cycles.findOpen(p.getId(), expiry.getYear()).orElseThrow();
    assertThat(cycle.getStage()).isEqualTo(EbCycleStage.RA_SENT);
    assertThat(cycle.getTargetInception()).isEqualTo(expiry);
    EbRenewalAdvice advice = advices.findByCycleId(cycle.getId()).orElseThrow();
    assertThat(advice.isManual()).isFalse();
    assertThat(advice.getExpiryDate()).isEqualTo(expiry);
    assertThat(advice.getRecipients()).hasSize(1);
    assertThat(cycles.findOpen(later.getId(), expiry.getYear())).isEmpty();

    assertThat(
            jdbc.queryForObject(
                "select count(*) from msg_outbound where purpose = 'EB_RENEWAL_ADVICE'"
                    + " and entity_type = 'EbProgramme' and entity_id = ? and password_for_id is null",
                Long.class,
                p.getId().toString()))
        .isEqualTo(1L);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from msg_outbound m where m.password_for_id = ?",
                Long.class,
                advice.getMessageId()))
        .isEqualTo(1L);
    assertThat(
            jdbc.queryForObject(
                "select document_type from doc_attachment where id = ?",
                String.class,
                advice.getAttachmentId()))
        .isEqualTo(EbDocumentTypes.RENEWAL_ADVICE);
    List<String> linked =
        jdbc.queryForList(
            "select entity_type from doc_attachment_link where attachment_id = ?",
            String.class,
            advice.getAttachmentId());
    assertThat(linked).contains("EbProgramme", "Client", "Account");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from eb_activity_log where cycle_id = ? and activity_code = 'RENEWAL_ADVICE'",
                Long.class,
                cycle.getId()))
        .isEqualTo(1L);

    job.execute(today);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from eb_renewal_advice where programme_id = ?",
                Long.class,
                p.getId()))
        .isEqualTo(1L);
  }

  private String accountArn() {
    return jdbc.queryForObject("select arn from acc_account order by id limit 1", String.class);
  }

  @Test
  void programmesThatCannotReceiveTheAdviceRaiseAnAlert() {
    LocalDate today = LocalDate.of(2032, 5, 3);
    LocalDate expiry = today.plusDays(30);
    EbProgramme notFlagged = fx.programme(false, List.of(hmo(expiry, "HMO-OLD")));
    EbProgramme noContact =
        fx.programme(
            true,
            List.of(hmo(expiry, null)),
            List.of(
                new com.iortatechnxt.brokerverse.eb.domain.EbProgrammeContact.Data(
                    "Finance",
                    "fin@client.example",
                    null,
                    com.iortatechnxt.brokerverse.eb.domain.EbContactRole.FINANCE,
                    false,
                    true)));
    job.execute(today);
    assertThat(cycles.findOpen(notFlagged.getId(), expiry.getYear())).isEmpty();
    assertThat(cycles.findOpen(noContact.getId(), expiry.getYear())).isEmpty();
    assertThat(alerts(notFlagged)).isEqualTo(1L);
    assertThat(alerts(noContact)).isEqualTo(1L);
    job.execute(today);
    assertThat(alerts(notFlagged)).isEqualTo(1L);
  }

  private Long alerts(EbProgramme p) {
    return jdbc.queryForObject(
        "select count(*) from alt_alert where exception_code = 'EB_RA_NOT_SENT' and entity_id = ?",
        Long.class,
        p.getId().toString());
  }

  @Test
  void remindersFollowUntilTheFeedbackMovesTheCycleOn() {
    LocalDate expiry = LocalDate.of(2033, 9, 1);
    EbProgramme p = fx.programme(true, List.of(hmo(expiry, null)));
    LocalDate sentOn = expiry.minusDays(130);
    job.execute(sentOn);
    EbCycle cycle = cycles.findOpen(p.getId(), 2033).orElseThrow();
    EbRenewalAdvice advice = advices.findByCycleId(cycle.getId()).orElseThrow();
    List<Integer> days = parameters.raReminderDays();

    assertThat(service.remindIfDue(advice.getId(), expiry.minusDays(125), days)).isFalse();
    assertThat(service.remindIfDue(advice.getId(), expiry.minusDays(120), days)).isTrue();
    assertThat(service.remindIfDue(advice.getId(), expiry.minusDays(119), days)).isFalse();
    assertThat(service.remindIfDue(advice.getId(), expiry.minusDays(100), days)).isTrue();
    assertThat(advices.findById(advice.getId()).orElseThrow().getRemindersSent()).isEqualTo(2);

    assertThatThrownBy(
            () ->
                as.run(
                    AO,
                    () ->
                        feedback.record(
                            fx.company(),
                            cycle.getId(),
                            new FeedbackInput(
                                EbFeedbackChannel.EMAIL, LocalDate.now(), " ", List.of()))))
        .extracting("code")
        .isEqualTo("EB_FEEDBACK_EMPTY");
    assertThatThrownBy(
            () ->
                as.run(
                    AO,
                    () ->
                        feedback.record(
                            fx.company(),
                            cycle.getId(),
                            new FeedbackInput(
                                EbFeedbackChannel.EMAIL,
                                LocalDate.now().plusDays(5),
                                "x",
                                List.of()))))
        .extracting("code")
        .isEqualTo("EB_FEEDBACK_DATE_FUTURE");
    as.run(
        "ebao2",
        () ->
            feedback.record(
                fx.company(),
                cycle.getId(),
                new FeedbackInput(
                    EbFeedbackChannel.EMAIL,
                    LocalDate.now().minusDays(1),
                    "Renew with the same benefits",
                    List.of(pdf("feedback.pdf")))));
    assertThat(cycles.findById(cycle.getId()).orElseThrow().getStage())
        .isEqualTo(EbCycleStage.REQUIREMENTS);
    assertThat(advices.findById(advice.getId()).orElseThrow().awaitingFeedback()).isFalse();
    assertThat(service.remindIfDue(advice.getId(), expiry.minusDays(90), days)).isFalse();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from eb_document where cycle_id = ? and document_type = 'EB_CLIENT_FEEDBACK'"
                    + " and source = 'CLIENT'",
                Long.class,
                cycle.getId()))
        .isEqualTo(1L);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from msg_notification where recipient = ? and entity_id = ?",
                Long.class,
                AO,
                cycle.getId().toString()))
        .isEqualTo(1L);
    assertThat(feedback.ofProgramme(fx.company(), p.getId())).hasSize(1);
  }

  @Test
  void theAoSendsTheAdviceManuallyAndIneligibleProgrammesAreRefused() {
    LocalDate expiry = LocalDate.now().plusDays(200);
    EbProgramme p = fx.programme(true, List.of(hmo(expiry, null)));
    EbProgramme notFlagged = fx.programme(false, List.of(hmo(expiry, null)));
    EbProgramme noExpiry = fx.programme(true, List.of(hmo(null, null)));
    List<RenewalAdviceBatch.Result> results =
        as.run(
            AO,
            () ->
                batch.send(
                    fx.company(), List.of(p.getId(), notFlagged.getId(), noExpiry.getId(), -1L)));
    assertThat(results).hasSize(4);
    assertThat(results.get(0).sent()).isTrue();
    assertThat(results.get(0).cycleNo()).startsWith("EBC-");
    assertThat(results.get(1).sent()).isFalse();
    assertThat(results.get(1).message()).contains("is not flagged for renewal");
    assertThat(results.get(2).message()).contains("no benefit line ending");
    assertThat(results.get(3).message()).isEqualTo("Programme not found");
    EbRenewalAdvice advice = advices.findByProgrammeIdOrderBySentAtDesc(p.getId()).get(0);
    assertThat(advice.isManual()).isTrue();
    assertThat(advice.getSentBy()).isEqualTo(AO);

    assertThatThrownBy(
            () -> as.run(AO, () -> service.sendManual(fx.company(), p.getId(), LocalDate.now())))
        .extracting("code")
        .isEqualTo("EB_RA_ALREADY_SENT");
    assertThatThrownBy(() -> as.run(AO, () -> batch.send(fx.company(), List.of())))
        .extracting("code")
        .isEqualTo("EB_RA_SELECTION");
  }
}
