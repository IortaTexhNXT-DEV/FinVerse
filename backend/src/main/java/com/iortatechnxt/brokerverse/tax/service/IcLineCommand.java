package com.iortatechnxt.brokerverse.tax.service;

import com.iortatechnxt.brokerverse.tax.domain.IcMeasure;
import com.iortatechnxt.brokerverse.tax.domain.IcSchedule;
import com.iortatechnxt.brokerverse.tax.domain.NormalBalance;
import java.math.BigDecimal;

/**
 * Values of an Insurance Commission schedule mapping line.
 *
 * @param companyId company
 * @param schedule schedule (immutable after creation)
 * @param lineCode line code (immutable after creation)
 * @param description description printed on the schedule
 * @param lineOrder print order
 * @param accountFrom first account of the range, null when mapped by report group only
 * @param accountTo last account of the range
 * @param reportGroup chart-of-accounts report group, null when mapped by range only
 * @param normalBalance natural side of the line
 * @param signFactor +1 adds to the schedule total, -1 deducts
 * @param measure balance or movement, null = the schedule default
 * @param rbcFactor RBC factor in percent (RBC schedule only)
 */
public record IcLineCommand(
    Long companyId,
    IcSchedule schedule,
    String lineCode,
    String description,
    int lineOrder,
    String accountFrom,
    String accountTo,
    String reportGroup,
    NormalBalance normalBalance,
    int signFactor,
    IcMeasure measure,
    BigDecimal rbcFactor) {}
