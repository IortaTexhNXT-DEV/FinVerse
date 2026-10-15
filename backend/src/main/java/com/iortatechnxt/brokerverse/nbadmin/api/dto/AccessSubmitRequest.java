package com.iortatechnxt.brokerverse.nbadmin.api.dto;

import jakarta.validation.constraints.Size;

/**
 * Submission of a draft or resubmission of a returned group-profile change: any checker other than
 * the maker decides it.
 *
 * @param remarks remarks (mandatory on a resubmission: what was corrected)
 */
public record AccessSubmitRequest(@Size(max = 1000) String remarks) {}
