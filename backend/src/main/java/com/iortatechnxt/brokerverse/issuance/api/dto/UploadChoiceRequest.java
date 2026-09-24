package com.iortatechnxt.brokerverse.issuance.api.dto;

import jakarta.validation.constraints.Size;

/**
 * The user's choice for a file of a bulk upload.
 *
 * @param arn account; empty to keep the proposal
 * @param included store the file on confirmation
 */
public record UploadChoiceRequest(@Size(max = 30) String arn, boolean included) {}
