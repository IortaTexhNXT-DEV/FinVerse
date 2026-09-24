package com.iortatechnxt.brokerverse.underwriting.domain;

import java.math.BigDecimal;

/**
 * Values of one insured risk (section) of a policy, at 100 %.
 *
 * @param description risk description (location, vehicle, cargo...)
 * @param sumInsured sum insured
 * @param rate premium rate %
 * @param premium gross premium
 * @param occupation occupation / usage (fire, engineering)
 * @param accumulationZone accumulation zone for catastrophe exposure
 * @param marine shipment details (marine certificates), may be null
 */
public record RiskValues(
    String description,
    BigDecimal sumInsured,
    BigDecimal rate,
    BigDecimal premium,
    String occupation,
    String accumulationZone,
    MarineDetails marine) {}
