package com.iortatechnxt.brokerverse.submitted.masterlist.service;

import com.iortatechnxt.brokerverse.submitted.domain.SbmAssured;
import com.iortatechnxt.brokerverse.submitted.domain.SbmExtractedValue;
import com.iortatechnxt.brokerverse.submitted.domain.SbmExtraction;
import com.iortatechnxt.brokerverse.submitted.domain.SbmLoan;
import com.iortatechnxt.brokerverse.submitted.domain.SbmMarks;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyData;
import com.iortatechnxt.brokerverse.submitted.domain.SbmRisk;
import com.iortatechnxt.brokerverse.submitted.domain.SbmTerms;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns the proposed values of an extraction into the data the Extraction Review screen shows for
 * confirmation (BRIDSP-02): each proposed value replaces the record's value; values that cannot be
 * read as a date or an amount are left out for the user to enter.
 */
public final class ProposalMapper {

  private static final List<DateTimeFormatter> DATES =
      List.of(
          DateTimeFormatter.ISO_LOCAL_DATE,
          DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH),
          DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH),
          DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH));

  private ProposalMapper() {}

  /**
   * The proposed data.
   *
   * @param x extraction
   * @param current data of the record, null for a new record
   * @return data to confirm
   */
  public static SbmPolicyData proposed(SbmExtraction x, SbmPolicyData current) {
    Map<String, SbmExtractedValue> f = x.getFields();
    SbmLoan l = current == null ? SbmLoan.NONE : current.loan();
    SbmAssured a =
        current == null ? new SbmAssured(null, null, null, null, null, null) : current.assured();
    SbmTerms t =
        current == null
            ? new SbmTerms(null, null, null, null, null, null, null, "PHP")
            : current.terms();
    SbmRisk r = current == null ? SbmRisk.NONE : current.risk();
    return new SbmPolicyData(
        current == null ? x.getSegment() : current.segment(),
        current == null ? x.getBusinessType() : current.businessType(),
        new SbmLoan(
            text(f, "PN_NO", l.pnNo()),
            l.loanApplicationNo(),
            l.cif(),
            l.valueDate(),
            l.maturityDate(),
            l.referringBranch(),
            l.originatingUnit(),
            l.borrowerName()),
        new SbmAssured(
            text(f, "ASSURED", a.assuredName()),
            a.mailingAddress(),
            a.telephone(),
            a.mobile(),
            a.email(),
            a.bankCounterpartEmail()),
        new SbmTerms(
            text(f, "INSURER", t.insurerCode()),
            text(f, "POLICY_NUMBER", t.policyNo()),
            date(f, "PERIOD_FROM", t.inceptionDate()),
            date(f, "PERIOD_TO", t.expiryDate()),
            null,
            amount(f, "SUM_INSURED", t.sumInsured()),
            amount(f, "PREMIUM", t.totalPremium()),
            t.currency()),
        new SbmRisk(
            text(f, "UNIT", r.unitDescription()),
            text(f, "SERIAL_NO", r.serialNo()),
            text(f, "MOTOR_NO", r.motorNo()),
            r.colour(),
            text(f, "PLATE_NO", r.plateNo()),
            r.vehicleType(),
            r.vehicleYear(),
            text(f, "LOCATION", r.propertyLocation()),
            r.occupancy(),
            r.mortgagee()),
        current == null ? SbmMarks.NONE : current.marks());
  }

  private static String text(Map<String, SbmExtractedValue> f, String field, String fallback) {
    SbmExtractedValue v = f.get(field);
    return v == null || v.value() == null ? fallback : v.value();
  }

  private static LocalDate date(
      Map<String, SbmExtractedValue> f, String field, LocalDate fallback) {
    String v = text(f, field, null);
    if (v == null) {
      return fallback;
    }
    for (DateTimeFormatter format : DATES) {
      try {
        return LocalDate.parse(v, format);
      } catch (DateTimeParseException e) {
        // try the next format
      }
    }
    return fallback;
  }

  private static BigDecimal amount(
      Map<String, SbmExtractedValue> f, String field, BigDecimal fallback) {
    String v = text(f, field, null);
    if (v == null) {
      return fallback;
    }
    try {
      return new BigDecimal(v.replace(",", ""));
    } catch (NumberFormatException e) {
      return fallback;
    }
  }
}
