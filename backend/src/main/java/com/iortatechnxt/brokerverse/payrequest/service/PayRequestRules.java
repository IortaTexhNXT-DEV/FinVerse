package com.iortatechnxt.brokerverse.payrequest.service;

import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.crm.domain.PayoutDetails;
import com.iortatechnxt.brokerverse.crm.domain.PayoutMode;
import com.iortatechnxt.brokerverse.crm.service.ClientPayoutAccounts;
import com.iortatechnxt.brokerverse.crm.service.ClientService;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.payrequest.domain.Payee;
import com.iortatechnxt.brokerverse.payrequest.domain.PayeeType;
import com.iortatechnxt.brokerverse.payrequest.domain.RefundLine;
import com.iortatechnxt.brokerverse.payrequest.domain.RefundLineRepository;
import com.iortatechnxt.brokerverse.payrequest.domain.RefundLineValues;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestContent;
import com.iortatechnxt.brokerverse.payrequest.service.RequestDrafts.Payout;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Checks of the request forms (Appendix D, MKT 1.10.0, 2.23.0, 2.25.0): coded fields against their
 * lists of values, amounts, the refund lines (one client, known invoices, no AR refunded twice) and
 * the CA / SA information of a credit to account or a check.
 */
@Component
public class PayRequestRules {

  /** Refund reason list (V890). */
  public static final String REASON_LOV = "REFUND_REASON";

  /** Payment mode list (V894). */
  public static final String MODE_LOV = "PRQ_PAYMENT_MODE";

  /** Reason code of a refund of a cancelled policy (MKT 1.11.0). */
  public static final String CANCELLED_POLICY = "CANCELLED_POLICY";

  private static final int MAX_LINES = 50;
  private static final String DEFAULT_CURRENCY = "PHP";

  private final RefundLineRepository lines;
  private final InvoiceLedgerQueryService ledger;
  private final ClientService clients;
  private final ClientPayoutAccounts payouts;
  private final LovService lovs;
  private final OrganizationService organization;
  private final Clock clock;

  /**
   * Creates the rules.
   *
   * @param lines refund lines (duplicate AR check)
   * @param ledger Operations invoice ledger
   * @param clients clients
   * @param payouts CA / SA checks
   * @param lovs lists of values
   * @param organization branches
   * @param clock clock
   */
  public PayRequestRules(
      RefundLineRepository lines,
      InvoiceLedgerQueryService ledger,
      ClientService clients,
      ClientPayoutAccounts payouts,
      LovService lovs,
      OrganizationService organization,
      Clock clock) {
    this.lines = lines;
    this.ledger = ledger;
    this.clients = clients;
    this.payouts = payouts;
    this.lovs = lovs;
    this.organization = organization;
    this.clock = clock;
  }

  /**
   * Checks the refund lines and builds them (MKT 1.10.0, 2.23.0).
   *
   * @param companyId company
   * @param values lines as entered
   * @param requestId request being changed, or null for a new one
   * @return the lines with their root invoice and cancelled-policy flag
   */
  public List<RefundLine> refundLines(
      Long companyId, List<RefundLineValues> values, Long requestId) {
    if (values.isEmpty() || values.size() > MAX_LINES) {
      throw new BusinessRuleException(
          "PRQ_LINES_REQUIRED", "A refund request has between 1 and " + MAX_LINES + " accounts");
    }
    String client = values.get(0).clientCode();
    requireOneClient(values, client);
    clients.requireByCode(companyId, client);
    requireNoDuplicateAr(values, requestId);
    LocalDate today = LocalDate.now(clock);
    List<RefundLine> built = new ArrayList<>();
    int no = 1;
    for (RefundLineValues v : values) {
      requireLine(v, today);
      Optional<OpsInvoice> invoice = invoiceOf(v);
      boolean cancelled =
          CANCELLED_POLICY.equals(v.reasonCode())
              || invoice.map(OpsInvoice::isCancelled).orElse(false);
      built.add(
          new RefundLine(
              no++, v, invoice.map(OpsInvoice::getRootInvoiceNo).orElse(null), cancelled));
    }
    return built;
  }

  /**
   * The payee and mode of a refund: the client of the lines, paid to the CA / SA or check name.
   *
   * @param payout payment mode and account
   * @param values refund lines
   * @return payee
   */
  public Payee refundPayee(Payout payout, List<RefundLineValues> values) {
    RefundLineValues first = values.get(0);
    String name = blank(payout.accountName()) ? first.assuredName() : payout.accountName().strip();
    return payee(PayeeType.CLIENT, first.clientCode(), name, payout);
  }

  /**
   * Checks the payment mode and CA / SA information and builds the payee (MKT 2.25.0).
   *
   * @param type client or employee
   * @param code payee code
   * @param name payee name
   * @param payout payment mode and account
   * @return payee
   */
  public Payee payee(PayeeType type, String code, String name, Payout payout) {
    if (blank(code) || blank(name)) {
      throw new BusinessRuleException("PRQ_PAYEE_REQUIRED", "Give the payee code and name");
    }
    lovs.requireValid(MODE_LOV, payout.mode(), LocalDate.now(clock));
    PayoutMode mode = payoutMode(payout.mode());
    if (mode != null) {
      payouts.validate(new PayoutDetails(mode, payout.accountName(), payout.accountNo()));
    }
    return new Payee(
        type,
        code.strip(),
        name.strip(),
        payout.mode(),
        mode == PayoutMode.CTA ? payout.accountNo() : null,
        blank(payout.accountName()) ? null : payout.accountName().strip());
  }

  /**
   * Checks the header fields and fills their defaults.
   *
   * @param content as entered
   * @param defaultPurpose purpose when none is given
   * @return content
   */
  public RequestContent content(RequestContent content, String defaultPurpose) {
    String currency = blank(content.currency()) ? DEFAULT_CURRENCY : content.currency().strip();
    if (!currency.matches("[A-Z]{3}")) {
      throw new BusinessRuleException("PRQ_CURRENCY_INVALID", "Currency must be a 3-letter code");
    }
    lovs.validateOptional("PRQ_RFP_TYPE", content.rfpType(), LocalDate.now(clock));
    return new RequestContent(
        trim(content.segment()),
        trim(content.referenceText()),
        trim(content.requestingUnit()),
        trim(content.rfpType()),
        blank(content.purpose()) ? defaultPurpose : content.purpose().strip(),
        currency);
  }

  /**
   * Checks a positive amount with two decimals.
   *
   * @param amount amount
   * @return the amount at scale 2
   */
  public BigDecimal amount(BigDecimal amount) {
    if (amount == null || amount.signum() <= 0 || amount.scale() > 2) {
      throw new BusinessRuleException(
          "PRQ_AMOUNT_INVALID", "The amount must be positive with at most two decimals");
    }
    return amount.setScale(2);
  }

  /**
   * The branch of a request: the branch of the first invoice of a refund, else the head office.
   *
   * @param companyId company
   * @param values refund lines, may be empty
   * @return branch id
   */
  public Long branchOf(Long companyId, List<RefundLineValues> values) {
    return values.stream()
        .map(this::invoiceOf)
        .flatMap(Optional::stream)
        .map(OpsInvoice::getBranchId)
        .findFirst()
        .orElseGet(() -> headOffice(companyId));
  }

  /**
   * The payout mode of CA / SA information, when the payment mode has one.
   *
   * @param mode payment mode
   * @return CTA, CHECK or null
   */
  public static PayoutMode payoutMode(String mode) {
    if (Payee.CTA.equals(mode)) {
      return PayoutMode.CTA;
    }
    return Payee.CHECK.equals(mode) ? PayoutMode.CHECK : null;
  }

  private Long headOffice(Long companyId) {
    List<Branch> branches = organization.listBranches(companyId);
    return branches.stream()
        .filter(Branch::isHeadOffice)
        .findFirst()
        .or(() -> branches.stream().findFirst())
        .map(Branch::getId)
        .orElseThrow(() -> new BusinessRuleException("PRQ_NO_BRANCH", "The company has no branch"));
  }

  private Optional<OpsInvoice> invoiceOf(RefundLineValues v) {
    if (blank(v.invoiceNo())) {
      return Optional.empty();
    }
    return Optional.of(
        ledger
            .find(v.invoiceNo().strip())
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "PRQ_INVOICE_UNKNOWN",
                        "Invoice " + v.invoiceNo() + " is not in the ledger")));
  }

  private void requireLine(RefundLineValues v, LocalDate today) {
    if (blank(v.arNo()) || blank(v.assuredName())) {
      throw new BusinessRuleException(
          "PRQ_LINE_INCOMPLETE", "Every line needs the AR number and the assured name");
    }
    amount(v.amount());
    lovs.requireValid(REASON_LOV, v.reasonCode(), today);
    lovs.validateOptional("RRF_CATEGORY_A", v.categoryA(), today);
    lovs.validateOptional("RRF_CATEGORY_B", v.categoryB(), today);
  }

  private static void requireOneClient(List<RefundLineValues> values, String client) {
    boolean mixed = blank(client) || values.stream().anyMatch(v -> !client.equals(v.clientCode()));
    if (mixed) {
      throw new BusinessRuleException(
          "PRQ_ONE_CLIENT", "All accounts of a refund request belong to one client (AQ18)");
    }
  }

  private void requireNoDuplicateAr(List<RefundLineValues> values, Long requestId) {
    Set<String> ars = new LinkedHashSet<>();
    for (RefundLineValues v : values) {
      if (!blank(v.arNo()) && !ars.add(v.arNo().strip().toUpperCase(Locale.ROOT))) {
        throw new BusinessRuleException(
            "PRQ_DUPLICATE_AR", "AR " + v.arNo() + " appears twice on the request (MKT 2.23.0)");
      }
    }
    List<String> entered = values.stream().map(RefundLineValues::arNo).toList();
    List<Object[]> live = lines.liveRefunds(entered, Objects.requireNonNullElse(requestId, -1L));
    if (!live.isEmpty()) {
      Object[] first = live.get(0);
      throw new BusinessRuleException(
          "PRQ_DUPLICATE_AR",
          "AR " + first[0] + " is already refunded by request " + first[1] + " (MKT 2.23.0)");
    }
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private static String trim(String value) {
    return blank(value) ? null : value.strip();
  }
}
