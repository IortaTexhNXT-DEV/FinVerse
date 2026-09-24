package com.iortatechnxt.brokerverse.remittance.service;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.opsledger.domain.OpsInvoice;
import com.iortatechnxt.brokerverse.opsledger.service.InvoiceLedgerQueryService;
import com.iortatechnxt.brokerverse.opsledger.service.LedgerSearch;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLine;
import com.iortatechnxt.brokerverse.remittance.domain.BatchLineRepository;
import com.iortatechnxt.brokerverse.remittance.domain.EodRequest;
import com.iortatechnxt.brokerverse.remittance.domain.EodRequestRepository;
import com.iortatechnxt.brokerverse.remittance.domain.ExtractionRun;
import com.iortatechnxt.brokerverse.remittance.domain.ExtractionRunRepository;
import com.iortatechnxt.brokerverse.remittance.domain.HoldRequest;
import com.iortatechnxt.brokerverse.remittance.domain.HoldRequestRepository;
import com.iortatechnxt.brokerverse.remittance.domain.InvoiceTag;
import com.iortatechnxt.brokerverse.remittance.domain.InvoiceTagRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatch;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceBatchRepository;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.BatchStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.HoldStage;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.RemittanceType;
import com.iortatechnxt.brokerverse.remittance.domain.RemittanceEnums.SpecialStage;
import com.iortatechnxt.brokerverse.remittance.domain.SpecialRemittance;
import com.iortatechnxt.brokerverse.remittance.domain.SpecialRemittanceRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remittance reads (RMTID.024/025/027/028/032): extraction runs and tags, batches by stage (the
 * Process Remittance queues), account search across batches by invoice, batch, endorsement, policy
 * or assured (case-insensitive, partial), holds, special remittances, end-of-day requests and the
 * DTIP status of the ledger invoices with their current extraction tag.
 */
@Service
@Transactional(readOnly = true)
public class RemittanceQueryService {

  private static final String COMPANY_ID = "companyId";
  private static final String STAGE = "stage";
  private static final String INVOICE_NO = "invoiceNo";
  private static final String ASSURED = "assuredName";
  private static final String REQUEST_NO = "requestNo";

  private final ExtractionRunRepository runs;
  private final InvoiceTagRepository tags;
  private final RemittanceBatchRepository batches;
  private final BatchLineRepository lines;
  private final HoldRequestRepository holds;
  private final SpecialRemittanceRepository specials;
  private final EodRequestRepository eod;
  private final InvoiceLedgerQueryService ledger;

  /**
   * Creates the service.
   *
   * @param runs extraction runs
   * @param tags extraction tags
   * @param batches batches
   * @param lines batch lines
   * @param holds hold requests
   * @param specials special remittance requests
   * @param eod end-of-day requests
   * @param ledger ledger reads
   */
  public RemittanceQueryService(
      ExtractionRunRepository runs,
      InvoiceTagRepository tags,
      RemittanceBatchRepository batches,
      BatchLineRepository lines,
      HoldRequestRepository holds,
      SpecialRemittanceRepository specials,
      EodRequestRepository eod,
      InvoiceLedgerQueryService ledger) {
    this.runs = runs;
    this.tags = tags;
    this.batches = batches;
    this.lines = lines;
    this.holds = holds;
    this.specials = specials;
    this.eod = eod;
    this.ledger = ledger;
  }

  /**
   * Extraction runs, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return runs
   */
  public Page<ExtractionRun> runs(Long companyId, Pageable pageable) {
    return runs.findByCompanyIdOrderByIdDesc(companyId, pageable);
  }

  /**
   * One run.
   *
   * @param id run
   * @return run
   */
  public ExtractionRun run(Long id) {
    return runs.findById(id).orElseThrow(() -> new ResourceNotFoundException("Extraction run", id));
  }

  /**
   * Tags given by a run.
   *
   * @param runId run
   * @param pageable page
   * @return tags
   */
  public Page<InvoiceTag> tags(Long runId, Pageable pageable) {
    return tags.findByRunIdOrderByIdAsc(runId, pageable);
  }

  /**
   * Batches (RMTID.024/027).
   *
   * @param criteria filters
   * @param pageable page and sort
   * @return batches
   */
  public Page<RemittanceBatch> batches(BatchSearch criteria, Pageable pageable) {
    return batches.findAll(batchSpec(criteria), pageable);
  }

  private static Specification<RemittanceBatch> batchSpec(BatchSearch s) {
    return (root, query, cb) -> {
      List<Predicate> where = new ArrayList<>();
      where.add(cb.equal(root.get(COMPANY_ID), s.companyId()));
      if (s.stages() != null && !s.stages().isEmpty()) {
        where.add(root.get(STAGE).in(s.stages()));
      }
      equalsIfSet(where, cb, root.get("insurerCode"), s.insurerCode());
      if (s.type() != null) {
        where.add(cb.equal(root.get("remittanceType"), s.type()));
      }
      String like = like(s.text());
      if (like != null) {
        var sub = query.subquery(Long.class);
        var line = sub.from(BatchLine.class);
        sub.select(line.get("id"))
            .where(
                cb.equal(line.get("batch"), root), cb.like(cb.lower(line.get(INVOICE_NO)), like));
        where.add(cb.or(cb.like(cb.lower(root.get("batchNo")), like), cb.exists(sub)));
      }
      return cb.and(where.toArray(Predicate[]::new));
    };
  }

  /**
   * Accounts in batches by invoice, batch, endorsement, policy or assured (RMTID.025).
   *
   * @param companyId company
   * @param text part of a reference or name
   * @param pageable page
   * @return lines with their batch
   */
  public Page<BatchLine> lines(Long companyId, String text, Pageable pageable) {
    Specification<BatchLine> spec =
        (root, query, cb) -> {
          var batch = root.join("batch");
          List<Predicate> where = new ArrayList<>();
          where.add(cb.equal(batch.get(COMPANY_ID), companyId));
          String like = like(text);
          if (like != null) {
            where.add(
                cb.or(
                    cb.like(cb.lower(root.get(INVOICE_NO)), like),
                    cb.like(cb.lower(batch.get("batchNo")), like),
                    cb.like(cb.lower(cb.coalesce(root.get("endorsementNo"), "")), like),
                    cb.like(cb.lower(cb.coalesce(root.get("policyNo"), "")), like),
                    cb.like(cb.lower(root.get(ASSURED)), like)));
          }
          return cb.and(where.toArray(Predicate[]::new));
        };
    Page<BatchLine> page = lines.findAll(spec, pageable);
    page.forEach(l -> Hibernate.initialize(l.getBatch()));
    return page;
  }

  /**
   * Hold requests.
   *
   * @param companyId company
   * @param stages stages, empty for all
   * @param text part of the request, invoice or assured
   * @param pageable page
   * @return requests
   */
  public Page<HoldRequest> holds(
      Long companyId, List<HoldStage> stages, String text, Pageable pageable) {
    return holds.findAll(requestSpec(companyId, stages, text), pageable);
  }

  /**
   * Special remittance requests (RMTID.030).
   *
   * @param companyId company
   * @param stages stages, empty for all
   * @param text part of the request, invoice or assured
   * @param pageable page
   * @return requests
   */
  public Page<SpecialRemittance> specials(
      Long companyId, List<SpecialStage> stages, String text, Pageable pageable) {
    return specials.findAll(requestSpec(companyId, stages, text), pageable);
  }

  private static <T, S extends Enum<S>> Specification<T> requestSpec(
      Long companyId, List<S> stages, String text) {
    return (root, query, cb) -> {
      List<Predicate> where = new ArrayList<>();
      where.add(cb.equal(root.get(COMPANY_ID), companyId));
      if (stages != null && !stages.isEmpty()) {
        where.add(root.get(STAGE).in(stages));
      }
      String like = like(text);
      if (like != null) {
        where.add(
            cb.or(
                cb.like(cb.lower(root.get(REQUEST_NO)), like),
                cb.like(cb.lower(root.get("invoice").get(INVOICE_NO)), like),
                cb.like(cb.lower(root.get("invoice").get(ASSURED)), like)));
      }
      return cb.and(where.toArray(Predicate[]::new));
    };
  }

  /**
   * DTIP status of the ledger invoices (RMTID.028/032, Annex III DTIP status): the invoices with
   * their components and current extraction tag.
   *
   * @param search ledger criteria
   * @param pageable page
   * @return invoices with their tag
   */
  public Page<DtipStatus> dtipStatus(LedgerSearch search, Pageable pageable) {
    return ledger
        .searchLoaded(search, pageable)
        .map(
            i ->
                new DtipStatus(
                    i, tags.findFirstByInvoiceNoOrderByIdDesc(i.getInvoiceNo()).orElse(null)));
  }

  /**
   * Recent end-of-day requests (RMTID.005).
   *
   * @param companyId company
   * @return requests, newest first
   */
  public List<EodRequest> endOfDayRequests(Long companyId) {
    return eod.findTop100ByCompanyIdOrderByIdDesc(companyId);
  }

  private static String like(String text) {
    return text == null || text.isBlank()
        ? null
        : "%" + text.strip().toLowerCase(Locale.ROOT) + "%";
  }

  private static void equalsIfSet(
      List<Predicate> where, CriteriaBuilder cb, Path<Object> path, String value) {
    if (value != null && !value.isBlank()) {
      where.add(cb.equal(path, value.strip()));
    }
  }

  /**
   * Batch criteria.
   *
   * @param companyId company
   * @param stages stages, empty for all
   * @param insurerCode insurer
   * @param type remittance type
   * @param text part of the batch or invoice number
   */
  public record BatchSearch(
      Long companyId,
      List<BatchStage> stages,
      String insurerCode,
      RemittanceType type,
      String text) {}

  /**
   * An invoice with its current extraction tag.
   *
   * @param invoice ledger invoice (loaded)
   * @param tag latest tag, null when never examined
   */
  public record DtipStatus(OpsInvoice invoice, InvoiceTag tag) {}
}
