package com.iortatechnxt.brokerverse.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iortatechnxt.brokerverse.booking.domain.DispatchStatus;
import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoice;
import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoiceType;
import com.iortatechnxt.brokerverse.booking.domain.SiKind;
import com.iortatechnxt.brokerverse.booking.domain.SiRecipient;
import com.iortatechnxt.brokerverse.booking.domain.SiTrigger;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceRegister;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceService;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceService.CreditRequest;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceService.IssueRequest;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceTypeService;
import com.iortatechnxt.brokerverse.messaging.service.MessageService;
import com.iortatechnxt.brokerverse.support.AsUser;
import com.iortatechnxt.brokerverse.support.IntegrationTest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Service invoices: gap-free numbering, PDF, dispatch with owner notification, credits, types. */
@IntegrationTest
class ServiceInvoiceIT {

  @Autowired private BookingFixtures fx;
  @Autowired private ServiceInvoiceService serviceInvoices;
  @Autowired private ServiceInvoiceRegister register;
  @Autowired private ServiceInvoiceTypeService types;
  @Autowired private MessageService messages;
  @Autowired private PlatformTransactionManager txManager;
  @Autowired private AsUser as;

  private IssueRequest internal(String amount) {
    return new IssueRequest(
        fx.company(),
        "INTERNAL",
        null,
        null,
        "NB-CBG-M",
        "NB CBG Metro Manila",
        BookingFixtures.BOOKED_ON,
        "PHP",
        new BigDecimal(amount),
        new BigDecimal("12.00"),
        new BigDecimal("10.00"),
        "Internal recharge");
  }

  private static long sequenceOf(ServiceInvoice si) {
    return Long.parseLong(si.getSiNo().substring(si.getSiNo().lastIndexOf('-') + 1));
  }

  @Test
  void numbersAreSequentialWithoutGapsEvenWhenATransactionRollsBack() {
    ServiceInvoice first = as.run("proc", () -> serviceInvoices.issue(internal("100")));
    TransactionTemplate tx = new TransactionTemplate(txManager);
    assertThatThrownBy(
            () ->
                tx.executeWithoutResult(
                    s -> {
                      as.run("proc", () -> serviceInvoices.issue(internal("200")));
                      throw new IllegalStateException("rolled back after numbering");
                    }))
        .isInstanceOf(IllegalStateException.class);
    ServiceInvoice second = as.run("proc", () -> serviceInvoices.issue(internal("300")));
    assertThat(sequenceOf(second)).isEqualTo(sequenceOf(first) + 1);
    assertThat(first.getSiNo()).startsWith("SI-HO-2026-");
    assertThat(first.getNetAmount()).isEqualByComparingTo("102.00");
    assertThat(first.getDispatchStatus()).isEqualTo(DispatchStatus.NOT_SENT);
    assertThat(new String(register.document(first.getId()), 0, 4, StandardCharsets.US_ASCII))
        .isEqualTo("%PDF");
    assertThat(register.search(fx.company(), first.getSiNo(), SiKind.INVOICE, PageRequest.of(0, 5)))
        .hasSize(1);
  }

  @Test
  void anInsurerServiceInvoiceIsMailedAndCreditedUpToItsAmount() {
    ServiceInvoice si =
        as.run(
            "proc",
            () ->
                serviceInvoices.issue(
                    new IssueRequest(
                        fx.company(),
                        "INSURER_COMMISSION",
                        null,
                        "ARN-TEST",
                        "INS-MGIC",
                        null,
                        null,
                        "PHP",
                        new BigDecimal("1000.00"),
                        new BigDecimal("120.00"),
                        new BigDecimal("100.00"),
                        null)));
    ServiceInvoice sent = serviceInvoices.get(si.getId());
    assertThat(sent.getRecipientEmail()).isNotBlank();
    assertThat(sent.getMessageId()).isNotNull();
    assertThat(sent.getDispatchStatus()).isEqualTo(DispatchStatus.SENT);
    assertThat(messages.forRecord("ServiceInvoice", String.valueOf(si.getId()))).isNotEmpty();
    ServiceInvoice resent = as.run("proc", () -> serviceInvoices.resend(si.getId()));
    assertThat(resent.getDispatchStatus()).isIn(DispatchStatus.QUEUED, DispatchStatus.SENT);

    ServiceInvoice part =
        as.run(
            "adjust",
            () ->
                serviceInvoices.credit(
                    si.getSiNo(),
                    new CreditRequest(new BigDecimal("400"), BigDecimal.ZERO, null, "Part")));
    assertThat(part.getKind()).isEqualTo(SiKind.CREDIT);
    assertThat(part.getWtaxAmount()).isEqualByComparingTo("40.00");
    ServiceInvoice rest =
        as.run(
            "adjust",
            () ->
                serviceInvoices.credit(si.getSiNo(), new CreditRequest(null, null, null, "Rest")));
    assertThat(rest.getCommission()).isEqualByComparingTo("600.00");
    assertThat(rest.getVatOnCommission()).isEqualByComparingTo("120.00");
    assertThatThrownBy(
            () ->
                as.run(
                    "adjust",
                    () ->
                        serviceInvoices.credit(
                            si.getSiNo(), new CreditRequest(null, null, null, "Again"))))
        .extracting("code")
        .isEqualTo("CREDIT_EXCEEDS_INVOICE");
    assertThatThrownBy(
            () ->
                as.run(
                    "adjust",
                    () ->
                        serviceInvoices.credit(
                            rest.getSiNo(), new CreditRequest(null, null, null, "No"))))
        .extracting("code")
        .isEqualTo("CREDIT_OF_CREDIT");
  }

  @Test
  void typesAreMaintainedAndInactiveTypesRefused() {
    String code = "TEST_" + BookingFixtures.token();
    ServiceInvoiceType type =
        as.run(
            "badmin",
            () ->
                types.create(
                    code,
                    new ServiceInvoiceType.Settings(
                        "Test type",
                        SiRecipient.INTERNAL,
                        SiTrigger.MANUAL,
                        null,
                        "proc",
                        "SERVICE_INVOICE_NOTE",
                        false)));
    assertThat(types.list()).extracting(ServiceInvoiceType::getCode).contains(code);
    assertThatThrownBy(
            () ->
                as.run(
                    "proc",
                    () ->
                        serviceInvoices.issue(
                            new IssueRequest(
                                fx.company(),
                                code,
                                null,
                                null,
                                "X",
                                null,
                                null,
                                "PHP",
                                BigDecimal.ONE,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                null))))
        .extracting("code")
        .isEqualTo("SERVICE_INVOICE_TYPE_INACTIVE");
    as.run(
        "badmin",
        () ->
            types.update(
                type.getId(),
                new ServiceInvoiceType.Settings(
                    "Test type",
                    SiRecipient.INTERNAL,
                    SiTrigger.MANUAL,
                    "BOOKING_PROCESS",
                    "proc",
                    "SERVICE_INVOICE_NOTE",
                    true)));
    ServiceInvoice issued =
        as.run(
            "proc",
            () ->
                serviceInvoices.issue(
                    new IssueRequest(
                        fx.company(),
                        code,
                        null,
                        null,
                        "UNIT",
                        null,
                        null,
                        "PHP",
                        BigDecimal.TEN,
                        BigDecimal.ONE,
                        BigDecimal.ZERO,
                        null)));
    assertThat(issued.getOwnerUsername()).isEqualTo("proc");
    assertThat(issued.getRecipientName()).isEqualTo("UNIT");
    assertThatThrownBy(
            () ->
                as.run(
                    "badmin",
                    () ->
                        types.create(
                            code,
                            new ServiceInvoiceType.Settings(
                                "Dup",
                                SiRecipient.INTERNAL,
                                SiTrigger.MANUAL,
                                null,
                                null,
                                "SERVICE_INVOICE_NOTE",
                                true))))
        .isInstanceOf(RuntimeException.class);
  }
}
