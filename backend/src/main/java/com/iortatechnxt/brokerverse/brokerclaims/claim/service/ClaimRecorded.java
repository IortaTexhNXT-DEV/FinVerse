package com.iortatechnxt.brokerverse.brokerclaims.claim.service;

import java.time.Instant;

/**
 * Spring application event published by {@link ClaimRecordingService} once a claim, its locations,
 * its insurer lines and its workflow case (stage NEW) are saved, inside the same transaction
 * (FR-CL-011). The status engine of wave CL1-B listens to it to set the first status chosen at
 * recording (phase NEW, BRCLM.010) and the next follow-up date (BRCLM.019); it publishes {@code
 * ClaimStatusChanged} in turn.
 *
 * @param claimId claim id
 * @param companyId company
 * @param claimNo claim number
 * @param initialStatus first status ({@code BCL_CLAIM_STATUS}, phase NEW): {@code
 *     NEW_COMPLETE_DOCS} or {@code NEW_INCOMPLETE_DOCS}
 * @param recordedBy user who recorded the claim
 * @param recordedAt time of recording
 */
public record ClaimRecorded(
    Long claimId,
    Long companyId,
    String claimNo,
    String initialStatus,
    String recordedBy,
    Instant recordedAt) {}
