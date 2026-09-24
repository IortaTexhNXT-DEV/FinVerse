package com.iortatechnxt.brokerverse.underwriting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.party.service.PartyService;
import com.iortatechnxt.brokerverse.subledger.domain.ItemDirection;
import com.iortatechnxt.brokerverse.subledger.domain.OpenItem;
import com.iortatechnxt.brokerverse.subledger.service.OpenItemService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.EndorsementRequest;
import com.iortatechnxt.brokerverse.underwriting.domain.Endorsement;
import com.iortatechnxt.brokerverse.underwriting.domain.EndorsementType;
import com.iortatechnxt.brokerverse.underwriting.domain.Policy;
import com.iortatechnxt.brokerverse.underwriting.domain.PolicyStatus;
import com.iortatechnxt.brokerverse.underwriting.domain.Product;
import com.iortatechnxt.brokerverse.underwriting.service.EndorsementService;
import com.iortatechnxt.brokerverse.underwriting.service.PolicyApprovalService;
import com.iortatechnxt.brokerverse.underwriting.service.PolicyQueryService;
import com.iortatechnxt.brokerverse.underwriting.service.PolicyService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class EndorsementIT {

  private static final LocalDate APPROVED_ON = LocalDate.of(2026, 3, 12);

  @Autowired private UwFixtures fx;
  @Autowired private EndorsementService endorsements;
  @Autowired private PolicyApprovalService approvals;
  @Autowired private PolicyService policies;
  @Autowired private PolicyQueryService query;
  @Autowired private OpenItemService openItems;
  @Autowired private PartyService parties;
  @Autowired private AsUser as;

  private Policy policy() {
    Product fire = fx.product("FIRE", false);
    return fx.issue(fx.brokerRequest(fire), APPROVED_ON);
  }

  private Endorsement endorse(Policy p, EndorsementRequest r, LocalDate accountingDate) {
    Endorsement draft = as.run("uw", () -> endorsements.create(p.getId(), r));
    as.run("uw", () -> endorsements.submit(draft.getId()));
    return as.run("fmanager", () -> approvals.approveEndorsement(draft.getId(), accountingDate));
  }

  private static EndorsementRequest request(
      EndorsementType type, LocalDate effective, String gross, String si) {
    return new EndorsementRequest(
        type,
        effective,
        effective,
        type + " endorsement",
        gross == null ? null : new BigDecimal(gross),
        si == null ? null : new BigDecimal(si),
        null,
        null);
  }

  private Optional<OpenItem> item(String partyCode, String sourceReference) {
    Long partyId = parties.getByCode(fx.companyId(), partyCode).getId();
    return openItems.partyItems(fx.companyId(), partyId).stream()
        .filter(i -> sourceReference.equals(i.getSourceReference()))
        .findFirst();
  }

  @Test
  void additionalPremiumRaisesDebitNote() {
    Policy p = policy();
    Endorsement e =
        endorse(
            p,
            request(EndorsementType.ADDITIONAL, LocalDate.of(2026, 6, 1), "20000", "2000000"),
            LocalDate.of(2026, 6, 2));

    assertThat(e.getStatus()).isEqualTo(PolicyStatus.APPROVED);
    assertThat(e.documentNo()).endsWith("/E01");
    assertThat(e.getPremium().getNetPremium()).isEqualByComparingTo("19000.00");
    assertThat(e.getPremium().getPolicyFee()).isZero();
    assertThat(e.getRefs().getDebitNoteNo()).startsWith("DN-");
    assertThat(item("C-000201", "POLICY:" + p.getId() + ":ENDT:1"))
        .get()
        .extracting(OpenItem::getDirection)
        .isEqualTo(ItemDirection.DEBIT);
  }

  @Test
  void refundRaisesCreditNoteAndCommissionRecovery() {
    Policy p = policy();
    Endorsement e =
        endorse(
            p,
            request(EndorsementType.REFUND, LocalDate.of(2026, 5, 1), "10000", "1000000"),
            LocalDate.of(2026, 5, 4));

    assertThat(e.getPremium().getGrossPremium()).isEqualByComparingTo("-10000.00");
    assertThat(e.getPremium().getTotalDue()).isNegative();
    assertThat(e.getRefs().getDebitNoteNo()).startsWith("CN-");
    String key = "POLICY:" + p.getId() + ":ENDT:1";
    assertThat(item("C-000201", key))
        .get()
        .satisfies(
            i -> {
              assertThat(i.getDirection()).isEqualTo(ItemDirection.CREDIT);
              assertThat(i.getDocumentType()).isEqualTo("CREDIT_NOTE");
            });
    assertThat(item("B-0001", key + ":COMM"))
        .get()
        .satisfies(
            i -> {
              assertThat(i.getDirection()).isEqualTo(ItemDirection.DEBIT);
              assertThat(i.getDocumentType()).isEqualTo("COMMISSION_RECOVERY");
              assertThat(i.getAmount()).isEqualByComparingTo("1710.00");
            });
  }

  @Test
  void cancellationReturnsUnexpiredPremiumProRataAndCancelsPolicy() {
    Policy p = policy();
    // period 2026-03-10 .. 2027-03-09 = 365 days; effective 2026-09-10 leaves 181 days
    Endorsement e =
        endorse(
            p,
            request(EndorsementType.CANCELLATION, LocalDate.of(2026, 9, 10), null, null),
            LocalDate.of(2026, 9, 10));

    assertThat(e.getPremium().getGrossPremium()).isEqualByComparingTo("-49589.04");
    assertThat(e.getPremium().getSumInsured()).isEqualByComparingTo("-10000000.00");
    Policy cancelled = policies.get(p.getId());
    assertThat(cancelled.getStatus()).isEqualTo(PolicyStatus.CANCELLED);
    assertThat(cancelled.getCancelledOn()).isEqualTo(LocalDate.of(2026, 9, 10));
    assertThat(query.isInForce(p.getId(), LocalDate.of(2026, 9, 9))).isTrue();
    assertThat(query.isInForce(p.getId(), LocalDate.of(2026, 9, 10))).isFalse();
    assertThat(query.policyTransactions(p.getId())).hasSize(2);
    assertThatThrownBy(
            () ->
                as.run(
                    "uw",
                    () ->
                        endorsements.create(
                            p.getId(),
                            request(EndorsementType.NIL, LocalDate.of(2026, 9, 11), null, null))))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void renewalExtendsPeriodAndNilEndorsementPostsNothing() {
    Policy p = policy();
    Endorsement nil =
        endorse(
            p,
            request(EndorsementType.NIL, LocalDate.of(2026, 4, 1), null, null),
            LocalDate.of(2026, 4, 1));
    assertThat(nil.getRefs().getDebitNoteNo()).isNull();
    assertThat(nil.getPremium().isFinancial()).isFalse();

    EndorsementRequest renewal =
        new EndorsementRequest(
            EndorsementType.RENEWAL,
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2027, 3, 10),
            "Renewal 2027",
            null,
            null,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2028, 3, 9));
    Endorsement renewed = endorse(p, renewal, LocalDate.of(2026, 9, 1));
    assertThat(renewed.getPremium().getGrossPremium()).isEqualByComparingTo("100000.00");
    assertThat(renewed.getPremium().getPolicyFee()).isEqualByComparingTo("250.00");
    assertThat(policies.get(p.getId()).getPeriodTo()).isEqualTo(LocalDate.of(2028, 3, 9));
  }

  @Test
  void onlyOneOpenEndorsementAndMakerCheckerApplies() {
    Policy p = policy();
    Endorsement draft =
        as.run(
            "uw",
            () ->
                endorsements.create(
                    p.getId(),
                    request(EndorsementType.ADDITIONAL, LocalDate.of(2026, 6, 1), "100", "0")));
    assertThatThrownBy(
            () ->
                as.run(
                    "uw",
                    () ->
                        endorsements.create(
                            p.getId(),
                            request(EndorsementType.NIL, LocalDate.of(2026, 6, 1), null, null))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("open endorsement");
    as.run("uw", () -> endorsements.submit(draft.getId()));
    assertThatThrownBy(() -> as.run("uw", () -> approvals.approveEndorsement(draft.getId(), null)))
        .isInstanceOf(BusinessRuleException.class);
    Endorsement rejected =
        as.run("fmanager", () -> approvals.rejectEndorsement(draft.getId(), "Wrong amount"));
    assertThat(rejected.getStatus()).isEqualTo(PolicyStatus.DRAFT);
    Endorsement discarded = as.run("uw", () -> endorsements.discard(draft.getId()));
    assertThat(discarded.getStatus()).isEqualTo(PolicyStatus.CANCELLED);
    assertThat(endorsements.forPolicy(p.getId())).hasSize(1);
    assertThatThrownBy(
            () ->
                as.run(
                    "uw",
                    () ->
                        endorsements.create(
                            p.getId(),
                            request(
                                EndorsementType.ADDITIONAL, LocalDate.of(2025, 1, 1), "100", "0"))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("Effective date");
  }
}
