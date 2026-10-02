package com.iortatechnxt.brokerverse.claims.service;

import com.iortatechnxt.brokerverse.underwriting.service.PolicySnapshot;
import com.iortatechnxt.brokerverse.underwriting.service.RiskSnapshot;
import java.util.List;

/**
 * Policy looked up for a claim notification.
 *
 * @param policy policy header
 * @param risks insured risks
 * @param inForce whether the policy is approved and in force at the loss date
 */
public record PolicyCover(PolicySnapshot policy, List<RiskSnapshot> risks, boolean inForce) {}
