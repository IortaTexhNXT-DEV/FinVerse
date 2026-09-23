package com.iortatechnxt.finverse.payables.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Loads the payables demo data at start-up of the {@code demo} profile (after the platform and
 * underwriting / claims / reinsurance demo runners). Idempotent: restarts do not duplicate data.
 */
@Component
@Profile("demo")
@Order(40)
public class PayablesDemoDataRunner implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(PayablesDemoDataRunner.class);

  private final PayablesDemoData demoData;

  /**
   * Creates the runner.
   *
   * @param demoData generator
   */
  public PayablesDemoDataRunner(PayablesDemoData demoData) {
    this.demoData = demoData;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (demoData.loadIfMissing()) {
      LOG.info("Payables demo data loaded (supplier invoices, payments, PDCs, petty cash)");
    }
  }
}
