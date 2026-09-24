package com.iortatechnxt.brokerverse.payables.service;

import com.iortatechnxt.brokerverse.subledger.domain.OpenItem;
import java.math.BigDecimal;

/**
 * An open payable of a party with the amount still available for payment.
 *
 * @param item open CREDIT item
 * @param available outstanding minus amounts reserved by draft or pending vouchers
 */
public record PayableItem(OpenItem item, BigDecimal available) {}
