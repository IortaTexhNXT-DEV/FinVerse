package com.iortatechnxt.brokerverse.brokerclaims.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.brokerclaims.cover.service.PremiumRule;
import com.iortatechnxt.brokerverse.brokerclaims.cover.service.PremiumRule.InvoiceState;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.domain.InsurerClaim;
import com.iortatechnxt.brokerverse.brokerclaims.insurer.domain.InsurerUpdate;
import com.iortatechnxt.brokerverse.brokerclaims.location.domain.LocationRef;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The rules of the claim parts owned by wave CL1-A (BRCLM.001/004/006/023/039/041/042/043): premium
 * check, cover snapshot, loss details, insurer line, insurer update and location reference.
 */
class ClaimPartsTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);
  private static final Instant NOW = Instant.parse("2026-09-26T01:00:00Z");

  private static InvoiceState invoice(PaymentStatus status, boolean dp, boolean cancelled) {
    return new InvoiceState("I-" + status, "BOOKING", status, dp, cancelled, BigDecimal.TEN, "PHP");
  }

  private static CoverSnapshot.Policy policy(String policyNo) {
    return new CoverSnapshot.Policy(
        "ARN-1",
        1L,
        1,
        policyNo,
        "PAR01",
        "PROPERTY",
        "CL-1",
        "Acme",
        "INS-A",
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2027, 1, 1),
        new BigDecimal("1000.00"),
        "USD");
  }

  private static CoverSnapshot.Sales sales(String ao) {
    return new CoverSnapshot.Sales("NCR", "CBG-NCR", "T-CBG1", ao, "CC", 2L);
  }

  @Test
  void thePremiumCheckFollowsThePaymentStatusOfTheLiveInvoices() {
    assertThat(PremiumRule.evaluate(List.of()).status()).isEqualTo(ClaimPremiumStatus.NO_INVOICE);
    assertThat(
            PremiumRule.evaluate(
                    List.of(
                        invoice(PaymentStatus.PAID, false, false),
                        invoice(PaymentStatus.UNPAID, false, false)))
                .status())
        .isEqualTo(ClaimPremiumStatus.UNPAID);
    var partly = PremiumRule.evaluate(List.of(invoice(PaymentStatus.PARTIALLY_PAID, false, false)));
    assertThat(partly.status()).isEqualTo(ClaimPremiumStatus.PARTIALLY_PAID);
    assertThat(partly.blocking()).isTrue();
    assertThat(partly.unpaid()).hasSize(1);
    var cancelled =
        PremiumRule.evaluate(
            List.of(
                invoice(PaymentStatus.PAID, false, false),
                invoice(PaymentStatus.UNPAID, false, true)));
    assertThat(cancelled.status()).isEqualTo(ClaimPremiumStatus.PAID);
    assertThat(cancelled.blocking()).isFalse();
    assertThat(
            PremiumRule.evaluate(List.of(invoice(PaymentStatus.NOT_APPLICABLE, true, false)))
                .status())
        .isEqualTo(ClaimPremiumStatus.DIRECT_PAYMENT);
    assertThat(
            PremiumRule.evaluate(
                    List.of(
                        invoice(PaymentStatus.PAID, false, false),
                        invoice(PaymentStatus.NOT_APPLICABLE, false, false)))
                .status())
        .isEqualTo(ClaimPremiumStatus.PAID);
  }

  @Test
  void theCoverSnapshotRefreshesSwitchesVersionAndIsAuthorisedOnce() {
    CoverSnapshot cover =
        CoverSnapshot.of(policy(null), new CoverSnapshot.Version(1, "END-1", TODAY), sales("ao"));
    assertThat(cover.versionLabel()).isEqualTo("Cover v1 (END-1)");
    assertThat(cover.getCurrency()).isEqualTo("USD");
    assertThat(cover.covers(LocalDate.of(2026, 6, 1))).isTrue();
    assertThat(cover.covers(LocalDate.of(2025, 12, 31))).isFalse();
    List<String> changes = cover.refresh(policy("POL-1"), sales("ao2"));
    assertThat(changes).hasSize(2).anyMatch(c -> c.startsWith("Account officer: ao -> ao2"));
    assertThat(cover.refresh(policy("POL-1"), sales("ao2"))).isEmpty();
    assertThatThrownBy(
            () ->
                cover.refresh(
                    new CoverSnapshot.Policy(
                        "ARN-2", 1L, 1, null, null, null, null, null, null, null, null, null,
                        "PHP"),
                    sales("ao")))
        .isInstanceOf(BusinessRuleException.class);
    assertThat(cover.useVersion(new CoverSnapshot.Version(2, "END-2", TODAY)))
        .isEqualTo("Cover v1 (END-1) -> Cover v2 (END-2)");
    cover.premiumChecked(ClaimPremiumStatus.PAID, NOW);
    assertThat(cover.getPremiumCheckedAt()).isEqualTo(NOW);
    assertThat(cover.isAuthorized()).isFalse();
    cover.authorize("CAC-2026-000001", "clmofficer", NOW, null);
    assertThat(cover.isAuthorized()).isTrue();
    assertThatThrownBy(() -> cover.authorize("CAC-2026-000002", "clmofficer", NOW, null))
        .isInstanceOf(BusinessRuleException.class);
    assertThat(
            CoverSnapshot.of(policy(null), new CoverSnapshot.Version(0, null, null), sales(null))
                .versionLabel())
        .isEqualTo("Cover v0");
  }

  @Test
  void theLossDetailsKeepTheirDatesClaimantAndCatastropheRules() {
    LossDetails.Loss loss =
        new LossDetails.Loss(
            TODAY.minusDays(3), TODAY.minusDays(1), "FIRE", "PROPERTY", "Fire", null, null, null);
    LossDetails.Amounts amounts = new LossDetails.Amounts(BigDecimal.TEN, BigDecimal.ONE, null);
    LossDetails details = LossDetails.of(loss, amounts, "Acme", TODAY);
    assertThat(details.getClaimantName()).isEqualTo("Acme");
    assertThat(details.isCatastrophe()).isFalse();
    assertThat(details.correctReportedDate(TODAY.minusDays(2), "DATA_CORRECTION", TODAY))
        .isEqualTo(TODAY.minusDays(1));
    assertThatThrownBy(
            () -> details.correctReportedDate(TODAY.minusDays(9), "DATA_CORRECTION", TODAY))
        .hasMessage("The reported date must be between the loss date and today");
    assertThatThrownBy(() -> details.overrideClaimant("X", " "))
        .hasMessage("Enter the reason for the change");
    assertThat(details.overrideClaimant(" Third Party ", "OTHERS")).isEqualTo("Acme");
    assertThat(details.isClaimantOverridden()).isTrue();
    details.amend(
        new LossDetails.Loss(
            TODAY.minusDays(4), null, "FIRE", "PROPERTY", "Fire", "Here", "TYPHOON", "Kristine"),
        amounts,
        TODAY);
    assertThat(details.isCatastrophe()).isTrue();
    assertThat(details.getCatastropheEvent()).isEqualTo("Kristine");
    assertThatThrownBy(
            () ->
                LossDetails.of(
                    loss, new LossDetails.Amounts(new BigDecimal("-1"), null, null), "Acme", TODAY))
        .hasMessage("The claim amount cannot be negative");
    assertThatThrownBy(
            () ->
                details.amend(
                    new LossDetails.Loss(
                        TODAY.plusDays(1), null, "FIRE", "PROPERTY", "x", null, null, null),
                    amounts,
                    TODAY))
        .hasMessage("Enter a loss date that is not in the future");
  }

  @Test
  void insurerLinesUpdatesAndReferencesKeepTheirRules() {
    InsurerClaim line =
        new InsurerClaim(1L, 2L, "INS-A", new BigDecimal("60"), new BigDecimal("100.00"));
    line.number(" C-1 ", TODAY, TODAY);
    assertThat(line.getInsurerClaimNo()).isEqualTo("C-1");
    assertThatThrownBy(() -> line.number("C-2", TODAY.plusDays(1), TODAY))
        .hasMessage("The date cannot be in the future");
    assertThatThrownBy(() -> line.changeShare(new BigDecimal("101")))
        .isInstanceOf(BusinessRuleException.class);
    assertThat(line.amendReserve(BigDecimal.ZERO)).isEqualByComparingTo("100.00");
    assertThatThrownBy(() -> line.amendReserve(null)).hasMessage("Enter the new reserve");
    assertThatThrownBy(() -> line.settled(new BigDecimal("-1")))
        .isInstanceOf(BusinessRuleException.class);
    line.settled(BigDecimal.ONE);
    assertThat(line.assignAdjuster("CRAWFORD")).isNull();
    assertThat(line.getSettledAmount()).isEqualByComparingTo("1");

    InsurerUpdate update =
        new InsurerUpdate(
            2L,
            new InsurerUpdate.Content(
                null, TODAY, "EMAIL", " ", " Noted ", List.of(5L, 6L), null, null, TODAY),
            "clmofficer",
            NOW);
    assertThat(update.getReference()).isNull();
    assertThat(update.getRemarks()).isEqualTo("Noted");
    assertThat(update.getAttachmentIdList()).containsExactly(5L, 6L);

    LocationRef ref =
        new LocationRef(1L, new LocationRef.Location(1L, "ARN-1", 1, "k"), "INS-A", " A-1 ", TODAY);
    assertThat(ref.validOn(TODAY)).isTrue();
    assertThatThrownBy(() -> ref.supersede(TODAY))
        .hasMessage("The effective date must be after 26-Sep-2026");
    ref.supersede(TODAY.plusDays(10));
    assertThat(ref.getEffectiveTo()).isEqualTo(TODAY.plusDays(9));
    assertThat(ref.validOn(TODAY.plusDays(10))).isFalse();
    assertThatThrownBy(
            () ->
                new LocationRef(
                    1L, new LocationRef.Location(1L, "ARN-1", 1, "k"), "INS-A", " ", TODAY))
        .hasMessage("Enter the insurer location reference");
  }
}
