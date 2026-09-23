package com.iortatechnxt.finverse.underwriting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iortatechnxt.finverse.common.exception.BusinessRuleException;
import com.iortatechnxt.finverse.organization.domain.Company;
import com.iortatechnxt.finverse.organization.service.OrganizationService;
import com.iortatechnxt.finverse.system.domain.JobRun;
import com.iortatechnxt.finverse.system.domain.JobTrigger;
import com.iortatechnxt.finverse.system.service.JobOutcome;
import com.iortatechnxt.finverse.system.service.JobRunService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class QuotationExpiryJobTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 9, 23);
  private static final Clock CLOCK =
      Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

  private final QuotationService quotations = mock(QuotationService.class);
  private final OrganizationService organization = mock(OrganizationService.class);
  private final JobRunService runs = mock(JobRunService.class);
  private final QuotationExpiryJob job =
      new QuotationExpiryJob(quotations, organization, runs, CLOCK, "0 45 0 * * *");

  private static Company company(long id, String code, boolean active) {
    Company c = mock(Company.class);
    when(c.getId()).thenReturn(id);
    when(c.getCode()).thenReturn(code);
    when(c.isActive()).thenReturn(active);
    return c;
  }

  @SuppressWarnings("unchecked")
  private void runsRecordTheWork() {
    when(runs.execute(eq(QuotationExpiryJob.JOB_NAME), eq(JobTrigger.MANUAL), any()))
        .thenAnswer(
            i -> {
              JobRun run = new JobRun(QuotationExpiryJob.JOB_NAME, JobTrigger.MANUAL, "uw", null);
              try {
                JobOutcome o = ((Supplier<JobOutcome>) i.getArgument(2)).get();
                run.succeed(o.itemsProcessed(), o.message(), Instant.EPOCH);
              } catch (RuntimeException ex) {
                run.fail(ex.getMessage(), Instant.EPOCH);
              }
              return run;
            });
  }

  @Test
  void scheduledRunExpiresEveryActiveCompany() {
    Company fvi = company(1L, "FVI", true);
    Company old = company(2L, "OLD", false);
    Company fvs = company(3L, "FVS", true);
    when(organization.listCompanies()).thenReturn(List.of(fvi, old, fvs));
    when(quotations.expireLapsed(1L, TODAY)).thenReturn(2);
    when(quotations.expireLapsed(3L, TODAY)).thenReturn(1);

    JobOutcome outcome = job.execute(TODAY);

    assertThat(outcome.itemsProcessed()).isEqualTo(3);
    assertThat(outcome.message())
        .isEqualTo("3 quotation(s) expired as of 2026-09-23 (FVI: 2; FVS: 1; )");
    verify(quotations, never()).expireLapsed(eq(2L), any());
    assertThat(job.name()).isEqualTo("QUOTATION_EXPIRY");
    assertThat(job.description()).contains("lapsed");
    assertThat(job.cron()).isEqualTo("0 45 0 * * *");
  }

  @Test
  void manualRunDefaultsToTodayAndReportsFailures() {
    Company fvi = company(1L, "FVI", true);
    when(organization.getCompany(1L)).thenReturn(fvi);
    runsRecordTheWork();
    when(quotations.expireLapsed(1L, TODAY)).thenReturn(4);

    assertThat(job.runFor(1L, null)).isEqualTo(4);

    when(quotations.expireLapsed(anyLong(), eq(TODAY.minusDays(1))))
        .thenThrow(new IllegalStateException("database unavailable"));
    assertThatThrownBy(() -> job.runFor(1L, TODAY.minusDays(1)))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("database unavailable");
  }
}
