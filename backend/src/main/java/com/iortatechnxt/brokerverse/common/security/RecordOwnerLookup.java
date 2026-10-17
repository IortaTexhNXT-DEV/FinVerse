package com.iortatechnxt.brokerverse.common.security;

import java.util.Optional;

/**
 * Finds the company of a record kept outside the entity model (a table written with SQL), for the
 * record scope check of {@link OwnedRecord#type()}.
 */
public interface RecordOwnerLookup {

  /**
   * The record type this lookup answers for.
   *
   * @return record type, as named in {@link OwnedRecord#type()}
   */
  String recordType();

  /**
   * The company of a record.
   *
   * @param id record id
   * @return company, empty when the record does not exist; a present empty-company answer is not
   *     possible (such records are programme-wide and need no lookup)
   */
  Optional<Long> companyOf(Long id);
}
