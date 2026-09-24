package com.iortatechnxt.brokerverse.underwriting.domain;

/** Whether the risk is written alone or shared with coinsurers. */
public enum BusinessType {
  /** 100 % written by the company. */
  DIRECT,
  /** Shared with a coinsurer; the company writes its share %. */
  DIRECT_WITH_COINSURANCE
}
