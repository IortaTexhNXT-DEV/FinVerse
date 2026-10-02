package com.iortatechnxt.brokerverse.fixedasset.service;

import java.math.BigDecimal;

/**
 * Result a disposal would have, computed without posting.
 *
 * @param depreciationReversed depreciation already charged for the disposal month or later that the
 *     disposal reverses (zero when none)
 * @param netBookValue net book value the gain or loss is measured against (end of the month before
 *     the disposal)
 * @param gainOrLoss proceeds minus net book value: positive is a gain, negative a loss
 */
public record DisposalPreview(
    BigDecimal depreciationReversed, BigDecimal netBookValue, BigDecimal gainOrLoss) {}
