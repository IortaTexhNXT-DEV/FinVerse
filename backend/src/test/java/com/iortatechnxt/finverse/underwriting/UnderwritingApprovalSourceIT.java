package com.iortatechnxt.finverse.underwriting;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.finverse.approval.service.ApprovalInboxService;
import com.iortatechnxt.finverse.approval.service.PendingApproval;
import com.iortatechnxt.finverse.support.AsUser;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.underwriting.api.dto.EndorsementRequest;
import com.iortatechnxt.finverse.underwriting.api.dto.IterationRequest;
import com.iortatechnxt.finverse.underwriting.api.dto.OpenCoverRequest;
import com.iortatechnxt.finverse.underwriting.api.dto.QuotationRequest;
import com.iortatechnxt.finverse.underwriting.domain.Endorsement;
import com.iortatechnxt.finverse.underwriting.domain.EndorsementType;
import com.iortatechnxt.finverse.underwriting.domain.OpenCover;
import com.iortatechnxt.finverse.underwriting.domain.Policy;
import com.iortatechnxt.finverse.underwriting.domain.Product;
import com.iortatechnxt.finverse.underwriting.domain.Quotation;
import com.iortatechnxt.finverse.underwriting.domain.SourceType;
import com.iortatechnxt.finverse.underwriting.service.EndorsementService;
import com.iortatechnxt.finverse.underwriting.service.OpenCoverService;
import com.iortatechnxt.finverse.underwriting.service.PolicyService;
import com.iortatechnxt.finverse.underwriting.service.QuotationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** Underwriting documents in the universal approval inbox; every test rolls back. */
@IntegrationTest
@Transactional
class UnderwritingApprovalSourceIT {

  @Autowired private UwFixtures fx;
  @Autowired private PolicyService policies;
  @Autowired private EndorsementService endorsements;
  @Autowired private QuotationService quotations;
  @Autowired private OpenCoverService openCovers;
  @Autowired private ApprovalInboxService inbox;
  @Autowired private AsUser as;

  private List<PendingApproval> inboxOf(String user) {
    return as.run(user, () -> inbox.inbox(fx.companyId()));
  }

  @Test
  void pendingUnderwritingDocumentsReachTheCheckerButNotTheMaker() {
    Product fire = fx.product("FIRE", false);
    Policy draft = as.run("uw", () -> policies.create(fx.brokerRequest(fire)));
    as.run("uw", () -> policies.submit(draft.getId()));

    Policy issued = fx.issue(fx.brokerRequest(fire), UwFixtures.ISSUE);
    Endorsement endorsement =
        as.run(
            "uw",
            () ->
                endorsements.create(
                    issued.getId(),
                    new EndorsementRequest(
                        EndorsementType.ADDITIONAL,
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 6, 1),
                        "Additional stock",
                        new BigDecimal("20000"),
                        new BigDecimal("2000000"),
                        null,
                        null)));
    as.run("uw", () -> endorsements.submit(endorsement.getId()));

    Quotation quotation = as.run("uw", () -> quotations.create(quotationRequest(fire)));
    as.run("uw", () -> quotations.submit(quotation.getId()));

    Product newProduct =
        as.run(
            "uw",
            () ->
                fx.productService()
                    .create(fx.productRequest(UwFixtures.uniqueCode("A"), "MOTOR", false)));
    OpenCover cover =
        as.run("uw", () -> openCovers.create(openCoverRequest(fx.product("MARINE", true))));

    List<PendingApproval> checker = inboxOf("fmanager");
    PendingApproval policy = byLink(checker, "/underwriting/policies/" + draft.getId());
    assertThat(policy.module()).isEqualTo("UNDERWRITING");
    assertThat(policy.type()).isEqualTo("Policy");
    assertThat(policy.reference()).isEqualTo(draft.getPolicyNo());
    assertThat(policy.amount()).isEqualByComparingTo("100000");
    assertThat(policy.submittedBy()).isEqualTo("uw");
    assertThat(policy.submittedAt()).isNotNull();
    assertThat(byLink(checker, "/underwriting/policies/" + issued.getId()).reference())
        .isEqualTo(endorsement.documentNo());
    PendingApproval quote = byLink(checker, "/underwriting/quotations/" + quotation.getId());
    assertThat(quote.amount()).isEqualByComparingTo("250000");
    String coverLink = "/underwriting/open-covers/" + cover.getId();
    assertThat(byLink(checker, coverLink).type()).isEqualTo("Open cover");
    assertThat(checker)
        .anySatisfy(
            i -> {
              assertThat(i.reference()).isEqualTo(newProduct.getCode());
              assertThat(i.link()).isEqualTo("/underwriting/products");
            });

    // Makers never see their own documents; users without POLICY_AUTHORIZE see none of them.
    assertThat(inboxOf("uw")).noneMatch(i -> "uw".equals(i.submittedBy()));
    assertThat(inboxOf("checker")).noneMatch(i -> "UNDERWRITING".equals(i.module()));
    assertThat(inbox.pendingAll())
        .extracting(PendingApproval::link)
        .contains(coverLink, quote.link(), policy.link());
  }

  private static PendingApproval byLink(List<PendingApproval> items, String link) {
    return items.stream().filter(i -> link.equals(i.link())).findFirst().orElseThrow();
  }

  private QuotationRequest quotationRequest(Product product) {
    return new QuotationRequest(
        fx.companyId(),
        fx.branchId(),
        product.getId(),
        "C-000204",
        "Metro Retail Holdings Corp.",
        SourceType.BROKER,
        "B-0002",
        UwFixtures.ISSUE,
        60,
        LocalDate.of(2026, 5, 1),
        LocalDate.of(2027, 4, 30),
        "PHP",
        new BigDecimal("100"),
        null,
        new IterationRequest(
            new BigDecimal("50000000"),
            new BigDecimal("250000"),
            new BigDecimal("25000"),
            BigDecimal.ZERO,
            new BigDecimal("1500"),
            "First offer"));
  }

  private OpenCoverRequest openCoverRequest(Product marine) {
    return new OpenCoverRequest(
        fx.companyId(),
        fx.branchId(),
        marine.getId(),
        "C-000202",
        "Visayas Shipping Lines Inc.",
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 12, 31),
        "PHP",
        new BigDecimal("20000000"),
        new BigDecimal("30000000"),
        new BigDecimal("0.35"),
        "Steel products");
  }
}
