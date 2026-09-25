package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.service.MinimalBalanceService.Sweep;
import com.iortatechnxt.brokerverse.system.service.JobOutcome;
import com.iortatechnxt.brokerverse.system.service.ManagedJob;
import java.time.LocalDate;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The cashiering background jobs (OPERATIONS_DESIGN section 10), scheduled by the platform's job
 * scheduler with the crons of {@code brokerverse.jobs.*} ("-" = manual only).
 */
@Configuration(proxyBeanMethods = false)
public class CashieringJobs {

  /** Pre-booked re-match job. */
  public static final String PREBOOKED_REMATCH = "PREBOOKED_REMATCH";

  /** Automatch job. */
  public static final String PAYMENT_AUTOMATCH = "PAYMENT_AUTOMATCH";

  /** PDC maturity job. */
  public static final String PDC_MATURITY = "PDC_MATURITY";

  /** Minimal balance sweep job. */
  public static final String MINIMAL_BALANCE_SWEEP = "MINIMAL_BALANCE_SWEEP";

  /**
   * {@code PREBOOKED_REMATCH} (CSHID.020): applies pre-booked payments whose account is booked and
   * raises {@code PREBOOKED_AGEING}.
   *
   * @param service pre-booked queue
   * @param cron schedule
   * @return job
   */
  @Bean
  ManagedJob prebookedRematchJob(
      PrebookedService service,
      @Value("${brokerverse.jobs.prebooked-rematch-cron:-}") String cron) {
    return new CashieringJob(
        PREBOOKED_REMATCH,
        "Applies pre-booked payments whose account is now booked (CSHID.020)",
        cron,
        date -> {
          int n = service.rematchAll(date);
          return new JobOutcome(n, n + " pre-booked payments applied");
        });
  }

  /**
   * {@code PAYMENT_AUTOMATCH} (CSHID.008 item 3): matches unapplied payments again.
   *
   * @param service automatch
   * @param cron schedule
   * @return job
   */
  @Bean
  ManagedJob paymentAutomatchJob(
      AutomatchService service,
      @Value("${brokerverse.jobs.payment-automatch-cron:-}") String cron) {
    return new CashieringJob(
        PAYMENT_AUTOMATCH,
        "Matches unapplied payments again against booked invoices (CSHID.008)",
        cron,
        date -> {
          int n = service.run(date);
          return new JobOutcome(n, n + " unapplied payments applied");
        });
  }

  /**
   * {@code PDC_MATURITY} (CSHID.008 item 4e): turns matured post-dated checks into payments.
   *
   * @param service PDC warehouse
   * @param cron schedule
   * @return job
   */
  @Bean
  ManagedJob pdcMaturityJob(
      PdcWarehouseService service, @Value("${brokerverse.jobs.pdc-maturity-cron:-}") String cron) {
    return new CashieringJob(
        PDC_MATURITY,
        "Creates the payment and AR of post-dated checks at maturity (CSHID.008)",
        cron,
        date -> {
          int n = service.mature(date);
          return new JobOutcome(n, n + " post-dated checks matured");
        });
  }

  /**
   * {@code MINIMAL_BALANCE_SWEEP} (CSHID.016, summary 5.f).
   *
   * @param service minimal balances
   * @param cron schedule
   * @return job
   */
  @Bean
  ManagedJob minimalBalanceSweepJob(
      MinimalBalanceService service,
      @Value("${brokerverse.jobs.minimal-balance-sweep-cron:-}") String cron) {
    return new CashieringJob(
        MINIMAL_BALANCE_SWEEP,
        "Reverses minimal premium balances and moves minimal excess to AP overages (CSHID.016)",
        cron,
        date -> {
          Sweep sweep = service.sweep(date);
          return new JobOutcome(
              sweep.premium() + sweep.excess(),
              sweep.premium()
                  + " premium balances reversed, "
                  + sweep.excess()
                  + " excess to overages");
        });
  }

  /**
   * A cashiering job.
   *
   * @param name job name
   * @param description description
   * @param cron cron
   * @param work work for a business date
   */
  record CashieringJob(
      String name, String description, String cron, Function<LocalDate, JobOutcome> work)
      implements ManagedJob {

    @Override
    public JobOutcome execute(LocalDate businessDate) {
      return work.apply(businessDate);
    }
  }
}
