package com.iortatechnxt.brokerverse.adjustment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The quotation linked to a TSI increase (ADJID.008).
 *
 * @param quotationRef quotation number
 */
public record QuotationLinkInput(@NotBlank @Size(max = 40) String quotationRef) {}
