package com.iortatechnxt.brokerverse.opsledger.demo;

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
 * Demo start-up (demo profile only): after {@code BookingDemoData} (order 80) has booked the demo
 * accounts, copies into the Operations ledger any booked invoice that is not there yet. The feed
 * listener already copies each new booking; this covers databases whose bookings predate
 * Operations. Idempotent.
 */
@Component
@Profile("demo")
@Order(90)
public class OpsLedgerDemoReplay implements ApplicationRunner {

  private static final Logger LOG = LoggerFactory.getLogger(OpsLedgerDemoReplay.class);

  private final InvoiceFeedReplayService replay;

  /**
   * Creates the loader.
   *
   * @param replay replay service
   */
  public OpsLedgerDemoReplay(InvoiceFeedReplayService replay) {
    this.replay = replay;
  }

  @Override
  public void run(ApplicationArguments args) {
    FlowInRun run = replay.replayAll(Trigger.REPLAY);
    LOG.info("Operations ledger demo replay {}: {}", run.getRunNo(), run.getMessage());
  }
}
