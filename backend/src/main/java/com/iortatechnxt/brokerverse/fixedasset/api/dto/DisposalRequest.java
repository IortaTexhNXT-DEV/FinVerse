package com.iortatechnxt.brokerverse.fixedasset.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dispose of (sell or scrap) an asset.
 *
 * @param disposalDate value date
 * @param proceeds sale proceeds (zero when scrapped)
 * @param bankAccount GL bank account receiving the proceeds (required when proceeds are positive)
 * @param reference reference (official receipt, deed of sale)
 * @param remarks remarks
 */
public record DisposalRequest(
    @NotNull LocalDate disposalDate,
    @NotNull @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal proceeds,
    @Size(max = 30) String bankAccount,
    @Size(max = 60) String reference,
    @Size(max = 250) String remarks) {}
