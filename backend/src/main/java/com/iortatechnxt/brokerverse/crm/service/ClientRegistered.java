package com.iortatechnxt.brokerverse.crm.service;

import com.iortatechnxt.brokerverse.crm.domain.ClientType;

/**
 * Published inside the transaction when a client (prospect) is created: on screen, by quotation or
 * account intake, or by the {@code CLIENT_CREATE} bulk upload (all through {@link ClientService}).
 * Sanction screening listens after commit and screens the new client (SNSRP-301). crm never depends
 * on its listeners.
 *
 * @param companyId company
 * @param clientId client id
 * @param code prospect code
 * @param type individual or corporate
 */
public record ClientRegistered(Long companyId, Long clientId, String code, ClientType type) {}
