package com.iortatechnxt.brokerverse.collections.promise.service;

/**
 * Published in the evaluating transaction when a promise to pay is broken (BRCLXN.055). The
 * escalation rules of basis BROKEN_PROMISES_COUNT listen to it and escalate the account at once
 * (BRCLXN.049); other Collections screens may use it to flag the account.
 *
 * @param companyId company
 * @param promiseId promise
 * @param invoiceNo invoice (collection account)
 */
public record PromiseBroken(Long companyId, Long promiseId, String invoiceNo) {}
