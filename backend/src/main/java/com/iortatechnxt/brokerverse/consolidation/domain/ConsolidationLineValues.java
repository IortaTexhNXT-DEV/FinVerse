package com.iortatechnxt.brokerverse.consolidation.domain;

import com.iortatechnxt.brokerverse.coa.domain.AccountClass;
import java.math.BigDecimal;

/**
 * Values of a consolidated ledger line. Amounts are net debit (debit positive).
 *
 * @param type line type
 * @param ruleCode elimination rule (IC_BALANCE, INVESTMENT_EQUITY) or null
 * @param companyId member company, or null for group-level lines
 * @param accountCode group account code
 * @param accountName account name
 * @param accountClass account class
 * @param localAmount balance in the member's base currency (0 for group-level lines)
 * @param rate translation rate applied (1 for group-level lines)
 * @param amount amount in the consolidation currency
 * @param description explanation
 */
public record ConsolidationLineValues(
    ConsolidationLineType type,
    String ruleCode,
    Long companyId,
    String accountCode,
    String accountName,
    AccountClass accountClass,
    BigDecimal localAmount,
    BigDecimal rate,
    BigDecimal amount,
    String description) {}
