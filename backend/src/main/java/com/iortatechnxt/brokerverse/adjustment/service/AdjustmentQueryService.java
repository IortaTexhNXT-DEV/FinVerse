package com.iortatechnxt.brokerverse.adjustment.service;

import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequest;
import com.iortatechnxt.brokerverse.adjustment.domain.EndorsementRequestRepository;
import com.iortatechnxt.brokerverse.adjustment.domain.MinBalanceItem;
import com.iortatechnxt.brokerverse.adjustment.domain.MinBalanceItemRepository;
import com.iortatechnxt.brokerverse.adjustment.domain.PostingBatch;
import com.iortatechnxt.brokerverse.adjustment.domain.PostingBatchRepository;
import com.iortatechnxt.brokerverse.adjustment.domain.RequestStage;
import com.iortatechnxt.brokerverse.coa.domain.BalanceSide;
import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.journal.domain.JournalBatchRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads of the adjustment module: requests (search by stage and text, one request with its
 * recompute and journals), counts per stage, the requests of an invoice (ADJID.020/024), posting
 * batches and minimal balance write-offs.
 */
@Service
@Transactional(readOnly = true)
public class AdjustmentQueryService {

  private final EndorsementRequestRepository requests;
  private final PostingBatchRepository batches;
  private final MinBalanceItemRepository writeOffs;
  private final JournalBatchRepository journals;

  /**
   * Creates the service.
   *
   * @param requests requests
   * @param batches posting batches
   * @param writeOffs minimal balance write-offs
   * @param journals journal batches (GL lines of a request)
   */
  public AdjustmentQueryService(
      EndorsementRequestRepository requests,
      PostingBatchRepository batches,
      MinBalanceItemRepository writeOffs,
      JournalBatchRepository journals) {
    this.requests = requests;
    this.batches = batches;
    this.writeOffs = writeOffs;
    this.journals = journals;
  }

  /**
   * Searches requests.
   *
   * @param companyId company
   * @param stage stage, null for all
   * @param text text on request, invoice, ARN, policy, assured or endorsement reference
   * @param pageable page
   * @return requests
   */
  public Page<EndorsementRequest> search(
      Long companyId, RequestStage stage, String text, Pageable pageable) {
    String like =
        text == null || text.isBlank() ? "%" : "%" + text.strip().toLowerCase(Locale.ROOT) + "%";
    return requests.search(companyId, stage, like, pageable);
  }

  /**
   * One request with its recompute and journals loaded.
   *
   * @param id request
   * @return request
   */
  public EndorsementRequest get(Long id) {
    EndorsementRequest request =
        requests
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(Adjustments.ENTITY, id));
    request.loadCollections();
    return request;
  }

  /**
   * Number of requests per stage.
   *
   * @param companyId company
   * @return count per stage
   */
  public Map<RequestStage, Long> counts(Long companyId) {
    Map<RequestStage, Long> counts = new EnumMap<>(RequestStage.class);
    for (RequestStage stage : RequestStage.values()) {
      counts.put(stage, requests.countByCompanyIdAndStage(companyId, stage));
    }
    return counts;
  }

  /**
   * Requests raised on an invoice or whose posting booked it.
   *
   * @param invoiceNo invoice
   * @return requests, newest first
   */
  public List<EndorsementRequest> forInvoice(String invoiceNo) {
    List<EndorsementRequest> list =
        new ArrayList<>(requests.findBySubjectInvoiceNoOrderByIdDesc(invoiceNo));
    requests
        .findFirstByOutcomeNewInvoiceNo(invoiceNo)
        .filter(r -> !list.contains(r))
        .ifPresent(list::add);
    return list;
  }

  /**
   * GL lines of the journals a request posted (validation list and slip, ADJID.017/018).
   *
   * @param request request (journals loaded)
   * @return lines
   */
  public List<GlLine> journalLines(EndorsementRequest request) {
    List<GlLine> lines = new ArrayList<>();
    for (String batchNo : request.getJournals()) {
      journals
          .findByCompanyIdAndBatchNo(request.getCompanyId(), batchNo)
          .ifPresent(
              batch ->
                  batch
                      .getLines()
                      .forEach(
                          l ->
                              lines.add(
                                  new GlLine(
                                      batchNo,
                                      l.getAccount().getCode(),
                                      l.getAccount().getName(),
                                      l.getSide(),
                                      l.getAmount(),
                                      l.getPartyCode(),
                                      l.getNarration()))));
    }
    return lines;
  }

  /**
   * Posting batches of a company.
   *
   * @param companyId company
   * @param pageable page
   * @return batches
   */
  public Page<PostingBatch> batches(Long companyId, Pageable pageable) {
    return batches.findByCompanyId(companyId, pageable);
  }

  /**
   * Minimal balance write-offs of a company, newest first.
   *
   * @param companyId company
   * @param pageable page
   * @return write-offs
   */
  public Page<MinBalanceItem> writeOffs(Long companyId, Pageable pageable) {
    return writeOffs.findByCompanyIdOrderByIdDesc(companyId, pageable);
  }

  /**
   * A posted GL line.
   *
   * @param batchNo journal batch
   * @param accountCode GL account
   * @param accountName account name
   * @param side debit or credit
   * @param amount amount
   * @param partyCode sub-ledger party
   * @param narration narration
   */
  public record GlLine(
      String batchNo,
      String accountCode,
      String accountName,
      BalanceSide side,
      BigDecimal amount,
      String partyCode,
      String narration) {}
}
