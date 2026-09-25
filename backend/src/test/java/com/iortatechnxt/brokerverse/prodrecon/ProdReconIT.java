package com.iortatechnxt.brokerverse.prodrecon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.account.domain.Account;
import com.iortatechnxt.brokerverse.booking.BookingFixtures;
import com.iortatechnxt.brokerverse.booking.domain.BookedInvoice;
import com.iortatechnxt.brokerverse.booking.domain.BookingSource;
import com.iortatechnxt.brokerverse.booking.service.BookingOptions;
import com.iortatechnxt.brokerverse.booking.service.BookingService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums.RunStatus;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.ExtractRepositoryService;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconCycle;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.Frequency;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.MatchMethod;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.ReconStatus;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.UnbookedStatus;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconEnums.UploadStatus;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconExtract;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconItem;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconItem.Feedback;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconSchedule;
import com.iortatechnxt.brokerverse.prodrecon.domain.ReconUploadRepository;
import com.iortatechnxt.brokerverse.prodrecon.service.EarlyIncentiveValidator;
import com.iortatechnxt.brokerverse.prodrecon.service.EarlyIncentiveValidator.Outcome;
import com.iortatechnxt.brokerverse.prodrecon.service.ProdReconWorkCounts;
import com.iortatechnxt.brokerverse.prodrecon.service.ProductionExtractJob;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconAutomatchJob;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconCycleService;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconItemService;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconItemService.BulkChange;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconMatchingService;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconScheduleService;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconSendService;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconUploadService;
import com.iortatechnxt.brokerverse.prodrecon.service.ReconUploadService.UploadResult;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

/**
 * Production reconciliation end to end (PRCID.001-039): extraction, locked workbook, sending,
 * insurer upload with duplicate block, matching buckets, feedback and disposition, manual pairing,
 * pre-booked production matched on booking, automatch, closing, schedules and reports.
 */
@IntegrationTest
class ProdReconIT {

  private static final String RECON = "recon";
  private static final String NO_BOOKING = "NO-BOOKING";

  @Autowired private ReconFixtures fx;
  @Autowired private BookingFixtures booking;
  @Autowired private BookingService bookings;
  @Autowired private ReconCycleService cycles;
  @Autowired private ReconSendService sender;
  @Autowired private ReconUploadService uploads;
  @Autowired private ReconUploadRepository uploadRows;
  @Autowired private ReconItemService items;
  @Autowired private ReconMatchingService matching;
  @Autowired private ReconScheduleService schedules;
  @Autowired private ProductionExtractJob extractJob;
  @Autowired private ReconAutomatchJob automatch;
  @Autowired private ProdReconWorkCounts counts;
  @Autowired private EarlyIncentiveValidator incentives;
  @Autowired private ExtractRepositoryService repository;
  @Autowired private MessageService messages;
  @Autowired private ReportService reports;
  @Autowired private AsUser as;

  private List<ReconItem> insurerOnly(Long cycleId, String ref) {
    return as.run(
            RECON,
            () ->
                items.items(
                    cycleId,
                    new ReconItemService.ItemFilter(
                        ReconItemService.Bucket.INSURER_ONLY, ref, null, null, null, null),
                    PageRequest.of(0, 20)))
        .getContent();
  }

  @Test
  void aRegisterIsExtractedSentUploadedAndMatched() throws IOException {
    OpsInvoice a = fx.invoice();
    OpsInvoice b = fx.invoice();
    ReconExtract extract = fx.extract();
    assertThat(extract.getRowCount()).isGreaterThanOrEqualTo(2);
    assertThat(extract.getFileName()).startsWith("INS-MGIC_PRODREG_202609_").endsWith(".xlsx");
    Long cycleId = extract.getCycleId();
    assertThat(fx.item(cycleId, a.getInvoiceNo()).getStatus()).isEqualTo(ReconStatus.BDOI_ONLY);

    byte[] workbook = repository.download(extract.getFileId()).getContent();
    try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(workbook))) {
      XSSFSheet sheet = wb.getSheetAt(0);
      assertThat(sheet.getProtect()).isTrue();
      int remarks = sheet.getRow(0).getLastCellNum() - 1;
      assertThat(sheet.getRow(0).getCell(remarks).getStringCellValue()).isEqualTo("Remarks");
      assertThat(sheet.getRow(1).getCell(remarks).getCellStyle().getLocked()).isFalse();
      assertThat(sheet.getRow(1).getCell(2).getCellStyle().getLocked()).isTrue();
    }

    ReconExtract sent =
        as.run(RECON, () -> sender.send(extract.getId(), List.of("recon@insurer-demo.ph"), null));
    assertThat(sent.getSentAt()).isNotNull();
    assertThat(sent.getRecipients()).isEqualTo("recon@insurer-demo.ph");
    assertThat(cycles.require(cycleId).getStage()).isIn(ReconCycle.SENT, ReconCycle.RECONCILING);
    assertThat(messages.forRecord(ReconCycleService.ENTITY, cycleId.toString())).isNotEmpty();

    String unknown = "INV-" + BookingFixtures.token();
    byte[] file =
        ReconFixtures.file(
            List.of(
                ReconFixtures.line(a, BigDecimal.ZERO),
                ReconFixtures.line(b, new BigDecimal("5.00")),
                ReconFixtures.row(unknown, NO_BOOKING, "Unknown Assured", BigDecimal.TEN, null),
                ",INS-MGIC,NO-MONTH,,,,,,,"));
    String name = "mgic-" + BookingFixtures.token() + ".csv";
    UploadResult result = as.run(RECON, () -> uploads.upload(fx.company(), name, file));
    assertThat(result.run().getStatus()).isEqualTo(RunStatus.PARTIAL);
    assertThat(result.attempts())
        .singleElement()
        .satisfies(u -> assertThat(u.getStatus()).isEqualTo(UploadStatus.PROCESSED));

    ReconItem matched = fx.item(cycleId, a.getInvoiceNo());
    assertThat(matched.getStatus()).isEqualTo(ReconStatus.MATCHED);
    assertThat(matched.isInOriginalExtract()).isTrue();
    assertThat(matched.getMatchMethod()).isEqualTo(MatchMethod.AUTO);
    ReconItem discrepant = fx.item(cycleId, b.getInvoiceNo());
    assertThat(discrepant.getStatus()).isEqualTo(ReconStatus.MATCHED_WITH_DISCREPANCY);
    assertThat(discrepant.getDiscrepancies()).isEqualTo("GROSS_PREMIUM");
    assertThat(insurerOnly(cycleId, unknown))
        .singleElement()
        .satisfies(
            i -> {
              assertThat(i.getStatus()).isEqualTo(ReconStatus.UNMATCHED_NO_BOOKING);
              assertThat(i.getUnbookedStatus()).isEqualTo(UnbookedStatus.OPEN);
            });
    assertThat(cycles.require(cycleId).getStage()).isEqualTo(ReconCycle.RECONCILING);
    assertThat(cycles.view(cycleId).counts()).containsKey(ReconStatus.MATCHED);

    assertThatThrownBy(() -> as.run(RECON, () -> uploads.upload(fx.company(), "again.csv", file)))
        .isInstanceOf(BusinessRuleException.class)
        .extracting(e -> ((BusinessRuleException) e).getCode())
        .isEqualTo("RECON_UPLOAD_DUPLICATE");
    assertThat(uploadRows.findByCycleIdOrderByIdDesc(cycleId))
        .extracting(u -> u.getStatus())
        .contains(UploadStatus.DUPLICATE_BLOCKED, UploadStatus.PROCESSED);
  }

  @Test
  void feedbackDispositionAndManualPairing() {
    OpsInvoice a = fx.invoice();
    Long cycleId = fx.extract().getCycleId();
    String ref = "REF-" + BookingFixtures.token();
    fx.upload(
        List.of(ReconFixtures.row(ref, NO_BOOKING, a.getAssuredName(), a.getGrossPremium(), null)));
    ReconItem booked = fx.item(cycleId, a.getInvoiceNo());
    ReconItem insurer = insurerOnly(cycleId, ref).get(0);

    ReconItem withFeedback =
        as.run(
            RECON,
            () ->
                items.feedback(
                    insurer.getId(),
                    new Feedback(
                        "INSURER", "Check the booking", "Not ours", null, "FOR_BOOKING", false)));
    assertThat(withFeedback.getDisposition()).isEqualTo("FOR_BOOKING");
    assertThatThrownBy(
            () ->
                as.run(
                    RECON,
                    () ->
                        items.feedback(
                            insurer.getId(), new Feedback(null, null, null, null, "NOPE", false))))
        .isInstanceOf(BusinessRuleException.class);

    ReconItem paired = as.run(RECON, () -> matching.pair(booked.getId(), insurer.getId()));
    assertThat(paired.getMatchMethod()).isEqualTo(MatchMethod.MANUAL);
    assertThat(paired.getStatus()).isEqualTo(ReconStatus.MATCHED_WITH_DISCREPANCY);
    assertThat(paired.getDiscrepancies()).contains("REFERENCE_NO");
    assertThat(insurerOnly(cycleId, ref)).isEmpty();
    assertThatThrownBy(() -> as.run(RECON, () -> matching.pair(booked.getId(), booked.getId())))
        .isInstanceOf(BusinessRuleException.class);

    as.run(RECON, () -> matching.rematch(cycleId));
    assertThat(fx.item(cycleId, a.getInvoiceNo()).getMatchMethod()).isEqualTo(MatchMethod.MANUAL);

    ReconItem split = as.run(RECON, () -> matching.split(booked.getId()));
    assertThat(split.getStatus()).isEqualTo(ReconStatus.BDOI_ONLY);
    ReconItem again = insurerOnly(cycleId, ref).get(0);
    assertThatThrownBy(() -> as.run(RECON, () -> matching.split(again.getId())))
        .isInstanceOf(BusinessRuleException.class);

    List<ReconItem> closed =
        as.run(
            RECON,
            () ->
                items.bulkFeedback(
                    List.of(again.getId(), booked.getId()),
                    new BulkChange("OTHERS", "FOR_CLOSURE", true)));
    assertThat(closed).allSatisfy(i -> assertThat(i.isForClosure()).isTrue());
    assertThat(insurerOnly(cycleId, ref).get(0).getUnbookedStatus())
        .isEqualTo(UnbookedStatus.CLOSED);
    assertThat(
            as.run(
                    RECON,
                    () ->
                        items.unbooked(
                            fx.company(),
                            ReconFixtures.INSURER,
                            UnbookedStatus.CLOSED,
                            ref,
                            PageRequest.of(0, 10)))
                .getContent())
        .hasSize(1);
  }

  @Test
  void preBookedProductionIsMatchedWhenTheAccountIsBooked() {
    fx.invoice();
    Long cycleId = fx.extract().getCycleId();
    Account account = booking.motor();
    String policy = account.getPolicyNumbers().get(0);
    String ref = "PEND-" + BookingFixtures.token();
    fx.upload(List.of(ReconFixtures.row(ref, policy, account.getClientName(), null, null)));
    ReconItem waiting = insurerOnly(cycleId, ref).get(0);
    assertThat(waiting.getStatus()).isEqualTo(ReconStatus.UNMATCHED_PREBOOKED);
    assertThat(waiting.getPrebookedArn()).isEqualTo(account.getArn());
    assertThat(waiting.getUnbookedStatus()).isEqualTo(UnbookedStatus.PREBOOKED);

    BookedInvoice booked =
        as.run(
            "proc",
            () ->
                bookings.book(
                    account.getArn(),
                    BookingOptions.of(BookingFixtures.BOOKED_ON, null),
                    BookingSource.INDIVIDUAL));
    ReconItem paired = fx.item(cycleId, booked.getInvoiceNo());
    assertThat(paired.getId()).isEqualTo(waiting.getId());
    assertThat(paired.isPaired()).isTrue();
    assertThat(paired.getUnbookedStatus()).isEqualTo(UnbookedStatus.BOOKED);
    assertThat(paired.getDiscrepancies()).contains("REFERENCE_NO");
  }

  @Test
  void cyclesAreAutomatchedValidatedCountedAndClosed() {
    OpsInvoice a = fx.invoice();
    fx.invoice();
    Long cycleId = fx.extract().getCycleId();
    fx.upload(List.of(ReconFixtures.line(a, BigDecimal.ZERO)));
    assertThat(automatch.execute(BookingFixtures.BOOKED_ON).itemsProcessed()).isPositive();
    assertThat(automatch.name()).isEqualTo("RECON_AUTOMATCH");
    assertThat(automatch.cron()).isEqualTo("-");

    assertThat(counts.counts(fx.company()))
        .extracting(c -> c.key())
        .contains("RECON_TO_SEND", "RECON_AWAITING", "RECON_RECONCILING", "RECON_UNBOOKED");
    assertThat(counts.itemsFor(a.getInvoiceNo()))
        .anySatisfy(r -> assertThat(r.status()).isEqualTo(ReconStatus.MATCHED.name()));
    assertThat(incentives.validate(fx.company(), cycleId))
        .anySatisfy(
            l -> {
              assertThat(l.invoiceNo()).isEqualTo(a.getInvoiceNo());
              assertThat(l.outcome()).isEqualTo(Outcome.NO_RULE);
            });

    ReconCycle closed = as.run(RECON, () -> cycles.close(cycleId, "Reconciled"));
    assertThat(closed.isClosed()).isTrue();
    assertThat(closed.getStage()).isEqualTo(ReconCycle.CLOSED);
    assertThatThrownBy(() -> as.run(RECON, () -> matching.rematch(cycleId)))
        .isInstanceOf(BusinessRuleException.class);
    assertThat(fx.extract().getCycleId()).isNotEqualTo(cycleId);
  }

  @Test
  void aFileOutsideTheLayoutFailsItsRun() {
    UploadResult result =
        as.run(
            RECON,
            () ->
                uploads.upload(
                    fx.company(),
                    "wrong-" + BookingFixtures.token() + ".csv",
                    "Policy,Premium\nP-1,10\n".getBytes(StandardCharsets.UTF_8)));
    assertThat(result.run().getStatus()).isEqualTo(RunStatus.FAILED);
    assertThat(result.attempts())
        .singleElement()
        .satisfies(u -> assertThat(u.getStatus()).isEqualTo(UploadStatus.FAILED));
  }

  @Test
  void schedulesExtractOnTheirDayAndRollToWorkingDays() {
    String insurer = "INS-T" + BookingFixtures.token();
    ReconSchedule schedule =
        as.run(
            RECON,
            () ->
                schedules.create(
                    fx.company(),
                    insurer,
                    new ReconSchedule.Terms(Frequency.MONTHLY, 1, false, null, true)));
    assertThat(schedule.getNextRunDate().getDayOfMonth()).isLessThanOrEqualTo(10);
    assertThatThrownBy(
            () ->
                as.run(
                    RECON,
                    () ->
                        schedules.create(
                            fx.company(),
                            insurer,
                            new ReconSchedule.Terms(Frequency.MONTHLY, 1, false, null, true))))
        .isInstanceOf(DuplicateResourceException.class);
    ReconSchedule weekly =
        as.run(
            RECON,
            () ->
                schedules.update(
                    schedule.getId(),
                    new ReconSchedule.Terms(Frequency.WEEKLY, 3, true, "a@insurer.ph", true)));
    assertThat(weekly.getFrequency()).isEqualTo(Frequency.WEEKLY);
    var due = weekly.getNextRunDate();
    assertThat(extractJob.execute(due).message()).contains("skipped");
    ReconSchedule after =
        as.run(RECON, () -> schedules.list(fx.company())).stream()
            .filter(s -> s.getId().equals(schedule.getId()))
            .findFirst()
            .orElseThrow();
    assertThat(after.getNextRunDate()).isAfter(due);
    assertThat(after.getLastRunAt()).isNotNull();
    assertThat(extractJob.name()).isEqualTo("PRODUCTION_EXTRACT");
    assertThat(extractJob.description()).contains("PRCID.001");
  }

  @Test
  void everyReconciliationReportRunsAndExports() {
    fx.invoice();
    fx.extract();
    Map<String, String> params = new HashMap<>();
    params.put("companyId", fx.company().toString());
    params.put("from", "2026-01-01");
    params.put("to", "2026-12-31");
    List<String> codes =
        List.of(
            "PRC-SUMMARY",
            "PRC-UNMATCHED-LOC",
            "PRC-UNMATCHED-AO",
            "PRC-DISPOSITION",
            "PRC-REGISTER",
            "PRC-UNBOOKED",
            "PRC-EXTRACT-LOG",
            "PRC-UNMATCHED-FEEDBACK",
            "PRC-EARLY-INCENTIVE");
    as.run(
        RECON,
        () -> {
          for (String code : codes) {
            assertThat(reports.run(code, params).code()).isEqualTo(code);
            assertThat(reports.export(code, params, ExportFormat.CSV).content()).isNotEmpty();
          }
          Map<String, String> both = new HashMap<>(params);
          both.put("variant", "BOTH");
          both.put("insurer", "ins-mgic");
          assertThat(reports.run("PRC-REGISTER", both).rows()).isNotEmpty();
          return null;
        });
  }
}
