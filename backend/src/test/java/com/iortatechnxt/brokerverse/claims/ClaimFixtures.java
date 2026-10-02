package com.iortatechnxt.brokerverse.claims;

import com.iortatechnxt.brokerverse.claims.api.dto.ClaimPartyRequest;
import com.iortatechnxt.brokerverse.claims.api.dto.ClaimRequest;
import com.iortatechnxt.brokerverse.claims.domain.Claim;
import com.iortatechnxt.brokerverse.claims.domain.ClaimPartyRole;
import com.iortatechnxt.brokerverse.claims.domain.CostType;
import com.iortatechnxt.brokerverse.claims.domain.DocumentStatus;
import com.iortatechnxt.brokerverse.claims.domain.EstimateLine;
import com.iortatechnxt.brokerverse.claims.domain.EstimateSide;
import com.iortatechnxt.brokerverse.claims.domain.ReserveChange;
import com.iortatechnxt.brokerverse.claims.domain.Settlement;
import com.iortatechnxt.brokerverse.claims.domain.SettlementType;
import com.iortatechnxt.brokerverse.claims.service.ClaimService;
import com.iortatechnxt.brokerverse.claims.service.ReserveService;
import com.iortatechnxt.brokerverse.claims.service.SettlementCommand;
import com.iortatechnxt.brokerverse.claims.service.SettlementService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.underwriting.UwFixtures;
import com.iortatechnxt.brokerverse.underwriting.api.dto.PolicyRequest;
import com.iortatechnxt.brokerverse.underwriting.domain.BusinessType;
import com.iortatechnxt.brokerverse.underwriting.domain.Policy;
import com.iortatechnxt.brokerverse.underwriting.domain.Product;
import com.iortatechnxt.brokerverse.underwriting.domain.SourceType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Builds claims test data through the services: policies via {@link UwFixtures}, claims entered by
 * the claims officer "claims" and approved by the finance manager "fmanager".
 */
@Component
public class ClaimFixtures {

  public static final String MAKER = "claims";
  public static final String CHECKER = "fmanager";
  public static final LocalDate LOSS = LocalDate.of(2026, 4, 2);
  public static final LocalDate REPORTED = LocalDate.of(2026, 4, 5);
  public static final LocalDate APPROVED = LocalDate.of(2026, 4, 8);

  private final UwFixtures uw;
  private final ClaimService claims;
  private final ReserveService reserves;
  private final SettlementService settlements;
  private final AsUser as;
  private final JdbcTemplate jdbc;

  ClaimFixtures(
      UwFixtures uw,
      ClaimService claims,
      ReserveService reserves,
      SettlementService settlements,
      AsUser as,
      JdbcTemplate jdbc) {
    this.uw = uw;
    this.claims = claims;
    this.reserves = reserves;
    this.settlements = settlements;
    this.as = as;
    this.jdbc = jdbc;
  }

  public Long companyId() {
    return uw.companyId();
  }

  /** Approved PHP policy of a line of business (share 100 %). */
  public Policy policy(String lob) {
    Product product = uw.product(lob, false);
    return uw.issue(
        uw.request(product, SourceType.DIRECT, null, List.of(uw.risk("10000000", "100000", "Z1"))),
        UwFixtures.ISSUE);
  }

  /** Approved coinsured PHP fire policy, company share 60 %. */
  public Policy coinsuredPolicy(boolean leader) {
    return uw.issue(variant(uw.product("FIRE", false), "PHP", true, leader), UwFixtures.ISSUE);
  }

  /** Approved USD marine policy. */
  public Policy usdPolicy() {
    return uw.issue(variant(uw.product("MARINE", false), "USD", false, false), UwFixtures.ISSUE);
  }

  private PolicyRequest variant(Product product, String currency, boolean coins, boolean leader) {
    PolicyRequest b =
        uw.request(product, SourceType.DIRECT, null, List.of(uw.risk("200000", "2000", "Z2")));
    return new PolicyRequest(
        b.companyId(),
        b.branchId(),
        b.productId(),
        b.customerCode(),
        b.insuredName(),
        b.sourceType(),
        null,
        b.issueDate(),
        b.periodFrom(),
        b.periodTo(),
        currency,
        coins ? BusinessType.DIRECT_WITH_COINSURANCE : BusinessType.DIRECT,
        new BigDecimal(coins ? "60" : "100"),
        coins ? "CO-0001" : null,
        leader,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        null,
        b.risks());
  }

  /** Notification request with optional initial reserves. */
  public ClaimRequest request(Policy policy, String loss, String expense) {
    return new ClaimRequest(
        companyId(),
        policy.getPolicyNo(),
        1,
        LOSS,
        REPORTED,
        "Fire",
        "Electrical short circuit",
        "Calamba, Laguna",
        "Fire damaged stocks",
        policy.getCurrency(),
        null,
        List.of(new ClaimPartyRequest(ClaimPartyRole.SURVEYOR, "SV-0001")),
        loss == null ? null : new BigDecimal(loss),
        expense == null ? null : new BigDecimal(expense));
  }

  /** Registered claim with its initial reserves approved (status OPEN). */
  public Claim openClaim(Policy policy, String loss, String expense) {
    Claim claim = as.run(MAKER, () -> claims.register(request(policy, loss, expense)));
    approvePending(claim.getId());
    return as.run(CHECKER, () -> claims.get(claim.getId()));
  }

  /** Approves every pending reserve change of a claim as the finance manager. */
  public void approvePending(Long claimId) {
    for (ReserveChange rc : reserves.forClaim(claimId)) {
      if (rc.getApproval().getStatus() == DocumentStatus.PENDING_APPROVAL) {
        as.run(CHECKER, () -> reserves.approve(rc.getId(), APPROVED));
      }
    }
  }

  /** Requested (pending) payment reserve change. */
  public ReserveChange requestReserve(Long claimId, CostType cost, String amount) {
    return as.run(
        MAKER,
        () ->
            reserves.request(
                claimId,
                new EstimateLine(EstimateSide.PAYMENT, cost, new BigDecimal(amount)),
                "Test change"));
  }

  /** Entered (pending) settlement. */
  public Settlement enterSettlement(
      Long claimId,
      String payee,
      CostType cost,
      SettlementType type,
      String assessed,
      String deductible) {
    return as.run(
        MAKER,
        () ->
            settlements.create(
                claimId,
                new SettlementCommand(
                    payee,
                    cost,
                    type,
                    new BigDecimal(assessed),
                    deductible == null ? null : new BigDecimal(deductible),
                    null,
                    "Test settlement")));
  }

  /** Entered and approved settlement. */
  public Settlement settle(
      Long claimId,
      String payee,
      CostType cost,
      SettlementType type,
      String assessed,
      String deductible) {
    Settlement s = enterSettlement(claimId, payee, cost, type, assessed, deductible);
    return as.run(CHECKER, () -> settlements.approve(s.getId(), APPROVED.plusDays(10)));
  }

  /** Signed (debit − credit) base amount posted to an account by a journal batch. */
  public BigDecimal posted(String batchNo, String account) {
    BigDecimal value =
        jdbc.queryForObject(
            """
            select coalesce(sum(e.debit_base - e.credit_base), 0) from gl_ledger_entry e
            join coa_account a on a.id = e.account_id
            where e.batch_no = ? and a.code = ?
            """,
            BigDecimal.class,
            batchNo,
            account);
    return value.setScale(2, RoundingMode.HALF_EVEN);
  }
}
