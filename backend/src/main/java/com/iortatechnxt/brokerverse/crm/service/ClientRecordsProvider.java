package com.iortatechnxt.brokerverse.crm.service;

import java.util.List;

/**
 * Port for the client 360 view (BRNB.099): modules that hold records of a client (accounts,
 * quotations, proposal requests, bookings...) implement it in their own {@code service} package so
 * the crm module lists them without depending on those modules.
 */
public interface ClientRecordsProvider {

  /**
   * Records of a client held by the implementing module.
   *
   * @param clientId client id
   * @return records, newest first
   */
  List<ClientRecord> recordsOf(Long clientId);
}
