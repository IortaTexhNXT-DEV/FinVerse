package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.UnappliedOrigin;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied;
import com.iortatechnxt.brokerverse.cashiering.domain.Unapplied.UnappliedSpec;
import com.iortatechnxt.brokerverse.opsledger.service.port.UnappliedSink;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cashiering's implementation of the ledger port {@code UnappliedSink} (CSHID.024/025): the
 * adjustment excess, DP reinstatement or remittance return becomes an item in the Unapplied tab of
 * the workbench, idempotent on (source module, source reference). The money is already in unapplied
 * collections through the caller's posting, so nothing is posted here.
 */
@Service
@Transactional
public class CashieringUnappliedSink implements UnappliedSink {

  private final UnappliedService unapplied;
  private final CashieringSettings settings;

  /**
   * Creates the sink.
   *
   * @param unapplied unapplied workbench
   * @param settings settings (Head Office)
   */
  public CashieringUnappliedSink(UnappliedService unapplied, CashieringSettings settings) {
    this.unapplied = unapplied;
    this.settings = settings;
  }

  @Override
  public UnappliedHandle create(UnappliedRequest request) {
    Unapplied item =
        unapplied.create(
            request.companyId(),
            settings.headOffice(request.companyId()).getId(),
            new UnappliedSpec(
                origin(request.origin()),
                null,
                null,
                request.invoiceNo(),
                request.party() == null ? null : request.party().clientCode(),
                null,
                request.party() == null ? null : request.party().salesUnit(),
                request.currency(),
                request.amount(),
                request.dispositionHint(),
                request.source().module(),
                request.source().reference(),
                request.source().remarks()));
    return new UnappliedHandle(
        Status.CREATED, item.getReference(), "Unapplied item " + item.getReference() + " created");
  }

  private static UnappliedOrigin origin(String origin) {
    if (origin == null) {
      return UnappliedOrigin.OTHER;
    }
    try {
      return UnappliedOrigin.valueOf(origin.strip().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      return UnappliedOrigin.OTHER;
    }
  }
}
