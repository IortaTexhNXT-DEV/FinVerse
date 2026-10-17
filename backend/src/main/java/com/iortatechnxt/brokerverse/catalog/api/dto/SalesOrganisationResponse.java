package com.iortatechnxt.brokerverse.catalog.api.dto;

import java.util.List;

/**
 * The sales organisation of a company.
 *
 * @param units regions, departments and teams
 * @param officers account officers by team
 */
public record SalesOrganisationResponse(
    List<SalesUnitResponse> units, List<SalesOfficerResponse> officers) {}
