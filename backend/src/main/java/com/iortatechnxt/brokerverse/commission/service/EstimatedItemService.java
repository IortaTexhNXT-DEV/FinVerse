package com.iortatechnxt.brokerverse.commission.service;

import com.iortatechnxt.brokerverse.opsledger.domain.InvoiceFlag;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerService.FlagChange;
import com.iortatechnxt.brokerverse.opsledger.service.LedgerSearch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Estimated items of the production reports (RMTID.037, CMRID.014): an invoice is flagged estimated
 * (or no longer) on the Operations ledger with a reason, and the production reports show estimated
 * items apart. The definition of an estimated item is parked with BDOI (OQ27).
 */
@Service
@Transactional
public class EstimatedItemService {

  private final InvoiceLedgerService ledger;
  private final InvoiceLedgerQueryService invoices;

  /**
   * Creates the service.
   *
   * @param ledger ledger flags
   * @param invoices ledger search
   */
  public EstimatedItemService(InvoiceLedgerService ledger, InvoiceLedgerQueryService invoices) {
    this.ledger = ledger;
    this.invoices = invoices;
  }

  /**
   * Flags or clears an invoice as estimated.
   *
   * @param invoiceNo invoice
   * @param estimated flag
   * @param reason reason
   * @return the invoice
   */
  public OpsInvoice flag(String invoiceNo, boolean estimated, String reason) {
    return ledger.setFlag(
        new FlagChange(
            invoiceNo, InvoiceFlag.ESTIMATED, estimated, DpIntakeService.MODULE, reason));
  }

  /**
   * Estimated invoices of a company.
   *
   * @param companyId company
   * @param pageable page
   * @return invoices
   */
  @Transactional(readOnly = true)
  public Page<OpsInvoice> estimated(Long companyId, Pageable pageable) {
    return invoices.search(
        new LedgerSearch(
            companyId, null, null, null, null, null, InvoiceFlag.ESTIMATED, null, null, null, null),
        pageable);
  }
}
