package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.crm.domain.ClientType;

/**
 * Published inside the transaction when a client (prospect) is created: on screen, by quotation or
 * account intake, or by the {@code CLIENT_CREATE} bulk upload (all through {@link ClientService}),
 * and when a legacy client is registered by the data migration ({@code migrated}). Sanction
 * screening listens after commit and screens the new client (SNSRP-301), except migrated clients,
 * which the cut-over plan screens in one full run. crm never depends on its listeners.
 *
 * @param companyId company
 * @param clientId client id
 * @param code prospect code
 * @param type individual or corporate
 * @param migrated registered by the data migration
 */
public record ClientRegistered(
    Long companyId, Long clientId, String code, ClientType type, boolean migrated) {

  /**
   * A client created in BIBS.
   *
   * @param companyId company
   * @param clientId client id
   * @param code prospect code
   * @param type individual or corporate
   */
  public ClientRegistered(Long companyId, Long clientId, String code, ClientType type) {
    this(companyId, clientId, code, type, false);
  }
}
