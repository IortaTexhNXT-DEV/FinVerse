package com.iortatechnxt.brokerverse.renewal.approval;

import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.AO;
import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.PO;
import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.TL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.renewal.RenewalFixtures;
import com.iortatechnxt.brokerverse.renewal.approval.api.DispositionSetController;
import com.iortatechnxt.brokerverse.renewal.approval.service.BookingOnlyAccounts;
import com.iortatechnxt.brokerverse.renewal.approval.service.ClientDecisions;
import com.iortatechnxt.brokerverse.renewal.approval.service.DirectToInsurerTags;
import com.iortatechnxt.brokerverse.renewal.approval.service.RenewalApprovals;
import com.iortatechnxt.brokerverse.renewal.approval.service.RenewalPayments;
import com.iortatechnxt.brokerverse.renewal.approval.service.SubmissionGate;
import com.iortatechnxt.brokerverse.renewal.billing.service.BillingFiles;
import com.iortatechnxt.brokerverse.renewal.domain.BillingFile;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateDecision;
import com.iortatechnxt.brokerverse.renewal.domain.CandidateTags;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.service.BatchOutcome;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The client's answer, the second approval, the For Booking Only and Direct-to-Insurer Payment
 * tags, the payment status and the CLPC billing files of BDOI's FRS against the real database
 * (FRRN.014.02 to FRRN.014.05, FRRN.019.01, FRRN.025.01, FRRN.26.01, FRRN.027, FRRN.028.01).
 */
@IntegrationTest
class RenewalApprovalsIT {

  @Autowired private RenewalFixtures fx;
  @Autowired private ClientDecisions decisions;
  @Autowired private RenewalApprovals approvals;
  @Autowired private SubmissionGate gate;
  @Autowired private BookingOnlyAccounts bookingOnly;
  @Autowired private DirectToInsurerTags directToInsurer;
  @Autowired private BillingFiles billing;
  @Autowired private DispositionSetController dispositions;
  @Autowired private SystemParameterService parameters;
  @Autowired private TransactionTemplate tx;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private Clock clock;
  @Autowired private AsUser as;

  private RenewalCandidate at(RenewalCandidate c, String stage) {
    jdbc.update("update rnw_candidate set stage = ? where id = ?", stage, c.getId());
    jdbc.update(
        "update wf_case set stage_code = ? where entity_type = 'RenewalCandidate' and entity_id = ?",
        stage,
        c.getId().toString());
    return fx.reload(c);
  }

  private RenewalCandidate mine(RenewalCandidate c) {
    jdbc.update("update rnw_candidate set assigned_ao = ? where id = ?", AO, c.getId());
    return fx.reload(c);
  }

  @Test
  void theClientStatusNeedsRemarksForARejectionAndIsRecordedOnceTheAdviceIsOut() {
    RenewalCandidate c = at(mine(fx.unassignedRetail()), "RA_SENT");
    assertThatThrownBy(
            () ->
                as.run(
                    AO,
                    () -> {
                      decisions.record(
                          fx.company(), c.getRenewalRef(), CandidateDecision.REJECTED, " ");
                      return null;
                    }))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("Remarks are required");
    as.run(
        AO,
        () -> {
          decisions.record(
              fx.company(), c.getRenewalRef(), CandidateDecision.REVISION, "Lower deductible");
          return null;
        });
    assertThat(fx.reload(c).getPlacement().getDecision().getClientStatus())
        .isEqualTo(CandidateDecision.REVISION);
    tx.executeWithoutResult(
        s -> {
          RenewalCandidate fresh = fx.reload(c);
          decisions.afterRaSent(fresh, false);
          assertThat(fresh.getPlacement().getDecision().getClientStatus())
              .isEqualTo(CandidateDecision.PENDING);
        });
  }

  @Test
  void withTheSecondApprovalAnAcceptedAccountWaitsAndANonCbgAccountNeedsThePaymentConfirmation() {
    parameters.update(SubmissionGate.PARAMETER, "true");
    try {
      RenewalCandidate c = at(mine(fx.unassignedRetail()), "ACCEPTED");
      tx.executeWithoutResult(s -> assertThat(gate.awaitsApproval(fx.reload(c))).isTrue());
      assertThat(fx.reload(c).getPlacement().getDecision().getApprovalStatus())
          .isEqualTo(CandidateDecision.SUBMITTED);
      List<String> ref = List.of(c.getRenewalRef());
      BatchOutcome missing = as.run(TL, () -> approvals.approve(fx.company(), ref, null));
      assertThat(missing.refused().get(c.getRenewalRef()))
          .isEqualTo(RenewalApprovals.CONFIRMATION_REQUIRED);
      BatchOutcome back = as.run(TL, () -> approvals.reject(fx.company(), ref, "Wrong premium"));
      assertThat(back.refused()).isEmpty();
      assertThat(back.done()).containsExactly(c.getRenewalRef());
      assertThat(fx.reload(c).getPlacement().getDecision().getApprovalStatus())
          .isEqualTo(CandidateDecision.RETURNED);
      as.run(
          AO,
          () -> {
            approvals.resubmit(fx.company(), c.getRenewalRef());
            return null;
          });
      BatchOutcome ok = as.run(TL, () -> approvals.approve(fx.company(), ref, "EMAIL"));
      assertThat(ok.done()).containsExactly(c.getRenewalRef());
      assertThat(fx.reload(c).getPlacement().getDecision().getApprovalStatus())
          .isEqualTo(CandidateDecision.APPROVED);
    } finally {
      parameters.update(SubmissionGate.PARAMETER, "false");
    }
  }

  @Test
  void anAccountForBookingOnlyNeedsItsPolicyReceiptDocumentAndPaymentOrAnApprovedOverride() {
    RenewalCandidate c = mine(fx.unassignedRetail());
    BatchOutcome tagged =
        as.run(AO, () -> bookingOnly.tag(fx.company(), List.of(c.getRenewalRef()), true));
    assertThat(tagged.refused()).isEmpty();
    List<String> missing = tx.execute(s -> bookingOnly.missing(fx.reload(c)));
    assertThat(missing)
        .contains("Policy Number", "OR Number", "insurer-issued policy document", "full payment");
    assertThatThrownBy(() -> tx.executeWithoutResult(s -> bookingOnly.requireReady(fx.reload(c))))
        .hasMessage(BookingOnlyAccounts.NOT_READY);
    as.run(
        AO,
        () -> {
          bookingOnly.requestOverride(fx.company(), c.getRenewalRef(), "Client paid the insurer");
          return null;
        });
    assertThatThrownBy(
            () ->
                as.run(
                    AO,
                    () -> {
                      bookingOnly.decideOverride(fx.company(), c.getRenewalRef(), true, "Self");
                      return null;
                    }))
        .isInstanceOf(BusinessRuleException.class);
    as.run(
        TL,
        () -> {
          bookingOnly.decideOverride(fx.company(), c.getRenewalRef(), true, "Receipt seen");
          return null;
        });
    List<String> none = tx.execute(s -> bookingOnly.missing(fx.reload(c)));
    assertThat(none).isEmpty();
  }

  @Test
  void aDirectToInsurerPaymentRoutedToTheUnitHeadBlocksUntilApprovedAndARejectionUntilUntagged() {
    RenewalCandidate c = mine(fx.unassignedRetail());
    as.run(
        AO,
        () -> {
          directToInsurer.tag(fx.company(), c.getRenewalRef(), CandidateTags.UNIT_HEAD);
          return null;
        });
    assertThatThrownBy(() -> directToInsurer.requireCleared(fx.reload(c)))
        .hasMessageContaining("awaits the Unit Head");
    assertThatThrownBy(
            () ->
                as.run(
                    TL,
                    () -> {
                      directToInsurer.decide(fx.company(), c.getRenewalRef(), false, "");
                      return null;
                    }))
        .isInstanceOf(BusinessRuleException.class);
    as.run(
        TL,
        () -> {
          directToInsurer.decide(fx.company(), c.getRenewalRef(), false, "No blanket approval");
          return null;
        });
    assertThatThrownBy(() -> directToInsurer.requireCleared(fx.reload(c)))
        .hasMessageContaining("remove the tag");
    as.run(
        AO,
        () -> {
          directToInsurer.tag(fx.company(), c.getRenewalRef(), null);
          return null;
        });
    directToInsurer.requireCleared(fx.reload(c));
    as.run(
        AO,
        () -> {
          directToInsurer.tag(fx.company(), c.getRenewalRef(), CandidateTags.BLANKET);
          return null;
        });
    directToInsurer.requireCleared(fx.reload(c));
  }

  @Test
  void thePaymentStatusFollowsTheOutstandingBalance() {
    BigDecimal premium = new BigDecimal("1000.00");
    assertThat(RenewalPayments.status(premium, premium).status()).isEqualTo(RenewalPayments.PAID);
    assertThat(RenewalPayments.status(premium, new BigDecimal("400")).status())
        .isEqualTo(RenewalPayments.PARTIALLY_PAID);
    assertThat(RenewalPayments.status(premium, BigDecimal.ZERO).status())
        .isEqualTo(RenewalPayments.UNPAID);
  }

  @Test
  void aCbgHomeAccountForBillingGenerationGoesInTheBuiltInFileOnce() {
    RenewalCandidate c = at(fx.extracted(fx.book("PAR01", "CBG")), "RA_SENT");
    jdbc.update(
        "update rnw_candidate set client_status = 'NOT_APPLICABLE', client_status_at = now(),"
            + " amortized = true where id = ?",
        c.getId());
    LocalDate today = BusinessClock.today(clock);
    assertThatThrownBy(
            () -> as.run(PO, () -> billing.manual(fx.company(), today.minusDays(40), today, today)))
        .hasMessageContaining("31 calendar days");
    List<BillingFile> files =
        as.run(PO, () -> billing.manual(fx.company(), today.minusDays(1), today, today));
    assertThat(files)
        .anyMatch(f -> f.isBuiltIn() && f.getFileName().contains("_CLPC Billing Built in_"));
    assertThat(fx.reload(c).getPlacement().getTags().getBillingFileId()).isNotNull();
    assertThat(as.run(PO, () -> billing.manual(fx.company(), today.minusDays(1), today, today)))
        .noneMatch(f -> f.getId().equals(fx.reload(c).getPlacement().getTags().getBillingFileId()));
  }

  @Test
  void withTheClientDispositionSetLostBusinessIsAReasonNotADisposition() {
    assertThat(as.run(AO, () -> dispositions.offered()))
        .extracting(DispositionSetController.Option::code)
        .contains("LOST_BUSINESS");
    parameters.update("RNW_DISPOSITION_SET", "CLIENT");
    try {
      assertThat(as.run(AO, () -> dispositions.offered()))
          .extracting(DispositionSetController.Option::code)
          .doesNotContain("LOST_BUSINESS")
          .contains("FOR_RENEWAL", "NOT_FOR_RENEWAL", "FOR_QUOTATION", "FOR_PROPOSAL");
    } finally {
      parameters.update("RNW_DISPOSITION_SET", "SYSTEM");
    }
  }
}
