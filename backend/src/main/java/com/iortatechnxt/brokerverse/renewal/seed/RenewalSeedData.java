package com.iortatechnxt.brokerverse.renewal.seed;

import com.iortatechnxt.brokerverse.opsledger.seed.SeedUsers;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.renewal.candidate.service.AccountHistoryService;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerResponse;
import com.iortatechnxt.brokerverse.renewal.domain.InsurerResponseCode;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidateRepository;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalDisposition;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.extraction.service.ExtractionService;
import com.iortatechnxt.brokerverse.renewal.insurer.service.InsurerBatchService;
import com.iortatechnxt.brokerverse.renewal.insurer.service.RenewalInsurerResponseService;
import com.iortatechnxt.brokerverse.renewal.marketing.service.RenewalAssignmentService;
import com.iortatechnxt.brokerverse.renewal.marketing.service.RenewalDispositionService;
import com.iortatechnxt.brokerverse.renewal.marketing.service.TransferService;
import com.iortatechnxt.brokerverse.renewal.processing.service.ProcessingService;
import com.iortatechnxt.brokerverse.renewal.rules.service.InitiationService;
import com.iortatechnxt.brokerverse.renewal.service.port.LegacyPolicySource.LegacyHeader;
import com.iortatechnxt.brokerverse.renewal.service.port.LegacyPolicySource.LegacyParties;
import com.iortatechnxt.brokerverse.renewal.service.port.LegacyPolicySource.LegacyPolicy;
import com.iortatechnxt.brokerverse.renewal.setup.service.PackageChoiceService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Renewal seed data (seed profile only, idempotent): the booked seed invoices expiring in 2027 are
 * extracted and seven migrated policies of the go-live window are taken over, then the renewals are
 * taken through the real services to fill every list - unassigned, for disposition, Team Leader
 * review, transfer pending, straight-through processing with the renewal account, with the insurer,
 * RA ready, letter pending, and the Exception bucket with a package choice waiting for approval.
 */
@Component
@Profile("seed")
@Order(140)
public class RenewalSeedData implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(RenewalSeedData.class);

  private static final LocalDate FROM = LocalDate.of(2027, 8, 1);
  private static final LocalDate TO = LocalDate.of(2027, 12, 31);
  private static final String TL = "mkttl";
  private static final String AO = "ao";
  private static final String PO = "proc";
  private static final String PROC_TL = "proctl";
  private static final String MGIC = "INS-MGIC";
  private static final java.util.Map<String, String> CLIENTS =
      java.util.Map.of(
          "CL-2026-000001", "Maria Clara Santos",
          "CL-2026-000002", "Jose Miguel Reyes",
          "CL-2026-000003", "Pacific Harbor Logistics Inc.",
          "CL-2026-000004", "Luzon Agri-Industrial Corp.",
          "CL-2026-000005", "Antonio Luis Garcia");

  private final SeedUsers users;
  private final CompanyRepository companies;
  private final RenewalCandidateRepository candidates;
  private final ExtractionService extraction;
  private final InitiationService initiation;
  private final RenewalAssignmentService assignments;
  private final AccountHistoryService history;
  private final RenewalDispositionService dispositions;
  private final TransferService transfers;
  private final ProcessingService processing;
  private final InsurerBatchService batches;
  private final RenewalInsurerResponseService responses;
  private final PackageChoiceService choices;

  /**
   * Creates the loader.
   *
   * @param users seed users
   * @param companies companies
   * @param candidates renewals (idempotency)
   * @param extraction extraction
   * @param initiation initiation
   * @param assignments assignment
   * @param history account history
   * @param dispositions disposition
   * @param transfers transfers
   * @param processing processing
   * @param batches insurer batches
   * @param responses insurer responses
   * @param choices package choices
   */
  @SuppressWarnings({"java:S107", "PMD.ExcessiveParameterList"}) // constructor injection
  public RenewalSeedData(
      SeedUsers users,
      CompanyRepository companies,
      RenewalCandidateRepository candidates,
      ExtractionService extraction,
      InitiationService initiation,
      RenewalAssignmentService assignments,
      AccountHistoryService history,
      RenewalDispositionService dispositions,
      TransferService transfers,
      ProcessingService processing,
      InsurerBatchService batches,
      RenewalInsurerResponseService responses,
      PackageChoiceService choices) {
    this.users = users;
    this.companies = companies;
    this.candidates = candidates;
    this.extraction = extraction;
    this.initiation = initiation;
    this.assignments = assignments;
    this.history = history;
    this.dispositions = dispositions;
    this.transfers = transfers;
    this.processing = processing;
    this.batches = batches;
    this.responses = responses;
    this.choices = choices;
  }

  @Override
  public void run(ApplicationArguments args) {
    Company company = companies.findByCode("FVI").orElse(null);
    if (company == null || candidates.count() > 0) {
      return;
    }
    Long co = company.getId();
    step("extraction", () -> users.run(TL, () -> extraction.extractRange(co, FROM, TO)));
    List<String> legacy = legacy(co);
    List<String> extracted =
        candidates.findByCompanyIdAndStageIn(co, List.of(RenewalStage.EXTRACTED)).stream()
            .map(RenewalCandidate::getRenewalRef)
            .toList();
    for (String ref : extracted) {
      step(
          "initiation " + ref,
          () -> users.run(PROC_TL, () -> initiation.initiate(co, List.of(ref))));
    }
    marketing(co, legacy);
    processing(co);
    step(
        "package choice",
        () ->
            users.run(
                PO,
                () ->
                    choices.propose(
                        co,
                        legacy.get(1),
                        "PAR01",
                        1,
                        "Home package withdrawn; renews on the standard fire package")));
    LOG.info("Renewal seed data: {} renewals", candidates.count());
  }

  private List<String> legacy(Long co) {
    Object[][] rows = {
      {
        "QPS-FI-0100001",
        "QPS-MOTOR-A",
        "MTR12",
        "MOTOR",
        "CBG",
        "CL-2026-000001",
        "2027-11-15",
        "PN-SEED-0001",
        false
      },
      {
        "QPS-FI-0100002",
        "QPS-HOME-B",
        "PAR01",
        "PROPERTY",
        "RETAIL",
        "CL-2026-000002",
        "2027-12-01",
        null,
        false
      },
      {
        "QPS-FI-0100003",
        "QPS-HOME-OLD",
        "PAR01",
        "PROPERTY",
        "CBG",
        "CL-2026-000005",
        "2028-01-20",
        "PN-SEED-0003",
        true
      },
      {
        "EBIX-CG-0100004",
        null,
        "CGL01",
        "LIABILITY",
        "CORBANK",
        "CL-2026-000003",
        "2027-12-15",
        null,
        false
      },
      {
        "EBIX-CG-0100005",
        null,
        "CGL01",
        "LIABILITY",
        "CORBANK",
        "CL-2026-000004",
        "2027-12-20",
        null,
        false
      },
      {
        "EBIX-EN-0100006",
        null,
        "CAR07",
        "ENGINEERING",
        "CORBANK",
        "CL-2026-000004",
        "2027-11-30",
        null,
        false
      },
      {
        "QPS-FI-0100007",
        null,
        "MTR10",
        "MOTOR",
        "RETAIL",
        "CL-2026-000002",
        "2028-01-10",
        null,
        true
      }
    };
    List<String> refs = new java.util.ArrayList<>();
    for (Object[] r : rows) {
      LocalDate expiry = LocalDate.parse((String) r[6]);
      LegacyHeader header =
          new LegacyHeader(
              (String) r[0],
              ((String) r[0]).startsWith("QPS") ? "QPS" : "EBIX",
              null,
              new LegacyPolicy(
                  "FI-" + r[0],
                  null,
                  (String) r[2],
                  (String) r[3],
                  (String) r[1],
                  null,
                  expiry.minusYears(1),
                  expiry,
                  new BigDecimal("1500000.00"),
                  new BigDecimal("18500.00"),
                  "PHP",
                  (String) r[7]),
              new LegacyParties(
                  (String) r[5],
                  CLIENTS.get((String) r[5]),
                  CLIENTS.get((String) r[5]),
                  MGIC,
                  AO,
                  "T-CBG1",
                  (String) r[4],
                  null),
              (Boolean) r[8],
              null);
      try {
        refs.add(
            users
                .as(PROC_TL, () -> extraction.createLegacy(co, header, null, false))
                .getRenewalRef());
      } catch (RuntimeException e) {
        LOG.warn("Seed legacy policy {} skipped: {}", r[0], e.getMessage());
        refs.add(null);
      }
    }
    return refs;
  }

  private void marketing(Long co, List<String> legacy) {
    String review = legacy.get(3);
    String transfer = legacy.get(4);
    step("assign", () -> users.run(TL, () -> assignments.assign(co, List.of(review), AO, null)));
    step("history", () -> users.run(AO, () -> history.open(co, review)));
    step(
        "disposition",
        () ->
            users.run(
                AO,
                () ->
                    dispositions.save(
                        co,
                        review,
                        new RenewalDispositionService.Input(
                            RenewalDisposition.FOR_RENEWAL,
                            null,
                            null,
                            null,
                            "Client confirmed the renewal by phone"))));
    step("push", () -> users.run(AO, () -> dispositions.push(co, List.of(review))));
    step(
        "transfer",
        () ->
            users.run(
                TL,
                () ->
                    transfers.request(
                        co,
                        transfer,
                        new TransferService.Request(
                            "T-CORP1",
                            "WRONG_UNIT",
                            "Corporate client handled by Corporate Team 1"))));
  }

  private void processing(Long co) {
    List<RenewalCandidate> stp =
        candidates.findByCompanyIdAndStageIn(co, List.of(RenewalStage.FOR_PROCESSING)).stream()
            .filter(c -> c.getExpiringArn() != null)
            .toList();
    if (stp.isEmpty()) {
      return;
    }
    RenewalCandidate first = stp.get(0);
    List<String> refs = stp.stream().limit(2).map(RenewalCandidate::getRenewalRef).toList();
    step("assign PO", () -> users.run(PROC_TL, () -> processing.assign(co, refs, PO)));
    step(
        "insurer batch",
        () ->
            users.run(
                PO,
                () -> {
                  String insurer = first.getSnapshot().insurerCode();
                  var batch =
                      batches.create(co, insurer, first.getExpiryDate(), first.getExpiryDate());
                  batches.send(co, batch.getBatchNo());
                }));
    step(
        "insurer response",
        () ->
            users.run(
                PO,
                () ->
                    responses.manual(
                        co,
                        first.getRenewalRef(),
                        new InsurerResponse.Content(
                            InsurerResponseCode.RENEW_AS_IS,
                            "MGIC-REN-2027-0001",
                            null,
                            null,
                            null,
                            null,
                            null,
                            "Renewed on the same terms"),
                        false)));
  }

  private static void step(String what, Runnable work) {
    try {
      work.run();
    } catch (RuntimeException e) {
      LOG.warn("Renewal seed step {} skipped: {}", what, e.getMessage());
    }
  }
}
