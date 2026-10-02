package com.iortatechnxt.brokerverse.catalog.domain;

/** Kind of the insured items of a product line (BRNB.051): drives the item fields. */
public enum RiskItemKind {
  /** Motor vehicle: plate, conduction sticker, engine, chassis, make, model. */
  VEHICLE,
  /** Location of risk with occupancy, construction class and items insured. */
  PROPERTY_LOCATION,
  /** Insured person (personal accident). */
  PERSON,
  /** Any other risk described in free text with a sum insured. */
  GENERIC
}
