package com.iortatechnxt.brokerverse.payables.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Loads the payables seed data at start-up of the {@code seed} profile (after the platform and
 * underwriting / claims / reinsurance seed runners). Idempotent: restarts do not duplicate data.
 */
@Component
@Profile("seed")
@Order(40)
public class PayablesSeedDataRunner implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(PayablesSeedDataRunner.class);

  private final PayablesSeedData seedData;

  /**
   * Creates the runner.
   *
   * @param seedData generator
   */
  public PayablesSeedDataRunner(PayablesSeedData seedData) {
    this.seedData = seedData;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (seedData.loadIfMissing()) {
      LOG.info("Payables seed data loaded (supplier invoices, payments, PDCs, petty cash)");
    }
  }
}
