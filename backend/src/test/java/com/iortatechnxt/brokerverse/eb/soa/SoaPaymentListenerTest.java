package com.iortatechnxt.brokerverse.eb.soa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.eb.domain.EbFunding;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbSoa;
import com.iortatechnxt.brokerverse.eb.domain.EbSoaRepository;
import com.iortatechnxt.brokerverse.eb.soa.service.SoaInvoices;
import com.iortatechnxt.brokerverse.eb.soa.service.SoaPaymentListener;
import com.iortatechnxt.brokerverse.messaging.service.NotificationService;
import com.iortatechnxt.brokerverse.opsledger.domain.MovementType;
import com.iortatechnxt.brokerverse.opsledger.service.OpsLedgerEvents.InvoiceMovementPosted;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** The payment notice of an SOA (FR-EB-053): once, when every invoice it bills reads PAID. */
class SoaPaymentListenerTest {

  private final EbSoaRepository soas = mock(EbSoaRepository.class);
  private final EbProgrammeRepository programmes = mock(EbProgrammeRepository.class);
  private final SoaInvoices invoices = mock(SoaInvoices.class);
  private final NotificationService notifications = mock(NotificationService.class);
  private final SoaPaymentListener listener =
      new SoaPaymentListener(
          soas, programmes, invoices, notifications, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

  private static EbSoa soa() {
    EbProgramme programme =
        new EbProgramme(
            1L,
            "EBP-2026-000001",
            new EbProgramme.ClientRef(3L, "CL-1", "Client"),
            new EbProgramme.Profile("Plan", "BDO", EbFunding.EMPLOYER, "ebao", null, true));
    EbSoa soa =
        new EbSoa(
            programme,
            "EBS-2026-000001",
            new EbSoa.Intake(
                "INS-MGIC",
                "S-1",
                LocalDate.EPOCH,
                LocalDate.EPOCH,
                BigDecimal.TEN,
                "PHP",
                LocalDate.EPOCH,
                null),
            new EbSoa.StoredFile(3L, "abc"));
    soa.linkInvoices(List.of("INV-1", "INV-2"));
    ReflectionTestUtils.setField(soa, "id", 5L);
    return soa;
  }

  private static InvoiceMovementPosted movement() {
    return new InvoiceMovementPosted(
        1L, "INV-1", MovementType.values()[0], "CASHIERING", "OR-1", Map.of());
  }

  @Test
  void theAoAndCollectionAreToldOnceWhenEveryInvoiceIsPaid() {
    EbSoa soa = soa();
    when(soas.findByInvoiceNo("INV-1")).thenReturn(List.of(soa));
    when(invoices.paymentStatus(soa.getInvoiceNos()))
        .thenReturn(Map.of("INV-1", "PAID", "INV-2", "PARTIALLY_PAID"));
    listener.on(movement());
    verify(notifications, never()).notifyPermission(any(), any(), any());

    when(invoices.paymentStatus(soa.getInvoiceNos()))
        .thenReturn(Map.of("INV-1", "PAID", "INV-2", "PAID"));
    when(programmes.findById(any()))
        .thenReturn(Optional.of(mock(EbProgramme.class, org.mockito.Answers.RETURNS_DEEP_STUBS)));
    listener.on(movement());
    verify(notifications).notifyPermission(eq("EB_COLLECT"), any(), eq("EB_INVOICE_PAID"));
    assertThat(soa.getPaidNotifiedAt()).isEqualTo(Instant.EPOCH);

    listener.on(movement());
    verify(notifications).notifyPermission(eq("EB_COLLECT"), any(), eq("EB_INVOICE_PAID"));
  }
}
