package com.iortatechnxt.finverse.underwriting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.accounting.domain.AccountingEventLogRepository;
import com.iortatechnxt.finverse.accounting.domain.EventStatus;
import com.iortatechnxt.finverse.coa.service.ChartOfAccountsService;
import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.ledger.service.LedgerQueryService;
import com.iortatechnxt.finverse.party.service.PartyService;
import com.iortatechnxt.finverse.subledger.domain.ItemDirection;
import com.iortatechnxt.finverse.subledger.domain.OpenItem;
import com.iortatechnxt.finverse.subledger.service.OpenItemService;
import com.iortatechnxt.finverse.support.AsUser;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.underwriting.api.dto.PolicyRequest;
import com.iortatechnxt.finverse.underwriting.domain.BusinessType;
import com.iortatechnxt.finverse.underwriting.domain.Policy;
import com.iortatechnxt.finverse.underwriting.domain.PolicyStatus;
import com.iortatechnxt.finverse.underwriting.domain.PremiumBreakdown;
import com.iortatechnxt.finverse.underwriting.domain.Product;
import com.iortatechnxt.finverse.underwriting.domain.SourceType;
import com.iortatechnxt.finverse.underwriting.service.PolicyApprovalService;
import com.iortatechnxt.finverse.underwriting.service.PolicyQueryService;
import com.iortatechnxt.finverse.underwriting.service.PolicyService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

@IntegrationTest
class PolicyApprovalIT {

  private static final LocalDate ACCOUNTING = LocalDate.of(2026, 3, 12);

  @Autowired private UwFixtures fx;
  @Autowired private PolicyService policies;
  @Autowired private PolicyApprovalService approvals;
  @Autowired private PolicyQueryService query;
  @Autowired private LedgerQueryService ledger;
  @Autowired private ChartOfAccountsService accounts;
  @Autowired private OpenItemService openItems;
  @Autowired private PartyService parties;
  @Autowired private AccountingEventLogRepository eventLog;
  @Autowired private AsUser as;

  private BigDecimal balance(String account) {
    Long company = fx.companyId();
    return ledger.netBalance(
        company, accounts.getByCode(company, account).getId(), null, LocalDate.of(2026, 12, 31));
  }

  private List<OpenItem> items(String partyCode, String sourceReference) {
    Long partyId = parties.getByCode(fx.companyId(), partyCode).getId();
    return openItems.partyItems(fx.companyId(), partyId).stream()
        .filter(i -> sourceReference.equals(i.getSourceReference()))
        .toList();
  }

  @Test
  void previewComputesPremiumWithoutSaving() {
    Product fire = fx.product("FIRE", false);
    PremiumBreakdown p = as.run("uw", () -> policies.preview(fx.brokerRequest(fire)));
    assertThat(p.getNetPremium()).isEqualByComparingTo("95000.00");
    assertThat(p.getFst()).isEqualByComparingTo("1900.00");
    assertThat(p.getCommissionRate()).isEqualByComparingTo("20");
    assertThat(p.getTotalDue()).isEqualByComparingTo("121137.50");
  }

  @Test
  void approvalPostsPremiumAndCommissionAndRecordsOpenItems() {
    Product fire = fx.product("FIRE", false);
    BigDecimal receivable = balance("1201");
    BigDecimal commissionPayable = balance("2300");
    BigDecimal premiumIncome = balance("4100");

    Policy policy = fx.issue(fx.brokerRequest(fire), ACCOUNTING);

    assertThat(policy.getStatus()).isEqualTo(PolicyStatus.APPROVED);
    assertThat(policy.getWorkflow().getApprovedBy()).isEqualTo("fmanager");
    assertThat(policy.getRefs().getDebitNoteNo()).startsWith("DN-HO-2026-");
    assertThat(policy.getRefs().getCreditNoteNo()).startsWith("CN-HO-2026-");
    assertThat(policy.getRefs().getPremiumBatchNo()).isNotBlank();
    assertThat(policy.getRefs().getCommissionBatchNo()).isNotBlank();
    assertThat(balance("1201").subtract(receivable)).isEqualByComparingTo("121137.50");
    assertThat(balance("4100").subtract(premiumIncome)).isEqualByComparingTo("-95000.00");
    assertThat(balance("2300").subtract(commissionPayable)).isEqualByComparingTo("-17100.00");

    String key = "POLICY:" + policy.getId();
    List<OpenItem> dn = items("C-000201", key);
    assertThat(dn)
        .singleElement()
        .satisfies(
            i -> {
              assertThat(i.getDirection()).isEqualTo(ItemDirection.DEBIT);
              assertThat(i.getDocumentType()).isEqualTo("DEBIT_NOTE");
              assertThat(i.getAmount()).isEqualByComparingTo("121137.50");
              assertThat(i.getJournalBatchNo()).isEqualTo(policy.getRefs().getPremiumBatchNo());
            });
    assertThat(items("B-0001", key + ":COMM"))
        .singleElement()
        .satisfies(
            i -> {
              assertThat(i.getDirection()).isEqualTo(ItemDirection.CREDIT);
              assertThat(i.getAmount()).isEqualByComparingTo("17100.00");
              assertThat(i.getJournalBatchNo()).isEqualTo(policy.getRefs().getCommissionBatchNo());
            });
    assertThat(
            eventLog
                .search(
                    fx.companyId(),
                    EventStatus.POSTED,
                    null,
                    ACCOUNTING,
                    ACCOUNTING,
                    Pageable.unpaged())
                .getContent())
        .extracting("sourceReference")
        .contains(key, key + ":COMM");

    assertThat(query.findByNumber(fx.companyId(), policy.getPolicyNo())).isPresent();
    assertThat(query.isInForce(policy.getId(), ACCOUNTING)).isTrue();
    assertThat(query.isInForce(policy.getId(), LocalDate.of(2025, 1, 1))).isFalse();
    assertThat(query.risks(policy.getId())).hasSize(1);
    assertThat(query.approvedTransactions(fx.companyId(), ACCOUNTING, ACCOUNTING))
        .anyMatch(t -> t.ref().policyId().equals(policy.getId()));
    assertThat(query.policyTransactions(policy.getId())).hasSize(1);
  }

  @Test
  void leadingCoinsurancePostsCoinsurerShare() {
    Product fire = fx.product("FIRE", false);
    PolicyRequest base = fx.brokerRequest(fire);
    PolicyRequest coinsured =
        new PolicyRequest(
            base.companyId(),
            base.branchId(),
            base.productId(),
            base.customerCode(),
            base.insuredName(),
            SourceType.DIRECT,
            null,
            base.issueDate(),
            base.periodFrom(),
            base.periodTo(),
            "PHP",
            BusinessType.DIRECT_WITH_COINSURANCE,
            new BigDecimal("60"),
            "CO-0001",
            true,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            null,
            base.risks());
    BigDecimal dueToCoinsurers = balance("2203");

    Policy policy = fx.issue(coinsured, ACCOUNTING);

    assertThat(policy.getPremium().getCoinsurerPremium()).isEqualByComparingTo("40000.00");
    assertThat(policy.getPremium().getBilledPremium()).isEqualByComparingTo("100000.00");
    assertThat(balance("2203").subtract(dueToCoinsurers)).isEqualByComparingTo("-40000.00");
    assertThat(items("CO-0001", "POLICY:" + policy.getId() + ":COINS")).hasSize(1);
    assertThat(policy.getRefs().getCommissionBatchNo()).isNull();
  }

  @Test
  void makerCannotApproveOwnPolicyAndRejectionReturnsItToDraft() {
    Product fire = fx.product("FIRE", false);
    Policy draft = as.run("uw", () -> policies.create(fx.brokerRequest(fire)));
    as.run("uw", () -> policies.submit(draft.getId()));

    assertThatThrownBy(() -> as.run("uw", () -> approvals.approvePolicy(draft.getId(), null)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("cannot be approved");

    Policy rejected = as.run("fmanager", () -> approvals.rejectPolicy(draft.getId(), "Check SI"));
    assertThat(rejected.getStatus()).isEqualTo(PolicyStatus.DRAFT);
    assertThat(rejected.getWorkflow().getRejectionReason()).isEqualTo("Check SI");

    Policy updated =
        as.run(
            "uw",
            () ->
                policies.update(
                    draft.getId(),
                    fx.request(
                        fire,
                        SourceType.AGENT,
                        "A-0001",
                        List.of(fx.risk("2000000", "20000", "NCR-2")))));
    assertThat(updated.getPremium().getCommissionRate()).isEqualByComparingTo("15");
    assertThat(updated.getIntermediary().getCode()).isEqualTo("A-0001");

    Policy discarded = as.run("uw", () -> policies.discard(draft.getId()));
    assertThat(discarded.getStatus()).isEqualTo(PolicyStatus.CANCELLED);
    assertThatThrownBy(() -> as.run("uw", () -> policies.submit(draft.getId())))
        .isInstanceOf(BusinessRuleException.class);
  }

  @Test
  void invalidTermsAreRejected() {
    Product fire = fx.product("FIRE", false);
    PolicyRequest noBroker =
        fx.request(fire, SourceType.BROKER, null, List.of(fx.risk("1000", "10", null)));
    assertThatThrownBy(() -> as.run("uw", () -> policies.create(noBroker)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("BROKER");
    PolicyRequest agentAsBroker =
        fx.request(fire, SourceType.BROKER, "A-0001", List.of(fx.risk("1000", "10", null)));
    assertThatThrownBy(() -> as.run("uw", () -> policies.create(agentAsBroker)))
        .isInstanceOf(BusinessRuleException.class);
    PolicyRequest directWithAgent =
        fx.request(fire, SourceType.DIRECT, "A-0001", List.of(fx.risk("1000", "10", null)));
    assertThatThrownBy(() -> as.run("uw", () -> policies.create(directWithAgent)))
        .isInstanceOf(BusinessRuleException.class);
    Product pending =
        as.run(
            "uw",
            () ->
                fx.productService()
                    .create(fx.productRequest(UwFixtures.uniqueCode("P"), "MOTOR", false)));
    assertThatThrownBy(
            () ->
                as.run(
                    "uw",
                    () ->
                        policies.create(
                            fx.request(
                                pending,
                                SourceType.DIRECT,
                                null,
                                List.of(fx.risk("1000", "10", null))))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("not authorized");
  }

  @Test
  void commissionOverrideIsSavedOnCreateAndUpdateLikeThePreviewShowsIt() {
    Product fire = fx.product("FIRE", false);
    PolicyRequest request = withCommission(fx.brokerRequest(fire), "7.5");
    PremiumBreakdown preview = as.run("uw", () -> policies.preview(request));
    Policy draft = as.run("uw", () -> policies.create(request));
    assertThat(draft.getPremium().getCommissionRate()).isEqualByComparingTo("7.5");
    assertThat(draft.getPremium().getCommission())
        .isEqualByComparingTo(preview.getCommission())
        .isEqualByComparingTo("7125.00");

    Policy updated =
        as.run("uw", () -> policies.update(draft.getId(), withCommission(request, "12")));
    assertThat(updated.getPremium().getCommissionRate()).isEqualByComparingTo("12");
    Policy reset =
        as.run("uw", () -> policies.update(draft.getId(), withCommission(request, null)));
    assertThat(reset.getPremium().getCommissionRate()).isEqualByComparingTo("20");

    assertThatThrownBy(
            () ->
                as.run(
                    "uw", () -> policies.update(draft.getId(), withCommission(request, "100.01"))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("between 0 and 100");
    assertThatThrownBy(() -> as.run("uw", () -> policies.create(withCommission(request, "-1"))))
        .isInstanceOf(BusinessRuleException.class);
  }

  private static PolicyRequest withCommission(PolicyRequest r, String rate) {
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
        r.currency(),
        r.businessType(),
        r.sharePct(),
        r.coinsurerCode(),
        r.coinsuranceLeader(),
        r.discountRate(),
        r.loadingRate(),
        rate == null ? null : new BigDecimal(rate),
        r.risks());
  }
}
