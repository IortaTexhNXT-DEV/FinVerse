package com.iortatechnxt.brokerverse.acsl.domain;

import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import java.math.BigDecimal;

/**
 * Values of a correction line (ACSL 2.9.0): the GL account, side and amount, the sub-ledger party,
 * the invoice and ledger component it corrects, the dimensions, and the original line it reverses.
 *
 * @param accountCode GL account
 * @param side debit or credit
 * @param amount positive amount
 * @param partyCode sub-ledger party, may be null
 * @param invoiceNo invoice corrected, may be null
 * @param component Operations ledger component corrected, may be null
 * @param costCenter cost centre, may be null
 * @param businessLine line of business, may be null
 * @param narration narration
 * @param origin reversal, re-post or manual
 * @param originalBatchNo journal of the original line, may be null
 * @param originalLineNo original line number, may be null
 */
public record CorrectionLineValues(
    String accountCode,
    BalanceSide side,
    BigDecimal amount,
    String partyCode,
    String invoiceNo,
    String component,
    String costCenter,
    String businessLine,
    String narration,
    LineOrigin origin,
    String originalBatchNo,
    Integer originalLineNo) {}
