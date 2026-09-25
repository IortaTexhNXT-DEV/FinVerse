package com.iortatechnxt.brokerverse.crm.service;

/**
 * Published inside the transaction when an update changes what identifies a client: the name, the
 * birth date, the nationality, the TIN or the ID document. Sanction screening listens after commit,
 * rebuilds the client's name keys and screens it again (SNSRP-301, 602).
 *
 * @param companyId company
 * @param clientId client id
 */
public record ClientIdentityChanged(Long companyId, Long clientId) {}
