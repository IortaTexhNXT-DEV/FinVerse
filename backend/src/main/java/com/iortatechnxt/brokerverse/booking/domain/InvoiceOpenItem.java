package com.iortatechnxt.brokerverse.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * A sub-ledger open item recorded for a booked invoice.
 *
 * @param openItemId open item id
 * @param role client premium, insurer DTIP or insurer commission
 */
@Embeddable
public record InvoiceOpenItem(
    @Column(name = "open_item_id", nullable = false) Long openItemId,
    @Enumerated(EnumType.STRING) @Column(name = "role", nullable = false, length = 30)
        OpenItemRole role) {}
