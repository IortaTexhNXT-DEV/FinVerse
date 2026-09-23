package com.iortatechnxt.finverse.receivables.api.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Bank statement upload (CSV text, see docs/samples/bank-statement-sample.csv).
 *
 * @param companyId company
 * @param bankAccountCode GL bank account of the statement
 * @param statementRef statement reference (defaults to the file name)
 * @param fileName uploaded file name
 * @param content CSV content
 * @param openingBalance balance before the first line (derived from the first line when omitted)
 */
public record StatementImportRequest(
    @NotNull Long companyId,
    @NotBlank @Size(max = 30) String bankAccountCode,
    @Size(max = 60) String statementRef,
    @Size(max = 200) String fileName,
    @NotBlank @Size(max = 5_000_000) String content,
    @Digits(integer = 17, fraction = 2) BigDecimal openingBalance) {}
