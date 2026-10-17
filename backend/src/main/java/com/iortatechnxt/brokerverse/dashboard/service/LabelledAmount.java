package com.iortatechnxt.brokerverse.dashboard.service;

import java.math.BigDecimal;

/**
 * Labelled amount (ageing bucket, account, statement line).
 *
 * @param label label
 * @param amount amount
 */
public record LabelledAmount(String label, BigDecimal amount) {}
