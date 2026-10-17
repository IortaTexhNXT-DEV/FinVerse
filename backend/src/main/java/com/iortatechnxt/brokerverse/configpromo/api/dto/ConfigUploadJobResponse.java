package com.iortatechnxt.brokerverse.configpromo.api.dto;

import com.iortatechnxt.brokerverse.bulk.api.dto.BulkJobResponse;

/**
 * An upload of a configuration screen with its type.
 *
 * @param job upload
 * @param type upload type, with what the user may do
 */
public record ConfigUploadJobResponse(BulkJobResponse job, ConfigUploadTypeResponse type) {}
