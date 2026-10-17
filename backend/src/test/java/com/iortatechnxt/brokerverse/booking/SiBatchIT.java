package com.iortatechnxt.brokerverse.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoice;
import com.iortatechnxt.brokerverse.booking.domain.SiBatch;
import com.iortatechnxt.brokerverse.booking.domain.SiBatchStatus;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceService;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceService.IssueRequest;
import com.iortatechnxt.brokerverse.booking.service.SiBatchFiles;
import com.iortatechnxt.brokerverse.booking.service.SiBatchJob;
import com.iortatechnxt.brokerverse.booking.service.SiBatchService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/** Daily service-invoice batch per insurer (G09). */
@IntegrationTest
class SiBatchIT {

  @Autowired private BookingFixtures fx;
  @Autowired private ServiceInvoiceService serviceInvoices;
  @Autowired private SiBatchService batches;
  @Autowired private SiBatchJob job;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private AsUser as;
  @Autowired private MockMvc mvc;

  private ServiceInvoice issue(String insurer, LocalDate date, String commission) {
    return as.run(
        "proc",
        () ->
            serviceInvoices.issue(
                new IssueRequest(
                    fx.company(),
                    "INSURER_COMMISSION",
                    null,
                    "ARN-BATCH",
                    insurer,
                    null,
                    date,
                    "PHP",
                    new BigDecimal(commission),
                    new BigDecimal("12.00"),
                    new BigDecimal("10.00"),
                    null)));
  }

  private static LocalDate uniqueDate() {
    return LocalDate.of(2025, 1, 1).plusDays(ThreadLocalRandom.current().nextInt(0, 360));
  }

  @Test
  void oneBatchPerInsurerAndDateIsGeneratedOnceAndSent() {
    LocalDate date = uniqueDate();
    ServiceInvoice a = issue("INS-MGIC", date, "100.00");
    ServiceInvoice b = issue("INS-MGIC", date, "200.00");

    List<SiBatch> first = as.run("proc", () -> batches.generate(date));
    SiBatch batch =
        first.stream().filter(x -> "INS-MGIC".equals(x.getInsurerCode())).findFirst().orElseThrow();
    assertThat(batch.getServiceInvoiceIds()).containsExactlyInAnyOrder(a.getId(), b.getId());
    assertThat(batch.getCommissionTotal()).isEqualByComparingTo("300.00");
    assertThat(batch.getCurrency()).isEqualTo("PHP");
    assertThat(batch.getAttempts()).isEqualTo(1);
    assertThat(batches.get(batch.getId()).getStatus()).isEqualTo(SiBatchStatus.SENT);

    issue("INS-MGIC", date, "50.00");
    assertThat(as.run("proc", () -> batches.generate(date)))
        .noneMatch(x -> "INS-MGIC".equals(x.getInsurerCode()));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from bkg_si_batch where insurer_code = ? and batch_date = ?",
                Integer.class,
                "INS-MGIC",
                date))
        .isEqualTo(1);
    assertThatThrownBy(() -> as.run("proc", () -> batches.resend(batch.getId())))
        .extracting("code")
        .isEqualTo("SI_BATCH_NOT_RESENDABLE");
  }

  @Test
  void aLateInvoiceGoesIntoASupplementaryBatchOrTheNextRun() {
    LocalDate date = uniqueDate();
    issue("INS-MGIC", date, "100.00");
    as.run("proc", () -> batches.generate(date));
    ServiceInvoice late = issue("INS-MGIC", date, "40.00");

    List<SiBatch> extra = as.run("proc", () -> batches.supplementary(fx.company(), date));
    SiBatch supplementary =
        extra.stream().filter(x -> "INS-MGIC".equals(x.getInsurerCode())).findFirst().orElseThrow();
    assertThat(supplementary.getSequenceNo()).isEqualTo(2);
    assertThat(supplementary.getServiceInvoiceIds()).containsExactly(late.getId());

    ServiceInvoice later = issue("INS-MGIC", date, "30.00");
    LocalDate next = date.plusDays(1);
    SiBatch nextRun =
        as.run("proc", () -> batches.generate(next)).stream()
            .filter(x -> "INS-MGIC".equals(x.getInsurerCode()))
            .findFirst()
            .orElseThrow();
    assertThat(nextRun.getServiceInvoiceIds()).contains(later.getId());
    assertThat(nextRun.getSequenceNo()).isEqualTo(1);
  }

  @Test
  void aBatchHasOneMergedPdfAndAManifest() {
    LocalDate date = uniqueDate();
    ServiceInvoice a = issue("INS-MGIC", date, "100.00");
    ServiceInvoice b = issue("INS-MGIC", date, "200.00");
    SiBatch batch =
        as.run("proc", () -> batches.generate(date)).stream()
            .filter(x -> "INS-MGIC".equals(x.getInsurerCode()))
            .findFirst()
            .orElseThrow();
    SiBatchFiles.Files files = batches.files(batch.getId());
    assertThat(new String(files.pdf(), 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    String manifest = new String(files.manifest(), StandardCharsets.UTF_8);
    assertThat(manifest)
        .contains(a.getSiNo(), b.getSiNo(), "PDF Pages")
        .contains("Total,,,," + batch.getItemCount() + ",");
    // The run also takes earlier service invoices of the insurer still in no batch (lookback).
    assertThat(batches.items(batch.getId()))
        .extracting(ServiceInvoice::getId)
        .contains(a.getId(), b.getId())
        .hasSize(batch.getItemCount());
    assertThat(files.pdfName()).isEqualTo("SI_BATCH_INS-MGIC_" + date + ".pdf");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from msg_outbound_attachment a join msg_outbound m"
                    + " on m.id = a.message_id where m.id = ?",
                Integer.class,
                batch.getMessageId()))
        .isEqualTo(2);
  }

  @Test
  void aFailedBatchIsRetried() {
    LocalDate date = uniqueDate();
    ServiceInvoice si = issue("INS-MGIC", date, "100.00");
    SiBatch batch =
        as.run("proc", () -> batches.generate(date)).stream()
            .filter(x -> "INS-MGIC".equals(x.getInsurerCode()))
            .findFirst()
            .orElseThrow();
    jdbc.update("update bkg_si_batch set delivery_status = 'FAILED' where id = ?", batch.getId());

    SiBatch retried = as.run("proc", () -> batches.resend(batch.getId()));
    assertThat(retried.getAttempts()).isEqualTo(2);
    assertThat(batches.get(batch.getId()).getStatus()).isEqualTo(SiBatchStatus.SENT);
    assertThat(batches.list(fx.company(), date, date))
        .extracting(SiBatch::getId)
        .contains(batch.getId());
    // The run also takes earlier service invoices of the insurer still in no batch (lookback).
    assertThat(batches.items(batch.getId()))
        .extracting(ServiceInvoice::getId)
        .contains(si.getId())
        .hasSize(batch.getItemCount());
  }

  @Test
  void theJobReportsWhatItGenerated() {
    LocalDate date = uniqueDate();
    issue("INS-MGIC", date, "70.00");
    assertThat(job.execute(date).itemsProcessed()).isGreaterThanOrEqualTo(1);
    assertThat(job.execute(date).itemsProcessed()).isZero();
  }

  @Test
  @WithMockUser(authorities = "MASTER_VIEW")
  void onlyBookingUsersSeeTheBatches() throws Exception {
    mvc.perform(get("/api/v1/booking/si-batches?companyId=1&from=2026-01-01&to=2026-01-31"))
        .andExpect(status().isForbidden());
    mvc.perform(post("/api/v1/booking/si-batches/1/resend")).andExpect(status().isForbidden());
  }
}
