package com.iortatechnxt.brokerverse.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.collections.common.domain.CollectionItem;
import com.iortatechnxt.brokerverse.collections.disposition.service.PrDispositionService;
import com.iortatechnxt.brokerverse.collections.disposition.service.PrDispositionService.DispositionCommand;
import com.iortatechnxt.brokerverse.collections.files.domain.ScheduledFile;
import com.iortatechnxt.brokerverse.collections.files.domain.ScheduledFile.Frequency;
import com.iortatechnxt.brokerverse.collections.files.domain.ScheduledFile.Spec;
import com.iortatechnxt.brokerverse.collections.files.domain.ScheduledFile.Status;
import com.iortatechnxt.brokerverse.collections.files.service.CollectionFiles;
import com.iortatechnxt.brokerverse.collections.files.service.FilePeriods;
import com.iortatechnxt.brokerverse.collections.files.service.ScheduledFileService;
import com.iortatechnxt.brokerverse.collections.report.ActivityReports;
import com.iortatechnxt.brokerverse.collections.report.ItemReports;
import com.iortatechnxt.brokerverse.collections.report.ReversalReports;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.report.core.ReportArchiveService;
import com.iortatechnxt.brokerverse.report.core.ReportService;
import com.iortatechnxt.brokerverse.report.render.ExportFormat;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

/**
 * The Collections files and reports (BRCLXN.024-029, 043/044, 045): daily files available at once,
 * weekly files per unit and branch available the next Monday, monthly files on the first working
 * day, the capped export, a failed file, and every Collections report run and exported.
 */
@IntegrationTest
class CollectionsFilesIT {

  @Autowired private CollectionsFixtures fx;
  @Autowired private CollectionFiles files;
  @Autowired private ScheduledFileService published;
  @Autowired private PrDispositionService dispositions;
  @Autowired private ReportArchiveService archive;
  @Autowired private ReportService reports;
  @Autowired private AsUser as;

  @AfterEach
  void drainOutbox() {
    fx.drainOutbox();
  }

  private void tagDp(CollectionItem item) {
    as.run(
        CollectionsFixtures.HANDLER,
        () ->
            dispositions.record(
                fx.company(),
                new DispositionCommand(
                    List.of(item.getInvoiceNo()), "DP_PR_FOR_REVERSAL", "Paid", null, Map.of())));
  }

  @Test
  void dailyFilesArePublishedAvailableAtOnceAndOncePerDay() {
    fx.listedMotor();
    LocalDate today = LocalDate.now(FilePeriods.MANILA);
    List<ScheduledFile> daily =
        as.run(CollectionsFixtures.LEAD, () -> files.daily(fx.company(), today));
    assertThat(daily)
        .hasSize(2)
        .allSatisfy(f -> assertThat(f.getStatus()).isEqualTo(Status.PUBLISHED))
        .extracting(ScheduledFile::getReportCode)
        .containsExactly(ItemReports.OUTSTANDING_PR, ItemReports.FULL_PRODUCTION);
    ScheduledFile outstanding = daily.get(0);
    assertThat(outstanding.getRowCount()).isPositive();
    assertThat(
            as.run(CollectionsFixtures.HANDLER, () -> archive.file(outstanding.getReportRunId())))
        .isNotNull();
    List<ScheduledFile> again =
        as.run(CollectionsFixtures.LEAD, () -> files.daily(fx.company(), today));
    assertThat(again.get(0).getId()).isEqualTo(outstanding.getId());
    assertThat(published.list(fx.company(), List.of(Frequency.DAILY), PageRequest.of(0, 10)))
        .isNotEmpty();
    assertThat(published.readyThisWeek(fx.company())).isPositive();
  }

  @Test
  void weeklyAndMonthlyFilesWaitForTheirAvailabilityTime() {
    tagDp(fx.listedMotor());
    // Files work on the Philippine calendar (tags are dated in Manila), so "today" must be too:
    // from 16:00 UTC the Manila date is already the next day.
    LocalDate today = LocalDate.now(FilePeriods.MANILA);
    LocalDate friday = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
    List<ScheduledFile> weekly =
        as.run(CollectionsFixtures.LEAD, () -> files.weekly(fx.company(), friday));
    assertThat(weekly)
        .anySatisfy(
            f -> {
              assertThat(f.getReportCode()).isEqualTo(ReversalReports.DP);
              assertThat(f.getScope()).startsWith("Unit ");
              assertThat(f.getRowCount()).isPositive();
              assertThat(f.getAvailableFrom())
                  .isAfter(friday.atStartOfDay().toInstant(ZoneOffset.UTC));
            });
    ScheduledFile week = weekly.get(0);
    assertThatThrownBy(
            () -> as.run(CollectionsFixtures.HANDLER, () -> archive.file(week.getReportRunId())))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("available from");

    List<ScheduledFile> monthly = new ArrayList<>();
    LocalDate day = today.plusMonths(1).withDayOfMonth(1);
    for (int i = 0; i < 7 && monthly.isEmpty(); i++) {
      LocalDate d = day.plusDays(i);
      monthly.addAll(as.run(CollectionsFixtures.LEAD, () -> files.monthly(fx.company(), d)));
    }
    assertThat(monthly)
        .hasSize(2)
        .allSatisfy(f -> assertThat(f.getFrequency()).isEqualTo(Frequency.MONTHLY));
    assertThat(monthly.get(0).getPeriodKey())
        .isEqualTo(today.getYear() + "-" + String.format("%02d", today.getMonthValue()));
    assertThat(as.run(CollectionsFixtures.LEAD, () -> files.monthly(fx.company(), day.plusDays(9))))
        .isEmpty();
  }

  @Test
  void exportsAreCappedAndAFailedFileIsRecorded() {
    fx.listedMotor();
    ScheduledFile export =
        as.run(CollectionsFixtures.HANDLER, () -> files.export(fx.company(), "CBG", null));
    assertThat(export.getFrequency()).isEqualTo(Frequency.ON_REQUEST);
    assertThat(export.getScope()).startsWith(CollectionsFixtures.HANDLER);

    ScheduledFile failed =
        as.run(
            CollectionsFixtures.LEAD,
            () ->
                published.publish(
                    fx.company(),
                    new Spec(
                        Frequency.DAILY,
                        "CLX-NO-SUCH-REPORT",
                        "2026-01-01",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 1, 1),
                        null),
                    Map.of("companyId", fx.company().toString()),
                    null));
    assertThat(failed.getStatus()).isEqualTo(Status.FAILED);
    assertThat(failed.getMessage()).isNotBlank();
  }

  @Test
  void everyCollectionsReportRunsAndExports() {
    tagDp(fx.listedMotor());
    Map<String, String> params =
        Map.of(
            "companyId", fx.company().toString(),
            "from", LocalDate.now().minusMonths(1).toString(),
            "to", LocalDate.now().plusDays(1).toString());
    for (String code :
        List.of(
            ItemReports.OUTSTANDING_PR,
            ItemReports.FULL_PRODUCTION,
            ItemReports.COMPLETED,
            ItemReports.WITH_DISPOSITION,
            ReversalReports.DP,
            ReversalReports.PR2307,
            ActivityReports.REASSIGNMENTS,
            ActivityReports.AUDIT_LOG)) {
      var result = as.run(CollectionsFixtures.HEAD, () -> reports.run(code, params));
      assertThat(result).as(code).isNotNull();
      for (ExportFormat format : List.of(ExportFormat.PDF, ExportFormat.XLSX, ExportFormat.CSV)) {
        assertThat(
                as.run(
                    CollectionsFixtures.HEAD, () -> reports.export(code, params, format).content()))
            .as(code + " " + format)
            .isNotEmpty();
      }
    }
    assertThat(
            as.run(CollectionsFixtures.HEAD, () -> reports.run(ReversalReports.DP, params).rows()))
        .isNotEmpty();
  }
}
