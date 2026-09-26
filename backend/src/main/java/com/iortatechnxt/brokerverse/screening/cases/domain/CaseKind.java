package com.iortatechnxt.brokerverse.screening.cases.domain;

/**
 * What a case is.
 *
 * @param trigger the screening trigger (or MANUAL)
 * @param reference the trigger's reference, may be null
 * @param caseType the case type
 * @param riskCategory the risk category, may be null
 * @param templateType the review template type
 * @param activePolicy whether the client has an active policy
 */
public record CaseKind(
    String trigger,
    String reference,
    String caseType,
    String riskCategory,
    String templateType,
    boolean activePolicy) {}
