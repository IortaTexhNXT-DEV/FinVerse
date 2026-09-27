package com.iortatechnxt.brokerverse.tax.service;

import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.system.service.SystemParameterService;
import com.iortatechnxt.brokerverse.tax.domain.AtcQuarterAmounts;
import com.iortatechnxt.brokerverse.tax.domain.Certificate2307Aggregator;
import com.iortatechnxt.brokerverse.tax.domain.PartyTaxProfile;
import com.iortatechnxt.brokerverse.tax.domain.PayeeClass;
import com.iortatechnxt.brokerverse.tax.domain.ReturnFigures;
import com.iortatechnxt.brokerverse.tax.domain.TaxPeriod;
import com.iortatechnxt.brokerverse.tax.domain.Taxpayer;
import com.iortatechnxt.brokerverse.tax.domain.WorksheetKind;
import com.iortatechnxt.brokerverse.tax.service.TaxpayerDirectory.Lookup;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BIR relief-style CSV exports of a quarter: Summary List of Sales (SLS), Summary List of Purchases
 * (SLP) and the Quarterly Alphalist of Payees (QAP, 1601-EQ). The layouts follow the record
 * structure of the BIR RELIEF and Alphalist data entry modules (header, detail and, for the QAP,
 * control records); the exact column layout is documented in {@code
 * docs/modules/TAX_AND_STATUTORY.md}. Names are upper case, amounts have two decimals without
 * thousand separators, text fields are quoted.
 */
@Service
@Transactional(readOnly = true)
public class BirExportService {

  private static final String CONTENT_TYPE = "text/csv";
  private static final String SEP = ",";
  private static final String EOL = "\r\n";
  private static final String FISCAL_YEAR_END_MONTH = "12";
  private static final String DEFAULT_RDO = "000";
  private static final String RDO_PARAMETER = "TAX_RDO_CODE";
  private static final int TIN_DIGITS = 9;
  private static final String FORM_QAP = "1601EQ";
  private static final DateTimeFormatter PERIOD_END = DateTimeFormatter.ofPattern("MM/dd/yyyy");
  private static final DateTimeFormatter RETURN_PERIOD = DateTimeFormatter.ofPattern("MM/yyyy");

  private final TaxWorksheetService worksheets;
  private final TaxpayerDirectory directory;
  private final OrganizationService organization;
  private final SystemParameterService parameters;

  /**
   * Creates the service.
   *
   * @param worksheets worksheets
   * @param directory party facts (individual names)
   * @param organization company (owner / withholding agent)
   * @param parameters RDO code
   */
  public BirExportService(
      TaxWorksheetService worksheets,
      TaxpayerDirectory directory,
      OrganizationService organization,
      SystemParameterService parameters) {
    this.worksheets = worksheets;
    this.directory = directory;
    this.organization = organization;
    this.parameters = parameters;
  }

  /**
   * Summary List of Sales of a quarter.
   *
   * @param companyId company
   * @param year year
   * @param quarter quarter
   * @return CSV file
   */
  public ExportFile sales(Long companyId, int year, int quarter) {
    TaxPeriod period = TaxPeriod.quarter(year, quarter);
    TaxWorksheet vat = worksheets.compute(companyId, WorksheetKind.VAT, period);
    List<PartySummary> rows = PartySummary.of(vat.section(TaxDocumentLine.SALES));
    Lookup lookup =
        directory.lookup(companyId, rows.stream().map(PartySummary::partyCode).toList());
    Taxpayer owner = owner(companyId);
    StringBuilder out = new StringBuilder();
    line(
        out,
        "H",
        "S",
        q(owner.tin()),
        q(owner.name()),
        q(""),
        q(""),
        q(""),
        q(owner.name()),
        q(owner.address()),
        q(""),
        amt(sum(rows, PartySummary::exempt)),
        amt(sum(rows, PartySummary::zeroRated)),
        amt(sum(rows, PartySummary::services)),
        amt(sum(rows, PartySummary::tax)),
        rdo(),
        PERIOD_END.format(period.to()),
        FISCAL_YEAR_END_MONTH);
    for (PartySummary r : rows) {
      Names n = names(lookup, r.partyCode(), r.partyName());
      line(
          out,
          "D",
          "S",
          q(tin(r.tin())),
          q(n.registered()),
          q(n.last()),
          q(n.first()),
          q(n.middle()),
          q(address(lookup, r.partyCode())),
          q(""),
          amt(r.exempt()),
          amt(r.zeroRated()),
          amt(r.services()),
          amt(r.tax()),
          owner.tin(),
          PERIOD_END.format(period.to()));
    }
    return file("SLS", owner, period, out);
  }

  /**
   * Summary List of Purchases of a quarter.
   *
   * @param companyId company
   * @param year year
   * @param quarter quarter
   * @return CSV file
   */
  public ExportFile purchases(Long companyId, int year, int quarter) {
    TaxPeriod period = TaxPeriod.quarter(year, quarter);
    TaxWorksheet vat = worksheets.compute(companyId, WorksheetKind.VAT, period);
    List<PartySummary> rows = PartySummary.of(vat.section(TaxDocumentLine.PURCHASES));
    Lookup lookup =
        directory.lookup(companyId, rows.stream().map(PartySummary::partyCode).toList());
    Taxpayer owner = owner(companyId);
    BigDecimal input = sum(rows, PartySummary::tax);
    StringBuilder out = new StringBuilder();
    line(
        out,
        "H",
        "P",
        q(owner.tin()),
        q(owner.name()),
        q(""),
        q(""),
        q(""),
        q(owner.name()),
        q(owner.address()),
        q(""),
        amt(sum(rows, PartySummary::exempt)),
        amt(sum(rows, PartySummary::zeroRated)),
        amt(sum(rows, PartySummary::services)),
        amt(sum(rows, PartySummary::capitalGoods)),
        amt(BigDecimal.ZERO),
        amt(input),
        amt(input),
        amt(BigDecimal.ZERO),
        rdo(),
        PERIOD_END.format(period.to()),
        FISCAL_YEAR_END_MONTH);
    for (PartySummary r : rows) {
      Names n = names(lookup, r.partyCode(), r.partyName());
      line(
          out,
          "D",
          "P",
          q(tin(r.tin())),
          q(n.registered()),
          q(n.last()),
          q(n.first()),
          q(n.middle()),
          q(address(lookup, r.partyCode())),
          q(""),
          amt(r.exempt()),
          amt(r.zeroRated()),
          amt(r.services()),
          amt(r.capitalGoods()),
          amt(BigDecimal.ZERO),
          amt(r.tax()),
          owner.tin(),
          PERIOD_END.format(period.to()));
    }
    return file("SLP", owner, period, out);
  }

  /**
   * Quarterly Alphalist of Payees (1601-EQ attachment).
   *
   * @param companyId company
   * @param year year
   * @param quarter quarter
   * @return CSV file
   */
  public ExportFile alphalist(Long companyId, int year, int quarter) {
    TaxPeriod period = TaxPeriod.quarter(year, quarter);
    TaxWorksheet ewt = worksheets.compute(companyId, WorksheetKind.EWT, period);
    Map<String, List<AtcQuarterAmounts>> byPayee =
        Certificate2307Aggregator.aggregate(period, EwtWorksheetBuilder.entries(ewt));
    Lookup lookup = directory.lookup(companyId, byPayee.keySet());
    Taxpayer owner = owner(companyId);
    String returnPeriod = RETURN_PERIOD.format(period.to());
    StringBuilder out = new StringBuilder();
    line(
        out,
        "HQAP",
        "H" + FORM_QAP,
        owner.tin(),
        owner.branchCode(),
        q(owner.name()),
        returnPeriod,
        rdo());
    int seq = 0;
    BigDecimal income = BigDecimal.ZERO;
    BigDecimal tax = BigDecimal.ZERO;
    for (Map.Entry<String, List<AtcQuarterAmounts>> e : byPayee.entrySet()) {
      Taxpayer payee = lookup.taxpayer(e.getKey(), e.getKey());
      Names n = names(lookup, e.getKey(), payee.name());
      for (AtcQuarterAmounts a : e.getValue()) {
        seq++;
        line(
            out,
            "D1",
            FORM_QAP,
            String.valueOf(seq),
            payee.tin(),
            payee.branchCode(),
            q(n.registered()),
            q(n.last()),
            q(n.first()),
            q(n.middle()),
            returnPeriod,
            a.atc(),
            ReturnFigures.effectiveRate(a.total(), a.tax()).toPlainString(),
            amt(a.total()),
            amt(a.tax()));
        income = income.add(a.total());
        tax = tax.add(a.tax());
      }
    }
    line(out, "C1", FORM_QAP, owner.tin(), owner.branchCode(), returnPeriod, amt(income), amt(tax));
    return file("QAP", owner, period, out);
  }

  private Taxpayer owner(Long companyId) {
    Company c = organization.getCompany(companyId);
    return Taxpayer.parse(c.getTaxId(), upper(c.getName()), upper(c.getAddress()), null);
  }

  private String rdo() {
    return parameters.text(RDO_PARAMETER, DEFAULT_RDO);
  }

  /** Individuals are listed by name parts, others by registered name. */
  private static Names names(Lookup lookup, String partyCode, String registeredName) {
    Optional<PartyTaxProfile> p = lookup.profile(partyCode);
    if (lookup.payeeClass(partyCode) == PayeeClass.INDIVIDUAL && p.isPresent()) {
      return new Names(
          "",
          upper(p.get().getLastName()),
          upper(p.get().getFirstName()),
          upper(p.get().getMiddleName()));
    }
    return new Names(upper(registeredName), "", "", "");
  }

  private static String address(Lookup lookup, String partyCode) {
    return upper(lookup.taxpayer(partyCode, partyCode).address());
  }

  private static ExportFile file(
      String list, Taxpayer owner, TaxPeriod period, StringBuilder content) {
    String name = owner.tin() + list + period.label().replace("-", "") + ".csv";
    return new ExportFile(name, CONTENT_TYPE, content.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static void line(StringBuilder out, String... fields) {
    out.append(String.join(SEP, fields)).append(EOL);
  }

  private static String tin(String formatted) {
    return formatted.replace("-", "").substring(0, TIN_DIGITS);
  }

  private static String q(String text) {
    String clean = text == null ? "" : text.replace("\r", " ").replace("\n", " ");
    return "\"" + clean.replace("\"", "\"\"") + "\"";
  }

  private static String amt(BigDecimal value) {
    return value.setScale(2, RoundingMode.HALF_EVEN).toPlainString();
  }

  private static String upper(String s) {
    return s == null ? "" : s.toUpperCase(Locale.ROOT);
  }

  private static BigDecimal sum(List<PartySummary> rows, Function<PartySummary, BigDecimal> field) {
    return rows.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  /** Name columns of a list record. */
  private record Names(String registered, String last, String first, String middle) {}

  /**
   * A generated file.
   *
   * @param fileName file name
   * @param contentType MIME type
   * @param content bytes
   */
  public record ExportFile(String fileName, String contentType, byte[] content) {}
}
