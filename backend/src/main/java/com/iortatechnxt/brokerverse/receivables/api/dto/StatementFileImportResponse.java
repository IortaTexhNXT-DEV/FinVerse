package com.iortatechnxt.brokerverse.receivables.api.dto;

import com.iortatechnxt.brokerverse.receivables.service.StatementFileImportService.Result;

/**
 * Outcome of a spreadsheet statement import (FRBS 3.3.1).
 *
 * @param statement imported statement
 * @param matched matches created by the auto reconciliation
 */
public record StatementFileImportResponse(StatementResponse statement, int matched) {

  /**
   * Maps the service result.
   *
   * @param r result
   * @return response
   */
  public static StatementFileImportResponse from(Result r) {
    return new StatementFileImportResponse(StatementResponse.from(r.statement()), r.matched());
  }
}
