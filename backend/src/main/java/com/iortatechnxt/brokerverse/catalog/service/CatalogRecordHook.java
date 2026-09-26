package com.iortatechnxt.brokerverse.catalog.service;

import com.iortatechnxt.brokerverse.common.domain.AuthorizableEntity;

/**
 * Reaction of a catalog service to the maker-checker actions of {@link CatalogRecords}, run in the
 * same transaction (for example the incentive criteria: end-dating the predecessor of an authorised
 * successor and publishing {@code IncentiveCriteriaChanged}, PMADD07/08).
 */
public interface CatalogRecordHook {

  /**
   * A record was authorized.
   *
   * @param kind kind
   * @param entity the record, now ACTIVE
   */
  default void authorized(CatalogKind kind, AuthorizableEntity entity) {}

  /**
   * A record was deactivated.
   *
   * @param kind kind
   * @param entity the record, now INACTIVE
   */
  default void deactivated(CatalogKind kind, AuthorizableEntity entity) {}
}
