package com.iortatechnxt.brokerverse.submitted.fee.service;

import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.booking.domain.ServiceInvoice;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceService;
import com.iortatechnxt.brokerverse.booking.service.ServiceInvoiceService.IssueRequest;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.common.time.BusinessClock;
import com.iortatechnxt.brokerverse.common.util.DisplayFormat;
import com.iortatechnxt.brokerverse.docgen.service.DocTemplateService;
import com.iortatechnxt.brokerverse.docgen.service.DocumentComposer;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Table;
import com.iortatechnxt.brokerverse.docgen.service.DocumentSpec.Text;
import com.iortatechnxt.brokerverse.docgen.service.MergedText;
import com.iortatechnxt.brokerverse.docgen.service.SheetSpec;
import com.iortatechnxt.brokerverse.organization.domain.BranchRepository;
import com.iortatechnxt.brokerverse.organization.service.OrganizationDirectory;
import com.iortatechnxt.brokerverse.storage.domain.FileOrigin;
import com.iortatechnxt.brokerverse.storage.domain.FileOwner;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService;
import com.iortatechnxt.brokerverse.storage.service.StoredFileService.StoreRequest;
import com.iortatechnxt.brokerverse.submitted.domain.SbmNoTouchBatch;
import com.iortatechnxt.brokerverse.submitted.domain.SbmNoTouchBatchRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmNoTouchLine;
import com.iortatechnxt.brokerverse.submitted.domain.SbmNoTouchLineRepository;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicy;
import com.iortatechnxt.brokerverse.submitted.domain.SbmPolicyRepository;
import com.iortatechnxt.brokerverse.submitted.service.adapter.SubmittedFileStorage;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * No Touch billing (Report List #164; OQ39; design section 3.7): the No Touch accounts of an
 * insurer expiring in a month are exported with the premium and fee columns blank; the insurer's
 * return (upload {@code SBM_NO_TOUCH_RETURN}) fills basic premium, gross service fee, VAT and
 * withholding tax and produces the billing statement; billing issues a service invoice of type
 * SERVICE_FEE_NO_TOUCH to the insurer and posts {@code SBM_NO_TOUCH_FEE}.
 */
@Service
@Transactional
public class NoTouchService {

  private static final List<Integer> AMOUNT_COLUMNS = List.of(3, 4, 5);

  /** Service invoice type. */
  public static final String SI_TYPE = "SERVICE_FEE_NO_TOUCH";

  private static final String XLSX =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private final SbmNoTouchBatchRepository batches;
  private final SbmNoTouchLineRepository lines;
  private final SbmPolicyRepository policies;
  private final DocumentComposer composer;
  private final DocTemplateService templates;
  private final StoredFileService files;
  private final ServiceInvoiceService serviceInvoices;
  private final AccountingEventPublisher accounting;
  private final OrganizationDirectory organizations;
  private final BranchRepository branches;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param batches No Touch batches
   * @param lines their lines
   * @param policies masterlist
   * @param composer document composer
   * @param templates templates
   * @param files file store
   * @param serviceInvoices service invoices (booking)
   * @param accounting accounting events
   * @param organizations company names
   * @param branches branches (head office)
   * @param numbers document numbers
   * @param audit audit trail
   * @param clock clock
   */
  @SuppressWarnings("java:S107") // collaborators of the billing
  public NoTouchService(
      SbmNoTouchBatchRepository batches,
      SbmNoTouchLineRepository lines,
      SbmPolicyRepository policies,
      DocumentComposer composer,
      DocTemplateService templates,
      StoredFileService files,
      ServiceInvoiceService serviceInvoices,
      AccountingEventPublisher accounting,
      OrganizationDirectory organizations,
      BranchRepository branches,
      DocumentNumberService numbers,
      AuditTrailService audit,
      Clock clock) {
    this.batches = batches;
    this.lines = lines;
    this.policies = policies;
    this.composer = composer;
    this.templates = templates;
    this.files = files;
    this.serviceInvoices = serviceInvoices;
    this.accounting = accounting;
    this.organizations = organizations;
    this.branches = branches;
    this.numbers = numbers;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Exports the No Touch accounts of an insurer and month (a new export replaces an export not yet
   * returned).
   *
   * @param companyId company
   * @param insurerCode insurer
   * @param period month
   * @return the batch
   */
  public SbmNoTouchBatch export(Long companyId, String insurerCode, YearMonth period) {
    SbmNoTouchBatch batch =
        batches
            .findByCompanyIdAndInsurerCodeAndPeriod(companyId, insurerCode, period.toString())
            .orElseGet(
                () ->
                    batches.save(
                        new SbmNoTouchBatch(
                            companyId,
                            numbers.next("SBN-" + period.getYear()),
                            insurerCode,
                            period.toString())));
    if (!SbmNoTouchBatch.EXPORTED.equals(batch.getStatus())) {
      throw new BusinessRuleException(
          "SBM_NO_TOUCH_RETURNED", batch.getBatchNo() + " was already returned by the insurer");
    }
    lines.deleteAll(lines.findByBatchIdOrderByIdAsc(batch.getId()));
    lines.flush();
    List<SbmPolicy> accounts = policies.findAll(noTouch(companyId, insurerCode, period));
    List<SbmNoTouchLine> saved =
        accounts.stream().map(p -> lines.save(new SbmNoTouchLine(batch.getId(), p))).toList();
    byte[] xlsx =
        composer.xlsx(
            new SheetSpec(
                "No Touch " + period,
                List.of(
                    "Masterlist No",
                    "PN No",
                    "Assured",
                    "Policy No",
                    "Plate No",
                    "Sum Insured",
                    "Basic Premium",
                    "Gross Service Fee",
                    "VAT",
                    "Withholding Tax"),
                saved.stream()
                    .map(
                        l ->
                            List.<Object>of(
                                l.getSbmNo(),
                                nz(l.getPnNo()),
                                l.getAssuredName(),
                                nz(l.getPolicyNo()),
                                nz(l.getPlateNo()),
                                l.getSumInsured() == null ? BigDecimal.ZERO : l.getSumInsured(),
                                "",
                                "",
                                "",
                                ""))
                    .toList()));
    batch.exported(saved.size(), store(batch, batch.getBatchNo() + ".xlsx", xlsx, XLSX));
    audit.record(
        "SubmittedNoTouch", batch.getBatchNo(), AuditAction.EXPORT, saved.size() + " accounts");
    return batch;
  }

  private static Specification<SbmPolicy> noTouch(
      Long companyId, String insurer, YearMonth period) {
    return (root, q, cb) ->
        cb.and(
            cb.equal(root.get("companyId"), companyId),
            cb.equal(root.get("terms").get("insurerCode"), insurer),
            cb.equal(root.get("renewalMonth"), period.toString()),
            cb.or(
                cb.isTrue(root.get("marks").get("noTouch")),
                cb.equal(root.get("bucket"), "NO_TOUCH")));
  }

  /**
   * Records the values the insurer returned for a line.
   *
   * @param batchNo batch
   * @param sbmNo masterlist number
   * @param values basic premium, gross fee, VAT and withholding tax
   * @return the line
   */
  public SbmNoTouchLine returnLine(String batchNo, String sbmNo, SbmNoTouchLine.Values values) {
    SbmNoTouchBatch batch = batch(batchNo);
    if (SbmNoTouchBatch.BILLED.equals(batch.getStatus())) {
      throw new BusinessRuleException("SBM_NO_TOUCH_BILLED", batchNo + " is already billed");
    }
    SbmNoTouchLine line =
        lines
            .findByBatchIdAndSbmNo(batch.getId(), sbmNo)
            .orElseThrow(
                () ->
                    new BusinessRuleException(
                        "SBM_NO_TOUCH_LINE_UNKNOWN", sbmNo + " is not in batch " + batchNo));
    line.returned(values);
    return line;
  }

  /**
   * Closes the return of a batch: totals and billing statement.
   *
   * @param batchNo batch
   * @return the batch
   */
  public SbmNoTouchBatch closeReturn(String batchNo) {
    SbmNoTouchBatch batch = batch(batchNo);
    List<SbmNoTouchLine> all = lines.findByBatchIdOrderByIdAsc(batch.getId());
    SbmNoTouchBatch.Totals totals =
        new SbmNoTouchBatch.Totals(
            sum(all, SbmNoTouchLine::getGrossFee),
            sum(all, SbmNoTouchLine::getVat),
            sum(all, SbmNoTouchLine::getWtax));
    batch.returned(
        totals,
        store(batch, batchNo + "-statement.pdf", statement(batch, all, totals), "application/pdf"),
        clock.instant());
    audit.record("SubmittedNoTouch", batchNo, AuditAction.UPDATE, "Returned by the insurer");
    return batch;
  }

  private byte[] statement(SbmNoTouchBatch b, List<SbmNoTouchLine> all, SbmNoTouchBatch.Totals t) {
    MergedText text =
        templates.merge(
            "SBM_NO_TOUCH_BILLING",
            BusinessClock.today(clock),
            Map.of(
                "insurerName", b.getInsurerCode(),
                "period", b.getPeriod(),
                "count", all.size(),
                "currency", "PHP",
                "grossFee", DisplayFormat.amount(t.grossFee()),
                "vat", DisplayFormat.amount(t.vat()),
                "wtax", DisplayFormat.amount(t.wtax()),
                "netDue", DisplayFormat.amount(t.grossFee().add(t.vat()).subtract(t.wtax()))));
    return composer.pdf(
        new DocumentSpec(
            organizations.company(b.getCompanyId()).name(),
            text.title(),
            b.getBatchNo(),
            List.of(
                new Text(null, text.text()),
                new Table(
                    "Accounts",
                    List.of(
                        "Masterlist No",
                        "Assured",
                        "Policy No",
                        "Gross Service Fee",
                        "VAT",
                        "Withholding Tax"),
                    all.stream()
                        .map(
                            l ->
                                List.of(
                                    l.getSbmNo(),
                                    l.getAssuredName(),
                                    nz(l.getPolicyNo()),
                                    DisplayFormat.amount(l.getGrossFee()),
                                    DisplayFormat.amount(l.getVat()),
                                    DisplayFormat.amount(l.getWtax())))
                        .toList(),
                    AMOUNT_COLUMNS)),
            List.of(),
            text.versionTag()));
  }

  /**
   * Bills a returned batch: service invoice to the insurer and the service-fee posting.
   *
   * @param id batch
   * @return the batch
   */
  public SbmNoTouchBatch bill(Long id) {
    SbmNoTouchBatch batch =
        batches.findById(id).orElseThrow(() -> new ResourceNotFoundException("No Touch batch", id));
    if (!SbmNoTouchBatch.RETURNED.equals(batch.getStatus())) {
      throw new BusinessRuleException(
          "SBM_NO_TOUCH_NOT_RETURNED",
          "Upload the insurer's return of " + batch.getBatchNo() + " first");
    }
    ServiceInvoice si =
        serviceInvoices.issue(
            new IssueRequest(
                batch.getCompanyId(),
                SI_TYPE,
                null,
                null,
                batch.getInsurerCode(),
                null,
                BusinessClock.today(clock),
                "PHP",
                batch.getGrossFee(),
                batch.getVat(),
                batch.getWtax(),
                "No Touch service fee " + batch.getPeriod() + " (" + batch.getBatchNo() + ")"));
    String journal =
        accounting
            .publish(
                new BusinessEvent(
                    "SBM_NO_TOUCH_FEE",
                    batch.getCompanyId(),
                    headOffice(batch.getCompanyId()),
                    BusinessClock.today(clock),
                    "PHP",
                    "SUBMITTED",
                    "NT:" + batch.getBatchNo() + ":" + batch.getInsurerCode(),
                    si.getSiNo(),
                    batch.getInsurerCode(),
                    null,
                    null,
                    "No Touch service fee " + batch.getPeriod(),
                    Map.of(
                        "RECEIVABLE", batch.getGrossFee().add(batch.getVat()),
                        "INCOME", batch.getGrossFee(),
                        "OUTPUT_VAT", batch.getVat()),
                    null,
                    null))
            .getBatchNo();
    batch.billed(si.getSiNo(), journal, clock.instant());
    audit.record(
        "SubmittedNoTouch", batch.getBatchNo(), AuditAction.POST, "Billed " + si.getSiNo());
    return batch;
  }

  private Long headOffice(Long companyId) {
    return branches.findByCompanyIdOrderByCode(companyId).stream()
        .filter(b -> b.isHeadOffice())
        .findFirst()
        .map(b -> b.getId())
        .orElse(null);
  }

  /**
   * A batch by number.
   *
   * @param batchNo number
   * @return batch
   */
  @Transactional(readOnly = true)
  public SbmNoTouchBatch batch(String batchNo) {
    return batches
        .findByBatchNo(batchNo)
        .orElseThrow(() -> new ResourceNotFoundException("No Touch batch", batchNo));
  }

  /**
   * A batch.
   *
   * @param id batch
   * @return batch
   */
  @Transactional(readOnly = true)
  public SbmNoTouchBatch get(Long id) {
    return batches
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("No Touch batch", id));
  }

  /**
   * Batches of a company.
   *
   * @param companyId company
   * @return batches, newest first
   */
  @Transactional(readOnly = true)
  public List<SbmNoTouchBatch> list(Long companyId) {
    return batches.findByCompanyIdOrderByIdDesc(companyId);
  }

  /**
   * Lines of a batch.
   *
   * @param id batch
   * @return lines
   */
  @Transactional(readOnly = true)
  public List<SbmNoTouchLine> lines(Long id) {
    return lines.findByBatchIdOrderByIdAsc(id);
  }

  private Long store(SbmNoTouchBatch b, String name, byte[] content, String type) {
    return files
        .storeChecked(
            new StoreRequest(
                new FileOwner(
                    b.getCompanyId(), SubmittedFileStorage.NO_TOUCH, b.getId().toString()),
                null,
                "WORKING_FILE",
                name,
                content,
                null),
            type,
            FileOrigin.GENERATED)
        .getId();
  }

  private static BigDecimal sum(List<SbmNoTouchLine> all, Function<SbmNoTouchLine, BigDecimal> f) {
    return all.stream().map(f).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static String nz(String value) {
    return value == null ? "" : value;
  }
}
