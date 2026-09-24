package com.iortatechnxt.brokerverse.dashboard.service;

import java.math.BigDecimal;

/**
 * Budget against actual of one expense account.
 *
 * @param account account code and name
 * @param budgetToDate budget up to the current month
 * @param actualToDate actual of the fiscal year
 */
public record BudgetAccountFigures(
    String account, BigDecimal budgetToDate, BigDecimal actualToDate) {}
