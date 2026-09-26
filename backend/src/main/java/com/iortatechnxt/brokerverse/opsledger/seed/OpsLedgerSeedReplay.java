package com.iortatechnxt.brokerverse.opsledger.seed;

import com.iortatechnxt.brokerverse.opsledger.domain.FlowInEnums.Trigger;
import com.iortatechnxt.brokerverse.opsledger.domain.FlowInRun;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceFeedReplayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Seed start-up (seed profile only): after {@code BookingSeedData} (order 80) has booked the seed
 * accounts, copies into the Operations ledger any booked invoice that is not there yet. The feed
 * listener already copies each new booking; this covers databases whose bookings predate
 * Operations. Runs as the seed administrator (FLOWIN_MANAGE, like the replay endpoint). Idempotent.
 */
@Component
@Profile("seed")
@Order(90)
public class OpsLedgerSeedReplay implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(OpsLedgerSeedReplay.class);

  private final InvoiceFeedReplayService replay;
  private final SeedUsers users;

  /**
   * Creates the loader.
   *
   * @param replay replay service
   * @param users seed sign-in
   */
  public OpsLedgerSeedReplay(InvoiceFeedReplayService replay, SeedUsers users) {
    this.replay = replay;
    this.users = users;
  }

  @Override
  public void run(ApplicationArguments args) {
    FlowInRun run = users.as("admin", () -> replay.replayAll(Trigger.REPLAY));
    LOG.info("Operations ledger seed replay {}: {}", run.getRunNo(), run.getMessage());
  }
}
