package com.iortatechnxt.finverse.claims.domain;

import java.math.BigDecimal;

/**
 * Coinsurance split of an amount paid or received on a claim.
 *
 * <ul>
 *   <li>not coinsured: ours = payable = 100 %, coinsurers = 0;
 *   <li>coinsured, company leads: the company pays / receives 100 % (payable) and the coinsurers'
 *       share (payable − ours) is recoverable from / payable to the coinsurer;
 *   <li>coinsured, company follows: the company pays / receives its own share only.
 * </ul>
 *
 * @param ours company share (accounted as claims expense / recovery)
 * @param payable amount paid to the payee or received from the third party
 * @param coinsurers coinsurers' share handled by the company as leader
 */
public record ShareSplit(BigDecimal ours, BigDecimal payable, BigDecimal coinsurers) {}
