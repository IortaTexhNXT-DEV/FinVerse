package com.iortatechnxt.brokerverse.screening.str.service;

import com.iortatechnxt.brokerverse.screening.str.domain.StrTransaction.Line;
import java.util.List;

/**
 * Port: the transactions of a client that prefill an STR (SNSRP-705; FR-SS-070 "the transactions
 * from the client's accounts, invoices and receipts"). The default adapter lists the client's
 * accounts with their gross premium; invoice and receipt adapters of the ledger modules can be
 * added as further beans (all sources are merged).
 */
public interface StrTransactionSource {

  /**
   * The transactions of a client.
   *
   * @param clientId the client
   * @return transaction lines
   */
  List<Line> transactionsOf(Long clientId);
}
