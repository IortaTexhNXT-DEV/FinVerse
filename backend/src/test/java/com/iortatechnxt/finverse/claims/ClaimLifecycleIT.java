package com.iortatechnxt.finverse.claims;

import static com.iortatechnxt.finverse.claims.ClaimFixtures.CHECKER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.claims.domain.Claim;
import com.iortatechnxt.finverse.claims.domain.ClaimPartyRole;
import com.iortatechnxt.finverse.claims.domain.ClaimStatus;
import com.iortatechnxt.finverse.claims.domain.CostType;
import com.iortatechnxt.finverse.claims.domain.DocumentStatus;
import com.iortatechnxt.finverse.claims.domain.EstimateType;
import com.iortatechnxt.finverse.claims.domain.MovementKind;
import com.iortatechnxt.finverse.claims.domain.MovementLine;
import com.iortatechnxt.finverse.claims.domain.ReserveChange;
import com.iortatechnxt.finverse.claims.domain.Settlement;
import com.iortatechnxt.finverse.claims.domain.SettlementType;
import com.iortatechnxt.finverse.claims.service.ClaimLifecycleService;
import com.iortatechnxt.finverse.claims.service.ClaimPostingService;
import com.iortatechnxt.finverse.claims.service.ClaimQueryService;
import com.iortatechnxt.finverse.claims.service.ClaimService;
import com.iortatechnxt.finverse.claims.service.ReserveService;
import com.iortatechnxt.finverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.finverse.ledger.service.LedgerQueryService;
import com.iortatechnxt.finverse.party.domain.PartyType;
import com.iortatechnxt.finverse.party.service.PartyService;
import com.iortatechnxt.finverse.payables.PayablesFixtures;
import com.iortatechnxt.finverse.payables.domain.PaymentCategory;
import com.iortatechnxt.finverse.payables.domain.PaymentMode;
import com.iortatechnxt.finverse.payables.domain.PaymentVoucher;
import com.iortatechnxt.finverse.payables.service.PaymentVoucherService;
import com.iortatechnxt.finverse.subledger.domain.ItemDirection;
import com.iortatechnxt.finverse.subledger.domain.OpenItem;
import com.iortatechnxt.finverse.subledger.domain.OpenItemStatus;
import com.iortatechnxt.finverse.subledger.service.OpenItemService;
import com.iortatechnxt.finverse.support.AsUser;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.support.TestParties;
import com.iortatechnxt.finverse.underwriting.domain.Policy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Full claim lifecycle with its accounting, open items and payment through payables. */
@IntegrationTest
class ClaimLifecycleIT {

  private static final LocalDate YEAR_END = LocalDate.of(2026, 12, 31);

  @Autowired private ClaimFixtures fx;
  @Autowired private ClaimService claims;
  @Autowired private ReserveService reserves;
  @Autowired private ClaimLifecycleService lifecycle;
  @Autowired private ClaimQueryService query;
  @Autowired private LedgerQueryService ledger;
  @Autowired private ChartOfAccountsService accounts;
  @Autowired private OpenItemService openItems;
  @Autowired private PartyService parties;
  @Autowired private PaymentVoucherService vouchers;
  @Autowired private PayablesFixtures payables;
  @Autowired private AsUser as;
  @Autowired private TestParties testParties;

  private BigDecimal balance(String account) {
    Long company = fx.companyId();
    return ledger.netBalance(company, accounts.getByCode(company, account).getId(), null, YEAR_END);
  }

  private List<OpenItem> items(String partyCode, String documentNo) {
    Long partyId = parties.getByCode(fx.companyId(), partyCode).getId();
    return openItems.partyItems(fx.companyId(), partyId).stream()
        .filter(i -> documentNo.equals(i.getDocumentNo()))
        .toList();
  }

  @Test
  void registrationReserveSettlementAndClosingPostTheLedger() {
    Policy policy = fx.policy("FIRE");
    BigDecimal reserve = balance("2102");
    BigDecimal change = balance("5200");
    BigDecimal paid = balance("5100");
    BigDecimal payable = balance("2204");

    Claim claim =
        as.run(ClaimFixtures.MAKER, () -> claims.register(fx.request(policy, "1000000", "50000")));
    assertThat(claim.getClaimNo()).matches("CL-HO-2026-\\d{6}");
    assertThat(claim.getStatus()).isEqualTo(ClaimStatus.REGISTERED);
    assertThat(claim.getParties())
        .extracting(p -> p.getRole())
        .containsExactlyInAnyOrder(ClaimPartyRole.CLAIMANT, ClaimPartyRole.SURVEYOR);
    assertThat(reserves.forClaim(claim.getId()))
        .hasSize(2)
        .allMatch(r -> r.getApproval().getStatus() == DocumentStatus.PENDING_APPROVAL);

    fx.approvePending(claim.getId());
    Claim open = as.run(CHECKER, () -> claims.get(claim.getId()));
    assertThat(open.getStatus()).isEqualTo(ClaimStatus.OPEN);
    assertThat(balance("2102").subtract(reserve)).isEqualByComparingTo("-1050000.00");
    assertThat(balance("5200").subtract(change)).isEqualByComparingTo("1050000.00");

    ReserveChange increase = fx.requestReserve(claim.getId(), CostType.LOSS, "1200000");
    as.run(CHECKER, () -> reserves.approve(increase.getId(), ClaimFixtures.APPROVED));
    assertThat(balance("2102").subtract(reserve)).isEqualByComparingTo("-1250000.00");

    Settlement partial =
        fx.settle(
            claim.getId(), "C-000201", CostType.LOSS, SettlementType.PARTIAL, "400000", "20000");
    assertThat(partial.getNetAmount()).isEqualByComparingTo("380000.00");
    assertThat(partial.getOurAmount()).isEqualByComparingTo("380000.00");
    assertThat(fx.posted(partial.getJournalBatchNo(), "5100")).isEqualByComparingTo("380000.00");
    assertThat(fx.posted(partial.getJournalBatchNo(), "2204")).isEqualByComparingTo("-380000.00");
    assertThat(fx.posted(partial.getJournalBatchNo(), "2102")).isEqualByComparingTo("380000.00");
    assertThat(as.run(CHECKER, () -> claims.get(claim.getId())).getStatus())
        .isEqualTo(ClaimStatus.PARTIALLY_SETTLED);

    assertThat(items("C-000201", partial.getSettlementNo()))
        .singleElement()
        .satisfies(
            i -> {
              assertThat(i.getDirection()).isEqualTo(ItemDirection.CREDIT);
              assertThat(i.getDocumentType()).isEqualTo(ClaimPostingService.SETTLEMENT_DOCUMENT);
              assertThat(i.getAmount()).isEqualByComparingTo("380000.00");
              assertThat(i.getSourceModule()).isEqualTo("CLAIMS");
            });

    fx.settle(claim.getId(), "SV-0001", CostType.EXPENSE, SettlementType.PARTIAL, "30000", null);
    Settlement last =
        fx.settle(claim.getId(), "C-000201", CostType.LOSS, SettlementType.FINAL, "500000", null);
    Claim closed = as.run(CHECKER, () -> claims.get(claim.getId()));
    assertThat(closed.getStatus()).isEqualTo(ClaimStatus.CLOSED);
    assertThat(closed.getClosedOn()).isEqualTo(last.getApproval().getApprovalDate());
    assertThat(closed.ourShare().outstanding()).isEqualByComparingTo("0");
    assertThat(balance("2102")).isEqualByComparingTo(reserve);
    assertThat(balance("5100").subtract(paid)).isEqualByComparingTo("910000.00");
    assertThat(balance("2204").subtract(payable)).isEqualByComparingTo("-910000.00");
    assertThat(balance("5200").subtract(change)).isEqualByComparingTo("0.00");
    assertThat(reserves.forClaim(claim.getId())).anyMatch(ReserveChange::isSystemGenerated);

    List<MovementLine> lines = query.movements(claim.getId());
    assertThat(lines).extracting(MovementLine::getEstimateType).contains(1, 3);
    assertThat(lines.stream().filter(l -> l.getKind() == MovementKind.PAID))
        .hasSize(3)
        .allMatch(l -> l.getEstimateType() == EstimateType.PAYMENT.code());

    Claim reopened = as.run(CHECKER, () -> lifecycle.reopen(claim.getId(), "Hidden damage"));
    assertThat(reopened.getStatus()).isEqualTo(ClaimStatus.REOPENED);
    assertThat(reopened.getClosedOn()).isNull();
    assertThatThrownBy(
            () ->
                as.run(
                    CHECKER,
                    () -> lifecycle.decline(claim.getId(), ClaimStatus.WITHDRAWN, "x", null)))
        .hasMessageContaining("has settlements");
  }

  @Test
  void settlementOpenItemIsPaidByPayablesAsClaimPayment() {
    Policy policy = fx.policy("FIRE");
    Claim claim = fx.openClaim(policy, "200000", null);
    String payee = testParties.create(PartyType.CORPORATE_CLIENT).getCode();
    Settlement s =
        fx.settle(claim.getId(), payee, CostType.LOSS, SettlementType.PARTIAL, "150000", null);
    OpenItem item = items(payee, s.getSettlementNo()).get(0);

    assertThat(vouchers.payableItems(fx.companyId(), payee, null))
        .anyMatch(p -> p.item().getId().equals(item.getId()));
    assertThat(
            PaymentCategory.defaultFor(PartyType.CORPORATE_CLIENT, Set.of(item.getDocumentType())))
        .isEqualTo(PaymentCategory.CLAIM);

    PaymentVoucher voucher =
        payables.approvedPayment(
            payables.paymentCommand(
                payee,
                PaymentMode.BANK_TRANSFER,
                "BDO-CA",
                PayablesFixtures.DATE,
                null,
                Map.of(item.getId(), new BigDecimal("150000.00"))));
    assertThat(voucher.getCategory()).isEqualTo(PaymentCategory.CLAIM);
    assertThat(payables.posted(voucher.getJournalBatchNo(), "2204"))
        .isEqualByComparingTo("150000.00");
    assertThat(openItems.get(item.getId()).getStatus()).isEqualTo(OpenItemStatus.SETTLED);
  }

  @Test
  void closingReleasesTheReserveAndDecisionsAreCheckerOnly() {
    Policy policy = fx.policy("FIRE");
    Claim claim = fx.openClaim(policy, "300000", "10000");
    BigDecimal reserve = balance("2102");

    Claim closed =
        as.run(
            CHECKER,
            () -> lifecycle.close(claim.getId(), "Nothing payable", ClaimFixtures.APPROVED));

    assertThat(closed.getStatus()).isEqualTo(ClaimStatus.CLOSED);
    assertThat(closed.getStatusReason()).isEqualTo("Nothing payable");
    assertThat(balance("2102").subtract(reserve)).isEqualByComparingTo("310000.00");
    assertThat(closed.getTotals().getEstimateLoss()).isEqualByComparingTo("0");
  }
}
