package com.iortatechnxt.brokerverse.common.domain;

/**
 * Master record outside the maker-checker control whose field changes are kept in the master change
 * log all the same (a client). Every {@link AuthorizableEntity} is logged without this marker.
 */
public interface ChangeTracked {}
