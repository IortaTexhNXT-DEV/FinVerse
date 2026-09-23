package com.iortatechnxt.finverse.underwriting.service;

import java.time.LocalDate;

/**
 * Period over which the premium of one premium transaction is earned (both dates inclusive).
 *
 * <ul>
 *   <li>original issue: the policy period it was written for;
 *   <li>renewal: the renewed period;
 *   <li>other endorsements (additional, refund, cancellation): from the effective date to the end
 *       of the policy period in force when the endorsement was made.
 * </ul>
 *
 * @param from first day of cover
 * @param to last day of cover (before {@code from} when the transaction covers no day)
 */
public record CoverPeriod(LocalDate from, LocalDate to) {}
