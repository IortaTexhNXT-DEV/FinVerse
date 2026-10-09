package com.iortatechnxt.brokerverse.renewal.lamd;

import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.PROC_TL;
import static com.iortatechnxt.brokerverse.renewal.RenewalFixtures.TL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.bulk.domain.BulkJob;
import com.iortatechnxt.brokerverse.bulk.service.BulkContext;
import com.iortatechnxt.brokerverse.bulk.service.BulkRow;
import com.iortatechnxt.brokerverse.bulk.service.BulkService;
import com.iortatechnxt.brokerverse.bulk.service.BulkUpload;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.renewal.RenewalFixtures;
import com.iortatechnxt.brokerverse.renewal.candidate.service.CandidateQueryService;
import com.iortatechnxt.brokerverse.renewal.domain.CheckOutcome;
import com.iortatechnxt.brokerverse.renewal.domain.CheckResult;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalCandidate;
import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import com.iortatechnxt.brokerverse.renewal.extraction.service.ExtractionService;
import com.iortatechnxt.brokerverse.renewal.insurer.service.InsurerDispositionService;
import com.iortatechnxt.brokerverse.renewal.lamd.service.BdofcSoldHandler;
import com.iortatechnxt.brokerverse.renewal.lamd.service.LamdCbgLoansHandler;
import com.iortatechnxt.brokerverse.renewal.lamd.service.LamdPaidOffHandler;
import com.iortatechnxt.brokerverse.renewal.lamd.service.RmuOfficerService;
import com.iortatechnxt.brokerverse.renewal.rules.service.InitiationService;
import com.iortatechnxt.brokerverse.renewal.service.port.LegacyPolicySource.LegacyHeader;
import com.iortatechnxt.brokerverse.renewal.service.port.LegacyPolicySource.LegacyParties;
import com.iortatechnxt.brokerverse.renewal.service.port.LegacyPolicySource.LegacyPolicy;
import com.iortatechnxt.brokerverse.renewal.upload.service.RenewalUploadCentre;
import com.iortatechnxt.brokerverse.renewal.upload.service.UploadKind;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * BDOI's Renewal uploads against the real database (FRRN.012, FRRN.013, FRRN.041.01): the LAMD
 * lists with the loan status, the paid-off repository and the RMU Account Officers, the BDOFC and
 * BDOSOLD reports, the insurer dispositions and the upload outcome summaries.
 */
@IntegrationTest
class RenewalUploadsIT {

  private static final String LAMD_USER = "lamd";

  @Autowired private RenewalFixtures fx;
  @Autowired private ExtractionService extraction;
  @Autowired private InitiationService initiation;
  @Autowired private LamdCbgLoansHandler cbgLoans;
  @Autowired private LamdPaidOffHandler paidOff;
  @Autowired private BdofcSoldHandler bdofc;
  @Autowired private RmuOfficerService rmu;
  @Autowired private BulkService bulk;
  @Autowired private RenewalUploadCentre centre;
  @Autowired private InsurerDispositionService insurerDispositions;
  @Autowired private CandidateQueryService queries;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;

  /** A migrated CBG home loan renewal with its own PN, initiated. */
  private RenewalCandidate homeLoan(String pn) {
    RenewalCandidate c =
        as.run(
            PROC_TL,
            () ->
                extraction.createLegacy(
                    fx.company(),
                    new LegacyHeader(
                        "QPS-" + pn,
                        "QPS",
                        null,
                        new LegacyPolicy(
                            "FI-" + pn,
                            null,
                            "PAR01",
                            "PROPERTY",
                            null,
                            null,
                            LocalDate.of(2027, 3, 1),
                            LocalDate.of(2028, 3, 1),
                            new BigDecimal("3000000.00"),
                            new BigDecimal("9000.00"),
                            "PHP",
                            pn),
                        new LegacyParties(
                            "CL-2026-000001",
                            "Seed Client",
                            null,
                            "INS-MGIC",
                            "ao",
                            "T-CBG1",
                            "CBG",
                            "BDO Unibank"),
                        false,
                        null),
                    null,
                    false));
    as.run(TL, () -> initiation.initiate(fx.company(), List.of(c.getRenewalRef())));
    return fx.reload(c);
  }

  private static String pn() {
    return "PN-U" + System.nanoTime() % 10_000_000;
  }

  private static BulkContext context(String jobNo, Map<String, String> parameters) {
    return new BulkContext(1L, jobNo, LocalDate.of(2026, 9, 20), parameters);
  }

  @Test
  void aCbgLoanOfAnRmuOfficerIsRmuAndAMotorLoanNeedsItsSerialAndMotorNumbers() {
    String pn = pn();
    RenewalCandidate c = homeLoan(pn);
    String ao = "RMU" + pn.substring(pn.length() - 6);
    as.run(
        "badmin",
        () ->
            rmu.add(fx.company(), new RmuOfficerService.Input(ao, "Remedial Officer", null, null)));
    assertThatThrownBy(
            () ->
                as.run(
                    "badmin",
                    () ->
                        rmu.add(
                            fx.company(),
                            new RmuOfficerService.Input(ao, "Remedial Officer", null, null))))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("Duplicate entry detected");

    BulkContext ctx =
        new BulkContext(fx.company(), "LMDC-" + pn, LocalDate.of(2026, 9, 20), Map.of());
    BulkRow motorWithout =
        new BulkRow(
            2,
            Map.of(
                "PN Number", pn, "Officer (AO Code) PN", ao, "Product Description", "AUTO LOAN"));
    assertThat(cbgLoans.validate(motorWithout, ctx))
        .contains("The Serial No. and Motor No. of a motor loan are required");

    BulkRow home =
        new BulkRow(
            3,
            Map.of(
                "PN Number", pn, "Officer (AO Code) PN", ao, "Product Description", "HOME LOAN"));
    assertThat(cbgLoans.validate(home, ctx)).isEmpty();
    var outcome = as.run(LAMD_USER, () -> cbgLoans.process(home, ctx));
    assertThat(outcome.category()).isEqualTo("MATCHED");
    assertThat(
            jdbc.queryForObject(
                "select loan_status from rnw_lamd_loan where job_no = ? and row_no = 3",
                String.class,
                "LMDC-" + pn))
        .isEqualTo("RMU");
    assertThat(fx.reload(c).getStage()).isNotEqualTo(RenewalStage.FOR_PROCESSING);

    BulkRow unknown =
        new BulkRow(
            4,
            Map.of(
                "PN Number",
                pn + "X",
                "Officer (AO Code) PN",
                "AO1",
                "Product Description",
                "HOME"));
    assertThat(as.run(LAMD_USER, () -> cbgLoans.process(unknown, ctx)).category())
        .isEqualTo("UNMATCHED");
  }

  @Test
  void aPaidOffListUploadIsSummarisedAndTheRepositoryKeepsEachAccountOnce() {
    String pn = pn();
    RenewalCandidate c = homeLoan(pn);
    String csv = "PN Number,Product Type Desc\n" + pn + ",HOME LOAN\n" + pn + "Z,HOME LOAN\n";
    BulkJob job =
        as.run(
            LAMD_USER,
            () ->
                bulk.upload(
                    new BulkUpload(
                        fx.company(),
                        LamdPaidOffHandler.CODE,
                        "paid-off.csv",
                        csv.getBytes(StandardCharsets.UTF_8),
                        Map.of())));
    as.run(LAMD_USER, () -> bulk.commit(job.getId()));
    assertThat(fx.reload(c).getStage()).isEqualTo(RenewalStage.LETTER_PENDING);
    Integer kept =
        jdbc.queryForObject(
            "select count(*) from rnw_paid_off_account where pn_no = ?", Integer.class, pn);
    assertThat(kept).isEqualTo(1);

    var uploads =
        as.run(LAMD_USER, () -> centre.uploads(fx.company(), UploadKind.LAMD, "paid-off", null));
    assertThat(uploads)
        .anyMatch(
            u ->
                u.uploadId().equals(job.getJobNo())
                    && RenewalUploadCentre.SUCCESSFUL.equals(u.status()));
    var records = as.run(LAMD_USER, () -> centre.records(fx.company(), job.getJobNo(), null, null));
    assertThat(records)
        .extracting(RenewalUploadCentre.Record::matchingStatus)
        .containsExactlyInAnyOrder("MATCHED", "UNMATCHED");
    assertThat(
            as.run(
                LAMD_USER, () -> centre.records(fx.company(), job.getJobNo(), "UNMATCHED", null)))
        .hasSize(1);
    var file =
        as.run(LAMD_USER, () -> centre.result(fx.company(), UploadKind.LAMD, job.getJobNo()));
    assertThat(file.fileName()).matches("LAMD Processing Result_ \\d{8}\\.xlsx");

    BulkJob again =
        as.run(
            LAMD_USER,
            () ->
                bulk.upload(
                    new BulkUpload(
                        fx.company(),
                        LamdPaidOffHandler.CODE,
                        "paid-off-again.csv",
                        csv.getBytes(StandardCharsets.UTF_8),
                        Map.of())));
    as.run(LAMD_USER, () -> bulk.commit(again.getId()));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from rnw_paid_off_account where pn_no = ?", Integer.class, pn))
        .isEqualTo(1);
    assertThat(RenewalUploadCentre.SUCCESSFUL).isNotEqualTo(RenewalUploadCentre.PARTIAL);
  }

  @Test
  void aRenewalInABdosoldReportIsHeldForReview() {
    String pn = pn();
    RenewalCandidate c = homeLoan(pn);
    BulkContext ctx =
        new BulkContext(
            fx.company(),
            "BDO-" + pn,
            LocalDate.of(2026, 9, 20),
            Map.of(BdofcSoldHandler.PARAM_TYPE, "BDOSOLD"));
    BulkRow row =
        new BulkRow(2, Map.of("PN", pn, "Assured's Name", "Seed Client", "Remarks", "Unit sold"));
    assertThat(bdofc.validate(row, ctx)).isEmpty();
    assertThat(as.run(LAMD_USER, () -> bdofc.process(row, ctx)).category()).isEqualTo("MATCHED");
    CheckResult result =
        as.run(PROC_TL, () -> queries.latestResults(fx.reload(c))).stream()
            .filter(r -> r.getCheckCode().equals("BDOFC_SOLD"))
            .findFirst()
            .orElseThrow();
    assertThat(result.getOutcome()).isEqualTo(CheckOutcome.FAIL);
    assertThat(result.getMessage()).contains("BDOSOLD: Unit sold");
    assertThat(bdofc.validate(new BulkRow(3, Map.of("PN", pn)), context("X", Map.of())))
        .hasSizeGreaterThanOrEqualTo(2);
  }

  @Test
  void theNineInsurerDispositionsMapToTheBrdResponsesWithBdoisBucketAndDisposition() {
    var values = insurerDispositions.values();
    assertThat(values).hasSize(9);
    assertThat(insurerDispositions.value("Renew As Is / Approved").orElseThrow().bucket())
        .isEqualTo("CLEAN");
    assertThat(insurerDispositions.value("Approved").orElseThrow().code()).isEqualTo("RENEW_AS_IS");
    var loss = insurerDispositions.value("Total Loss").orElseThrow();
    assertThat(loss.bucket()).isEqualTo("NON_RENEWABLE");
    assertThat(loss.disposition()).isEqualTo("NOT_FOR_RENEWAL");
    assertThat(insurerDispositions.value("Renew with surcharge").orElseThrow().brdResponse())
        .isEqualTo("REVISE");
    assertThat(insurerDispositions.value("rejected").orElseThrow().disposition())
        .isEqualTo("FOR_DISPOSITION");
    String pn = pn();
    RenewalCandidate c = homeLoan(pn);
    var match =
        insurerDispositions.find(
            fx.company(), new InsurerDispositionService.Keys(null, null, "FI-" + pn));
    assertThat(match).isPresent();
    assertThat(match.get().candidate().getId()).isEqualTo(c.getId());
    assertThat(match.get().on()).isEqualTo("EXPIRING_POLICY");
  }
}
