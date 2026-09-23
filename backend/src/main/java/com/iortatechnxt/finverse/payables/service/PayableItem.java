package com.iortatechnxt.finverse.payables.service;

import com.iortatechnxt.finverse.subledger.domain.OpenItem;
import java.math.BigDecimal;

/**
 * An open payable of a party with the amount still available for payment.
 *
 * @param item open CREDIT item
 * @param available outstanding minus amounts reserved by draft or pending vouchers
 */
public record PayableItem(OpenItem item, BigDecimal available) {}
