package com.iortatechnxt.brokerverse.eb.soa.service;

import com.iortatechnxt.brokerverse.eb.domain.EbCodes;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbSoa;
import com.iortatechnxt.brokerverse.eb.domain.EbSoaRepository;
import com.iortatechnxt.brokerverse.messaging.domain.Notice;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.InvoiceMovementPosted;
import java.time.Clock;
import java.util.Optional;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Tells the AO and Collection when the invoices an SOA bills are paid (FR-EB-053): on each movement
 * of the invoice ledger, an SOA whose invoices all read PAID is notified once ({@code
 * EB_INVOICE_PAID}).
 */
@Component
public class SoaPaymentListener {

  private final EbSoaRepository soas;
  private final EbProgrammeRepository programmes;
  private final SoaInvoices invoices;
  private final NotificationService notifications;
  private final Clock clock;

  /**
   * Creates the listener.
   *
   * @param soas SOAs
   * @param programmes programmes (account officer)
   * @param invoices payment status of the ledger
   * @param notifications in-app notices
   * @param clock clock
   */
  public SoaPaymentListener(
      EbSoaRepository soas,
      EbProgrammeRepository programmes,
      SoaInvoices invoices,
      NotificationService notifications,
      Clock clock) {
    this.soas = soas;
    this.programmes = programmes;
    this.invoices = invoices;
    this.notifications = notifications;
    this.clock = clock;
  }

  /**
   * Checks the SOAs of an invoice after a ledger movement.
   *
   * @param event movement
   */
  @EventListener
  public void on(InvoiceMovementPosted event) {
    for (EbSoa soa : soas.findByInvoiceNo(event.invoiceNo())) {
      if (soa.getPaidNotifiedAt() != null || soa.getStatus() == EbSoa.Status.REJECTED) {
        continue;
      }
      boolean paid =
          invoices.paymentStatus(soa.getInvoiceNos()).values().stream().allMatch("PAID"::equals);
      if (paid) {
        soa.paidNotified(clock.instant());
        notify(soa);
      }
    }
  }

  private void notify(EbSoa soa) {
    Notice notice =
        new Notice(
            soa.getSoaNo() + ": invoices paid",
            "SOA " + soa.getInsurerSoaNo() + " - " + String.join(", ", soa.getInvoiceNos()),
            EbCodes.SOA_LINK + soa.getId(),
            EbCodes.ENTITY_SOA,
            soa.getId().toString());
    Optional<EbProgramme> programme = programmes.findById(soa.getProgrammeId());
    programme.ifPresent(
        p -> notifications.notifyUser(p.getAccountOfficer(), notice, EbCodes.EVENT_INVOICE_PAID));
    notifications.notifyPermission(EbCodes.PERMISSION_COLLECT, notice, EbCodes.EVENT_INVOICE_PAID);
  }
}
