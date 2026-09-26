package com.iortatechnxt.brokerverse.tax.service;

import com.iortatechnxt.brokerverse.accounting.service.AccountingEventPublisher;
import com.iortatechnxt.brokerverse.accounting.service.BusinessEvent;
import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.DuplicateResourceException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.organization.domain.Branch;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.tax.domain.ReceivedCertificate;
import com.iortatechnxt.brokerverse.tax.domain.ReceivedCertificate.Facts;
import com.iortatechnxt.brokerverse.tax.domain.ReceivedCertificateLine;
import com.iortatechnxt.brokerverse.tax.domain.ReceivedCertificateLine.Kind;
import com.iortatechnxt.brokerverse.tax.domain.ReceivedCertificateRepository;
import com.iortatechnxt.brokerverse.tax.domain.TaxPeriod;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The register of BIR 2307 certificates received from withholding agents (DIS 2.11.0-2.11.2, OQ41,
 * AQ16; design 2.1 and row 14). Disbursement's CWT tag, Commission's certificate submission and the
 * tax screen record certificates here; each posts {@code TAX_CWT_CERT_RECEIVED}: AR-BIR on
 * commission ({@code COMMISSION_CWT}) and on incentives ({@code INCENTIVE_CWT}) moved to AR-BIR on
 * hand. A cancellation posts the same event with negative amounts. The SAWT and the income tax
 * worksheets read the register.
 *
 * <p>Contract for other modules: {@link #record(Long, Facts, List)} and {@link #bySource(String,
 * String)}.
 */
@Service
@Transactional
public class ReceivedCertificateService {

  /** Accounting event (V890; seed rules V999). */
  public static final String EVENT = "TAX_CWT_CERT_RECEIVED";

  /** Source module of the postings. */
  public static final String MODULE = "TAX";

  private static final String ENTITY = "ReceivedCertificate";

  private final ReceivedCertificateRepository certificates;
  private final AccountingEventPublisher publisher;
  private final OrganizationService organization;
  private final AuditTrailService audit;
  private final Clock clock;

  /**
   * Creates the service.
   *
   * @param certificates register
   * @param publisher accounting engine
   * @param organization company branches
   * @param audit audit trail
   * @param clock clock
   */
  public ReceivedCertificateService(
      ReceivedCertificateRepository certificates,
      AccountingEventPublisher publisher,
      OrganizationService organization,
      AuditTrailService audit,
      Clock clock) {
    this.certificates = certificates;
    this.publisher = publisher;
    this.organization = organization;
    this.audit = audit;
    this.clock = clock;
  }

  /**
   * Searches the register.
   *
   * @param companyId company
   * @param status RECORDED or CANCELLED, null for all
   * @param q certificate or agent contains, blank for all
   * @param pageable page
   * @return certificates
   */
  @Transactional(readOnly = true)
  public Page<ReceivedCertificate> search(
      Long companyId, String status, String q, Pageable pageable) {
    String pattern =
        q == null || q.isBlank() ? null : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
    return certificates.search(companyId, status, pattern, pageable);
  }

  /**
   * One certificate.
   *
   * @param id id
   * @return certificate
   */
  @Transactional(readOnly = true)
  public ReceivedCertificate get(Long id) {
    return certificates
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Received certificate", id));
  }

  /**
   * The certificates recorded from a source record (a DV, a commission submission).
   *
   * @param sourceModule module
   * @param sourceRef reference
   * @return certificates
   */
  @Transactional(readOnly = true)
  public List<ReceivedCertificate> bySource(String sourceModule, String sourceRef) {
    return certificates.findBySourceModuleAndSourceRef(sourceModule, sourceRef);
  }

  /**
   * The live certificates whose period ends in a tax period (SAWT, income tax credits).
   *
   * @param companyId company
   * @param period tax period
   * @return certificates by agent
   */
  @Transactional(readOnly = true)
  public List<ReceivedCertificate> inPeriod(Long companyId, TaxPeriod period) {
    return certificates.findByCompanyIdAndPeriodToBetweenAndStatusOrderByAgentCodeAsc(
        companyId, period.from(), period.to(), ReceivedCertificate.RECORDED);
  }

  /**
   * Records a certificate received and posts it (DIS 2.11.1-2.11.2).
   *
   * @param companyId company
   * @param facts certificate facts
   * @param lines income payments by kind and ATC
   * @return the certificate
   */
  public ReceivedCertificate record(
      Long companyId, Facts facts, List<ReceivedCertificateLine> lines) {
    validate(facts, lines);
    if (certificates.existsByCompanyIdAndAgentCodeAndCertificateNoAndStatusNot(
        companyId, facts.agentCode(), facts.certificateNo(), ReceivedCertificate.CANCELLED)) {
      throw new DuplicateResourceException(
          "Received certificate", facts.agentCode() + " " + facts.certificateNo());
    }
    ReceivedCertificate saved = certificates.save(new ReceivedCertificate(companyId, facts, lines));
    saved.posted(post(saved, BigDecimal.ONE, "CRT:" + saved.getId(), facts.receivedOn()));
    audit.record(
        ENTITY,
        saved.getId(),
        AuditAction.CREATE,
        "2307 "
            + facts.certificateNo()
            + " of "
            + facts.agentCode()
            + " received, tax "
            + saved.getTaxTotal());
    return saved;
  }

  /**
   * Cancels a certificate recorded in error: the posting is reversed.
   *
   * @param id certificate
   * @param reason reason
   * @return the certificate
   */
  public ReceivedCertificate cancel(Long id, String reason) {
    if (reason == null || reason.isBlank()) {
      throw new BusinessRuleException("CERTIFICATE_REASON", "Give the reason of the cancellation");
    }
    ReceivedCertificate c = get(id);
    String batch =
        c.getJournalBatchNo() == null
            ? null
            : post(c, BigDecimal.ONE.negate(), "CRT:" + id + ":CANCEL", LocalDate.now(clock));
    c.cancel(reason.strip(), batch);
    audit.record(ENTITY, id, AuditAction.REVERSE, "Cancelled: " + reason.strip());
    return c;
  }

  private String post(ReceivedCertificate c, BigDecimal sign, String key, LocalDate valueDate) {
    Map<String, BigDecimal> amounts = new LinkedHashMap<>();
    BigDecimal commission = c.taxOf(Kind.COMMISSION);
    BigDecimal incentive = c.taxOf(Kind.INCENTIVE);
    if (commission.signum() != 0) {
      amounts.put("COMMISSION_CWT", commission.multiply(sign));
    }
    if (incentive.signum() != 0) {
      amounts.put("INCENTIVE_CWT", incentive.multiply(sign));
    }
    return publisher
        .publish(
            new BusinessEvent(
                EVENT,
                c.getCompanyId(),
                headOffice(c.getCompanyId()),
                valueDate,
                organization.getCompany(c.getCompanyId()).getBaseCurrency(),
                MODULE,
                key,
                c.getCertificateNo(),
                c.getAgentCode(),
                null,
                null,
                "BIR 2307 " + c.getCertificateNo() + " received from " + c.getAgentName(),
                amounts,
                Map.of()))
        .getBatchNo();
  }

  private void validate(Facts f, List<ReceivedCertificateLine> lines) {
    if (blank(f.certificateNo()) || blank(f.agentCode()) || blank(f.agentName())) {
      throw new BusinessRuleException(
          "CERTIFICATE_INCOMPLETE", "Give the certificate number and the withholding agent");
    }
    requireDates(f);
    validateLines(lines);
  }

  private void requireDates(Facts f) {
    if (f.periodFrom() == null || f.periodTo() == null || f.periodTo().isBefore(f.periodFrom())) {
      throw new BusinessRuleException(
          "CERTIFICATE_PERIOD", "Give the period covered: its end is after its start");
    }
    if (f.receivedOn() == null || f.receivedOn().isAfter(LocalDate.now(clock))) {
      throw new BusinessRuleException(
          "CERTIFICATE_RECEIVED_ON", "Give the date received, not in the future");
    }
  }

  private static void validateLines(List<ReceivedCertificateLine> lines) {
    if (lines.isEmpty()) {
      throw new BusinessRuleException(
          "CERTIFICATE_LINES", "Give at least one income payment with the tax withheld");
    }
    for (ReceivedCertificateLine l : lines) {
      if (!complete(l) || l.income().signum() < 0 || l.tax().signum() <= 0) {
        throw new BusinessRuleException(
            "CERTIFICATE_LINES",
            "Each income payment needs its kind, ATC, income and a tax withheld above zero");
      }
    }
  }

  private static boolean complete(ReceivedCertificateLine l) {
    boolean amounts = l.income() != null && l.tax() != null;
    return l.kind() != null && !blank(l.atc()) && amounts;
  }

  private static boolean blank(String s) {
    return s == null || s.isBlank();
  }

  private Long headOffice(Long companyId) {
    return organization.listBranches(companyId).stream()
        .filter(Branch::isHeadOffice)
        .findFirst()
        .map(Branch::getId)
        .orElseThrow(
            () -> new BusinessRuleException("NO_HEAD_OFFICE", "The company has no head office"));
  }
}
