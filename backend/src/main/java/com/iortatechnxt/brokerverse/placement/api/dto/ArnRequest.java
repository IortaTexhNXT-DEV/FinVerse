package com.iortatechnxt.brokerverse.placement.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * One account chosen by the user (e.g. the match of a payment report line).
 *
 * @param arn Account Reference Number
 */
public record ArnRequest(@NotBlank @Size(max = 30) String arn) {}
