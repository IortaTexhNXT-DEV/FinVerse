package com.iortatechnxt.brokerverse.eb.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.service.EndorsementRequestService;
import com.iortatechnxt.brokerverse.adjustment.service.RequestDraft;
import com.iortatechnxt.brokerverse.booking.domain.InvoiceKind;
import com.iortatechnxt.brokerverse.eb.domain.EbFunding;
import com.iortatechnxt.brokerverse.eb.domain.EbMemberChange;
import com.iortatechnxt.brokerverse.eb.domain.EbMemberRepository;
import com.iortatechnxt.brokerverse.eb.domain.EbProgramme;
import com.iortatechnxt.brokerverse.eb.domain.EbProgrammeLine;
import com.iortatechnxt.brokerverse.eb.service.EbParameters;
import com.iortatechnxt.brokerverse.eb.service.EbWorkingDays;
import com.iortatechnxt.brokerverse.eb.tracked.service.TrackedItemService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoiceRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * The endorsement request of a member change with a premium effect (FR-EB-055): raised on the
 * booked invoice of the line's current account with the change number as reference.
 */
class MemberChangeEffectsTest {

  private final EndorsementRequestService endorsements = mock(EndorsementRequestService.class);
  private final OpsInvoiceRepository invoices = mock(OpsInvoiceRepository.class);

  private final MemberChangeEffects effects =
      new MemberChangeEffects(
          endorsements,
          invoices,
          mock(EbMemberRepository.class),
          mock(TrackedItemService.class),
          mock(EbParameters.class),
          mock(EbWorkingDays.class),
          Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

  private static EbProgramme programme() {
    return new EbProgramme(
        1L,
        "EBP-2026-000001",
        new EbProgramme.ClientRef(3L, "CL-1", "Client"),
        new EbProgramme.Profile("Plan", "BDO", EbFunding.EMPLOYER, "ebao", null, true));
  }

  @Test
  void theRequestIsRaisedOnTheBookedInvoiceOfTheLine() {
    EbProgramme programme = programme();
    EbProgrammeLine line =
        programme.addLine(
            new EbProgrammeLine.Data("HMO", null, "INS-MGIC", "P-1", "ARN-1", null, null, 5));
    EbMemberChange change =
        new EbMemberChange(
            programme, "EBM-2026-000001", line, new EbMemberChange.Header(2026, "AO", true, null));
    change.addLine(
        new EbMemberChange.LineData(
            EbMemberChange.Action.DELETE, "E-1", null, LocalDate.of(2026, 5, 1), 1L));
    change.relayed(Instant.EPOCH, 1L);
    change.mirror(EbMemberChange.Status.RELAYED, Instant.EPOCH);
    change.billed(
        new EbMemberChange.Billing(LocalDate.EPOCH, "B-1", new BigDecimal("-500"), false));

    assertThatThrownBy(() -> effects.raiseEndorsement(change, line))
        .extracting("code")
        .isEqualTo("EB_NO_INVOICE");

    OpsInvoice booked = mock(OpsInvoice.class);
    when(booked.getKind()).thenReturn(InvoiceKind.BOOKING);
    when(booked.getId()).thenReturn(7L);
    when(booked.getInvoiceNo()).thenReturn("INV-7");
    when(invoices.findByArnOrderByPolicyYearAscIdAsc("ARN-1")).thenReturn(List.of(booked));
    EndorsementRequest request = mock(EndorsementRequest.class);
    when(request.getId()).thenReturn(9L);
    when(request.getRequestNo()).thenReturn("ENR-2026-000009");
    AtomicReference<RequestDraft> sent = new AtomicReference<>();
    when(endorsements.create(any(RequestDraft.class)))
        .thenAnswer(
            i -> {
              sent.set(i.getArgument(0));
              return request;
            });
    effects.raiseEndorsement(change, line);
    assertThat(change.getEndorsementRequestNo()).isEqualTo("ENR-2026-000009");
    assertThat(sent.get().invoiceNo()).isEqualTo("INV-7");
    assertThat(sent.get().terms().endorsementRef()).isEqualTo("EBM-2026-000001");
    assertThat(sent.get().terms().effectiveDate()).isEqualTo(LocalDate.of(2026, 5, 1));
    assertThat(sent.get().amounts().basic()).isEqualByComparingTo("-500");
    assertThat(sent.get().terms().description()).contains("1 delete");
  }
}
