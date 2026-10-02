package com.iortatechnxt.brokerverse.csf.service.port;

import java.util.List;

/**
 * Port: accounts that exist only in the legacy systems (QPS, EBIX, LOS), found by account, PN or
 * application number (BRCSF-003; CUSTOMER_SERVICING_DESIGN section 2.2; CSQ01, CSQ02). The default
 * adapter finds nothing; the data migration module, or a connection to the legacy systems,
 * implements it.
 */
public interface LegacyAccountLookup {

  /**
   * Legacy accounts of a search.
   *
   * @param companyId company
   * @param keyType ACCOUNT_NO, PN_NO or APPLICATION_NO
   * @param value searched value
   * @return accounts found, empty when none or not connected
   */
  List<LegacyAccount> find(Long companyId, String keyType, String value);

  /**
   * An account found in a legacy system.
   *
   * @param source legacy system
   * @param reference legacy account or policy reference
   * @param clientName client name
   * @param description one-line description (product, period, status)
   */
  record LegacyAccount(String source, String reference, String clientName, String description) {}
}
