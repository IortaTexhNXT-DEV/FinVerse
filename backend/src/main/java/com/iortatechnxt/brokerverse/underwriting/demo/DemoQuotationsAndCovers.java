package com.iortatechnxt.brokerverse.underwriting.demo;

import static com.iortatechnxt.brokerverse.underwriting.demo.DemoPolicyGenerator.CHECKER;
import static com.iortatechnxt.brokerverse.underwriting.demo.DemoPolicyGenerator.MAKER;

import com.iortatechnxt.brokerverse.underwriting.api.dto.CertificateRequest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.ConvertQuotationRequest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.IterationRequest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.OpenCoverRequest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.QuotationRequest;
import com.iortatechnxt.brokerverse.underwriting.api.dto.RiskRequest;
import com.iortatechnxt.brokerverse.underwriting.domain.OpenCover;
import com.iortatechnxt.brokerverse.underwriting.domain.Policy;
import com.iortatechnxt.brokerverse.underwriting.domain.Product;
import com.iortatechnxt.brokerverse.underwriting.domain.Quotation;
import com.iortatechnxt.brokerverse.underwriting.domain.SourceType;
import com.iortatechnxt.brokerverse.underwriting.service.OpenCoverService;
import com.iortatechnxt.brokerverse.underwriting.service.PolicyApprovalService;
import com.iortatechnxt.brokerverse.underwriting.service.PolicyService;
import com.iortatechnxt.brokerverse.underwriting.service.QuotationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Demo quotations in every status (draft, pending, approved, rejected, converted, expired) and a
 * USD marine open cover with monthly shipment certificates.
 */
final class DemoQuotationsAndCovers {

  private static final BigDecimal INITIAL_RATE = new BigDecimal("0.003");
  private static final BigDecimal REVISED_RATE = new BigDecimal("0.0025");
  private static final BigDecimal CHARGES_RATE = new BigDecimal("0.26");
  private static final LocalDate COVER_FROM = LocalDate.of(2026, 1, 1);
  private static final LocalDate COVER_TO = LocalDate.of(2026, 12, 31);
  private static final LocalDate EXPIRY_RUN = LocalDate.of(2026, 9, 22);
  private static final int CONVERSION_DAYS = 7;
  private static final int CERTIFICATES = 8;
  private static final int SHIPMENT_INTERVAL_DAYS = 28;
  private static final LocalDate FIRST_SAILING = LocalDate.of(2026, 2, 10);
  private static final BigDecimal SHIPMENT_STEP = new BigDecimal("150000");
  private static final BigDecimal FIRST_SHIPMENT = new BigDecimal("350000");
  private static final List<String> VESSELS =
      List.of("MV Pacific Star", "MV Mindanao Pride", "MV Luzon Trader", "MV Asia Carrier");
  private static final List<String> PORTS = List.of("Shanghai", "Singapore", "Busan", "Kaohsiung");

  private static final List<Plan> PLANS =
      List.of(
          new Plan("FIRE-COM", "C-000203", LocalDate.of(2026, 2, 3), 15, Target.EXPIRED),
          new Plan("ENGG-CAR", "C-000201", LocalDate.of(2026, 2, 17), 15, Target.EXPIRED),
          new Plan("FIRE-COM", "C-000204", LocalDate.of(2026, 3, 9), 30, Target.CONVERTED),
          new Plan("CAS-CGL", "C-000201", LocalDate.of(2026, 4, 14), 30, Target.CONVERTED),
          new Plan("BONDS-SUR", "C-000203", LocalDate.of(2026, 5, 6), 30, Target.REJECTED),
          new Plan("MOTOR-PC", "C-000101", LocalDate.of(2026, 6, 2), 30, Target.REJECTED),
          new Plan("FIRE-COM", "C-000201", LocalDate.of(2026, 9, 1), 60, Target.APPROVED),
          new Plan("HEALTH-GRP", "C-000204", LocalDate.of(2026, 9, 4), 60, Target.APPROVED),
          new Plan("ENGG-CAR", "C-000203", LocalDate.of(2026, 9, 8), 60, Target.PENDING),
          new Plan("CAS-CGL", "C-000204", LocalDate.of(2026, 9, 11), 60, Target.PENDING),
          new Plan("PA-IND", "C-000102", LocalDate.of(2026, 9, 15), 60, Target.PENDING),
          new Plan("MARINE-CGO", "C-000202", LocalDate.of(2026, 9, 18), 60, Target.DRAFT),
          new Plan("MOTOR-PC", "C-000102", LocalDate.of(2026, 9, 21), 60, Target.DRAFT));

  private final DemoUserContext users;
  private final QuotationService quotations;
  private final OpenCoverService openCovers;
  private final PolicyService policies;
  private final PolicyApprovalService approvals;

  DemoQuotationsAndCovers(
      DemoUserContext users,
      QuotationService quotations,
      OpenCoverService openCovers,
      PolicyService policies,
      PolicyApprovalService approvals) {
    this.users = users;
    this.quotations = quotations;
    this.openCovers = openCovers;
    this.policies = policies;
    this.approvals = approvals;
  }

  /**
   * Creates the demo quotations.
   *
   * @param companyId company
   * @param branchId issuing branch
   * @param products products by code
   * @return number created
   */
  int quotations(Long companyId, Long branchId, Map<String, Product> products) {
    int n = 0;
    for (Plan plan : PLANS) {
      Quotation q =
          users.runAs(MAKER, () -> quotations.create(request(companyId, branchId, products, plan)));
      advance(q, plan);
      n++;
    }
    users.runAs(CHECKER, () -> quotations.expireLapsed(companyId, EXPIRY_RUN));
    return n;
  }

  private void advance(Quotation q, Plan plan) {
    if (plan.target() == Target.DRAFT) {
      return;
    }
    if (plan.validityDays() > Plan.SHORT_VALIDITY) {
      users.runAs(MAKER, () -> quotations.iterate(q.getId(), figures(plan, true)));
    }
    users.runAs(MAKER, () -> quotations.submit(q.getId()));
    switch (plan.target()) {
      case REJECTED ->
          users.runAs(
              CHECKER, () -> quotations.reject(q.getId(), "Terms not acceptable to the insured"));
      case APPROVED, EXPIRED -> users.runAs(CHECKER, () -> quotations.approve(q.getId()));
      case CONVERTED -> convert(q, plan);
      default -> {
        // PENDING: left awaiting approval
      }
    }
  }

  private void convert(Quotation q, Plan plan) {
    users.runAs(CHECKER, () -> quotations.approve(q.getId()));
    LocalDate issue = plan.issueDate().plusDays(CONVERSION_DAYS);
    Policy draft =
        users.runAs(
            MAKER,
            () ->
                quotations.convert(
                    q.getId(), new ConvertQuotationRequest(issue, null, false, null)));
    users.runAs(MAKER, () -> policies.submit(draft.getId()));
    users.runAs(CHECKER, () -> approvals.approvePolicy(draft.getId(), issue));
  }

  private static QuotationRequest request(
      Long companyId, Long branchId, Map<String, Product> products, Plan plan) {
    boolean brokered = plan.issueDate().getDayOfMonth() % 2 != 0;
    return new QuotationRequest(
        companyId,
        branchId,
        products.get(plan.productCode()).getId(),
        plan.clientCode(),
        DemoCatalog.INSURED.get(DemoCatalog.CLIENTS.indexOf(plan.clientCode())),
        brokered ? SourceType.BROKER : SourceType.DIRECT,
        brokered ? "B-0001" : null,
        plan.issueDate(),
        plan.validityDays(),
        plan.issueDate().plusMonths(1),
        plan.issueDate().plusMonths(1).plusYears(1).minusDays(1),
        "PHP",
        BigDecimal.valueOf(100),
        null,
        figures(plan, false));
  }

  private static IterationRequest figures(Plan plan, boolean revised) {
    BigDecimal si =
        BigDecimal.valueOf(Plan.BASE_SUM_INSURED)
            .multiply(BigDecimal.valueOf(plan.issueDate().getMonthValue()));
    BigDecimal gross = si.multiply(revised ? REVISED_RATE : INITIAL_RATE);
    return new IterationRequest(
        si,
        gross,
        revised ? gross.movePointLeft(1) : BigDecimal.ZERO,
        BigDecimal.ZERO,
        gross.multiply(CHARGES_RATE),
        revised ? "Revised terms after negotiation" : "Initial offer");
  }

  /**
   * Creates an authorized USD marine open cover with approved certificates.
   *
   * @param companyId company
   * @param branchId branch
   * @param marine marine product
   * @return open cover
   */
  OpenCover openCover(Long companyId, Long branchId, Product marine) {
    OpenCover cover =
        users.runAs(
            MAKER,
            () ->
                openCovers.create(
                    new OpenCoverRequest(
                        companyId,
                        branchId,
                        marine.getId(),
                        "C-000202",
                        "Visayas Shipping Lines Inc.",
                        COVER_FROM,
                        COVER_TO,
                        "USD",
                        new BigDecimal("2000000"),
                        new BigDecimal("20000000"),
                        new BigDecimal("0.30"),
                        "Steel products, machinery and spare parts, all risks")));
    users.runAs(CHECKER, () -> openCovers.authorize(cover.getId()));
    for (int k = 0; k < CERTIFICATES; k++) {
      LocalDate sail = FIRST_SAILING.plusDays((long) k * SHIPMENT_INTERVAL_DAYS);
      CertificateRequest declaration =
          new CertificateRequest(sail.minusDays(2), null, null, null, shipment(k, sail));
      Policy cert =
          users.runAs(MAKER, () -> openCovers.issueCertificate(cover.getId(), declaration));
      users.runAs(MAKER, () -> policies.submit(cert.getId()));
      users.runAs(CHECKER, () -> approvals.approvePolicy(cert.getId(), sail.minusDays(1)));
    }
    return cover;
  }

  private static RiskRequest shipment(int k, LocalDate sail) {
    String no = String.format("%03d", k + 1);
    return new RiskRequest(
        "Shipment " + no + " - steel coils and machinery",
        FIRST_SHIPMENT.add(SHIPMENT_STEP.multiply(BigDecimal.valueOf(k))),
        null,
        null,
        null,
        null,
        VESSELS.get(k % VESSELS.size()),
        PORTS.get(k % PORTS.size()),
        "Cebu",
        sail,
        "BL-2026-" + no,
        sail,
        "LC-VSL-2026-" + no,
        "BDO Unibank, Inc.",
        "CIF + 10%");
  }

  /** Target status of a demo quotation. */
  enum Target {
    DRAFT,
    PENDING,
    APPROVED,
    REJECTED,
    CONVERTED,
    EXPIRED
  }

  /**
   * Demo quotation plan.
   *
   * @param productCode product
   * @param clientCode client
   * @param issueDate issue date
   * @param validityDays validity
   * @param target final status
   */
  record Plan(
      String productCode, String clientCode, LocalDate issueDate, int validityDays, Target target) {

    static final int SHORT_VALIDITY = 15;
    static final long BASE_SUM_INSURED = 12_500_000L;
  }
}
