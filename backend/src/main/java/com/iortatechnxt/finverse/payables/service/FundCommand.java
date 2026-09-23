package com.iortatechnxt.finverse.payables.service;

import java.math.BigDecimal;

/**
 * Values to create or update a petty cash fund.
 *
 * @param companyId company
 * @param branchId branch
 * @param code code (immutable)
 * @param name name
 * @param custodian person responsible for the cash box
 * @param glAccountCode petty cash GL account (e.g. 1102)
 * @param replenishBankAccountId bank account that funds and replenishes the box
 * @param imprestAmount imprest (box limit)
 */
public record FundCommand(
    Long companyId,
    Long branchId,
    String code,
    String name,
    String custodian,
    String glAccountCode,
    Long replenishBankAccountId,
    BigDecimal imprestAmount) {}
