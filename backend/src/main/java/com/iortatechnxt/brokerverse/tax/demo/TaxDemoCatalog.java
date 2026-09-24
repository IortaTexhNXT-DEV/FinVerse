package com.iortatechnxt.brokerverse.tax.demo;

import com.iortatechnxt.brokerverse.tax.domain.FilingFrequency;
import com.iortatechnxt.brokerverse.tax.domain.IcMeasure;
import com.iortatechnxt.brokerverse.tax.domain.IcSchedule;
import com.iortatechnxt.brokerverse.tax.domain.NormalBalance;
import com.iortatechnxt.brokerverse.tax.domain.PayeeClass;
import com.iortatechnxt.brokerverse.tax.domain.TaxAuthority;
import com.iortatechnxt.brokerverse.tax.domain.TaxType;
import com.iortatechnxt.brokerverse.tax.domain.VatTreatment;
import com.iortatechnxt.brokerverse.tax.domain.WorksheetKind;
import java.util.List;

/**
 * Reference values of the tax demo data for the demo chart of accounts (V900): tax codes and ATCs
 * with the rates of RR 11-2018 as understood at the time of writing, the filing calendar, tax
 * profiles of the demo suppliers, intermediaries and two customers, and the IC schedule mapping.
 * Rates and ATCs are demo values: a tax officer confirms them against the current BIR issuances
 * before go-live.
 */
final class TaxDemoCatalog {

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
              "2000",
              "Documentary stamp tax declaration (policies)",
              TaxAuthority.BIR,
              FilingFrequency.MONTHLY,
              WorksheetKind.DST,
              5,
              "2503",
              null),
          new FormSpec(
              "2551Q",
              "Quarterly percentage (premium) tax return",
              TaxAuthority.BIR,
              FilingFrequency.QUARTERLY,
              WorksheetKind.PREMIUM_TAX,
              25,
              "2507",
              null),
          new FormSpec(
              "LBT",
              "Local business tax on premiums",
              TaxAuthority.LGU,
              FilingFrequency.QUARTERLY,
              WorksheetKind.LGT,
              20,
              "2505",
              null),
          new FormSpec(
              "FST",
              "Fire service tax remittance",
              TaxAuthority.BFP,
              FilingFrequency.MONTHLY,
              WorksheetKind.FST,
              20,
              "2506",
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
          corporate("S-0003", "AYALA PROPERTY LEASING (DEMO)", "WC100", "1226"),
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

  /** IC schedule mapping. */
  static final List<IcSpec> IC_LINES =
      List.of(
          ic(
              IcSchedule.PREMIUMS,
              "GROSS_WRITTEN",
              "Gross premiums written",
              "4100",
              NormalBalance.CREDIT,
              1,
              null),
          ic(
              IcSchedule.PREMIUMS,
              "RI_CEDED",
              "Less: reinsurance premiums ceded",
              "4200",
              NormalBalance.DEBIT,
              -1,
              null),
          ic(
              IcSchedule.LOSSES,
              "LOSSES_PAID",
              "Gross losses and claims paid",
              "5100",
              NormalBalance.DEBIT,
              1,
              null),
          ic(
              IcSchedule.LOSSES,
              "RESERVE_CHANGE",
              "Change in claims reserves",
              "5200",
              NormalBalance.DEBIT,
              1,
              null),
          ic(
              IcSchedule.LOSSES,
              "RI_SHARE",
              "Less: reinsurers' share of losses",
              "5300",
              NormalBalance.CREDIT,
              -1,
              null),
          ic(
              IcSchedule.COMMISSIONS,
              "COMMISSION_EXPENSE",
              "Commissions incurred",
              "5400",
              NormalBalance.DEBIT,
              1,
              null),
          ic(
              IcSchedule.COMMISSIONS,
              "RI_COMMISSION",
              "Less: reinsurance commissions earned",
              "4400",
              NormalBalance.CREDIT,
              -1,
              null),
          range(
              IcSchedule.NET_WORTH,
              "TOTAL_ASSETS",
              "Total assets",
              "1000",
              "1999",
              NormalBalance.DEBIT,
              1),
          ic(
              IcSchedule.NET_WORTH,
              "NON_ADMITTED_PREPAID",
              "Less: prepaid expenses (non-admitted)",
              "1601",
              NormalBalance.DEBIT,
              -1,
              null),
          ic(
              IcSchedule.NET_WORTH,
              "NON_ADMITTED_ADVANCES",
              "Less: advances to employees (non-admitted)",
              "1604",
              NormalBalance.DEBIT,
              -1,
              null),
          ic(
              IcSchedule.NET_WORTH,
              "NON_ADMITTED_SUSPENSE",
              "Less: suspense account (non-admitted)",
              "1606",
              NormalBalance.DEBIT,
              -1,
              null),
          range(
              IcSchedule.NET_WORTH,
              "TOTAL_LIABILITIES",
              "Less: total liabilities",
              "2000",
              "2999",
              NormalBalance.CREDIT,
              -1),
          ic(
              IcSchedule.RESERVES,
              "UPR",
              "Reserve for unearned premiums",
              "2101",
              NormalBalance.CREDIT,
              1,
              null),
          ic(
              IcSchedule.RESERVES,
              "OSLR",
              "Outstanding losses reserve",
              "2102",
              NormalBalance.CREDIT,
              1,
              null),
          ic(
              IcSchedule.RESERVES,
              "IBNR",
              "Claims incurred but not reported",
              "2103",
              NormalBalance.CREDIT,
              1,
              null),
          ic(
              IcSchedule.RESERVES,
              "PDR",
              "Premium deficiency reserve",
              "2104",
              NormalBalance.CREDIT,
              1,
              null),
          ic(
              IcSchedule.RESERVES,
              "RI_UPR",
              "Less: reinsurers' share of UPR",
              "1301",
              NormalBalance.DEBIT,
              -1,
              null),
          ic(
              IcSchedule.RESERVES,
              "RI_OSLR",
              "Less: reinsurers' share of outstanding losses",
              "1302",
              NormalBalance.DEBIT,
              -1,
              null),
          ic(
              IcSchedule.RESERVES,
              "RI_IBNR",
              "Less: reinsurers' share of IBNR",
              "1303",
              NormalBalance.DEBIT,
              -1,
              null),
          ic(
              IcSchedule.INVESTMENTS,
              "PLACEMENTS",
              "Short-term placements",
              "1120",
              NormalBalance.DEBIT,
              1,
              null),
          ic(
              IcSchedule.INVESTMENTS,
              "FVPL",
              "Financial assets at FVPL",
              "1501",
              NormalBalance.DEBIT,
              1,
              null),
          ic(
              IcSchedule.INVESTMENTS,
              "FVOCI",
              "Financial assets at FVOCI",
              "1502",
              NormalBalance.DEBIT,
              1,
              null),
          ic(
              IcSchedule.INVESTMENTS,
              "AMORTISED_COST",
              "Government securities at amortised cost",
              "1503",
              NormalBalance.DEBIT,
              1,
              null),
          ic(
              IcSchedule.INVESTMENTS,
              "ACCRUED_INTEREST",
              "Accrued interest receivable",
              "1504",
              NormalBalance.DEBIT,
              1,
              null),
          ic(
              IcSchedule.INVESTMENTS,
              "SECURITY_FUND",
              "Security fund deposit",
              "1505",
              NormalBalance.DEBIT,
              1,
              null),
          ic(
              IcSchedule.RBC,
              "EQUITY_RISK",
              "Investment risk - FVPL securities",
              "1501",
              NormalBalance.DEBIT,
              1,
              "30"),
          ic(
              IcSchedule.RBC,
              "FVOCI_RISK",
              "Investment risk - FVOCI securities",
              "1502",
              NormalBalance.DEBIT,
              1,
              "15"),
          ic(
              IcSchedule.RBC,
              "PLACEMENT_RISK",
              "Investment risk - placements",
              "1120",
              NormalBalance.DEBIT,
              1,
              "1"),
          range(
                  IcSchedule.RBC,
                  "CREDIT_RISK",
                  "Credit risk - insurance receivables",
                  "1201",
                  "1206",
                  NormalBalance.DEBIT,
                  1)
              .withFactor("10"),
          range(
                  IcSchedule.RBC,
                  "RI_CREDIT_RISK",
                  "Credit risk - reinsurance assets",
                  "1301",
                  "1303",
                  NormalBalance.DEBIT,
                  1)
              .withFactor("5"),
          movement("PREMIUM_RISK", "Premium risk - gross premiums written", "4100", "15"),
          movement("PREMIUM_RISK_CEDED", "Premium risk - less ceded premiums", "4200", "15"),
          range(
                  IcSchedule.RBC,
                  "RESERVE_RISK",
                  "Reserve risk - claims reserves",
                  "2102",
                  "2103",
                  NormalBalance.CREDIT,
                  1)
              .withFactor("10"),
          movement("OPERATIONAL_RISK", "Operational risk - gross premiums written", "4100", "2"));

  private TaxDemoCatalog() {}

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

  private static IcSpec ic(
      IcSchedule schedule,
      String code,
      String description,
      String account,
      NormalBalance side,
      int sign,
      String factor) {
    return new IcSpec(schedule, code, description, account, account, side, sign, null, factor);
  }

  private static IcSpec range(
      IcSchedule schedule,
      String code,
      String description,
      String from,
      String to,
      NormalBalance side,
      int sign) {
    return new IcSpec(schedule, code, description, from, to, side, sign, null, null);
  }

  /**
   * RBC premium line: the period movement presented as a credit, so ceded premiums (a debit
   * balance) come out negative and reduce the premium risk base.
   */
  private static IcSpec movement(String code, String description, String account, String factor) {
    return new IcSpec(
        IcSchedule.RBC,
        code,
        description,
        account,
        account,
        NormalBalance.CREDIT,
        1,
        IcMeasure.MOVEMENT,
        factor);
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

  /** IC mapping line values. */
  record IcSpec(
      IcSchedule schedule,
      String code,
      String description,
      String from,
      String to,
      NormalBalance side,
      int sign,
      IcMeasure measure,
      String factor) {

    IcSpec withFactor(String rbcFactor) {
      return new IcSpec(schedule, code, description, from, to, side, sign, measure, rbcFactor);
    }
  }
}
