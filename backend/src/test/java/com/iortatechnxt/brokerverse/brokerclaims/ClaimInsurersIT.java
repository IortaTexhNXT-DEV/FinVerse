package com.iortatechnxt.brokerverse.brokerclaims;

import static com.iortatechnxt.brokerverse.brokerclaims.ClaimsFixtures.CO_INSURER;
import static com.iortatechnxt.brokerverse.brokerclaims.ClaimsFixtures.LEAD;
import static com.iortatechnxt.brokerverse.brokerclaims.ClaimsFixtures.OFFICER;
import static com.iortatechnxt.brokerverse.brokerclaims.ClaimsFixtures.TL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.brokerclaims.claim.service.BrokerClaimQueryService;
import com.iortatechnxt.brokerverse.brokerclaims.claim.service.ClaimRecordingService;
import com.iortatechnxt.brokerverse.brokerclaims.claim.service.ClaimViewService;
import com.iortatechnxt.brokerverse.brokerclaims.domain.Claim;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.domain.InsurerClaim;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.domain.InsurerUpdate;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.InsurerClaimService;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.InsurerClaimService.NewLine;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.InsurerUpdateService;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.InsurerUpdateService.NewUpdate;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.LossAdviceService;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.service.LossAdviceService.Recipient;
import com.iortatechnxt.brokerverse.brokerclaims.location.domain.LocationRef;
import com.iortatechnxt.brokerverse.brokerclaims.location.service.ClaimLocationService;
import com.iortatechnxt.brokerverse.brokerclaims.location.service.ClaimLocationService.LocationPick;
import com.iortatechnxt.brokerverse.brokerclaims.location.service.LocationRefService;
import com.iortatechnxt.brokerverse.brokerclaims.location.service.LocationRefService.NewRef;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * A multi-location and multi-insurer claim (BRCLM.023/024/037/041/042/043; wave CL1-A exit
 * criterion 2): two of three locations of a property cover and the two co-insurers of its invoice
 * under one claim number, the insurer claim numbers with their duplicate rules, the reserve and its
 * history, insurer updates, insurer location references and the loss advice e-mailed and stored as
 * a claims report.
 */
@IntegrationTest
class ClaimInsurersIT {

  @Autowired private ClaimsFixtures fx;
  @Autowired private ClaimRecordingService recording;
  @Autowired private BrokerClaimQueryService claims;
  @Autowired private ClaimViewService views;
  @Autowired private ClaimLocationService locations;
  @Autowired private LocationRefService refs;
  @Autowired private InsurerClaimService insurers;
  @Autowired private InsurerUpdateService updates;
  @Autowired private LossAdviceService advice;
  @Autowired private AsUser as;
  @Autowired private JdbcTemplate jdbc;

  private Claim property(OpsInvoice invoice, List<LocationPick> picks) {
    return as.run(
        OFFICER,
        () ->
            recording.record(
                fx.company(),
                ClaimsFixtures.request(
                    invoice.getArn(), ClaimsFixtures.loss("FIRE"), picks, List.of())));
  }

  private InsurerClaim line(Claim claim, String insurer) {
    return insurers.ofClaim(claim.getId()).stream()
        .filter(l -> l.getInsurerCode().equals(insurer))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void oneClaimCarriesSeveralLocationsAndInsurers() {
    OpsInvoice invoice = fx.propertyInvoice(3);
    Claim claim =
        property(invoice, List.of(new LocationPick(1, "Roof"), new LocationPick(2, "Warehouse")));
    Long id = claim.getId();

    assertThat(locations.ofClaim(id)).extracting(l -> l.getAccountItemNo()).containsExactly(1, 2);
    assertThat(insurers.ofClaim(id))
        .extracting(InsurerClaim::getInsurerCode, l -> l.getSharePct().intValue())
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(LEAD, 60),
            org.assertj.core.groups.Tuple.tuple(CO_INSURER, 40));
    var view = views.view(fx.company(), id);
    assertThat(view.locationCount()).isEqualTo(2);
    assertThat(view.insurerCount()).isEqualTo(2);
    assertThat(claims.ofCover(fx.company(), invoice.getArn())).hasSize(1);

    assertThatThrownBy(
            () -> as.run(OFFICER, () -> locations.link(claim, List.of(new LocationPick(1, null)))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("is already on this claim");
    assertThatThrownBy(
            () -> as.run(OFFICER, () -> locations.link(claim, List.of(new LocationPick(9, null)))))
        .hasMessage("Location 9 is not on the cover " + invoice.getArn());
    as.run(OFFICER, () -> locations.link(claim, List.of(new LocationPick(3, "Fence"))));
    as.run(
        OFFICER,
        () -> {
          locations.remove(claim, 3);
          return null;
        });
    assertThat(locations.ofClaim(id)).hasSize(2);
  }

  @Test
  void insurerClaimNumbersAreUniquePerInsurerAndWarnedAcrossClaims() {
    OpsInvoice invoice = fx.propertyInvoice(1);
    Claim claim = property(invoice, List.of(new LocationPick(1, null)));
    LocalDate today = LocalDate.now();
    String number = "C-INSA-" + System.nanoTime();
    InsurerClaim lead = line(claim, LEAD);
    as.run(OFFICER, () -> insurers.number(claim, lead.getId(), number, today, false));
    InsurerClaim co = line(claim, CO_INSURER);
    as.run(
        OFFICER,
        () -> insurers.number(claim, co.getId(), "C-INSB-" + System.nanoTime(), today, false));

    assertThatThrownBy(
            () -> as.run(OFFICER, () -> insurers.number(claim, lead.getId(), number, today, false)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessage(
            "Insurer claim number "
                + number
                + " is already recorded for "
                + LEAD
                + " on this claim");
    assertThatThrownBy(
            () ->
                as.run(
                    OFFICER,
                    () -> insurers.number(claim, co.getId(), "X", today.plusDays(1), false)))
        .hasMessageContaining("cannot be in the future");
    assertThatThrownBy(
            () ->
                as.run(
                    OFFICER,
                    () -> insurers.add(claim, new NewLine(" ", null, null, null, null), false)))
        .hasMessage("Select the insurer");

    Claim other = property(fx.propertyInvoice(1), List.of());
    InsurerClaim otherLead = line(other, LEAD);
    assertThatThrownBy(
            () ->
                as.run(
                    OFFICER, () -> insurers.number(other, otherLead.getId(), number, today, false)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("is already on claim " + claim.getClaimNo());
    as.run(OFFICER, () -> insurers.number(other, otherLead.getId(), number, today, true));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from alt_alert where dedup_key = ?",
                Long.class,
                InsurerClaimService.REUSED + ":" + LEAD + ":" + number))
        .isEqualTo(1L);

    InsurerClaim amended =
        as.run(
            TL,
            () ->
                insurers.amendReserve(
                    claim, lead.getId(), new BigDecimal("90000.00"), "Adjuster's estimate"));
    as.run(
        TL,
        () -> insurers.amendReserve(claim, lead.getId(), new BigDecimal("120000.00"), "Revised"));
    as.run(TL, () -> insurers.amendReserve(claim, co.getId(), BigDecimal.ZERO.setScale(2), "Nil"));
    assertThat(amended.getReserveAmount()).isEqualByComparingTo("90000.00");
    var history = insurers.reserveHistory(List.of(lead.getId()));
    assertThat(history).hasSize(2);
    assertThat(history.get(0).getPreviousAmount()).isEqualByComparingTo("90000.00");
    assertThat(history.get(0).getNewAmount()).isEqualByComparingTo("120000.00");
    assertThat(history.get(0).getChangedBy()).isEqualTo(TL);
    assertThat(views.view(fx.company(), claim.getId()).totalReserve())
        .isEqualByComparingTo("120000.00");
    assertThatThrownBy(
            () ->
                as.run(
                    TL,
                    () -> insurers.amendReserve(claim, lead.getId(), new BigDecimal("-1.00"), "x")))
        .hasMessage("The reserve cannot be negative");
    assertThatThrownBy(
            () -> as.run(TL, () -> insurers.amendReserve(claim, lead.getId(), BigDecimal.ONE, " ")))
        .hasMessage("Enter the reason for the change");
    as.run(TL, () -> insurers.assignAdjuster(claim, co.getId(), "CRAWFORD"));
    assertThat(line(claim, CO_INSURER).getAdjusterCode()).isEqualTo("CRAWFORD");
    assertThatThrownBy(() -> as.run(TL, () -> insurers.assignAdjuster(claim, co.getId(), "")))
        .hasMessage("Select the adjuster");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from jnl_batch where narration like ? or reference like ?",
                Long.class,
                "%" + claim.getClaimNo() + "%",
                "%" + claim.getClaimNo() + "%"))
        .isZero();
  }

  @Test
  void insurerUpdatesAreInsertOnlyAndAllowedOnClosedClaims() {
    OpsInvoice invoice = fx.propertyInvoice(1);
    Claim claim = property(invoice, List.of());
    LocalDate today = LocalDate.now();
    Long file = fx.attach("BrokerClaim", claim.getId(), "OTHERS");
    InsurerUpdate first =
        as.run(
            OFFICER,
            () ->
                updates.record(
                    claim,
                    new NewUpdate(
                        line(claim, LEAD).getId(),
                        today,
                        "EMAIL",
                        "LTR-1",
                        "Adjuster appointed",
                        List.of(file),
                        null,
                        null)));
    assertThat(first.getAttachmentIdList()).containsExactly(file);
    assertThatThrownBy(
            () ->
                as.run(
                    OFFICER,
                    () ->
                        updates.record(
                            claim,
                            new NewUpdate(null, today, "EMAIL", null, " ", null, null, null))))
        .hasMessage("Enter the remarks");
    assertThatThrownBy(
            () ->
                as.run(
                    OFFICER,
                    () ->
                        updates.record(
                            claim,
                            new NewUpdate(
                                null, today.plusDays(1), "EMAIL", null, "x", null, null, null))))
        .hasMessage("Enter an update date that is not in the future");
    jdbc.update("update bcl_claim set phase = 'CLOSED' where id = ?", claim.getId());
    InsurerUpdate correction =
        as.run(
            OFFICER,
            () ->
                updates.record(
                    claim,
                    new NewUpdate(
                        null, today, "LETTER", null, "Correction", null, first.getId(), null)));
    assertThat(correction.getCorrectsUpdateId()).isEqualTo(first.getId());
    assertThat(updates.ofClaim(claim.getId())).hasSize(2);
  }

  @Test
  void insurerLocationReferencesKeepTheirHistory() {
    OpsInvoice invoice = fx.propertyInvoice(1);
    String arn = invoice.getArn();
    LocalDate start = LocalDate.now().minusMonths(1);
    LocationRef first =
        as.run(
            OFFICER,
            () -> refs.maintain(fx.company(), new NewRef(arn, 1, LEAD, "A-LOC-0091", start)));
    as.run(
        OFFICER,
        () -> refs.maintain(fx.company(), new NewRef(arn, 1, CO_INSURER, "B-77-12", start)));
    assertThatThrownBy(
            () ->
                as.run(
                    OFFICER,
                    () -> refs.maintain(fx.company(), new NewRef(arn, 1, LEAD, " ", start))))
        .hasMessage("Enter the insurer location reference");
    assertThatThrownBy(
            () ->
                as.run(
                    OFFICER,
                    () ->
                        refs.maintain(fx.company(), new NewRef(arn, 1, LEAD, "A-LOC-0105", start))))
        .hasMessageContaining("The effective date must be after");
    LocalDate next = LocalDate.now().plusMonths(1);
    as.run(
        OFFICER, () -> refs.maintain(fx.company(), new NewRef(arn, 1, LEAD, "A-LOC-0105", next)));

    List<LocationRef> all = refs.ofCover(fx.company(), arn);
    assertThat(all).hasSize(3);
    assertThat(
            all.stream()
                .filter(r -> r.getId().equals(first.getId()))
                .findFirst()
                .orElseThrow()
                .getEffectiveTo())
        .isEqualTo(next.minusDays(1));
    assertThat(refs.validOn(fx.company(), arn, LocalDate.now()))
        .extracting(LocationRef::getInsurerLocationRef)
        .containsExactlyInAnyOrder("A-LOC-0091", "B-77-12");
    assertThat(
            refs.search(fx.company(), arn, org.springframework.data.domain.PageRequest.of(0, 10))
                .getTotalElements())
        .isEqualTo(3);
  }

  @Test
  void theLossAdviceIsEmailedAndKeptAsAClaimsReport() {
    OpsInvoice invoice = fx.propertyInvoice(1);
    Claim claim = property(invoice, List.of(new LocationPick(1, "Roof")));
    as.run(TL, () -> insurers.assignAdjuster(claim, line(claim, LEAD).getId(), "CRAWFORD"));
    as.run(
        OFFICER,
        () ->
            insurers.number(
                claim,
                line(claim, LEAD).getId(),
                "C-ADV-" + System.nanoTime(),
                LocalDate.now(),
                false));

    var drafts = as.run(OFFICER, () -> advice.drafts(claim));
    assertThat(drafts).hasSize(2);
    assertThat(drafts.get(0).body())
        .contains(claim.getClaimNo())
        .contains("Crawford & Company Philippines, Inc.")
        .contains("C-ADV-");
    assertThatThrownBy(() -> as.run(OFFICER, () -> advice.send(claim, List.of())))
        .hasMessage("Select at least one insurer");
    assertThatThrownBy(
            () ->
                as.run(
                    OFFICER,
                    () ->
                        advice.send(
                            claim,
                            List.of(new Recipient(CO_INSURER, List.of(), null, null, null)))))
        .hasMessage("Enter the recipient address for " + CO_INSURER);

    var sent =
        as.run(
            OFFICER,
            () ->
                advice.send(
                    claim,
                    List.of(
                        new Recipient(LEAD, List.of("claims@insurer.test"), null, null, null))));
    assertThat(sent).hasSize(1);
    Long attachment = sent.get(0).attachmentId();
    assertThat(
            jdbc.queryForObject(
                "select document_type from doc_attachment where id = ?", String.class, attachment))
        .isEqualTo("CLAIM_REPORT");
    assertThat(
            jdbc.queryForList(
                "select entity_type from doc_attachment_link where attachment_id = ?",
                String.class,
                attachment))
        .containsExactlyInAnyOrder("Account", "Client");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from msg_outbound where entity_type = 'BrokerClaim' and entity_id"
                    + " = ?",
                Long.class,
                String.valueOf(claim.getId())))
        .isEqualTo(1L);
  }
}
