package com.iortatechnxt.brokerverse.csf.service.adapter;

import com.iortatechnxt.brokerverse.csf.service.port.LegacyAccountLookup;
import java.util.List;

/**
 * Default {@link LegacyAccountLookup}: the legacy systems are not connected, so a search finds only
 * the accounts in BIBS (CSQ01, CSQ02).
 */
public class NoLegacyAccountLookup implements LegacyAccountLookup {

  @Override
  public List<LegacyAccount> find(Long companyId, String keyType, String value) {
    return List.of();
  }
}
