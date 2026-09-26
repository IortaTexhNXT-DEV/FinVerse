package com.iortatechnxt.brokerverse.acsl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.acsl.domain.GlSlControl;
import com.iortatechnxt.brokerverse.acsl.domain.GlSlRecon;
import com.iortatechnxt.brokerverse.acsl.domain.GlSlRun;
import com.iortatechnxt.brokerverse.acsl.domain.ReconBucket;
import com.iortatechnxt.brokerverse.acsl.domain.ReconResult;
import com.iortatechnxt.brokerverse.acsl.domain.SlSource;
import com.iortatechnxt.brokerverse.acsl.domain.SoaLine;
import com.iortatechnxt.brokerverse.acsl.domain.SoaUpload;
import com.iortatechnxt.brokerverse.acsl.service.AcslQueryService;
import com.iortatechnxt.brokerverse.acsl.service.GlSlReconJob;
import com.iortatechnxt.brokerverse.acsl.service.GlSlReconciliationService;
import com.iortatechnxt.brokerverse.acsl.service.SoaUploadService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Insurer SOA upload and reconciliation (ACSL 2.4.0, 2.13.0-2.14.2) and the GL-SL reconciliation
 * with its job and report (ACSL 2.13.2).
 */
@IntegrationTest
class AcslSoaIT {

  private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
  private static final LocalDate TO = LocalDate.of(2026, 12, 31);

  @Autowired private AcslFixtures fx;
  @Autowired private SoaUploadService soa;
  @Autowired private AcslQueryService queries;
  @Autowired private GlSlReconciliationService glsl;
  @Autowired private GlSlReconJob job;
  @Autowired private ReportService reports;
  @Autowired private AsUser as;

  @Test
  void anSoaUploadProducesTheUploadLogAndTheReconciliationReport() {
    OpsInvoice outstanding = fx.invoice();
    OpsInvoice direct = fx.directInvoice();
    byte[] file =
        AcslFixtures.file(
            AcslFixtures.row(
                outstanding.getInvoiceNo(),
                outstanding.getGrossPremium().toPlainString(),
                "\"" + "1,000.00" + "\""),
            AcslFixtures.row(direct.getInvoiceNo(), direct.getGrossPremium().toPlainString(), "0"),
            AcslFixtures.row("NOT-BOOKED-" + System.nanoTime(), "500", "(500.00)"),
            ",,No keys,2026-01-01,2026-12-31,1,1,0\n",
            AcslFixtures.row(outstanding.getInvoiceNo() + "X", "abc", "0"));
    SoaUpload upload =
        as.run(
            "acsl",
            () ->
                soa.upload(
                    fx.company(),
                    new SoaUpload.Period(AcslFixtures.insurer(outstanding), FROM, TO),
                    "insurer-soa.csv",
                    file));
    assertThat(upload.getUploadNo()).startsWith("SOA-");
    assertThat(upload.getRowsRead()).isEqualTo(5);
    assertThat(upload.getRowsLoaded()).isEqualTo(3);
    assertThat(upload.getRowsFailed()).isEqualTo(2);
    assertThat(upload.getLastRunId()).isNotNull();
    assertThat(upload.reportName()).isEqualTo(outstanding.getInsurerCode() + "_" + FROM + "_" + TO);

    List<SoaLine> log = queries.uploadLog(upload.getId());
    assertThat(log).hasSize(5).filteredOn(l -> SoaLine.FAILED.equals(l.getStatus())).hasSize(2);
    List<ReconResult> results = queries.results(upload, null);
    assertThat(results)
        .extracting(ReconResult::getBucket)
        .containsExactlyInAnyOrder(
            ReconBucket.OUTSTANDING, ReconBucket.DIRECT_BILLED, ReconBucket.NOT_FOUND);
    ReconResult first =
        results.stream()
            .filter(r -> r.getBucket() == ReconBucket.OUTSTANDING)
            .findFirst()
            .orElseThrow();
    assertThat(first.getRootInvoiceNo()).isEqualTo(outstanding.getRootInvoiceNo());
    assertThat(first.bookOrNone().outstanding()).isEqualByComparingTo(outstanding.premiumBalance());
    assertThat(first.getOutstandingVariance())
        .isEqualByComparingTo(
            new java.math.BigDecimal("1000.00").subtract(outstanding.premiumBalance()));
    assertThat(queries.results(upload, ReconBucket.NOT_FOUND))
        .singleElement()
        .satisfies(r -> assertThat(r.getSoaBalance()).isEqualByComparingTo("-500.00"));
    assertThat(queries.lastRun(upload))
        .get()
        .satisfies(r -> assertThat(r.getNotFound()).isEqualTo(1));

    SoaUpload again = as.run("acsl", () -> soa.reconcile(upload.getId()));
    assertThat(queries.lastRun(again)).get().satisfies(r -> assertThat(r.getRunNo()).isEqualTo(2));
    assertThatThrownBy(
            () ->
                as.run(
                    "acsl",
                    () ->
                        soa.upload(
                            fx.company(),
                            new SoaUpload.Period(AcslFixtures.insurer(outstanding), FROM, TO),
                            "again.csv",
                            file)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining(upload.getUploadNo());

    Map<String, String> params =
        Map.of("companyId", String.valueOf(fx.company()), "uploadNo", upload.getUploadNo());
    String csv =
        as.run(
            "acsl",
            () -> {
              assertThat(reports.run("ACSL-SOA-UPLOAD-LOG", params).code())
                  .isEqualTo("ACSL-SOA-UPLOAD-LOG");
              return new String(
                  reports.export("ACSL-SOA-RECON", params, ExportFormat.CSV).content(),
                  StandardCharsets.UTF_8);
            });
    assertThat(csv).contains(outstanding.getInvoiceNo()).contains("NOT_FOUND");
  }

  @Test
  void aFileWithoutTheLayoutColumnsOrPeriodIsRefused() {
    OpsInvoice invoice = fx.invoice();
    byte[] wrong = "Invoice,Amount\nX,1\n".getBytes(StandardCharsets.UTF_8);
    SoaUpload.Period period = new SoaUpload.Period(invoice.getInsurerCode(), FROM, TO);
    assertThatThrownBy(() -> as.run("acsl", () -> soa.upload(fx.company(), period, "w.csv", wrong)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("missing column");
    SoaUpload.Period backwards = new SoaUpload.Period(invoice.getInsurerCode(), TO, FROM);
    assertThatThrownBy(
            () ->
                as.run(
                    "acsl",
                    () -> soa.upload(fx.company(), backwards, "b.csv", AcslFixtures.file())))
        .isInstanceOf(BusinessRuleException.class);
    assertThat(soa.layouts()).anySatisfy(l -> assertThat(l.getInsurerCode()).isEqualTo("*"));
  }

  @Test
  void theGlSlReconciliationComparesEveryControlAccount() {
    fx.invoice();
    GlSlControl control =
        as.run(
            "acsltl",
            () ->
                glsl.configure(
                    fx.company(),
                    "2210",
                    new GlSlControl.Setting(SlSource.OPS_LEDGER, "DTIP", null, "PHP", true)));
    assertThat(control.componentList()).containsExactly("DTIP");
    as.run(
        "acsltl",
        () ->
            glsl.configure(
                fx.company(),
                "1220",
                new GlSlControl.Setting(SlSource.OPEN_ITEMS, null, "DEBIT_NOTE", null, true)));
    assertThatThrownBy(
            () ->
                glsl.configure(
                    fx.company(),
                    "1211",
                    new GlSlControl.Setting(SlSource.OPS_LEDGER, null, null, null, true)))
        .isInstanceOf(BusinessRuleException.class);

    GlSlRun run = as.run("acsl", () -> glsl.run(fx.company(), LocalDate.now()));
    assertThat(run.getAccounts()).isPositive();
    List<GlSlRecon> rows = glsl.rows(run.getId());
    assertThat(rows)
        .anySatisfy(
            r -> {
              assertThat(r.getAccountCode()).isEqualTo("2210");
              assertThat(r.getSource()).isEqualTo(SlSource.OPS_LEDGER);
              assertThat(r.getDifference())
                  .isEqualByComparingTo(r.getGlBalance().subtract(r.getSlBalance()));
            })
        .anySatisfy(r -> assertThat(r.getSource()).isEqualTo(SlSource.PARTY_LEDGER));
    assertThat(glsl.latest(fx.company(), LocalDate.now())).isPresent();
    assertThat(glsl.runs(fx.company())).isNotEmpty();
    assertThat(glsl.controls(fx.company())).hasSizeGreaterThanOrEqualTo(2);

    assertThat(job.name()).isEqualTo("ACSL_GL_SL_RECON");
    assertThat(job.execute(LocalDate.now()).itemsProcessed()).isPositive();
    Map<String, String> params =
        Map.of("companyId", String.valueOf(fx.company()), "asOf", LocalDate.now().toString());
    assertThat(as.run("acslhead", () -> reports.run("ACSL-GL-SL-RECON", params)).code())
        .isEqualTo("ACSL-GL-SL-RECON");
    Map<String, String> booked =
        Map.of(
            "companyId", String.valueOf(fx.company()),
            "from", LocalDate.now().minusYears(1).toString(),
            "to", LocalDate.now().plusDays(1).toString());
    assertThat(
            as.run(
                "acsl",
                () ->
                    reports.export("ACSL-BOOKED-FIN-DETAILS", booked, ExportFormat.XLSX).content()))
        .isNotEmpty();
  }
}
