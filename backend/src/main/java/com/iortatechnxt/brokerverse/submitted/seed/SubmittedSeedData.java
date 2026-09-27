package com.iortatechnxt.brokerverse.submitted.seed;

import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmAssured;
import com.iortatechnxt.brokerverse.submitted.domain.SbmBusinessType;
import com.iortatechnxt.brokerverse.submitted.domain.SbmHandlingFee;
import com.iortatechnxt.brokerverse.submitted.domain.SbmHistorySource;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIaaf;
import com.iortatechnxt.brokerverse.submitted.domain.SbmIaafReview;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLoan;
import com.iortatechnxt.brokerverse.submitted.domain.SbmMarks;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyData;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyStatus;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRisk;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRun;
import com.iortatechnxt.brokerverse.submitted.domain.SbmTerms;
import com.iortatechnxt.brokerverse.submitted.domain.SbmTracking;
import com.iortatechnxt.brokerverse.submitted.fee.service.HandlingFeeService;
import com.iortatechnxt.brokerverse.submitted.fee.service.NoTouchService;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistService;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistService.UpsertContext;
import com.iortatechnxt.brokerverse.submitted.processing.service.SbmProcessingService;
import com.iortatechnxt.brokerverse.submitted.processing.service.SbmProcessingService.RunRequest;
import com.iortatechnxt.brokerverse.submitted.renewal.service.HandOffService;
import com.iortatechnxt.brokerverse.submitted.review.service.IaafService;
import com.iortatechnxt.brokerverse.submitted.review.service.TorService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Loads the Submitted Policies seed records through the real services (seed profile only,
 * idempotent; reference data in V1970): source intake of motor, fire, corporate and retail
 * policies, handler assignment, a processing run (sanitation, LAMD matching, classification,
 * disposition and limits), policy reviews with an IAAF approved and one waiting, a TOR of a record
 * above the insurer limits, a renewal started with Renew with BDOI, handling fees billed and a No
 * Touch export.
 */
@Component
@Profile("seed")
@Order(140)
public class SubmittedSeedData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(SubmittedSeedData.class);
  private static final String FIRST_PN = "PN-MTR-26001";
  private static final String SOURCE_MOTOR = "LFS_INSURANCE";
  private static final String SOURCE_FIRE = "HLS_INSURANCE";
  private static final String SOURCE_CORPORATE = "IBG_LEASING_DOC";
  private static final String SOURCE_RETAIL = "SPI";

  private final CompanyRepository companies;
  private final SbmPolicyRepository policies;
  private final MasterlistService masterlist;
  private final SbmProcessingService processing;
  private final Work work;
  private final Clock clock;

  /**
   * The services of the review, renewal and fee steps.
   *
   * @param iaafs IAAF
   * @param tors TOR
   * @param handOff renewal hand-off
   * @param fees handling fees
   * @param noTouch No Touch billing
   */
  public record Work(
      IaafService iaafs,
      TorService tors,
      HandOffService handOff,
      HandlingFeeService fees,
      NoTouchService noTouch) {}

  /**
   * Creates the loader.
   *
   * @param companies companies
   * @param policies masterlist (idempotency)
   * @param masterlist masterlist
   * @param processing processing runs
   * @param iaafs IAAF
   * @param tors TOR
   * @param handOff renewal hand-off
   * @param fees handling fees
   * @param noTouch No Touch billing
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // the services the seed goes through
  public SubmittedSeedData(
      CompanyRepository companies,
      SbmPolicyRepository policies,
      MasterlistService masterlist,
      SbmProcessingService processing,
      IaafService iaafs,
      TorService tors,
      HandOffService handOff,
      HandlingFeeService fees,
      NoTouchService noTouch,
      Clock clock) {
    this.companies = companies;
    this.policies = policies;
    this.masterlist = masterlist;
    this.processing = processing;
    this.work = new Work(iaafs, tors, handOff, fees, noTouch);
    this.clock = clock;
  }

  @Override
  public void run(ApplicationArguments args) {
    Company company = companies.findByCode("FVI").orElse(null);
    if (company == null || policies.existsByCompanyIdAndNaturalKey(company.getId(), FIRST_PN)) {
      return;
    }
    Long companyId = company.getId();
    LocalDate today = BusinessClock.today(clock);
    List<SbmPolicy> loaded = new ArrayList<>();
    for (String[] row : SubmittedSeedRows.ROWS) {
      step("intake of " + row[0], () -> loaded.add(intake(companyId, row, today)));
    }
    step("assignment", () -> assign(companyId, loaded));
    step(
        "processing run",
        () ->
            as(
                "sanitation",
                () ->
                    processing.run(
                        new RunRequest(
                            companyId,
                            SbmRun.Trigger.MANUAL,
                            "Seed intake",
                            loaded.stream().map(SbmPolicy::getId).toList()))));
    step("reviews and IAAF", () -> reviews(loaded, today));
    step("TOR", () -> tor(loaded));
    step("renewal", () -> renewal(loaded));
    step("handling fees", () -> fees(companyId, loaded, today));
    step(
        "No Touch export",
        () ->
            as(
                "upphandler",
                () ->
                    work.noTouch()
                        .export(companyId, "INS-MGIC", YearMonth.from(today.plusDays(40)))));
    LOG.info("Submitted Policies seed data: {} policies loaded", loaded.size());
  }

  private SbmPolicy intake(Long companyId, String[] r, LocalDate today) {
    String segment = r[1];
    boolean motor = "CBG_MOTOR".equals(segment);
    LocalDate expiry = today.plusDays(Integer.parseInt(r[5]));
    SbmPolicyData data =
        new SbmPolicyData(
            segment,
            SbmBusinessType.valueOf(r[2]),
            r[0].isEmpty()
                ? SbmLoan.NONE
                : new SbmLoan(r[0], null, "CIF-" + r[0].substring(3), null, null, null, null, r[3]),
            new SbmAssured(
                r[3],
                "Seed address of " + r[3] + ", Makati City",
                null,
                "0917" + (1_000_000 + Math.floorMod(r[3].hashCode(), 8_999_999)),
                r[3].toLowerCase(java.util.Locale.ROOT).replace(' ', '.') + "@example.ph",
                "Branch.counterpart@bank-seed.ph"),
            new SbmTerms(
                r[4],
                "POL-"
                    + (r[0].isEmpty()
                        ? "C" + Math.floorMod(r[3].hashCode(), 99_999)
                        : r[0].substring(3)),
                expiry.minusYears(1),
                expiry,
                null,
                new BigDecimal(r[6]),
                new BigDecimal(r[6])
                    .multiply(new BigDecimal("0.015"))
                    .setScale(2, java.math.RoundingMode.HALF_UP),
                "PHP"),
            motor
                ? new SbmRisk(
                    r[7],
                    "SN" + r[8],
                    "MN" + r[8],
                    "White",
                    r[8],
                    r[9],
                    today.getYear() - 3,
                    null,
                    null,
                    "BDO Unibank")
                : new SbmRisk(null, null, null, null, null, null, null, r[7], r[9], "BDO Unibank"),
            new SbmMarks("FFY".equals(r[10]), "EMP".equals(r[10]), "NT".equals(r[10])));
    String source = source(segment);
    return masterlist
        .upsert(
            companyId,
            data,
            UpsertContext.of(
                source,
                null,
                today.minusDays(2),
                SbmPolicyStatus.RECEIVED,
                SbmHistorySource.INTAKE,
                "Seed " + source))
        .policy();
  }

  private static String source(String segment) {
    return switch (segment) {
      case "CBG_MOTOR" -> SOURCE_MOTOR;
      case "CBG_FIRE" -> SOURCE_FIRE;
      case "NONCBG_CORPORATE" -> SOURCE_CORPORATE;
      default -> SOURCE_RETAIL;
    };
  }

  private void assign(Long companyId, List<SbmPolicy> loaded) {
    as(
        "sbmtl",
        () -> {
          for (String handler : List.of("sbmhandler", "firehandler", "sbmtl")) {
            List<Long> ids =
                loaded.stream()
                    .filter(p -> handler.equals(handlerOf(p.getSegment())))
                    .map(SbmPolicy::getId)
                    .toList();
            masterlist.assign(companyId, ids, handler);
          }
          for (SbmPolicy p : loaded) {
            if ("CBG_MOTOR".equals(p.getSegment())) {
              String ao = p.getId() % 2 == 0 ? "ao" : "ao2";
              masterlist.track(
                  p.getId(), new SbmTracking(handlerOf(p.getSegment()), ao, null, null, null));
            }
          }
          return null;
        });
  }

  private static String handlerOf(String segment) {
    return switch (segment) {
      case "CBG_MOTOR" -> "sbmhandler";
      case "CBG_FIRE" -> "firehandler";
      default -> "sbmtl";
    };
  }

  private void reviews(List<SbmPolicy> loaded, LocalDate today) {
    List<SbmPolicy> corporate =
        loaded.stream().filter(p -> "NONCBG_CORPORATE".equals(p.getSegment())).toList();
    for (int i = 0; i < corporate.size(); i++) {
      Long id = corporate.get(i).getId();
      boolean findings = i % 2 == 1;
      as(
          "polreview",
          () ->
              work.iaafs()
                  .review(
                      id,
                      new SbmIaafReview.Content(
                          today.minusDays(1),
                          findings ? SbmIaafReview.WITH_FINDINGS : SbmIaafReview.ADEQUATE,
                          findings ? List.of("MORTGAGEE_CLAUSE_MISSING") : List.of(),
                          findings
                              ? "The mortgagee clause in favour of the bank is missing"
                              : "Cover adequate for the loan")));
      if (!findings) {
        SbmIaaf iaaf = as("polreview", () -> work.iaafs().generate(id, Map.of()));
        as("polreview", () -> work.iaafs().submit(iaaf.getId()));
        if (i == 0) {
          as("sbmchecker", () -> work.iaafs().approve(iaaf.getId()));
        }
      }
    }
  }

  private void tor(List<SbmPolicy> loaded) {
    loaded.stream()
        .map(p -> masterlist.require(p.getId()))
        .filter(SbmPolicy::isInsurerApprovalRequired)
        .findFirst()
        .ifPresent(
            p -> {
              var t =
                  as(
                      "sbmhandler",
                      () ->
                          work.tors()
                              .generate(
                                  p.getId(),
                                  "Accept the sum insured above the motor limit with a 20% "
                                      + "deductible and the anti-theft device warranty",
                                  "ao"));
              as("sbmhandler", () -> work.tors().submit(t.getId()));
            });
  }

  private void renewal(List<SbmPolicy> loaded) {
    loaded.stream()
        .map(p -> masterlist.require(p.getId()))
        .filter(
            p -> p.getStatus() == SbmPolicyStatus.FOR_RENEWAL && "CBG_FIRE".equals(p.getSegment()))
        .findFirst()
        .ifPresent(p -> as("firehandler", () -> work.handOff().renewWithBdoi(p.getId())));
  }

  private void fees(Long companyId, List<SbmPolicy> loaded, LocalDate today) {
    loaded.stream()
        .filter(p -> "CBG_MOTOR".equals(p.getSegment()) && p.getLoan().pnNo() != null)
        .limit(3)
        .forEach(
            p ->
                as(
                    "upphandler",
                    () ->
                        work.fees()
                            .bill(
                                companyId,
                                new SbmHandlingFee.Bill(
                                    p.getId(),
                                    p.getLoan().pnNo(),
                                    null,
                                    new BigDecimal("1120.00"),
                                    "PHP",
                                    today.minusDays(5)),
                                null)));
  }

  private static void step(String what, Runnable action) {
    try {
      action.run();
    } catch (RuntimeException ex) {
      LOG.warn("Submitted Policies seed step {} skipped: {}", what, ex.getMessage());
    }
  }

  private static <T> T as(String user, Supplier<T> action) {
    SecurityContext previous = SecurityContextHolder.getContext();
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    SecurityContextHolder.setContext(context);
    try {
      return action.get();
    } finally {
      SecurityContextHolder.setContext(previous);
    }
  }
}
