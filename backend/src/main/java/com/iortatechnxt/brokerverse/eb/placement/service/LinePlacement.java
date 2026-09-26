package com.iortatechnxt.brokerverse.eb.placement.service;

import com.iortatechnxt.brokerverse.account.service.AccountDraft;
import com.iortatechnxt.brokerverse.catalog.service.PremiumBreakdown;

/**
 * The account of one benefit line to create at placement (BRID-017; FR-EB-046 R1): the chosen
 * proposal of the line turned into account data by the confirmation step.
 *
 * @param lineNo programme line
 * @param draft account data (product, insurer, period, members as risk items); the client is the
 *     programme's
 * @param premium premium of the chosen proposal, null to rate the account now
 */
public record LinePlacement(int lineNo, AccountDraft draft, PremiumBreakdown premium) {}
