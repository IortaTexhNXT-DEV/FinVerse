package com.iortatechnxt.brokerverse.catalog.domain;

/** Whether a minimum field belongs to the account or to each risk item. */
public enum FieldTarget {
  /** Account-level field. */
  ACCOUNT,
  /** Field of every risk item. */
  ITEM
}
