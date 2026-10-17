package com.iortatechnxt.brokerverse.renewal.service.adapter;

import com.iortatechnxt.brokerverse.renewal.service.port.LegacyPolicySource;
import java.time.LocalDate;
import java.util.List;

/** The default {@link LegacyPolicySource}: no migrated policy header is available. */
public class NotConnectedLegacyPolicySource implements LegacyPolicySource {

  @Override
  public boolean connected() {
    return false;
  }

  @Override
  public List<LegacyHeader> expiringHeaders(Long companyId, LocalDate from, LocalDate to) {
    return List.of();
  }

  @Override
  public GoLiveHeaders goLiveCandidates(Long companyId, LocalDate goLive, LocalDate to) {
    return new GoLiveHeaders(List.of(), 0);
  }
}
