package com.iortatechnxt.brokerverse.payrequest.domain;

import java.math.BigDecimal;

/**
 * Values of one RRF line as entered (Appendix D).
 *
 * @param arNo acknowledgement receipt of the payment to refund
 * @param clientCode client
 * @param assuredName assured or client name
 * @param invoiceNo invoice the payment was applied to, may be null
 * @param amount refund amount
 * @param reasonCode reason (LOV {@code REFUND_REASON})
 * @param branchUnit branch or unit
 * @param categoryA category A (LOV {@code RRF_CATEGORY_A})
 * @param categoryB category B (LOV {@code RRF_CATEGORY_B})
 * @param accountName account or check name
 */
public record RefundLineValues(
    String arNo,
    String clientCode,
    String assuredName,
    String invoiceNo,
    BigDecimal amount,
    String reasonCode,
    String branchUnit,
    String categoryA,
    String categoryB,
    String accountName) {}
