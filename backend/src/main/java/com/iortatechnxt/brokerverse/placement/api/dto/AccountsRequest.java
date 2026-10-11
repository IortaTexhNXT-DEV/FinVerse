package com.iortatechnxt.brokerverse.placement.api.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Accounts selected on a list (generate slips, send, bill).
 *
 * @param companyId company
 * @param arns Account Reference Numbers
 */
public record AccountsRequest(
    @NotNull Long companyId, @NotEmpty @Size(max = 200) List<@NotNull String> arns) {}
