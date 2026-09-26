package com.iortatechnxt.brokerverse.fixedasset.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Transfer an asset to another branch.
 *
 * @param toBranchId receiving branch
 * @param transferDate value date
 * @param location new location
 * @param custodian new custodian
 * @param remarks remarks
 */
public record TransferRequest(
    @NotNull Long toBranchId,
    @NotNull LocalDate transferDate,
    @Size(max = 120) String location,
    @Size(max = 120) String custodian,
    @Size(max = 250) String remarks) {}
