package com.iortatechnxt.brokerverse.issuance.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Accounts selected on a list (Insurance Advice generation).
 *
 * @param arns Account Reference Numbers
 */
public record ArnsRequest(@NotEmpty @Size(max = 200) List<@NotNull String> arns) {}
