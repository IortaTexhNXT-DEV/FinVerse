package com.iortatechnxt.brokerverse.tax.seed;

import com.iortatechnxt.brokerverse.tax.domain.FilingFrequency;
import com.iortatechnxt.brokerverse.tax.domain.PayeeClass;
import com.iortatechnxt.brokerverse.tax.domain.TaxAuthority;
import com.iortatechnxt.brokerverse.tax.domain.TaxType;
import com.iortatechnxt.brokerverse.tax.domain.VatTreatment;
import com.iortatechnxt.brokerverse.tax.domain.WorksheetKind;
import java.util.List;

/**
 * Reference values of the tax seed data for the seed chart of accounts (V900): tax codes and ATCs
 * with the rates of RR 11-2018 as understood at the time of writing, the filing calendar, tax
 * profiles of the seed suppliers, intermediaries and two customers. Rates and ATCs are seed values:
 * a tax officer confirms them against the current BIR issuances before go-live.
 */
final class TaxSeedCatalog {

  private static final String EWT_PAYABLE = "2508";
  private static final String OUTPUT_VAT = "2504";
  private static final String COMMISSION_ATC = "WC515";
  private static final String SERVICES_ATC = "WC160";
  private static final String COMMISSION_NATURE =
      "Commissions of independent and exclusive sales representatives and marketing agents";

  /** Tax codes. */
  static final List<CodeSpec> CODES =
      List.of(
          code("VAT-OUT", "Output VAT 12%", TaxType.VAT_OUTPUT, "12", OUTPUT_VAT),
          code("VAT-IN", "Input VAT 12%", TaxType.VAT_INPUT, "12", "1603"),
          code("VAT-ZR", "Zero-rated sales and purchases", TaxType.VAT_ZERO_RATED, "0", OUTPUT_VAT),
          code("VAT-EX", "VAT-exempt sales and purchases", TaxType.VAT_EXEMPT, "0", OUTPUT_VAT),
          code("PT-2", "Premium tax 2% (non-VAT business)", TaxType.PREMIUM_TAX, "2", "2507"),
          code("DST-PREM", "DST on premiums (P0.50 per P4.00)", TaxType.DST, "12.5", "2503"),
          code("LGT", "Local government tax on premiums", TaxType.LGT, "0.75", "2505"),
          code("FST", "Fire service tax (fire premiums)", TaxType.FST, "2", "2506"),
          ewt(
              "WI010",
              PayeeClass.INDIVIDUAL,
              "5",
              "Professional fees - individual, gross up to P3M"),
          ewt(
              "WI011",
              PayeeClass.INDIVIDUAL,
              "10",
              "Professional fees - individual, gross over P3M"),
          ewt(
              "WC010",
              PayeeClass.CORPORATE,
              "10",
              "Professional fees - juridical, gross up to P720K"),
          ewt(
              "WC011",
              PayeeClass.CORPORATE,
              "15",
              "Professional fees - juridical, gross over P720K"),
          ewt("WI100", PayeeClass.INDIVIDUAL, "5", "Rentals of real and personal property"),
          ewt("WC100", PayeeClass.CORPORATE, "5", "Rentals of real and personal property"),
          ewt("WI120", PayeeClass.INDIVIDUAL, "2", "Income payments to contractors"),
          ewt("WC120", PayeeClass.CORPORATE, "2", "Income payments to contractors"),
          ewt("WC158", PayeeClass.CORPORATE, "1", "Purchase of goods by top withholding agents"),
          ewt(
              SERVICES_ATC,
              PayeeClass.CORPORATE,
              "2",
              "Purchase of services by top withholding agents"),
          ewt("WI515", PayeeClass.INDIVIDUAL, "10", COMMISSION_NATURE),
          ewt(COMMISSION_ATC, PayeeClass.CORPORATE, "10", COMMISSION_NATURE));

  /** Filing calendar. */
  static final List<FormSpec> FORMS =
      List.of(
          new FormSpec(
              "2550Q",
              "Quarterly VAT return",
              TaxAuthority.BIR,
              FilingFrequency.QUARTERLY,
              WorksheetKind.VAT,
              25,
              OUTPUT_VAT,
              "1603"),
          new FormSpec(
              "0619-E",
              "Monthly remittance of creditable EWT",
              TaxAuthority.BIR,
              FilingFrequency.MONTHLY_EXCEPT_QUARTER_END,
              WorksheetKind.EWT,
              10,
              EWT_PAYABLE,
              null),
          new FormSpec(
              "1601-EQ",
              "Quarterly remittance of creditable EWT",
              TaxAuthority.BIR,
              FilingFrequency.QUARTERLY,
              WorksheetKind.EWT,
              31,
              EWT_PAYABLE,
              null),
          new FormSpec(
              "1601-C",
              "Withholding tax on compensation (payroll system)",
              TaxAuthority.BIR,
              FilingFrequency.MONTHLY,
              WorksheetKind.NONE,
              10,
              null,
              null));

  /** Party tax profiles (TIN taken from the party master). */
  static final List<ProfileSpec> PROFILES =
      List.of(
          corporate("S-0001", "METRO OFFICE SUPPLIES CO.", SERVICES_ATC, "1550"),
          corporate("S-0002", "CLOUD SYSTEMS PHILIPPINES INC.", SERVICES_ATC, "1634"),
          corporate("S-0003", "AYALA PROPERTY LEASING", "WC100", "1226"),
          corporate("G-0001", "AUTOFIX SERVICE CENTER", "WC120", "1100"),
          corporate("G-0002", "CEBU MOTOR WORKS", "WC120", "6000"),
          corporate("A-0001", "ROSA MENDOZA INSURANCE AGENCY", COMMISSION_ATC, "1226"),
          new ProfileSpec(
              "A-0002",
              PayeeClass.INDIVIDUAL,
              "LIM, PEDRO",
              "LIM",
              "PEDRO",
              "",
              "6000",
              VatTreatment.REGULAR,
              "WI515"),
          corporate("B-0001", "PACIFIC INSURANCE BROKERS INC.", COMMISSION_ATC, "1634"),
          corporate("B-0002", "ASIA RISK ADVISORY BROKERS", COMMISSION_ATC, "1605"),
          new ProfileSpec(
              "C-000101",
              PayeeClass.INDIVIDUAL,
              "DELA CRUZ, JUAN",
              "DELA CRUZ",
              "JUAN",
              "",
              "1100",
              VatTreatment.REGULAR,
              null),
          new ProfileSpec(
              "C-000202",
              PayeeClass.CORPORATE,
              "VISAYAS SHIPPING LINES INC.",
              null,
              null,
              null,
              "6000",
              VatTreatment.ZERO_RATED,
              null));

  private TaxSeedCatalog() {}

  private static CodeSpec code(
      String code, String name, TaxType type, String rate, String account) {
    return new CodeSpec(code, name, type, null, null, rate, account, null);
  }

  private static CodeSpec ewt(String atc, PayeeClass payee, String rate, String nature) {
    return new CodeSpec(
        atc, atc + " " + nature, TaxType.EWT, atc, payee, rate, EWT_PAYABLE, nature);
  }

  private static ProfileSpec corporate(String party, String name, String atc, String zip) {
    return new ProfileSpec(
        party, PayeeClass.CORPORATE, name, null, null, null, zip, VatTreatment.REGULAR, atc);
  }

  /** Tax code values. */
  record CodeSpec(
      String code,
      String name,
      TaxType type,
      String atc,
      PayeeClass payee,
      String rate,
      String account,
      String nature) {}

  /** Tax form values. */
  record FormSpec(
      String code,
      String name,
      TaxAuthority authority,
      FilingFrequency frequency,
      WorksheetKind worksheet,
      int dueDay,
      String payable,
      String credit) {}

  /** Party tax profile values. */
  record ProfileSpec(
      String party,
      PayeeClass payee,
      String name,
      String lastName,
      String firstName,
      String middleName,
      String zip,
      VatTreatment vat,
      String atc) {}
}
