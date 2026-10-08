package com.iortatechnxt.brokerverse.submitted;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.report.core.ReportRow;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.submitted.domain.SbmAssured;
import com.iortatechnxt.brokerverse.submitted.domain.SbmBusinessType;
import com.iortatechnxt.brokerverse.submitted.domain.SbmHistorySource;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLamdLoan;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLamdLoan.LoanFacts;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLamdLoan.LoanKey;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLamdLoanRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLoan;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLoanMatch;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLoanMatch.MatchKey;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLoanMatchRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmMarks;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyData;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyStatus;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRisk;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRun;
import com.iortatechnxt.brokerverse.submitted.domain.SbmTerms;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistService;
import com.iortatechnxt.brokerverse.submitted.masterlist.service.MasterlistService.UpsertContext;
import com.iortatechnxt.brokerverse.submitted.processing.service.SbmProcessingService;
import com.iortatechnxt.brokerverse.submitted.processing.service.SbmProcessingService.RunRequest;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import com.iortatechnxt.brokerverse.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Loan files LAMD, LMS and LAD and the matching log of the processing run (FR-SP-035). */
@IntegrationTest
class LoanFilesIT {

  private static final String TL = "sbmtl";

  @Autowired private TestData data;
  @Autowired private AsUser as;
  @Autowired private MasterlistService masterlist;
  @Autowired private SbmProcessingService processing;
  @Autowired private SbmLamdLoanRepository loans;
  @Autowired private SbmLoanMatchRepository matches;
  @Autowired private ReportService reports;

  private Long company() {
    return data.company().getId();
  }

  private static String token(String prefix) {
    return prefix + "-" + System.nanoTime();
  }

  private void loan(String report, LocalDate date, String pn, String application) {
    as.run(
        TL,
        () ->
            loans.save(
                new SbmLamdLoan(
                    company(),
                    new LoanKey(report, date, pn, application),
                    new LoanFacts("Borrower", "ACTIVE", false, null, null, null, null, null),
                    null)));
  }

  private SbmPolicy intake(String pn, String application) {
    LocalDate expiry = LocalDate.now().plusDays(60);
    SbmPolicyData d =
        new SbmPolicyData(
            "NONCBG_RETAIL",
            SbmBusinessType.NB,
            new SbmLoan(pn, application, "CIF-1", null, null, null, null, "Loan Borrower"),
            new SbmAssured("Assured " + pn, "Makati City", null, null, "a@example.ph", null),
            new SbmTerms(
                "INS-LAC",
                "POL-" + pn,
                expiry.minusYears(1),
                expiry,
                null,
                new BigDecimal("900000"),
                new BigDecimal("15000.00"),
                "PHP"),
            new SbmRisk(
                null, null, null, null, null, null, null, "Makati City", "Residential", null),
            SbmMarks.NONE);
    return as.run(
        TL,
        () ->
            masterlist
                .upsert(
                    company(),
                    d,
                    UpsertContext.of(
                        "SPI",
                        null,
                        LocalDate.now(),
                        SbmPolicyStatus.RECEIVED,
                        SbmHistorySource.INTAKE,
                        "IT"))
                .policy());
  }

  private SbmLoanMatch processed(SbmPolicy p) {
    SbmRun run =
        as.run(
            "sanitation",
            () ->
                processing.run(
                    new RunRequest(company(), SbmRun.Trigger.MANUAL, "IT", List.of(p.getId()))));
    List<SbmLoanMatch> log = matches.findByPolicyIdOrderByIdDesc(p.getId());
    assertThat(log).isNotEmpty();
    assertThat(log.get(0).getRunId()).isEqualTo(run.getId());
    return log.get(0);
  }

  @Test
  void theLoanFilesAreMatchedOnThePnNumberThenOnTheLoanApplicationNumber() {
    LocalDate day = LocalDate.now().minusDays(1);
    String pn = token("PN-L");
    loan("LAD", day, pn, null);
    loan("LMS", day, pn, null);
    SbmLoanMatch byPn = processed(intake(pn, null));
    assertThat(byPn.getOutcome()).isEqualTo(SbmLoanMatch.Outcome.MATCHED);
    assertThat(byPn.getKeyUsed()).isEqualTo(MatchKey.PN);
    assertThat(byPn.getLoanReport()).isEqualTo("LMS");
    assertThat(byPn.getSnapshotDate()).isEqualTo(day);

    String application = token("LA");
    loan("LAD", day, null, application);
    SbmLoanMatch byApplication = processed(intake(token("PN-NONE"), application));
    assertThat(byApplication.getKeyUsed()).isEqualTo(MatchKey.LOAN_APPLICATION);
    assertThat(byApplication.getLoanReport()).isEqualTo("LAD");

    SbmLoanMatch unmatched = processed(intake(token("PN-U"), token("LA-U")));
    assertThat(unmatched.getOutcome()).isEqualTo(SbmLoanMatch.Outcome.UNMATCHED);
    assertThat(unmatched.getReason()).startsWith("No loan file holds PN number");
  }

  @Test
  void theReportListsTheActiveLoansWithoutAPolicyInForce() {
    LocalDate day = LocalDate.now();
    String uninsured = token("PN-UNINSURED");
    String insured = token("PN-INSURED");
    loan("LMS", day, uninsured, null);
    loan("LMS", day, insured, null);
    intake(insured, null);
    Map<String, String> params = new HashMap<>();
    params.put("companyId", company().toString());
    params.put("to", LocalDate.now().toString());
    List<Map<String, Object>> rows =
        as.run(TL, () -> reports.run("SBM-EXPIRING-UNINSURED", params).rows()).stream()
            .map(ReportRow::cells)
            .toList();
    assertThat(rows).anyMatch(r -> uninsured.equals(r.get("pn_no")));
    assertThat(rows).noneMatch(r -> insured.equals(r.get("pn_no")) && r.get("sbm_no") == null);
  }
}
