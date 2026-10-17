package com.iortatechnxt.brokerverse.account.api.dto;

import com.iortatechnxt.brokerverse.account.domain.PaymentArrangement;
import jakarta.validation.constraints.NotNull;

/**
 * Payment arrangement (BRNB.114).
 *
 * @param arrangement via BDOI or direct to insurer
 */
public record PaymentArrangementRequest(@NotNull PaymentArrangement arrangement) {}
