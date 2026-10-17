package com.iortatechnxt.brokerverse.crm.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.iortatechnxt.brokerverse.renewal.domain.RenewalStage;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** The client's Records tab reads names, formatted amounts and dates, never codes. */
class RecordDescriptionsTest {

  private static final String CURRENCY = "PHP";

  @Test
  void aQuotationShowsTheProductNameAndTheFormattedPremium() {
    assertThat(
            RecordDescriptions.quotation(
                "ARN-2026-000001", "Motor Comprehensive", CURRENCY, new BigDecimal("1234.5")))
        .isEqualTo(
            "ARN-2026-000001 - Motor Comprehensive - Gross premium " + CURRENCY + " 1,234.50");
    assertThat(RecordDescriptions.quotation("ARN-2026-000001", "Fire", CURRENCY, null))
        .isEqualTo("ARN-2026-000001 - Fire - Not rated");
  }

  @Test
  void aProposalNamesTheInsurerOrSaysItIsToBeChosen() {
    assertThat(RecordDescriptions.proposal("ARN-1", "Fire", "Pacific Insurance"))
        .isEqualTo("ARN-1 - Fire - Pacific Insurance");
    assertThat(RecordDescriptions.proposal("ARN-1", "Fire", "")).endsWith("Insurer to be chosen");
  }

  @Test
  void anAccountShowsTheSumInsuredWithItsCurrency() {
    assertThat(RecordDescriptions.account("Fire", null, CURRENCY, new BigDecimal("1000000.00")))
        .isEqualTo("Fire - Insurer to be selected - Sum insured " + CURRENCY + " 1,000,000.00");
  }

  @Test
  void aBookedInvoiceShowsItsKindAsWords() {
    assertThat(
            RecordDescriptions.bookedInvoice(
                "DEBIT_NOTE", "ARN-1", "Pacific Insurance", CURRENCY, new BigDecimal("12000")))
        .isEqualTo(
            "Debit Note - ARN-1 - Pacific Insurance - Gross premium " + CURRENCY + " 12,000.00");
  }

  @Test
  void datesReadAsDayMonthYear() {
    assertThat(RecordDescriptions.claim("ARN-1", "Fire", LocalDate.of(2026, 9, 5)))
        .isEqualTo("ARN-1 - Fire - Loss on 05-Sep-2026");
    assertThat(RecordDescriptions.renewal("POL-1", LocalDate.of(2027, 3, 31)))
        .isEqualTo("POL-1 - Expires 31-Mar-2027");
  }

  @Test
  void anAmountWithoutCurrencyIsTheFigureAlone() {
    assertThat(RecordDescriptions.money(null, new BigDecimal("5"))).isEqualTo("5.00");
    assertThat(RecordDescriptions.money(CURRENCY, null)).isEmpty();
    assertThat(RecordDescriptions.insuranceAdvice("ARN-1", null)).isEqualTo("ARN-1");
  }

  @Test
  void renewalStagesReadAsTheFrsScreenLabels() {
    assertThat(RenewalStage.RA_SENT.label()).isEqualTo("RA Sent / Awaiting Response");
    assertThat(RenewalStage.NB_PATH.label()).isEqualTo("For Proposal / New Business Path");
  }
}
