package com.iortatechnxt.finverse.underwriting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.support.AsUser;
import com.iortatechnxt.finverse.support.IntegrationTest;
import com.iortatechnxt.finverse.underwriting.api.dto.CertificateRequest;
import com.iortatechnxt.finverse.underwriting.api.dto.ConvertQuotationRequest;
import com.iortatechnxt.finverse.underwriting.api.dto.IterationRequest;
import com.iortatechnxt.finverse.underwriting.api.dto.OpenCoverRequest;
import com.iortatechnxt.finverse.underwriting.api.dto.QuotationRequest;
import com.iortatechnxt.finverse.underwriting.api.dto.RiskRequest;
import com.iortatechnxt.finverse.underwriting.domain.BusinessType;
import com.iortatechnxt.finverse.underwriting.domain.OpenCover;
import com.iortatechnxt.finverse.underwriting.domain.Policy;
import com.iortatechnxt.finverse.underwriting.domain.PolicyStatus;
import com.iortatechnxt.finverse.underwriting.domain.Product;
import com.iortatechnxt.finverse.underwriting.domain.Quotation;
import com.iortatechnxt.finverse.underwriting.domain.QuotationStatus;
import com.iortatechnxt.finverse.underwriting.domain.SourceType;
import com.iortatechnxt.finverse.underwriting.service.OpenCoverService;
import com.iortatechnxt.finverse.underwriting.service.PolicyApprovalService;
import com.iortatechnxt.finverse.underwriting.service.PolicyService;
import com.iortatechnxt.finverse.underwriting.service.QuotationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class QuotationAndOpenCoverIT {

  private static final LocalDate ISSUED = LocalDate.of(2026, 4, 1);

  @Autowired private UwFixtures fx;
  @Autowired private QuotationService quotations;
  @Autowired private OpenCoverService openCovers;
  @Autowired private PolicyService policies;
  @Autowired private PolicyApprovalService approvals;
  @Autowired private AsUser as;

  private QuotationRequest quotationRequest(Product product, String share, int validity) {
    return new QuotationRequest(
        fx.companyId(),
        fx.branchId(),
        product.getId(),
        "C-000204",
        "Metro Retail Holdings Corp.",
        SourceType.BROKER,
        "B-0002",
        ISSUED,
        validity,
        LocalDate.of(2026, 5, 1),
        LocalDate.of(2027, 4, 30),
        "PHP",
        new BigDecimal(share),
        null,
        new IterationRequest(
            new BigDecimal("50000000"),
            new BigDecimal("250000"),
            new BigDecimal("25000"),
            BigDecimal.ZERO,
            new BigDecimal("1500"),
            "First offer"));
  }

  @Test
  void quotationIsIteratedApprovedAndConverted() {
    Product fire = fx.product("FIRE", false);
    Quotation q = as.run("uw", () -> quotations.create(quotationRequest(fire, "80", 60)));
    assertThat(q.getQuotationNo()).startsWith("Q-HO-2026-");
    assertThat(q.getCommissionRate()).isEqualByComparingTo("15");

    as.run(
        "uw",
        () ->
            quotations.iterate(
                q.getId(),
                new IterationRequest(
                    new BigDecimal("50000000"),
                    new BigDecimal("240000"),
                    new BigDecimal("12000"),
                    new BigDecimal("2400"),
                    null,
                    "Revised")));
    as.run("uw", () -> quotations.submit(q.getId()));
    assertThatThrownBy(() -> as.run("uw", () -> quotations.approve(q.getId())))
        .isInstanceOf(BusinessRuleException.class);
    Quotation approved = as.run("fmanager", () -> quotations.approve(q.getId()));
    assertThat(approved.getStatus()).isEqualTo(QuotationStatus.APPROVED);
    assertThat(approved.getCurrentIteration()).isEqualTo(2);

    Policy policy =
        as.run(
            "uw",
            () ->
                quotations.convert(
                    q.getId(),
                    new ConvertQuotationRequest(ISSUED.plusDays(5), "CO-0001", false, null)));
    assertThat(policy.getBusinessType()).isEqualTo(BusinessType.DIRECT_WITH_COINSURANCE);
    assertThat(policy.getQuotationId()).isEqualTo(q.getId());
    assertThat(policy.getPremium().getGrossPremium()).isEqualByComparingTo("240000.00");
    assertThat(policy.getPremium().getDiscountAmount()).isEqualByComparingTo("12000.00");
    assertThat(policy.getPremium().getLoadingAmount()).isEqualByComparingTo("2400.00");
    assertThat(policy.getPremium().getCommissionRate()).isEqualByComparingTo("15");
    assertThat(quotations.get(q.getId()).getStatus()).isEqualTo(QuotationStatus.CONVERTED);
    assertThat(quotations.search(fx.companyId(), QuotationStatus.CONVERTED, null, null))
        .anyMatch(x -> x.getId().equals(q.getId()));
  }

  @Test
  void quotationRejectionAndExpiry() {
    Product motor = fx.product("MOTOR", false);
    Quotation q = as.run("uw", () -> quotations.create(quotationRequest(motor, "100", 30)));
    as.run("uw", () -> quotations.submit(q.getId()));
    Quotation rejected = as.run("fmanager", () -> quotations.reject(q.getId(), "Rate too low"));
    assertThat(rejected.getStatus()).isEqualTo(QuotationStatus.REJECTED);
    assertThat(rejected.getDecisionReason()).isEqualTo("Rate too low");

    Quotation open = as.run("uw", () -> quotations.create(quotationRequest(motor, "100", 10)));
    Quotation edited =
        as.run("uw", () -> quotations.update(open.getId(), quotationRequest(motor, "100", 15)));
    assertThat(edited.getValidityDays()).isEqualTo(15);
    int expired =
        as.run("fmanager", () -> quotations.expireLapsed(fx.companyId(), ISSUED.plusDays(16)));
    assertThat(expired).isPositive();
    assertThat(quotations.get(open.getId()).getStatus()).isEqualTo(QuotationStatus.EXPIRED);
    assertThatThrownBy(
            () ->
                as.run(
                    "uw",
                    () ->
                        quotations.convert(
                            open.getId(), new ConvertQuotationRequest(null, null, false, "x"))))
        .isInstanceOf(BusinessRuleException.class);
  }

  private RiskRequest shipment(String si, LocalDate sail) {
    return new RiskRequest(
        "Steel coils, 40 containers",
        new BigDecimal(si),
        null,
        null,
        null,
        null,
        "MV Pacific Star",
        "Shanghai",
        "Manila",
        sail,
        "BL-778812",
        sail,
        "LC-2026-0045",
        "BDO Unibank",
        "CIF + 10%");
  }

  @Test
  void openCoverCertificatesRespectLimitsAndAreApprovedLikePolicies() {
    Product marine = fx.product("MARINE", true);
    OpenCover cover =
        as.run(
            "uw",
            () ->
                openCovers.create(
                    new OpenCoverRequest(
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
                        "Steel products")));
    CertificateRequest declaration =
        new CertificateRequest(
            LocalDate.of(2026, 2, 3),
            null,
            null,
            null,
            shipment("15000000", LocalDate.of(2026, 2, 5)));
    assertThatThrownBy(
            () -> as.run("uw", () -> openCovers.issueCertificate(cover.getId(), declaration)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("not authorized");
    as.run("fmanager", () -> openCovers.authorize(cover.getId()));

    Policy cert = as.run("uw", () -> openCovers.issueCertificate(cover.getId(), declaration));
    assertThat(cert.getPolicyNo()).startsWith("MC-HO-2026-");
    assertThat(cert.getPremium().getGrossPremium()).isEqualByComparingTo("52500.00");
    assertThat(cert.getPeriodFrom()).isEqualTo(LocalDate.of(2026, 2, 5));
    as.run("uw", () -> policies.submit(cert.getId()));
    Policy approved =
        as.run("fmanager", () -> approvals.approvePolicy(cert.getId(), LocalDate.of(2026, 2, 6)));
    assertThat(approved.getStatus()).isEqualTo(PolicyStatus.APPROVED);
    assertThat(openCovers.certificates(cover.getId())).hasSize(1);

    assertThatThrownBy(
            () ->
                as.run(
                    "uw",
                    () ->
                        openCovers.issueCertificate(
                            cover.getId(),
                            new CertificateRequest(
                                LocalDate.of(2026, 3, 1),
                                30,
                                null,
                                null,
                                shipment("25000000", LocalDate.of(2026, 3, 2))))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("limit per shipment");
    assertThatThrownBy(
            () ->
                as.run(
                    "uw",
                    () ->
                        openCovers.issueCertificate(
                            cover.getId(),
                            new CertificateRequest(
                                LocalDate.of(2026, 3, 1),
                                30,
                                null,
                                null,
                                shipment("16000000", LocalDate.of(2026, 3, 2))))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("annual limit");
    assertThat(openCovers.list(fx.companyId())).anyMatch(c -> c.getId().equals(cover.getId()));
  }

  @Test
  void openCoverNeedsAnOpenCoverProduct() {
    Product fire = fx.product("FIRE", false);
    assertThatThrownBy(
            () ->
                as.run(
                    "uw",
                    () ->
                        openCovers.create(
                            new OpenCoverRequest(
                                fx.companyId(),
                                fx.branchId(),
                                fire.getId(),
                                "C-000202",
                                "Visayas Shipping Lines Inc.",
                                LocalDate.of(2026, 1, 1),
                                LocalDate.of(2026, 12, 31),
                                "PHP",
                                BigDecimal.TEN,
                                BigDecimal.TEN,
                                BigDecimal.ONE,
                                null))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("does not allow open covers");
  }
}
