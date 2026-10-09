package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.cashiering.domain.Application;
import com.iortatechnxt.brokerverse.cashiering.domain.ApplicationRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.PaymentMode;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.FormText;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptSeriesRepository;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptFormPdf.FormView;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.domain.CompanyRepository;
import com.iortatechnxt.brokerverse.party.domain.Party;
import com.iortatechnxt.brokerverse.party.service.PartyService;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills the AR and OR forms (FRS.CSH.02.04.06, 02.06.02 / 02.06.03): the approved version of the
 * form on the print date, the company record for the blank header fields, the payor's address and
 * TIN, the paid amount in words, the insurer, reference and policy numbers of the accounts paid,
 * the check details, the certificate number, the series range and the copy label.
 */
// Fills the fields of the two forms from the receipt, the company, the parties and the accounts:
// one
// small mapper per field group, kept together so that the forms read in one place.
@SuppressWarnings("PMD.GodClass")
@Component
@Transactional(readOnly = true)
public class ReceiptForms {

  /** Copy for the client. */
  public static final String CLIENT_COPY = "CLIENT";

  /** Copy kept by the company. */
  public static final String COMPANY_COPY = "COMPANY";

  private static final DateTimeFormatter LONG_DATE =
      DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.ENGLISH);
  private static final String NONE = "";

  private final ReceiptFormService forms;
  private final CompanyRepository companies;
  private final PartyService parties;
  private final ApplicationRepository applications;
  private final InvoiceLedgerQueryService invoices;
  private final ReceiptSeriesRepository series;
  private final LovService lovs;

  /**
   * Creates the filler.
   *
   * @param forms versions of the forms
   * @param companies company record
   * @param parties payor and insurer records
   * @param applications accounts paid
   * @param invoices policy and insurer of an account
   * @param series series range
   * @param lovs OR type labels
   */
  public ReceiptForms(
      ReceiptFormService forms,
      CompanyRepository companies,
      PartyService parties,
      ApplicationRepository applications,
      InvoiceLedgerQueryService invoices,
      ReceiptSeriesRepository series,
      LovService lovs) {
    this.forms = forms;
    this.companies = companies;
    this.parties = parties;
    this.applications = applications;
    this.invoices = invoices;
    this.series = series;
    this.lovs = lovs;
  }

  /**
   * A receipt in its form as a PDF.
   *
   * @param r receipt
   * @param print copy, reprint mark, certificate number and print date
   * @return PDF
   */
  public byte[] pdf(Receipt r, PrintMark print) {
    Company company =
        companies
            .findById(r.getCompanyId())
            .orElseThrow(() -> new ResourceNotFoundException("Company", r.getCompanyId()));
    String kind = r.getKind().name();
    FormText text = forms.current(r.getCompanyId(), kind, print.printDate());
    String copyLabel = copyLabel(company, print.copy());
    Map<String, String> tokens =
        Map.of(
            "{COMPANY_NAME}", company.getName(),
            "{CERTIFICATE_NO}", nz(print.certificateNo()),
            "{PRINT_DATE}", print.printDate().format(LONG_DATE),
            "{SERIES_RANGE}", seriesRange(r),
            "{COPY_LABEL}", copyLabel);
    Optional<Party> payor = party(r.getCompanyId(), r.getPayorCode());
    boolean ar = r.getKind() == ReceiptKind.AR;
    List<String> footer = new ArrayList<>();
    if (ar && text.noteLine() != null) {
      footer.add(text.noteLine());
    }
    footer.addAll(
        List.of(nz(text.footer1()), nz(text.footer2()), nz(text.footer3()), nz(text.footer4())));
    return ReceiptFormPdf.render(
        new FormView(
            ar ? "ACKNOWLEDGEMENT RECEIPT" : "OFFICIAL RECEIPT",
            r.getReceiptNo(),
            copyLabel,
            print.reprint(),
            header(company, text),
            ar ? arFields(r, payor) : orFields(r, payor, print),
            ar && r.getCheckNo() != null ? checks(r) : List.of(),
            footer.stream().map(l -> fill(l, tokens)).toList()));
  }

  private static List<String> header(Company company, FormText text) {
    String vat = or(text.companyVat(), company.getTaxId());
    return List.of(
        or(text.companyName(), company.getName()),
        nz(text.companyDescription()),
        or(text.companyAddress(), nz(company.getAddress())),
        vat == null || vat.isBlank() ? NONE : "VAT TIN " + vat);
  }

  private List<String[]> arFields(Receipt r, Optional<Party> payor) {
    Accounts accounts = accounts(r);
    List<String[]> fields = new ArrayList<>();
    fields.add(row("RECEIVED FROM", r.getPayorName()));
    fields.add(row("DATE", r.getReceiptDate().format(LONG_DATE)));
    fields.add(row("TIN", payor.map(Party::getTaxId).orElse(NONE)));
    fields.add(
        row("THE SUM OF", words(r) + " (" + r.getCurrency() + " " + amount(r.getAmount()) + ")"));
    fields.add(row("PAYMENT RECEIVED ON BEHALF OF INSURANCE COMPANY", NONE));
    fields.add(row("INSURER", String.join(", ", accounts.insurers())));
    fields.add(row("REMARKS", nz(r.getRemarks())));
    fields.add(row("REFERENCE NO.", String.join(", ", accounts.references())));
    fields.add(row("POLICY NO.", String.join(", ", accounts.policies())));
    fields.add(row("PAYMENT IN FORM OF", mode(r.getMode())));
    fields.add(
        row(
            "CASH TOTAL",
            r.getMode() == PaymentMode.CASH ? amount(r.getAmount()) : amount(BigDecimal.ZERO)));
    return fields;
  }

  private List<String[]> orFields(Receipt r, Optional<Party> payor, PrintMark print) {
    List<String[]> fields = new ArrayList<>();
    fields.add(row("Received From", r.getPayorName()));
    fields.add(row("Address", payor.map(Party::getAddress).orElse(NONE)));
    fields.add(row("TIN", payor.map(Party::getTaxId).orElse(NONE)));
    fields.add(row("Form of Payment", mode(r.getMode())));
    fields.add(row("Paid Amount (words)", words(r)));
    fields.add(row("Paid Amount", r.getCurrency() + " " + amount(r.getAmount())));
    fields.add(row("In payment of", lovs.label(CashReceiptService.OR_TYPE, r.getReceiptClass())));
    fields.add(row("Bank/Branch", nz(r.getCheckBank())));
    fields.add(row("Check No", nz(r.getCheckNo())));
    fields.add(
        row("Check Date", r.getCheckDate() == null ? NONE : r.getCheckDate().format(LONG_DATE)));
    fields.add(row("AC No", nz(print.certificateNo())));
    fields.add(row("Date Issued", print.printDate().format(LONG_DATE)));
    return fields;
  }

  private static List<String[]> checks(Receipt r) {
    return List.of(
        new String[] {"Check Total", "Check No.", "Check Date", "Bank/Branch"},
        new String[] {
          amount(r.getAmount()),
          r.getCheckNo(),
          r.getCheckDate() == null ? NONE : r.getCheckDate().format(LONG_DATE),
          nz(r.getCheckBank())
        });
  }

  private Accounts accounts(Receipt r) {
    Set<String> insurers = new LinkedHashSet<>();
    Set<String> references = new LinkedHashSet<>();
    Set<String> policies = new LinkedHashSet<>();
    for (Application a : applications.findByReceiptIdOrderByIdAsc(r.getId())) {
      if (!a.isActive()) {
        continue;
      }
      references.add(a.getInvoiceNo());
      invoices
          .find(a.getInvoiceNo())
          .ifPresent(
              i -> {
                insurers.add(insurerName(r.getCompanyId(), i));
                if (i.getPolicyNo() != null) {
                  policies.add(i.getPolicyNo());
                }
              });
    }
    return new Accounts(List.copyOf(insurers), List.copyOf(references), List.copyOf(policies));
  }

  private String insurerName(Long companyId, OpsInvoice invoice) {
    return party(companyId, invoice.getInsurerCode())
        .map(Party::getName)
        .orElse(invoice.getInsurerCode());
  }

  private Optional<Party> party(Long companyId, String code) {
    if (code == null || code.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(parties.getByCode(companyId, code));
    } catch (ResourceNotFoundException ex) {
      return Optional.empty();
    }
  }

  private String seriesRange(Receipt r) {
    if (r.getSeriesId() == null) {
      return NONE;
    }
    return series
        .findById(r.getSeriesId())
        .map(
            s -> {
              int width = String.valueOf(s.getToNo()).length();
              return pad(s.getFromNo(), width) + " - " + pad(s.getToNo(), width);
            })
        .orElse(NONE);
  }

  /**
   * The copy label: Client's Copy, or the company's copy with its short name.
   *
   * @param company company
   * @param copy CLIENT or COMPANY
   * @return label
   */
  static String copyLabel(Company company, String copy) {
    if (!COMPANY_COPY.equals(copy)) {
      return "Client's Copy";
    }
    String name = or(company.getShortName(), company.getName());
    return name + " Copy";
  }

  /**
   * Writes the values of the tokens in a line.
   *
   * @param line line
   * @param tokens values
   * @return line
   */
  static String fill(String line, Map<String, String> tokens) {
    String result = line;
    for (Map.Entry<String, String> t : tokens.entrySet()) {
      result = result.replace(t.getKey(), t.getValue());
    }
    return result;
  }

  private static String words(Receipt r) {
    return ReceiptAmountWords.of(r.getAmount(), r.getCurrency());
  }

  private static String mode(PaymentMode mode) {
    return switch (mode) {
      case CASH -> "Cash";
      case CHECK, PDC -> "Check";
      case BILLS_PAYMENT -> "Bills Payment";
      case DIRECT_CREDIT -> "Direct Credit";
      case NON_CASH -> "Non-cash (settlement)";
      default -> mode.name().replace('_', ' ');
    };
  }

  private static String[] row(String label, String value) {
    return new String[] {label, value == null ? NONE : value};
  }

  private static String pad(long number, int width) {
    String digits = Long.toString(number);
    return "0".repeat(Math.max(0, width - digits.length())) + digits;
  }

  private static String amount(BigDecimal value) {
    return new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US)).format(value);
  }

  private static String or(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private static String nz(String value) {
    return value == null ? NONE : value;
  }

  /**
   * How a receipt is printed.
   *
   * @param copy CLIENT or COMPANY
   * @param reprint whether it was printed before
   * @param certificateNo certificate number, may be null
   * @param printDate print date
   */
  public record PrintMark(
      String copy, boolean reprint, String certificateNo, LocalDate printDate) {}

  private record Accounts(List<String> insurers, List<String> references, List<String> policies) {}
}
