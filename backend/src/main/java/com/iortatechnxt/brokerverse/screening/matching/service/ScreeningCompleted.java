package com.iortatechnxt.brokerverse.screening.matching.service;

/**
 * A screening run ended (SNSRP-303, 602; FR-SS-030 to 034). Published inside the run's own
 * transaction, which is a new transaction after the business transaction that triggered it. The
 * case wave listens (for example with {@code @TransactionalEventListener(AFTER_COMMIT)}) and opens
 * or joins cases for the case matches and the risk outcomes; for the trigger {@code
 * ACCOUNT_SUBMITTED} it opens the ACCOUNT_APPLICATION case.
 *
 * @param result the run result
 */
public record ScreeningCompleted(ScreeningResult result) {}
