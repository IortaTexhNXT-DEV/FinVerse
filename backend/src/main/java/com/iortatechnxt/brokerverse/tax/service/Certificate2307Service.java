package com.iortatechnxt.brokerverse.tax.service;

import com.iortatechnxt.brokerverse.audit.domain.AuditAction;
import com.iortatechnxt.brokerverse.audit.service.AuditTrailService;
import com.iortatechnxt.brokerverse.common.exception.BusinessRuleException;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.common.sequence.DocumentNumberService;
import com.iortatechnxt.brokerverse.organization.domain.Company;
import com.iortatechnxt.brokerverse.organization.service.OrganizationService;
import com.iortatechnxt.brokerverse.tax.domain.AtcQuarterAmounts;
import com.iortatechnxt.brokerverse.tax.domain.Certificate2307;
import com.iortatechnxt.brokerverse.tax.domain.Certificate2307Aggregator;
import com.iortatechnxt.brokerverse.tax.domain.Certificate2307Batch;
import com.iortatechnxt.brokerverse.tax.domain.Certificate2307BatchRepository;
import com.iortatechnxt.brokerverse.tax.domain.Certificate2307Repository;
import com.iortatechnxt.brokerverse.tax.domain.CertificateStatus;
import com.iortatechnxt.brokerverse.tax.domain.TaxPeriod;
import com.iortatechnxt.brokerverse.tax.domain.Taxpayer;
import com.iortatechnxt.brokerverse.tax.domain.WorksheetKind;
import com.iortatechnxt.brokerverse.tax.service.TaxpayerDirectory.Lookup;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BIR Form 2307 certificates: batch generation per quarter from the EWT worksheet, register,
 * cancellation and PDF.
 *
 * <p>A batch certifies every payee with tax withheld in the quarter who does not already hold an
 * issued certificate for it; lines under the "UNMAPPED" ATC are left out (the payee's tax profile
 * must be completed first) and reported as skipped. Payor facts come from the company master.
 */
@Service
@Transactional
public class Certificate2307Service {

  /** Audit entity name. */
  public static final String ENTITY = "Certificate2307";

  private final Certificate2307Repository certificates;
  private final Certificate2307BatchRepository batches;
  private final TaxWorksheetService worksheets;
  private final TaxpayerDirectory directory;
  private final OrganizationService organization;
  private final DocumentNumberService numbers;
  private final AuditTrailService audit;
  private final Certificate2307Pdf pdf;

  /**
   * Creates the service.
   *
   * @param certificates certificates
   * @param batches batches
   * @param worksheets EWT worksheet
   * @param directory payee facts
   * @param organization company (payor) facts
   * @param numbers document numbers
   * @param audit audit trail
   * @param pdf PDF renderer
   */
  public Certificate2307Service(
      Certificate2307Repository certificates,
      Certificate2307BatchRepository batches,
      TaxWorksheetService worksheets,
      TaxpayerDirectory directory,
      OrganizationService organization,
      DocumentNumberService numbers,
      AuditTrailService audit,
      Certificate2307Pdf pdf) {
    this.certificates = certificates;
    this.batches = batches;
    this.worksheets = worksheets;
    this.directory = directory;
    this.organization = organization;
    this.numbers = numbers;
    this.audit = audit;
    this.pdf = pdf;
  }

  /**
   * Issues the certificates of a quarter.
   *
   * @param companyId company
   * @param year year
   * @param quarter quarter 1-4
   * @return batch result
   * @throws BusinessRuleException when no payee needs a certificate
   */
  public BatchResult generate(Long companyId, int year, int quarter) {
    TaxPeriod period = TaxPeriod.quarter(year, quarter);
    TaxWorksheet ewt = worksheets.compute(companyId, WorksheetKind.EWT, period);
    Map<String, List<AtcQuarterAmounts>> byPayee =
        Certificate2307Aggregator.aggregate(period, EwtWorksheetBuilder.entries(ewt));
    Lookup lookup = directory.lookup(companyId, byPayee.keySet());
    Company company = organization.getCompany(companyId);
    Taxpayer payor =
        Taxpayer.parse(company.getTaxId(), company.getName(), company.getAddress(), null);
    String label = period.label().replace("-", "");
    Certificate2307Batch batch =
        batches.save(new Certificate2307Batch(companyId, numbers.next("B2307-" + label), period));
    List<String> skipped = new ArrayList<>();
    byPayee.forEach(
        (payee, lines) -> {
          List<AtcQuarterAmounts> mapped =
              lines.stream()
                  .filter(l -> !TaxpayerDirectory.UNMAPPED_ATC.equals(l.atc()))
                  .filter(l -> l.tax().signum() > 0)
                  .toList();
          if (mapped.size() < lines.size()) {
            skipped.add(payee);
          }
          if (!mapped.isEmpty() && !alreadyIssued(companyId, payee, period)) {
            Certificate2307 c =
                new Certificate2307(
                    batch,
                    numbers.next("2307-" + label),
                    payee,
                    lookup.taxpayer(payee, payee),
                    payor);
            c.certify(mapped);
            batch.count(certificates.save(c));
          }
        });
    if (batch.getCertificateCount() == 0) {
      throw new BusinessRuleException(
          "NO_CERTIFICATES", "No payee of " + period.label() + " needs a new certificate");
    }
    audit.record(
        ENTITY,
        batch.getBatchNo(),
        AuditAction.RUN,
        "Issued " + batch.getCertificateCount() + " certificates for " + period.label());
    return new BatchResult(batch, skipped);
  }

  /**
   * Certificates of a year.
   *
   * @param companyId company
   * @param year year
   * @return certificates ordered by quarter and number
   */
  @Transactional(readOnly = true)
  public List<Certificate2307> register(Long companyId, int year) {
    TaxPeriod q1 = TaxPeriod.quarter(year, 1);
    return certificates.findByCompanyIdAndPeriodStartBetweenOrderByPeriodStartAscCertificateNoAsc(
        companyId, q1.from(), q1.from().plusYears(1).minusDays(1));
  }

  /**
   * Batches of a company.
   *
   * @param companyId company
   * @return batches, newest first
   */
  @Transactional(readOnly = true)
  public List<Certificate2307Batch> batches(Long companyId) {
    return batches.findByCompanyIdOrderByIdDesc(companyId);
  }

  /**
   * Gets a certificate with its lines.
   *
   * @param id id
   * @return certificate
   */
  @Transactional(readOnly = true)
  public Certificate2307 get(Long id) {
    return certificates
        .findWithLinesById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ENTITY, id));
  }

  /**
   * Cancels an issued certificate (the payee can then be certified again by a new batch).
   *
   * @param id id
   * @param reason reason
   * @return certificate
   */
  public Certificate2307 cancel(Long id, String reason) {
    Certificate2307 c = get(id);
    c.cancel(reason);
    audit.record(ENTITY, c.getCertificateNo(), AuditAction.DEACTIVATE, "Cancelled: " + reason);
    return c;
  }

  /**
   * PDF of one certificate.
   *
   * @param id certificate
   * @return PDF bytes
   */
  @Transactional(readOnly = true)
  public byte[] pdf(Long id) {
    return pdf.render(List.of(get(id)));
  }

  /**
   * PDF of all issued certificates of a batch, one per page.
   *
   * @param batchId batch
   * @return PDF bytes
   */
  @Transactional(readOnly = true)
  public byte[] batchPdf(Long batchId) {
    List<Certificate2307> list =
        certificates.findByBatchIdOrderByCertificateNo(batchId).stream()
            .filter(c -> c.getStatus() == CertificateStatus.ISSUED)
            .toList();
    if (list.isEmpty()) {
      throw new ResourceNotFoundException("Certificate batch", batchId);
    }
    return pdf.render(list);
  }

  private boolean alreadyIssued(Long companyId, String payee, TaxPeriod period) {
    return certificates.existsByCompanyIdAndPartyCodeAndPeriodStartAndStatus(
        companyId, payee, period.from(), CertificateStatus.ISSUED);
  }

  /**
   * Outcome of a batch run.
   *
   * @param batch the batch
   * @param skippedPayees payees with income under an unmapped ATC (not certified for it)
   */
  public record BatchResult(Certificate2307Batch batch, List<String> skippedPayees) {

    /** Canonical constructor copying the list. */
    public BatchResult {
      skippedPayees = List.copyOf(skippedPayees);
    }
  }
}
