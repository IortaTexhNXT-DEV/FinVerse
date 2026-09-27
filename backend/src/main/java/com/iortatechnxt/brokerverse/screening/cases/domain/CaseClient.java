package com.iortatechnxt.brokerverse.screening.cases.domain;

/**
 * The client of a case and its sales facts.
 *
 * @param companyId company
 * @param clientId client id
 * @param clientCode client or prospect code
 * @param clientName display name
 * @param clientType INDIVIDUAL or CORPORATE
 * @param marketingUnit marketing unit (sales department) code, may be null
 * @param unitHead unit head user, may be null
 * @param accountOfficer the client's account officer, may be null
 */
public record CaseClient(
    Long companyId,
    Long clientId,
    String clientCode,
    String clientName,
    String clientType,
    String marketingUnit,
    String unitHead,
    String accountOfficer) {}
