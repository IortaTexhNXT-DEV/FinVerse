package com.iortatechnxt.finverse.dimension.domain;

/** Financial dimensions that can be attached to journal lines for analysis. */
public enum DimensionType {
  COST_CENTER,
  PROFIT_CENTER,
  DEPARTMENT,
  /** Line / class of business (Fire, Motor, Marine, Engineering, Casualty, ...). */
  BUSINESS_LINE
}
