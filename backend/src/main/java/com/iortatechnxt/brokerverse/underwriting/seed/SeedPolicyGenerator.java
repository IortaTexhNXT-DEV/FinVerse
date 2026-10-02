package com.iortatechnxt.brokerverse.underwriting.seed;

import com.iortatechnxt.brokerverse.underwriting.api.dto.EndorsementRequest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.PolicyRequest;
import com.iortatechnxt.brokerverse.underwriting.domain.Endorsement;
import com.iortatechnxt.brokerverse.underwriting.domain.EndorsementType;
import com.iortatechnxt.brokerverse.underwriting.domain.Policy;
import com.iortatechnxt.brokerverse.underwriting.domain.Product;
import com.iortatechnxt.brokerverse.underwriting.seed.SeedCatalog.ProductProfile;
import com.iortatechnxt.brokerverse.underwriting.service.EndorsementService;
import com.iortatechnxt.brokerverse.underwriting.service.PolicyApprovalService;
import com.iortatechnxt.brokerverse.underwriting.service.PolicyService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generates the seed policy portfolio (January - September 2026) through the underwriting services:
 * most policies approved (premium, commission and coinsurance posted, debit notes and open items
 * recorded), a few pending or in draft, with additional, refund, NIL, renewal and cancellation
 * endorsements.
 */
final class SeedPolicyGenerator {

  static final String MAKER = "uw";
  static final String CHECKER = "fmanager";

  private static final int FIRST_PENDING = 138;
  private static final int FIRST_DRAFT = 146;
  private static final LocalDate LAST_POSTING = LocalDate.of(2026, 9, 22);
  private static final int PRODUCTS = SeedCatalog.PRODUCTS.size();
  private static final int APPROVAL_LAG_DAYS = 3;
  private static final int ADDITIONAL_CYCLE = 7;
  private static final int ADDITIONAL_SLOT = 3;
  private static final int ADDITIONAL_AFTER_DAYS = 45;
  private static final int REFUND_CYCLE = 11;
  private static final int REFUND_SLOT = 5;
  private static final int REFUND_AFTER_DAYS = 30;
  private static final int NIL_CYCLE = 13;
  private static final int NIL_SLOT = 6;
  private static final int NIL_AFTER_DAYS = 20;
  private static final int CANCEL_CYCLE = 17;
  private static final int CANCEL_SLOT = 9;
  private static final int CANCEL_AFTER_DAYS = 90;
  private static final int RENEWAL_NOTICE_DAYS = 5;

  private final SeedUserContext users;
  private final PolicyService policies;
  private final PolicyApprovalService approvals;
  private final EndorsementService endorsements;

  SeedPolicyGenerator(
      SeedUserContext users,
      PolicyService policies,
      PolicyApprovalService approvals,
      EndorsementService endorsements) {
    this.users = users;
    this.policies = policies;
    this.approvals = approvals;
    this.endorsements = endorsements;
  }

  /**
   * Creates the portfolio.
   *
   * @param companyId company
   * @param branches branch ids by code
   * @param products products by code
   * @return policies created
   */
  List<Policy> generate(Long companyId, Map<String, Long> branches, Map<String, Product> products) {
    List<Policy> created = new ArrayList<>();
    for (int i = 0; i < SeedPolicyRequests.POLICY_COUNT; i++) {
      ProductProfile profile = SeedCatalog.PRODUCTS.get(i % PRODUCTS);
      PolicyRequest request =
          SeedPolicyRequests.request(i, companyId, branches, products.get(profile.code()), profile);
      Policy draft = users.runAs(MAKER, () -> policies.create(request));
      Policy current =
          i < FIRST_DRAFT ? users.runAs(MAKER, () -> policies.submit(draft.getId())) : draft;
      if (i < FIRST_PENDING) {
        LocalDate approval = capped(request.issueDate().plusDays(i % APPROVAL_LAG_DAYS));
        Policy approved =
            users.runAs(CHECKER, () -> approvals.approvePolicy(draft.getId(), approval));
        endorse(i, approved);
        created.add(approved);
      } else {
        created.add(current);
      }
    }
    return created;
  }

  private void endorse(int i, Policy policy) {
    LocalDate issue = policy.getIssueDate();
    BigDecimal gross = policy.getPremium().getGrossPremium();
    BigDecimal si = policy.getPremium().getSumInsured();
    if (i % ADDITIONAL_CYCLE == ADDITIONAL_SLOT) {
      change(
          policy,
          EndorsementType.ADDITIONAL,
          issue.plusDays(ADDITIONAL_AFTER_DAYS),
          tenth(gross),
          tenth(si));
    } else if (i % REFUND_CYCLE == REFUND_SLOT) {
      BigDecimal refund = tenth(gross).divide(BigDecimal.TWO, 2, RoundingMode.HALF_EVEN);
      change(policy, EndorsementType.REFUND, issue.plusDays(REFUND_AFTER_DAYS), refund, null);
    } else if (i % NIL_CYCLE == NIL_SLOT) {
      change(policy, EndorsementType.NIL, issue.plusDays(NIL_AFTER_DAYS), null, null);
    }
    if (i % SeedPolicyRequests.SHORT_TERM_CYCLE == 1
        && policy.getPeriodTo().isBefore(LAST_POSTING)) {
      renew(policy);
    }
    if (i % CANCEL_CYCLE == CANCEL_SLOT) {
      change(policy, EndorsementType.CANCELLATION, issue.plusDays(CANCEL_AFTER_DAYS), null, null);
    }
  }

  private void change(
      Policy policy, EndorsementType type, LocalDate effective, BigDecimal gross, BigDecimal si) {
    if (effective.isAfter(LAST_POSTING.minusDays(2)) || effective.isAfter(policy.getPeriodTo())) {
      return;
    }
    EndorsementRequest r =
        new EndorsementRequest(
            type, effective, effective, description(type), gross, si, null, null);
    approve(policy, r, effective.plusDays(1));
  }

  private void renew(Policy policy) {
    LocalDate from = policy.getPeriodTo().plusDays(1);
    EndorsementRequest r =
        new EndorsementRequest(
            EndorsementType.RENEWAL,
            policy.getPeriodTo().minusDays(RENEWAL_NOTICE_DAYS),
            from,
            "Renewal for a further term",
            null,
            null,
            from,
            from.plusYears(1).minusDays(1));
    approve(policy, r, policy.getPeriodTo().minusDays(RENEWAL_NOTICE_DAYS - 1L));
  }

  private void approve(Policy policy, EndorsementRequest r, LocalDate accountingDate) {
    Endorsement draft = users.runAs(MAKER, () -> endorsements.create(policy.getId(), r));
    users.runAs(MAKER, () -> endorsements.submit(draft.getId()));
    users.runAs(CHECKER, () -> approvals.approveEndorsement(draft.getId(), capped(accountingDate)));
  }

  private static String description(EndorsementType type) {
    return switch (type) {
      case ADDITIONAL -> "Increase in sum insured - additional stocks";
      case REFUND -> "Reduction of sum insured - return premium";
      case NIL -> "Change of mailing address of the insured";
      case CANCELLATION -> "Cancellation at the request of the insured (pro-rata)";
      case RENEWAL -> "Renewal";
    };
  }

  private static BigDecimal tenth(BigDecimal amount) {
    return amount.divide(BigDecimal.TEN, 2, RoundingMode.HALF_EVEN);
  }

  private static LocalDate capped(LocalDate date) {
    return date.isAfter(LAST_POSTING) ? LAST_POSTING : date;
  }
}
