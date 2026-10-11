package com.iortatechnxt.brokerverse.placement.api.dto;

import java.util.List;

/**
 * Placement of one account: payment gate, slips, hold covers and insurer returns.
 *
 * @param gate payment gate with rule and evidence
 * @param slips slip versions covering the account, newest first
 * @param holdCovers hold covers, newest first
 * @param returns insurer returns, newest first
 */
public record PlacementViewResponse(
    GateResponse gate,
    List<SlipResponse> slips,
    List<HoldCoverResponse> holdCovers,
    List<InsurerReturnResponse> returns) {}
