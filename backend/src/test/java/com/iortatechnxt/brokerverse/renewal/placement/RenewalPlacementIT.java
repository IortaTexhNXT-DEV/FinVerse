package com.iortatechnxt.brokerverse.renewal.placement;

import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.PO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.RenewalFixtures;
import com.iortatechnxt.brokerverse.renewal.allocation.service.InsurerAllocationService;
import com.iortatechnxt.brokerverse.renewal.domain.AdviceVersion;
import com.iortatechnxt.brokerverse.renewal.domain.CandidatePlacement;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.mft.service.MftInbox;
import com.iortatechnxt.brokerverse.renewal.placement.service.InsuranceAdvices;
import com.iortatechnxt.brokerverse.renewal.placement.service.PlacementGeneration;
import com.iortatechnxt.brokerverse.renewal.placement.service.PlacementResponseHandler;
import com.iortatechnxt.brokerverse.renewal.placement.service.PlacementSending;
import com.iortatechnxt.brokerverse.renewal.placement.service.PlacementTat;
import com.iortatechnxt.brokerverse.renewal.placement.service.PlacementTracking;
import com.iortatechnxt.brokerverse.renewal.service.BatchOutcome;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Placement of the renewal accounts of BDOI's FRS against the real database (FRRN.029 to FRRN.032):
 * the slips per co-insurer, the sending with the recipients, the turnaround time with the With
 * Issue tag, the insurer's response by upload or MFT, the return to Marketing and the Insurance
 * Advice versions.
 */
@IntegrationTest
class RenewalPlacementIT {

  private static final String MAILBOX = "placement@insurer.example.ph";
  private static final String MFT_COMPANY = "RNW_MFT_COMPANY_CODE";

  @Autowired private RenewalFixtures fx;
  @Autowired private PlacementGeneration generation;
  @Autowired private PlacementSending sending;
  @Autowired private PlacementTracking tracking;
  @Autowired private PlacementResponseHandler responses;
  @Autowired private InsurerAllocationService allocations;
  @Autowired private InsuranceAdvices advices;
  @Autowired private MftInbox inbox;
  @Autowired private SystemParameterService parameters;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private Clock clock;
  @Autowired private AsUser as;

  private RenewalCandidate submitted() {
    RenewalCandidate c = fx.unassignedRetail();
    jdbc.update("update rnw_candidate set stage = 'FOR_PLACEMENT_BOOKING' where id = ?", c.getId());
    jdbc.update(
        "update wf_case set stage_code = 'FOR_PLACEMENT_BOOKING' where entity_type ="
            + " 'RenewalCandidate' and entity_id = ?",
        c.getId().toString());
    return fx.reload(c);
  }

  private String otherInsurer(RenewalCandidate c) {
    return jdbc.queryForObject(
        "select party_code from cat_insurer where company_id = ? and party_code <> ?"
            + " order by party_code limit 1",
        String.class,
        fx.company(),
        c.getSnapshot().insurerCode());
  }

  private PlacementSending.Summary sendAll(RenewalCandidate c, String... insurers) {
    Map<String, List<String>> to =
        java.util.Arrays.stream(insurers)
            .collect(java.util.stream.Collectors.toMap(i -> i, i -> List.of(MAILBOX)));
    return as.run(
        PO,
        () ->
            sending.send(
                fx.company(),
                new PlacementSending.Request(
                    List.of(c.getRenewalRef()), to, List.of("copy@bank.example.ph"))));
  }

  private BulkRow row(int no, Map<String, String> values) {
    return new BulkRow(no, values);
  }

  @Test
  void aCoInsuredAccountGetsOneSlipPerInsurerIsSentAndBookedOnceBothInsurersApprove() {
    RenewalCandidate c = submitted();
    String lead = c.getSnapshot().insurerCode();
    String other = otherInsurer(c);
    as.run(
        PO,
        () ->
            allocations.allocate(
                fx.company(),
                c.getRenewalRef(),
                List.of(
                    new InsurerAllocationService.Input(lead, BigDecimal.valueOf(70), null),
                    new InsurerAllocationService.Input(other, BigDecimal.valueOf(30), null))));
    BatchOutcome generated =
        as.run(PO, () -> generation.generate(fx.company(), List.of(c.getRenewalRef())));
    assertThat(generated.refused()).isEmpty();
    List<PlacementTracking.View> slips =
        as.run(PO, () -> tracking.of(fx.company(), c.getRenewalRef()));
    assertThat(slips).hasSize(2);
    assertThat(slips)
        .allMatch(
            p -> p.docs().slipFileName().contains("_PlacementSlip_" + c.getRenewalRef() + "_"));
    assertThat(as.run(PO, () -> sending.recipients(fx.company(), List.of(c.getRenewalRef()))))
        .extracting(PlacementSending.Recipients::insurerCode)
        .containsExactlyInAnyOrder(lead, other);

    assertThatThrownBy(
            () ->
                as.run(
                    PO,
                    () ->
                        sending.send(
                            fx.company(),
                            new PlacementSending.Request(
                                List.of(c.getRenewalRef()),
                                Map.of(lead, List.of("not-an-address"), other, List.of(MAILBOX)),
                                List.of()))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("Invalid email address format");
    PlacementSending.Summary summary = sendAll(c, lead, other);
    assertThat(summary.submitted()).containsExactly(c.getRenewalRef());
    assertThat(fx.reload(c).getPlacement().getStatus()).isEqualTo(CandidatePlacement.FOR_BOOKING);
    PlacementTracking.View sent =
        as.run(PO, () -> tracking.of(fx.company(), c.getRenewalRef())).get(0);
    assertThat(sent.sending().recipients()).isEqualTo(MAILBOX);
    assertThat(sent.tat().target()).isEqualTo(c.getSnapshot().packaged() ? 3 : 10);
    assertThat(sent.tat().status()).isEqualTo(PlacementTat.WITHIN);

    LocalDate today = BusinessClock.today(clock);
    assertThatThrownBy(
            () ->
                as.run(
                    PO,
                    () ->
                        tracking.issue(
                            fx.company(), c.getRenewalRef(), sent.id(), true, today.minusDays(30))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("earlier than the placement submission date");
    PlacementTracking.View issue =
        as.run(PO, () -> tracking.issue(fx.company(), c.getRenewalRef(), sent.id(), true, today));
    assertThat(issue.withIssue()).isTrue();
    assertThat(issue.tat().end()).isEqualTo(today);

    parameters.update("RNW_BOOK_ON_PLACEMENT_APPROVAL", "true");
    try {
      BulkContext ctx = new BulkContext(fx.company(), "PR-" + c.getId(), today, Map.of());
      assertThat(
              responses.validate(
                  row(
                      2,
                      Map.of(
                          "Renewal Reference Number", "RNW-NOPE", "Insurer Response", "Approved")),
                  ctx))
          .contains("Record not found");
      BulkRow first =
          row(
              3,
              Map.of(
                  "Renewal Reference Number",
                  c.getRenewalRef(),
                  "Insurer Code",
                  lead,
                  "Insurer Response",
                  "Approved"));
      assertThat(responses.validate(first, ctx)).isEmpty();
      as.run(PO, () -> responses.process(first, ctx));
      assertThat(fx.reload(c).getPlacement().getStatus()).isEqualTo(CandidatePlacement.FOR_BOOKING);
      BulkRow second =
          row(
              4,
              Map.of(
                  "Renewal Reference Number",
                  c.getRenewalRef(),
                  "Insurer Code",
                  other,
                  "Insurer Response",
                  "Approved"));
      as.run(PO, () -> responses.process(second, ctx));
      RenewalCandidate booked = fx.reload(c);
      assertThat(booked.getPlacement().getStatus()).isEqualTo(CandidatePlacement.BOOKED);
      assertThat(booked.getMarketingLockedAt()).isNotNull();
    } finally {
      parameters.update("RNW_BOOK_ON_PLACEMENT_APPROVAL", "false");
    }
  }

  @Test
  void aRejectedPlacementCarriesTheReasonAndTheAccountReturnsToMarketing() {
    RenewalCandidate c = submitted();
    as.run(PO, () -> generation.generate(fx.company(), List.of(c.getRenewalRef())));
    sendAll(c, c.getSnapshot().insurerCode());
    BulkContext ctx =
        new BulkContext(fx.company(), "PR-R" + c.getId(), LocalDate.now(clock), Map.of());
    BulkRow noReason =
        row(
            2,
            Map.of("Renewal Reference Number", c.getRenewalRef(), "Insurer Response", "Rejected"));
    assertThat(responses.validate(noReason, ctx)).contains("Enter a valid rejection reason");
    BulkRow rejected =
        row(
            3,
            Map.of(
                "Renewal Reference Number", c.getRenewalRef(),
                "Insurer Response", "Rejected",
                "Rejection Reason", "Claims/Losses",
                "Insurer Remarks", "Two claims this term"));
    as.run(PO, () -> responses.process(rejected, ctx));
    assertThat(fx.reload(c).getPlacement().getStatus())
        .isEqualTo(CandidatePlacement.REJECTED_PLACEMENT);
    assertThat(
            as.run(PO, () -> tracking.of(fx.company(), c.getRenewalRef()))
                .get(0)
                .response()
                .reason())
        .isEqualTo("Claims/Losses");

    BatchOutcome back =
        as.run(
            PO,
            () ->
                tracking.returnToMarketing(
                    fx.company(),
                    List.of(c.getRenewalRef()),
                    "INSURER_DECLINED",
                    "Insurer declined"));
    assertThat(back.refused()).isEmpty();
    RenewalCandidate returned = fx.reload(c);
    assertThat(returned.getStage()).isEqualTo(RenewalStage.FOR_DISPOSITION);
    assertThat(returned.getFlags().isReturned()).isTrue();
    assertThat(returned.getPlacement().getStatus()).isNull();
  }

  @Test
  void anAccountForBookingOnlyIsLeftOutOfThePlacementFiles() {
    RenewalCandidate c = submitted();
    jdbc.update("update rnw_candidate set for_booking_only = true where id = ?", c.getId());
    BatchOutcome generated =
        as.run(PO, () -> generation.generate(fx.company(), List.of(c.getRenewalRef())));
    assertThat(generated.refused()).containsKey(c.getRenewalRef());
  }

  @Test
  void aMortgagedAccountGetsItsInsuranceAdviceAndANewVersionWhenThePlacementChanges() {
    RenewalCandidate c = submitted();
    jdbc.update(
        "update rnw_candidate set mortgaged = true, mortgagee_bank = 'Mortgagee Bank' where id = ?",
        c.getId());
    as.run(PO, () -> generation.generate(fx.company(), List.of(c.getRenewalRef())));
    sendAll(c, c.getSnapshot().insurerCode());
    List<AdviceVersion> first = as.run(PO, () -> advices.of(fx.company(), c.getRenewalRef()));
    assertThat(first).hasSize(1);
    assertThat(first.get(0).getFileName())
        .startsWith("IA_" + c.getRenewalRef() + "_")
        .endsWith(".pdf");
    assertThat(as.run(PO, () -> advices.afterPlacement(fx.reload(c), "PLACEMENT_CHANGED")))
        .isNull();
    jdbc.update("update rnw_placement set premium = premium + 1 where candidate_id = ?", c.getId());
    AdviceVersion second =
        as.run(PO, () -> advices.afterPlacement(fx.reload(c), "PLACEMENT_CHANGED"));
    assertThat(second.getVersionNo()).isEqualTo(2);
    assertThat(
            as.run(
                PO,
                () ->
                    advices.send(
                        fx.company(),
                        List.of(c.getRenewalRef()),
                        List.of("loans@bank.example.ph"),
                        List.of())))
        .hasSize(1);
  }

  @Test
  void aPlacementResponseFileReceivedThroughMftIsProcessed() {
    // Other tests create companies of their own; the inbox must deliver to the fixture company.
    String mftCompany = parameters.text(MFT_COMPANY, "");
    String company =
        jdbc.queryForObject(
            "select code from org_company where id = ?", String.class, fx.company());
    as.run("admin", () -> parameters.update(MFT_COMPANY, company));
    try {
      placementResponseThroughMft();
    } finally {
      as.run("admin", () -> parameters.update(MFT_COMPANY, mftCompany));
    }
  }

  private void placementResponseThroughMft() {
    RenewalCandidate c = submitted();
    as.run(PO, () -> generation.generate(fx.company(), List.of(c.getRenewalRef())));
    sendAll(c, c.getSnapshot().insurerCode());
    String csv = "Renewal Reference Number,Insurer Response\n" + c.getRenewalRef() + ",Approved\n";
    inbox.place(
        MftInbox.PLACEMENT, "response-" + c.getId() + ".csv", csv.getBytes(StandardCharsets.UTF_8));
    assertThat(inbox.poll()).isPositive();
    assertThat(
            as.run(PO, () -> tracking.of(fx.company(), c.getRenewalRef()))
                .get(0)
                .response()
                .response())
        .isEqualTo("APPROVED");
    Integer processed =
        jdbc.queryForObject(
            "select count(*) from rnw_channel_message where direction = 'INBOUND'"
                + " and file_name = ? and status = 'PROCESSED'",
            Integer.class,
            "response-" + c.getId() + ".csv");
    assertThat(processed).isEqualTo(1);
  }
}
