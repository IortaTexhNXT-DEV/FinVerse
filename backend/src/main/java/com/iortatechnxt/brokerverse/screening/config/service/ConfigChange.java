package com.iortatechnxt.brokerverse.screening.config.service;

/**
 * One line of the difference between a draft and the version in force (SNSRP-101, 108 "before /
 * after values"; FR-SS-010 "Changes": rule, attribute, before, after).
 *
 * @param item the rule or row, e.g. "SANCTION INDIVIDUAL FUZZY" or "Rule 10"
 * @param attribute the attribute changed, e.g. "Threshold"
 * @param before the value in force, {@code null} for an added row
 * @param after the draft value, {@code null} for a removed row
 */
public record ConfigChange(String item, String attribute, String before, String after) {}
