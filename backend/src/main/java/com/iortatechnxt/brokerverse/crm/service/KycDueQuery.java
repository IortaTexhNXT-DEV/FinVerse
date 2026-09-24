package com.iortatechnxt.brokerverse.crm.service;

import java.time.LocalDate;

/**
 * Filter of the KYC reviews due list (BRNB.110).
 *
 * @param companyId company (null = every company, used by the monthly job)
 * @param dueBy last review date included (null = the configured window from today)
 * @param bankClient BDO bank clients only (true), non-bank only (false) or both (null)
 * @param riskRating risk rating code, null for all
 * @param marketSegment market segment, null for all
 */
public record KycDueQuery(
    Long companyId, LocalDate dueBy, Boolean bankClient, String riskRating, String marketSegment) {}
