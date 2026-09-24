package com.iortatechnxt.brokerverse.payables.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result of posting an approved supplier invoice.
 *
 * @param openItemId CREDIT open item recorded for the supplier
 * @param batchNos GL journal batches (one per expense account / cost centre group)
 * @param basePayable payable amount in base currency
 */
public record InvoicePosting(Long openItemId, List<String> batchNos, BigDecimal basePayable) {

  /** Canonical constructor copying the list. */
  public InvoicePosting {
    batchNos = List.copyOf(batchNos);
  }
}
