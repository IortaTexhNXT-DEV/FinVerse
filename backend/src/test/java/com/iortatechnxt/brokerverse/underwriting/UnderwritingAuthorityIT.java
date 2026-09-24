package com.iortatechnxt.brokerverse.underwriting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.approval.service.ApprovalInboxService;
import com.iortatechnxt.brokerverse.approval.service.PendingApproval;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.EndorsementRequest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.IterationRequest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.PolicyRequest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.QuotationRequest;
import com.iortatechnxt.brokerverse.underwriting.domain.Endorsement;
import com.iortatechnxt.brokerverse.underwriting.domain.EndorsementType;
import com.iortatechnxt.brokerverse.underwriting.domain.Policy;
import com.iortatechnxt.brokerverse.underwriting.domain.PolicyStatus;
import com.iortatechnxt.brokerverse.underwriting.domain.Product;
import com.iortatechnxt.brokerverse.underwriting.domain.Quotation;
import com.iortatechnxt.brokerverse.underwriting.domain.QuotationStatus;
import com.iortatechnxt.brokerverse.underwriting.domain.SourceType;
import com.iortatechnxt.brokerverse.underwriting.service.EndorsementService;
import com.iortatechnxt.brokerverse.underwriting.service.PolicyApprovalService;
import com.iortatechnxt.brokerverse.underwriting.service.PolicyService;
import com.iortatechnxt.brokerverse.underwriting.service.QuotationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Authorization limit on underwriting approvals: an approver with a limit of 50,000.00 (base
 * currency, PHP) can neither approve nor see in My Approvals a policy, endorsement or quotation
 * whose gross premium at 100 % is above it; an approver without a limit (the demo "fmanager") can.
 */
@IntegrationTest
class UnderwritingAuthorityIT {

  private static final AtomicInteger SEQ = new AtomicInteger();
  private static final String LIMIT = "50000";
  private static final String LIMIT_EXCEEDED = "AUTHORIZATION_LIMIT_EXCEEDED";

  @Autowired private UwFixtures fx;
  @Autowired private PolicyService policies;
  @Autowired private PolicyApprovalService approvals;
  @Autowired private EndorsementService endorsements;
  @Autowired private QuotationService quotations;
  @Autowired private ApprovalInboxService inbox;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  private String limited;

  /** A fresh underwriting approver (role UNDERWRITER) with an authorization limit. */
  @BeforeEach
  void createLimitedApprover() {
    limited = "uwlim" + SEQ.incrementAndGet() + System.nanoTime() % 100_000;
    jdbc.update(
        """
        insert into sec_user (username, full_name, email, password_hash, authorization_limit,
            home_branch_id, created_at, created_by)
        select ?, 'Limited Underwriting Approver', ? || '@brokerverse-test.ph', password_hash, ?,
            home_branch_id, now(), 'TEST'
        from sec_user where username = 'uw'
        """,
        limited,
        limited,
        new BigDecimal(LIMIT));
    jdbc.update(
        """
        insert into sec_user_role (user_id, role_id)
        select u.id, r.id from sec_user u, sec_role r
        where u.username = ? and r.code = 'UNDERWRITER'
        """,
        limited);
  }

  private List<PendingApproval> inboxOf(String user) {
    return as.run(user, () -> inbox.inbox(fx.companyId()));
  }

  private static boolean lists(List<PendingApproval> items, String reference) {
    return items.stream().anyMatch(i -> reference.equals(i.reference()));
  }

  private Policy pending(PolicyRequest request) {
    Policy draft = as.run("uw", () -> policies.create(request));
    return as.run("uw", () -> policies.submit(draft.getId()));
  }

  @Test
  void policiesAboveTheLimitAreRefusedAndHiddenFromTheInbox() {
    Product fire = fx.product("FIRE", false);
    Policy large = pending(fx.brokerRequest(fire));
    Policy small =
        pending(
            fx.request(
                fire, SourceType.BROKER, "B-0001", List.of(fx.risk("2000000", "40000", "NCR-1"))));

    assertThat(lists(inboxOf(limited), large.getPolicyNo())).isFalse();
    assertThat(lists(inboxOf(limited), small.getPolicyNo())).isTrue();
    assertThat(lists(inboxOf("fmanager"), large.getPolicyNo())).isTrue();
    assertThat(inbox.pendingAll()).anyMatch(i -> large.getPolicyNo().equals(i.reference()));

    assertThatThrownBy(
            () -> as.run(limited, () -> approvals.approvePolicy(large.getId(), UwFixtures.ISSUE)))
        .isInstanceOf(BusinessRuleException.class)
        .hasFieldOrPropertyWithValue("code", LIMIT_EXCEEDED)
        .hasMessage(
            "Policy "
                + large.getPolicyNo()
                + ": gross premium 100000.00 exceeds your authorization limit 50000.00");
    assertThat(policies.get(large.getId()).getStatus()).isEqualTo(PolicyStatus.PENDING_APPROVAL);

    Policy approved =
        as.run(limited, () -> approvals.approvePolicy(small.getId(), UwFixtures.ISSUE));
    assertThat(approved.getStatus()).isEqualTo(PolicyStatus.APPROVED);
    assertThat(approved.getWorkflow().getApprovedBy()).isEqualTo(limited);
    assertThat(
            as.run("fmanager", () -> approvals.approvePolicy(large.getId(), UwFixtures.ISSUE))
                .getStatus())
        .isEqualTo(PolicyStatus.APPROVED);
  }

  @Test
  void foreignCurrencyPremiumIsConvertedAtTheApprovalRate() {
    Product fire = fx.product("FIRE", false);
    PolicyRequest php =
        fx.request(fire, SourceType.BROKER, "B-0001", List.of(fx.risk("500000", "1000", "NCR-1")));
    Policy usd = pending(inUsd(php));

    // USD 1,000.00 is within 50,000.00 nominally, but about PHP 57,850 at the SPOT rate.
    assertThat(lists(inboxOf(limited), usd.getPolicyNo())).isFalse();
    assertThatThrownBy(
            () -> as.run(limited, () -> approvals.approvePolicy(usd.getId(), UwFixtures.ISSUE)))
        .isInstanceOf(BusinessRuleException.class)
        .hasFieldOrPropertyWithValue("code", LIMIT_EXCEEDED)
        .hasMessageContaining("exceeds your authorization limit 50000.00");
  }

  @Test
  void endorsementsAreLimitedOnTheAbsoluteGrossPremium() {
    Policy policy = fx.issue(fx.brokerRequest(fx.product("FIRE", false)), UwFixtures.ISSUE);
    Endorsement additional = pendingEndorsement(policy, EndorsementType.ADDITIONAL, "20000");
    assertThat(lists(inboxOf(limited), additional.documentNo())).isTrue();
    assertThat(
            as.run(
                    limited,
                    () -> approvals.approveEndorsement(additional.getId(), UwFixtures.ISSUE))
                .getStatus())
        .isEqualTo(PolicyStatus.APPROVED);

    Endorsement refund = pendingEndorsement(policy, EndorsementType.REFUND, "60000");
    assertThat(refund.getPremium().getGrossPremium()).isEqualByComparingTo("-60000");
    assertThat(lists(inboxOf(limited), refund.documentNo())).isFalse();
    assertThat(lists(inboxOf("fmanager"), refund.documentNo())).isTrue();
    assertThatThrownBy(
            () ->
                as.run(
                    limited, () -> approvals.approveEndorsement(refund.getId(), UwFixtures.ISSUE)))
        .isInstanceOf(BusinessRuleException.class)
        .hasFieldOrPropertyWithValue("code", LIMIT_EXCEEDED)
        .hasMessageContaining(refund.documentNo() + ": gross premium 60000.00");
    assertThat(endorsements.get(refund.getId()).getStatus())
        .isEqualTo(PolicyStatus.PENDING_APPROVAL);
  }

  @Test
  void quotationsAreLimitedOnTheOfferedGrossPremium() {
    Quotation q = as.run("uw", () -> quotations.create(quotation(fx.product("MOTOR", false))));
    as.run("uw", () -> quotations.submit(q.getId()));

    assertThat(lists(inboxOf(limited), q.getQuotationNo())).isFalse();
    assertThat(lists(inboxOf("fmanager"), q.getQuotationNo())).isTrue();
    assertThatThrownBy(() -> as.run(limited, () -> quotations.approve(q.getId())))
        .isInstanceOf(BusinessRuleException.class)
        .hasFieldOrPropertyWithValue("code", LIMIT_EXCEEDED)
        .hasMessage(
            "Quotation "
                + q.getQuotationNo()
                + ": gross premium 250000.00 exceeds your authorization limit 50000.00");
    assertThat(quotations.get(q.getId()).getStatus()).isEqualTo(QuotationStatus.PENDING_APPROVAL);
    assertThat(as.run("fmanager", () -> quotations.approve(q.getId())).getStatus())
        .isEqualTo(QuotationStatus.APPROVED);
  }

  private Endorsement pendingEndorsement(Policy policy, EndorsementType type, String gross) {
    LocalDate effective = UwFixtures.ISSUE.plusMonths(2);
    Endorsement draft =
        as.run(
            "uw",
            () ->
                endorsements.create(
                    policy.getId(),
                    new EndorsementRequest(
                        type,
                        effective,
                        effective,
                        type + " premium",
                        new BigDecimal(gross),
                        BigDecimal.ZERO,
                        null,
                        null)));
    return as.run("uw", () -> endorsements.submit(draft.getId()));
  }

  private static PolicyRequest inUsd(PolicyRequest r) {
    return new PolicyRequest(
        r.companyId(),
        r.branchId(),
        r.productId(),
        r.customerCode(),
        r.insuredName(),
        r.sourceType(),
        r.intermediaryCode(),
        r.issueDate(),
        r.periodFrom(),
        r.periodTo(),
        "USD",
        r.businessType(),
        r.sharePct(),
        r.coinsurerCode(),
        r.coinsuranceLeader(),
        r.discountRate(),
        r.loadingRate(),
        r.commissionRate(),
        r.risks());
  }

  private QuotationRequest quotation(Product product) {
    return new QuotationRequest(
        fx.companyId(),
        fx.branchId(),
        product.getId(),
        "C-000204",
        "Metro Retail Holdings Corp.",
        SourceType.DIRECT,
        null,
        UwFixtures.ISSUE,
        30,
        UwFixtures.ISSUE,
        UwFixtures.ISSUE.plusYears(1).minusDays(1),
        "PHP",
        new BigDecimal("100"),
        null,
        new IterationRequest(
            new BigDecimal("5000000"),
            new BigDecimal("250000"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            "First offer"));
  }
}
