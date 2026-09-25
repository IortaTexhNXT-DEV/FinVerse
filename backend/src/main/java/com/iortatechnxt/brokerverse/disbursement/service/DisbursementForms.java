package com.iortatechnxt.brokerverse.disbursement.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.disbursement.domain.DisbursementEnums.DisbursementMode;
import com.iortatechnxt.brokerverse.disbursement.domain.Instrument;
import com.iortatechnxt.brokerverse.disbursement.domain.PayeeAccount;
import com.iortatechnxt.brokerverse.disbursement.domain.Voucher;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Field;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Fields;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Section;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Table;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Text;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.payables.domain.BankAccount;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * The printed documents of Disbursement (DIS 2.7.5-2.7.9, 2.16.2-2.16.6, 2.7.12): the disbursement
 * voucher with its proforma entry, the check, the authority to debit, the manager's check / demand
 * draft request, the credit ticket and telegraphic transfer forms, their end-of-day batches and the
 * payment advice text. Wording comes from the versioned templates {@code DSB_*} (V893); the bank
 * layouts and signatories are parked (AQ13, AQ14).
 */
@Component
public class DisbursementForms {

  /** PDF media type. */
  public static final String PDF = "application/pdf";

  /** The debit and credit columns of the entry table, right aligned. */
  private static final List<Integer> AMOUNT_COLUMNS = List.of(3, 4);

  private static final List<String> BANK_SIGNATURES =
      List.of("Authorised signatory", "Authorised signatory");
  private static final List<String> SIGNATURES =
      List.of("Prepared by", "Checked by", "Approved by");
  private static final Map<DisbursementMode, String> TEMPLATES =
      Map.of(
          DisbursementMode.CHECK, "DSB_CHECK",
          DisbursementMode.ATD, "DSB_ATD",
          DisbursementMode.MC_DD, "DSB_MC_DD",
          DisbursementMode.CREDIT_TICKET, "DSB_CREDIT_TICKET",
          DisbursementMode.TT, "DSB_TT");
  private static final Map<DisbursementMode, String> TITLES =
      Map.of(
          DisbursementMode.CHECK, "CHECK",
          DisbursementMode.ATD, "AUTHORITY TO DEBIT",
          DisbursementMode.MC_DD, "MANAGER'S CHECK / DEMAND DRAFT REQUEST",
          DisbursementMode.CREDIT_TICKET, "CREDIT TICKET",
          DisbursementMode.TT, "TELEGRAPHIC TRANSFER");

  private final DocumentComposer composer;
  private final DocTemplateService templates;
  private final OrganizationService organization;
  private final Clock clock;

  /**
   * Creates the forms.
   *
   * @param composer PDF composer
   * @param templates document templates
   * @param organization company names
   * @param clock clock
   */
  public DisbursementForms(
      DocumentComposer composer,
      DocTemplateService templates,
      OrganizationService organization,
      Clock clock) {
    this.composer = composer;
    this.templates = templates;
    this.organization = organization;
    this.clock = clock;
  }

  /**
   * Whether a mode has a printed form (credit to account and online banking have none).
   *
   * @param mode mode
   * @return true for check, ATD, MC / DD, credit ticket and TT
   */
  public static boolean hasForm(DisbursementMode mode) {
    return TEMPLATES.containsKey(mode);
  }

  /**
   * The disbursement voucher (DIS 2.7.5, 2.16.6).
   *
   * @param v voucher
   * @param facts bank and request facts
   * @return PDF
   */
  public byte[] voucher(Voucher v, FormFacts facts) {
    return composer.pdf(
        new DocumentSpec(
            company(v),
            "DISBURSEMENT VOUCHER",
            v.getDvNo(),
            voucherSections(v, facts),
            SIGNATURES,
            "DSB_VOUCHER"));
  }

  /**
   * Several vouchers in one document (EOD, DIS 2.16.6).
   *
   * @param reference batch reference
   * @param items vouchers with their facts
   * @return PDF
   */
  public byte[] vouchers(String reference, List<FormItem> items) {
    List<Section> sections = new ArrayList<>();
    items.forEach(i -> sections.addAll(voucherSections(i.voucher(), i.facts())));
    return composer.pdf(
        new DocumentSpec(
            company(items.get(0).voucher()),
            "DISBURSEMENT VOUCHERS",
            reference,
            sections,
            SIGNATURES,
            "DSB_VOUCHER"));
  }

  private List<Section> voucherSections(Voucher v, FormFacts f) {
    List<Section> out = new ArrayList<>();
    out.add(new Fields("DV " + v.getDvNo(), FormTexts.voucherFields(v, f)));
    out.add(
        new Table(
            v.isProformaEdited() ? "Accounting entry (edited)" : "Accounting entry",
            List.of("Account", "Party", "Cost centre", "Debit", "Credit", "Origin"),
            v.getLines().stream().map(FormTexts::lineRow).toList(),
            AMOUNT_COLUMNS));
    MergedText text = templates.merge("DSB_VOUCHER", today(), values(v, null, f));
    out.add(new Text(text.title(), text.text()));
    return out;
  }

  /**
   * The form of an instrument (check, ATD, MC / DD, credit ticket, TT; DIS 2.7.7-2.7.9, 2.16.2).
   *
   * @param v voucher
   * @param i instrument
   * @param facts bank and payee account
   * @return PDF
   */
  public byte[] instrument(Voucher v, Instrument i, FormFacts facts) {
    return composer.pdf(
        new DocumentSpec(
            company(v),
            title(i.getMode()),
            i.getInstrumentNo() == null ? v.getDvNo() : i.getInstrumentNo(),
            instrumentSections(v, i, facts),
            BANK_SIGNATURES,
            TEMPLATES.get(i.getMode())));
  }

  /**
   * Several instrument forms of one mode in one document (EOD, DIS 2.16.2-2.16.5).
   *
   * @param mode mode
   * @param reference batch reference
   * @param items vouchers, instruments and facts
   * @return PDF
   */
  public byte[] instruments(DisbursementMode mode, String reference, List<FormItem> items) {
    List<Section> sections = new ArrayList<>();
    items.forEach(
        it -> sections.addAll(instrumentSections(it.voucher(), it.instrument(), it.facts())));
    return composer.pdf(
        new DocumentSpec(
            company(items.get(0).voucher()),
            title(mode),
            reference,
            sections,
            BANK_SIGNATURES,
            TEMPLATES.get(mode)));
  }

  private List<Section> instrumentSections(Voucher v, Instrument i, FormFacts f) {
    MergedText text = templates.merge(TEMPLATES.get(i.getMode()), today(), values(v, i, f));
    return List.of(
        new Fields(
            title(i.getMode()) + " " + (i.getInstrumentNo() == null ? "" : i.getInstrumentNo()),
            List.of(
                new Field("DV", v.getDvNo()),
                new Field("Payee", v.getPayeeName()),
                new Field("Amount", v.getCurrency() + " " + FormTexts.amount(v.getNet())),
                new Field("Paying account", FormTexts.bankText(f.bank())),
                new Field("Payee account", FormTexts.payeeAccountText(f.payeeAccount())),
                new Field(
                    "Date",
                    FormTexts.text(i.getPrintedOn() == null ? today() : i.getPrintedOn())))),
        new Text(text.title(), text.text()));
  }

  /**
   * The payment advice e-mailed to the payee after end of day (DIS 2.7.12).
   *
   * @param v voucher
   * @param i instrument, may be null
   * @return merged subject and body
   */
  public MergedText advice(Voucher v, Instrument i) {
    return mail("DSB_PAYMENT_ADVICE", values(v, i, FormFacts.NONE));
  }

  /**
   * The e-mail sending an ATD to the processing branch (DIS 2.7.7).
   *
   * @param v voucher
   * @param i instrument
   * @return merged subject and body
   */
  public MergedText atdEmail(Voucher v, Instrument i) {
    return mail("DSB_ATD_EMAIL", values(v, i, FormFacts.NONE));
  }

  /** An e-mail template with its subject (the template title) merged too. */
  private MergedText mail(String code, Map<String, Object> values) {
    MergedText m = templates.merge(code, today(), values);
    return new MergedText(
        m.code(), m.versionNo(), DocTemplateService.fill(m.title(), values), m.text());
  }

  private String title(DisbursementMode mode) {
    String title = TITLES.get(mode);
    if (title == null) {
      throw new BusinessRuleException("DISB_NO_FORM", "Mode " + mode + " has no printed form");
    }
    return title;
  }

  private Map<String, Object> values(Voucher v, Instrument i, FormFacts f) {
    Map<String, Object> m = new HashMap<>();
    m.put("dvNo", v.getDvNo());
    m.put("requestNo", f.requestNo());
    m.put("payeeName", v.getPayeeName());
    m.put("currency", v.getCurrency());
    m.put("netAmount", FormTexts.amount(v.getNet()));
    m.put("purpose", v.getPurpose());
    m.put("mode", v.getMode() == null ? FormTexts.DASH : v.getMode().name().replace('_', ' '));
    m.put("valueDate", FormTexts.text(v.getValueDate()));
    m.putAll(instrumentValues(i));
    m.putAll(FormTexts.accountValues(f));
    return m;
  }

  private Map<String, Object> instrumentValues(Instrument i) {
    String no = i == null ? null : i.getInstrumentNo();
    LocalDate printed = i == null ? null : i.getPrintedOn();
    return Map.of(
        "instrumentNo", FormTexts.orEmpty(no),
        "instrumentText", no == null ? "" : " (" + no + ")",
        "printDate", FormTexts.text(printed == null ? today() : printed));
  }

  private String company(Voucher v) {
    return organization.getCompany(v.getCompanyId()).getName();
  }

  private LocalDate today() {
    return LocalDate.now(clock);
  }

  /**
   * Facts printed on the forms besides the voucher.
   *
   * @param requestNo payment request number
   * @param bank paying bank account, may be null
   * @param payeeAccount payee bank account, may be null
   */
  public record FormFacts(String requestNo, BankAccount bank, PayeeAccount payeeAccount) {

    /** No facts (e-mail texts). */
    public static final FormFacts NONE = new FormFacts(null, null, null);
  }

  /**
   * One voucher of a batch document.
   *
   * @param voucher voucher
   * @param instrument instrument, may be null for the DV batch
   * @param facts facts
   */
  public record FormItem(Voucher voucher, Instrument instrument, FormFacts facts) {}
}
