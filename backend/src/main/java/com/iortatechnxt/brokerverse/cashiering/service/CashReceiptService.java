package com.iortatechnxt.brokerverse.cashiering.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.cashiering.domain.Application;
import com.iortatechnxt.brokerverse.cashiering.domain.ApplicationRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.CashCodes.ReceiptKind;
import com.iortatechnxt.brokerverse.cashiering.domain.CashReceiptRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.OrAmounts;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt.ReceiptHeader;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt.ReceiptMoney;
import com.iortatechnxt.brokerverse.cashiering.domain.Receipt.ReceiptTender;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptAction;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptActionRepository;
import com.iortatechnxt.brokerverse.cashiering.domain.ReceiptLine;
import com.iortatechnxt.brokerverse.cashiering.service.CashieringPosting.PostingContext;
import com.iortatechnxt.brokerverse.cashiering.service.ReceiptSeriesService.Allocation;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.util.Money;
import com.iortatechnxt.brokerverse.lov.domain.LovValue;
import com.iortatechnxt.brokerverse.lov.service.LovService;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues acknowledgement receipts and Head Office official receipts (CSHID.001/002/006/014/021):
 * number from the branch series, receipt in the currency received at the BOOK rate, and the
 * accounting event of the receipt class.
 *
 * <ul>
 *   <li>Premium AR: {@code OPS_AR_RECEIPT} (cash in to unapplied collections of the client).
 *   <li>Non-premium AR (refund, other expenses, AR Insurance): {@code OPS_AR_INSURANCE_RECEIPT} (AR
 *       Insurer of the paying insurer), segregated from premium (CSHID.021).
 *   <li>OR: {@code OPS_OR_ISSUE} with the income of the OR type, output VAT and creditable
 *       withholding tax; a settlement OR of another Operations module (no cash) only makes the
 *       deferred VAT of a commission due (OPERATIONS_DESIGN section 5, rows 14-15).
 * </ul>
 */
@Service
@Transactional
public class CashReceiptService {

  /** Audit entity. */
  public static final String ENTITY = "Receipt";

  /** LOV of AR classes. */
  public static final String AR_CLASS = "AR_CLASS";

  /** LOV of OR types. */
  public static final String OR_TYPE = "OR_TYPE";

  private static final String NON_PREMIUM = "NON_PREMIUM";
  private static final String COMMISSION = "COMMISSION";

  private final CashReceiptRepository receipts;
  private final ApplicationRepository applications;
  private final ReceiptActionRepository actions;
  private final ReceiptSeriesService series;
  private final CashieringSettings settings;
  private final CashieringPosting posting;
  private final LovService lovs;
  private final AuditTrailService audit;

  /**
   * Creates the service.
   *
   * @param receipts receipts
   * @param applications applications
   * @param actions receipt actions
   * @param series receipt series
   * @param settings settings
   * @param posting accounting events
   * @param lovs lists of values
   * @param audit audit trail
   */
  public CashReceiptService(
      CashReceiptRepository receipts,
      ApplicationRepository applications,
      ReceiptActionRepository actions,
      ReceiptSeriesService series,
      CashieringSettings settings,
      CashieringPosting posting,
      LovService lovs,
      AuditTrailService audit) {
    this.receipts = receipts;
    this.applications = applications;
    this.actions = actions;
    this.series = series;
    this.settings = settings;
    this.posting = posting;
    this.lovs = lovs;
    this.audit = audit;
  }

  /**
   * One receipt.
   *
   * @param id id
   * @return receipt with its lines
   */
  @Transactional(readOnly = true)
  public Receipt get(Long id) {
    Receipt r = receipts.findById(id).orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
    Hibernate.initialize(r.getLines());
    return r;
  }

  /**
   * A receipt with its applications and actions, loaded for display.
   *
   * @param id receipt
   * @return detail
   */
  @Transactional(readOnly = true)
  public ReceiptDetail detail(Long id) {
    Receipt r = get(id);
    List<Application> apps = applications.findByReceiptIdOrderByIdAsc(id);
    apps.forEach(a -> Hibernate.initialize(a.getLines()));
    return new ReceiptDetail(r, apps, actions.findByReceiptIdOrderByIdAsc(id));
  }

  /**
   * Whether an AR class is a non-premium (insurance) payment (CSHID.021).
   *
   * @param arClass AR class
   * @param date date of validity
   * @return true for refund, other expenses and AR Insurance
   */
  @Transactional(readOnly = true)
  public boolean nonPremium(String arClass, LocalDate date) {
    LovValue value = lovs.requireValid(AR_CLASS, arClass, date);
    return NON_PREMIUM.equals(value.getCode()) || NON_PREMIUM.equals(value.getParentCode());
  }

  /**
   * Issues an acknowledgement receipt and posts it.
   *
   * @param issue branch, class, payor, amount and tender
   * @return the AR
   */
  public Receipt issueAr(ArIssue issue) {
    Optional<Receipt> earlier = earlier(issue.companyId(), issue.tender());
    if (earlier.isPresent()) {
      return earlier.get();
    }
    boolean nonPremium = nonPremium(issue.arClass(), issue.receiptDate());
    if (nonPremium && (issue.payorCode() == null || issue.payorCode().isBlank())) {
      throw new BusinessRuleException(
          "AR_INSURANCE_PAYOR_REQUIRED", "A non-premium AR needs the paying insurer's code");
    }
    requirePositive(issue.amount());
    Branch branch = settings.branch(issue.companyId(), issue.branchId());
    Allocation number = series.allocate(issue.companyId(), branch.getId(), ReceiptKind.AR);
    BigDecimal rate = settings.bookRate(issue.companyId(), issue.currency(), issue.receiptDate());
    Receipt receipt =
        receipts.save(
            new Receipt(
                new ReceiptHeader(
                    issue.companyId(),
                    branch.getId(),
                    number.number(),
                    ReceiptKind.AR,
                    issue.arClass(),
                    number.seriesId(),
                    issue.receiptDate(),
                    issue.payorCode(),
                    issue.payorName(),
                    issue.assuredName(),
                    issue.salesUnit()),
                new ReceiptMoney(
                    issue.currency(),
                    rate,
                    issue.amount(),
                    Money.convert(issue.amount(), rate),
                    zeroTaxes()),
                issue.tender()));
    receipt.posted(
        posting.publish(
            context(receipt, issue.tender(), "AR " + receipt.getReceiptNo()),
            nonPremium ? CashieringPosting.AR_INSURANCE_RECEIPT : CashieringPosting.AR_RECEIPT,
            "AR:" + receipt.getReceiptNo(),
            Map.of(CashieringPosting.AMOUNT, receipt.getAmount())));
    audit.record(
        ENTITY,
        receipt.getReceiptNo(),
        AuditAction.CREATE,
        "AR "
            + receipt.getReceiptClass()
            + " "
            + receipt.getCurrency()
            + " "
            + receipt.getAmount());
    return receipt;
  }

  /**
   * Issues a Head Office official receipt and posts it (CSHID.002/006/007).
   *
   * @param issue OR type, payee, lines and tender
   * @return the OR
   */
  public Receipt issueOr(OrIssue issue) {
    Optional<Receipt> earlier = earlier(issue.companyId(), issue.tender());
    if (earlier.isPresent()) {
      return earlier.get();
    }
    lovs.requireValid(OR_TYPE, issue.orType(), issue.receiptDate());
    if (issue.lines().isEmpty()) {
      throw new BusinessRuleException("OR_WITHOUT_LINES", "An official receipt needs a line");
    }
    Branch office = settings.headOffice(issue.companyId());
    if (issue.branchId() != null && !issue.branchId().equals(office.getId())) {
      throw new BusinessRuleException(
          "OR_HEAD_OFFICE_ONLY", "Official receipts are issued by Head Office only (CSHID.006)");
    }
    OrAmounts totals =
        issue.lines().stream()
            .map(l -> new OrAmounts(l.getGross(), l.getVat(), l.getWtax()))
            .reduce(zeroTaxes(), OrAmounts::plus);
    requirePositive(totals.net());
    Allocation number = series.allocate(issue.companyId(), office.getId(), ReceiptKind.OR);
    BigDecimal rate = settings.bookRate(issue.companyId(), issue.currency(), issue.receiptDate());
    Receipt receipt =
        receipts.save(
            new Receipt(
                new ReceiptHeader(
                    issue.companyId(),
                    office.getId(),
                    number.number(),
                    ReceiptKind.OR,
                    issue.orType(),
                    number.seriesId(),
                    issue.receiptDate(),
                    issue.payorCode(),
                    issue.payorName(),
                    null,
                    null),
                new ReceiptMoney(
                    issue.currency(),
                    rate,
                    totals.net(),
                    Money.convert(totals.net(), rate),
                    totals),
                issue.tender()));
    issue.lines().forEach(receipt::addLine);
    receipt.posted(
        posting.publish(
            context(receipt, issue.tender(), "OR " + receipt.getReceiptNo()),
            CashieringPosting.OR_ISSUE,
            "OR:" + receipt.getReceiptNo(),
            orAmounts(receipt, issue.settlement(), BigDecimal.ONE)));
    audit.record(
        ENTITY,
        receipt.getReceiptNo(),
        AuditAction.CREATE,
        "OR " + receipt.getReceiptClass() + " " + receipt.getCurrency() + " " + totals.net());
    return receipt;
  }

  /**
   * The amounts of {@code OPS_OR_ISSUE} for an OR, scaled (cancellation -1, partial reinstatement).
   *
   * @param receipt OR
   * @param settlement whether the OR is a no-cash settlement OR
   * @param factor scale
   * @return event amounts
   */
  public static Map<String, BigDecimal> orAmounts(
      Receipt receipt, boolean settlement, BigDecimal factor) {
    BigDecimal gross = Money.round(receipt.getGross().multiply(factor));
    BigDecimal vat = Money.round(receipt.getVat().multiply(factor));
    BigDecimal wtax = Money.round(receipt.getWtax().multiply(factor));
    boolean commission = COMMISSION.equals(receipt.getReceiptClass());
    if (settlement) {
      return commission ? Map.of("VAT_DUE", vat) : Map.of();
    }
    BigDecimal cash = gross.add(vat).subtract(wtax);
    if (commission) {
      return Map.of(
          "CASH", cash, "CWT", wtax, "COMMISSION_COLLECTED", gross.add(vat), "VAT_DUE", vat);
    }
    return Map.of(
        "CASH",
        cash,
        "CWT",
        wtax,
        incomeComponent(receipt.getReceiptClass()),
        gross,
        "OUTPUT_VAT",
        vat);
  }

  /**
   * An OR line.
   *
   * @param invoiceNo invoice, may be null
   * @param insurerCode insurer, may be null
   * @param amounts gross, VAT and withholding tax
   * @param description description
   * @return line
   */
  public static ReceiptLine line(
      String invoiceNo, String insurerCode, OrAmounts amounts, String description) {
    return new ReceiptLine(invoiceNo, insurerCode, amounts, description);
  }

  private static String incomeComponent(String orType) {
    return switch (orType) {
      case "SERVICE_FEE" -> "SERVICE_FEE_INCOME";
      case "PROFIT_SHARE" -> "PROFIT_SHARE_INCOME";
      case "INCENTIVE" -> "INCENTIVE_INCOME";
      default -> "OTHER_INCOME";
    };
  }

  /**
   * The posting facts of a receipt.
   *
   * @param receipt receipt
   * @param tender tender (mode for the @BANK role)
   * @param narration narration
   * @return context with the payor as party
   */
  PostingContext context(Receipt receipt, ReceiptTender tender, String narration) {
    return new PostingContext(
        receipt.getCompanyId(),
        receipt.getBranchId(),
        receipt.getReceiptDate(),
        receipt.getCurrency(),
        receipt.getReceiptNo(),
        receipt.getPayorCode(),
        null,
        null,
        narration + " " + receipt.getPayorName(),
        settings.collectionAccount(tender.mode()));
  }

  private Optional<Receipt> earlier(Long companyId, ReceiptTender tender) {
    if (tender.sourceModule() == null || tender.sourceRef() == null) {
      return Optional.empty();
    }
    return receipts.findByCompanyIdAndSourceModuleAndSourceRef(
        companyId, tender.sourceModule(), tender.sourceRef());
  }

  private static void requirePositive(BigDecimal amount) {
    if (amount == null || amount.signum() <= 0) {
      throw new BusinessRuleException("RECEIPT_AMOUNT", "A receipt needs an amount above zero");
    }
  }

  private static OrAmounts zeroTaxes() {
    BigDecimal zero = BigDecimal.ZERO.setScale(2);
    return new OrAmounts(zero, zero, zero);
  }

  /**
   * A receipt with its applications and actions.
   *
   * @param receipt receipt (lines loaded)
   * @param applications applications (lines loaded)
   * @param actions cancellations and reinstatements
   */
  public record ReceiptDetail(
      Receipt receipt, List<Application> applications, List<ReceiptAction> actions) {

    /** Defensive copies. */
    public ReceiptDetail {
      applications = List.copyOf(applications);
      actions = List.copyOf(actions);
    }
  }

  /**
   * An AR to issue.
   *
   * @param companyId company
   * @param branchId issuing branch
   * @param arClass AR class (LOV AR_CLASS)
   * @param receiptDate receipt date
   * @param payorCode payor code (client or insurer), may be null for premium
   * @param payorName payor name
   * @param assuredName assured, may be null
   * @param salesUnit marketing unit, may be null
   * @param currency currency received
   * @param amount amount received
   * @param tender mode, check and source
   */
  public record ArIssue(
      Long companyId,
      Long branchId,
      String arClass,
      LocalDate receiptDate,
      String payorCode,
      String payorName,
      String assuredName,
      String salesUnit,
      String currency,
      BigDecimal amount,
      ReceiptTender tender) {}

  /**
   * An OR to issue.
   *
   * @param companyId company
   * @param branchId requesting branch, null for Head Office
   * @param orType OR type (LOV OR_TYPE)
   * @param receiptDate receipt date
   * @param payorCode payee party code (insurer, client), may be null
   * @param payorName payee name
   * @param currency currency
   * @param lines lines (CSHID.007)
   * @param tender mode, certificate and source
   * @param settlement no-cash settlement OR of another Operations module
   */
  public record OrIssue(
      Long companyId,
      Long branchId,
      String orType,
      LocalDate receiptDate,
      String payorCode,
      String payorName,
      String currency,
      List<ReceiptLine> lines,
      ReceiptTender tender,
      boolean settlement) {

    /** Defensive copy. */
    public OrIssue {
      lines = List.copyOf(lines);
    }
  }
}
