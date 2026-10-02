package com.iortatechnxt.brokerverse.underwriting.service;

/**
 * An insured risk in force, with its policy, for accumulation (catastrophe exposure) control.
 *
 * @param risk risk
 * @param policy policy header
 * @param endorsements number of approved endorsements of the policy
 */
public record RiskExposure(RiskSnapshot risk, PolicySnapshot policy, int endorsements) {}
