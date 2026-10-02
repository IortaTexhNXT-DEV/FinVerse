package com.iortatechnxt.brokerverse.opsledger.service;

import com.iortatechnxt.brokerverse.common.domain.RecordOrigin;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceData;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceOriginSnapshot;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceShare;
import java.util.List;

/**
 * An open legacy invoice handed to {@link LegacyInvoiceIntake} by the Data Migration loader of
 * object F01 (DATA_MIGRATION_DESIGN 14.1). The keys carry no invoice number and no root: the intake
 * numbers the invoice from the legacy number and the family from the legacy parent.
 *
 * @param data header facts; {@code keys.parentInvoiceNo} is the legacy number of the parent
 * @param shares insurer shares
 * @param origin source system, legacy policy reference and migration batch
 * @param legacyInvoiceNo number in the source system
 * @param header legacy dates, service invoice and commission facts kept on the snapshot
 * @param positions position of each component at cut-over
 * @param hold the invoice was on hold in legacy
 */
public record LegacyInvoice(
    OpsInvoiceData data,
    List<OpsInvoiceShare> shares,
    RecordOrigin origin,
    String legacyInvoiceNo,
    OpsInvoiceOriginSnapshot.Header header,
    List<OpsInvoiceOriginSnapshot.Line> positions,
    boolean hold) {

  /** Defensive copies. */
  public LegacyInvoice {
    shares = List.copyOf(shares);
    positions = List.copyOf(positions);
  }
}
