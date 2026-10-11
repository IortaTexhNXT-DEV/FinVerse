package com.iortatechnxt.brokerverse.eb.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.BusinessType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Invariants of the marketing and servicing entities (waves E1-B, E1-C): TOR versions, franchise
 * decisions, proposals, revisions, comparatives, confirmations, roster versions, members, member
 * changes and SOAs.
 */
class EbMarketingDomainTest {

  private static final Instant NOW = Instant.parse("2026-09-26T02:00:00Z");
  private static final LocalDate TODAY = LocalDate.of(2026, 9, 26);

  private static EbCycle cycle() {
    return new EbCycle(1L, "EBC-2026-000001", 2L, BusinessType.NEW_BUSINESS, 2027, null);
  }

  @Test
  void aTorIsReleasedWithItemsAndFrozenAfterwards() {
    EbTor tor = new EbTor(cycle(), 1);
    assertThatThrownBy(() -> tor.release(NOW, "ao", 5L))
        .extracting("code")
        .isEqualTo("EB_TOR_EMPTY");
    tor.replaceItems(List.of(new EbTorItem.Data("HMO", "A", "Limit", "PHP 100,000")));
    tor.release(NOW, "ao", 5L);
    assertThat(tor.getStatus()).isEqualTo(EbTor.Status.RELEASED);
    assertThat(tor.getItems().get(0).getSortOrder()).isEqualTo(1);
    assertThat(tor.getItems().get(0).getTor()).isSameAs(tor);
    assertThatThrownBy(() -> tor.replaceItems(List.of()))
        .extracting("code")
        .isEqualTo("EB_TOR_RELEASED");
    tor.supersede();
    assertThat(tor.getStatus()).isEqualTo(EbTor.Status.SUPERSEDED);
    assertThat(tor.getReleasedBy()).isEqualTo("ao");
    assertThat(tor.getAttachmentId()).isEqualTo(5L);
  }

  @Test
  void aFranchiseIsDecidedOnceWhileWaiting() {
    EbFranchiseRequest request = new EbFranchiseRequest(cycle(), "EBF-2026-000001", "INS-MGIC");
    EbFranchiseRequest.Decision decision =
        new EbFranchiseRequest.Decision(TODAY, "ao", null, "ok", 9L);
    assertThatThrownBy(
            () -> request.decide(EbFranchiseRequest.Status.APPROVED, decision, TODAY.plusDays(2)))
        .extracting("code")
        .isEqualTo("EB_FRANCHISE_NOT_WAITING");
    request.submitted(NOW, TODAY.plusDays(5), 3L);
    request.mirror(EbFranchiseRequest.Status.SUBMITTED);
    request.decide(EbFranchiseRequest.Status.APPROVED, decision, TODAY.plusDays(2));
    request.mirror(EbFranchiseRequest.Status.APPROVED);
    request.advised(NOW, "ao");
    request.mirror(EbFranchiseRequest.Status.ADVISED);
    assertThat(request.isApproved()).isTrue();
    assertThat(request.getEvidenceAttachmentId()).isEqualTo(9L);
    assertThat(request.getMessageId()).isEqualTo(3L);
    assertThat(request.getAdvisedBy()).isEqualTo("ao");
    assertThat(request.getRemarks()).isEqualTo("ok");
  }

  private static EbProposal proposal() {
    EbProposal p =
        new EbProposal(
            cycle(),
            "EBPR-2026-000001",
            new EbProposal.Origin("INS-MGIC", EbProposal.Kind.PROPOSAL, 1, 4L, null),
            new EbProposal.Content(TODAY, null, "PHP", "terms", "none", 8L));
    p.addLine(
        new EbProposalLine.Data(
            "HMO", "A", "Plan A", 10, new BigDecimal("1000"), new BigDecimal("10000"), null));
    p.addLine(new EbProposalLine.Data("HMO", "B", "Plan B", 5, null, new BigDecimal("7500"), null));
    p.addLine(
        new EbProposalLine.Data(
            "GLI", "L", null, null, null, new BigDecimal("300"), new BigDecimal("900000")));
    p.addItem(new EbProposalItem.Data(11L, "Yes", true, "Lower limit"));
    p.addFactor(new EbProposalFactor.Data("TECHNOLOGY", "App", 4));
    return p;
  }

  @Test
  void aProposalSumsItsPlansAndIsDecidedOnce() {
    EbProposal p = proposal();
    assertThat(p.premiumOf("HMO")).isEqualByComparingTo("17500");
    assertThat(p.sumInsuredOf("GLI")).isEqualByComparingTo("900000");
    assertThat(p.sumInsuredOf("HMO")).isEqualByComparingTo("0");
    assertThat(p.offers("GPA")).isFalse();
    assertThat(p.getItems().get(0).isDeviation()).isTrue();
    assertThat(p.getFactors().get(0).getRating()).isEqualTo(4);
    assertThatThrownBy(() -> p.reject(" ", "ao", NOW))
        .extracting("code")
        .isEqualTo("EB_PROPOSAL_REASON_REQUIRED");
    p.validate("ao", NOW);
    assertThatThrownBy(() -> p.validate("ao", NOW))
        .extracting("code")
        .isEqualTo("EB_PROPOSAL_DECIDED");
    p.supersede();
    assertThat(p.getStatus()).isEqualTo(EbProposal.Status.SUPERSEDED);
    assertThat(p.getSource()).isEqualTo("AO");
  }

  @Test
  void aRevisionIsAnsweredWhenEveryTargetAnswered() {
    EbRevisionRequest revision =
        new EbRevisionRequest(
            cycle(), 1, "More", new EbInsurerRequest.Sending(NOW, "ao", TODAY.plusDays(5)));
    revision.addItem(null, "Add dental");
    revision.addTarget("INS-MGIC").sentAs(4L);
    revision.addTarget("INS-LAC");
    revision.answer(revision.openTarget("INS-MGIC").orElseThrow(), 20L);
    assertThat(revision.getStatus()).isEqualTo(EbRevisionRequest.Status.OPEN);
    revision.answer(revision.openTarget("INS-LAC").orElseThrow(), 21L);
    assertThat(revision.getStatus()).isEqualTo(EbRevisionRequest.Status.ANSWERED);
    assertThat(revision.openTarget("INS-LAC")).isEmpty();
    assertThat(revision.getTargets().get(0).getMessageId()).isEqualTo(4L);
    assertThat(revision.getItems().get(0).getTorItemId()).isNull();
  }

  @Test
  void aComparativeNeedsARecommendationPerLineAndFollowsItsApprovals() {
    EbComparative c = new EbComparative(cycle(), "EBCA-2026-000001", 1, "{}", TODAY);
    c.addLine("HMO", new BigDecimal("100"), null);
    assertThatThrownBy(() -> c.submit(NOW, "ao"))
        .extracting("code")
        .isEqualTo("EB_RECOMMENDATION_REQUIRED");
    assertThatThrownBy(() -> c.recommend("GPA", 1L))
        .extracting("code")
        .isEqualTo("EB_COMPARATIVE_LINE");
    c.recommend("HMO", 1L);
    c.summarise("Best value");
    c.submit(NOW, "ao");
    assertThat(c.maker()).isEqualTo("ao");
    c.signedOff("HMO annual premium", "EB_THRESHOLD_APPROVE");
    assertThat(c.getStatus()).isEqualTo(EbComparative.Status.THRESHOLD_APPROVAL);
    c.returned();
    assertThat(c.getStatus()).isEqualTo(EbComparative.Status.DRAFT);
    assertThatThrownBy(c::returned).extracting("code").isEqualTo("EB_COMPARATIVE_STATUS");
    c.submit(NOW, "ao");
    c.signedOff(null, null);
    assertThat(c.getStatus()).isEqualTo(EbComparative.Status.APPROVED);
    assertThatThrownBy(c::thresholdApproved).extracting("code").isEqualTo("EB_COMPARATIVE_STATUS");
    c.presented(NOW, "ao", 7L);
    c.needsThresholdApproval("GLI TSI", "EB_THRESHOLD_APPROVE");
    c.thresholdApproved();
    assertThat(c.getStatus()).isEqualTo(EbComparative.Status.APPROVED);
    c.supersede();
    assertThat(c.getStatus()).isEqualTo(EbComparative.Status.SUPERSEDED);
    assertThat(c.getSummary()).isEqualTo("Best value");
  }

  @Test
  void aConfirmationIsVoidedWithAReason() {
    EbClientConfirmation confirmation =
        new EbClientConfirmation(
            cycle(), 3L, new EbClientConfirmation.Evidence("EMAIL", TODAY, 8L, null));
    EbClientConfirmation.Line line =
        confirmation.addLine(
            new EbClientConfirmation.Choice(1, "HMO", 2L, "INS-MGIC", BigDecimal.TEN, null));
    line.placedAs("ARN-1");
    assertThat(confirmation.getLines().get(0).getAccountArn()).isEqualTo("ARN-1");
    assertThatThrownBy(() -> confirmation.voidWith(null))
        .extracting("code")
        .isEqualTo("EB_CONFIRMATION_REASON_REQUIRED");
    confirmation.voidWith(" wrong ");
    assertThat(confirmation.getStatus()).isEqualTo(EbClientConfirmation.VOIDED);
    assertThat(confirmation.getVoidReason()).isEqualTo("wrong");
  }

  @Test
  void rosterMembersAndMemberChangesKeepTheirRules() {
    EbProgramme programme =
        new EbProgramme(
            1L,
            "EBP-2026-000001",
            new EbProgramme.ClientRef(3L, "CL-1", "Client"),
            new EbProgramme.Profile("Plan", "BDO", EbFunding.EMPLOYER, "ebao", null, true));
    EbRosterVersion version = new EbRosterVersion(programme, 2026, 1, "BLK-1");
    version.counted();
    assertThatThrownBy(() -> version.reject(" ", "ao", NOW))
        .extracting("code")
        .isEqualTo("EB_ROSTER_REASON_REQUIRED");
    version.accept("ao", NOW);
    assertThatThrownBy(() -> version.accept("ao", NOW))
        .extracting("code")
        .isEqualTo("EB_ROSTER_NOT_STAGED");
    version.recount(4);
    assertThat(version.getHeadcount()).isEqualTo(4);

    EbMember member =
        new EbMember(
            version,
            "E-1",
            new EbMember.Data("Cruz", "Ana", LocalDate.of(1990, 1, 1), null, null, "A", null),
            TODAY);
    assertThat(member.getDependants()).isZero();
    assertThat(member.displayName()).isEqualTo("Cruz, Ana");
    member.changePlan("B");
    member.delete(TODAY);
    assertThat(member.getStatus()).isEqualTo(EbMember.Status.DELETED);
    assertThat(member.getPlanCode()).isEqualTo("B");

    EbProgrammeLine line =
        programme.addLine(
            new EbProgrammeLine.Data("HMO", null, "INS-MGIC", null, null, null, null, 5));
    EbMemberChange change =
        new EbMemberChange(
            programme, "EBM-2026-000001", line, new EbMemberChange.Header(2026, "AO", true, null));
    change.addLine(
        new EbMemberChange.LineData(EbMemberChange.Action.DELETE, "E-1", null, TODAY, 5L));
    assertThatThrownBy(
            () -> change.billed(new EbMemberChange.Billing(TODAY, "B", BigDecimal.ONE, false)))
        .extracting("code")
        .isEqualTo("EB_MEMBER_CHANGE_NOT_RELAYED");
    change.relayed(NOW, 1L);
    change.mirror(EbMemberChange.Status.RELAYED, NOW);
    change.billed(new EbMemberChange.Billing(TODAY, "B", BigDecimal.ONE, true));
    change.validated("proc", NOW);
    change.endorsementRaised(9L, "ENR-2026-1");
    change.mirror(EbMemberChange.Status.CLOSED, NOW);
    assertThat(change.getClosedAt()).isEqualTo(NOW);
    assertThat(change.isDirectBilled()).isTrue();
    assertThat(change.getLines().get(0).memberData().planCode()).isNull();
  }

  @Test
  void anSoaKeepsItsInvoicesAndStages() {
    EbProgramme programme =
        new EbProgramme(
            1L,
            "EBP-2026-000001",
            new EbProgramme.ClientRef(3L, "CL-1", "Client"),
            new EbProgramme.Profile("Plan", "BDO", EbFunding.EMPLOYER, "ebao", null, true));
    EbSoa soa =
        new EbSoa(
            programme,
            "EBS-2026-000001",
            new EbSoa.Intake("INS-MGIC", "S-1", TODAY, TODAY, BigDecimal.TEN, "PHP", TODAY, null),
            new EbSoa.StoredFile(3L, "abc"));
    soa.linkInvoices(List.of("INV-2", "INV-1"));
    assertThat(soa.getInvoiceNos()).containsExactly("INV-2", "INV-1");
    soa.validated("proc", NOW);
    soa.released("proc", NOW);
    soa.rejected("OTHERS");
    soa.paidNotified(NOW);
    soa.mirror(EbSoa.Status.RELEASED);
    assertThat(soa.getStatus()).isEqualTo(EbSoa.Status.RELEASED);
    assertThat(soa.getPaidNotifiedAt()).isEqualTo(NOW);
  }
}
