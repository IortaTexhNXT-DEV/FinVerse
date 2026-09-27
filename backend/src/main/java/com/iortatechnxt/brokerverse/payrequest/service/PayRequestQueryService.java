package com.iortatechnxt.brokerverse.payrequest.service;

import com.iortatechnxt.brokerverse.common.exception.ResourceNotFoundException;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequest;
import com.iortatechnxt.brokerverse.payrequest.domain.PaymentRequestRepository;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestKind;
import com.iortatechnxt.brokerverse.payrequest.domain.RequestStage;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads of the requests (MKT 1.3.0, 1.5.0, 1.6.0, 1.18.0, 2.26.0): the work list by stage, kind,
 * date range and text; the counts per stage; one request with its lines.
 */
@Service
@Transactional(readOnly = true)
public class PayRequestQueryService {

  private final PaymentRequestRepository requests;

  /**
   * Creates the service.
   *
   * @param requests requests
   */
  public PayRequestQueryService(PaymentRequestRepository requests) {
    this.requests = requests;
  }

  /**
   * Searches requests.
   *
   * @param companyId company
   * @param filter stage, kind, dates and text
   * @param pageable page
   * @return requests
   */
  public Page<PaymentRequest> search(Long companyId, Filter filter, Pageable pageable) {
    String text = filter.text();
    String like =
        text == null || text.isBlank() ? "%" : "%" + text.strip().toLowerCase(Locale.ROOT) + "%";
    return requests.search(
        companyId, filter.stage(), filter.kind(), filter.from(), filter.to(), like, pageable);
  }

  /**
   * Requests per stage (work list tabs).
   *
   * @param companyId company
   * @return count per stage
   */
  public Map<RequestStage, Long> counts(Long companyId) {
    Map<RequestStage, Long> counts = new EnumMap<>(RequestStage.class);
    for (Object[] row : requests.countByStage(companyId)) {
      counts.put((RequestStage) row[0], (Long) row[1]);
    }
    return counts;
  }

  /**
   * One request with its lines.
   *
   * @param id request
   * @return request
   */
  public PaymentRequest get(Long id) {
    return requests
        .findLoaded(id)
        .orElseThrow(() -> new ResourceNotFoundException(PayRequests.ENTITY, id));
  }

  /**
   * One request by number.
   *
   * @param requestNo request number
   * @return request
   */
  public PaymentRequest byNumber(String requestNo) {
    return requests
        .findByRequestNo(requestNo)
        .orElseThrow(() -> new ResourceNotFoundException(PayRequests.ENTITY, requestNo));
  }

  /**
   * Requests of a period by request date (reports).
   *
   * @param companyId company
   * @param from first date
   * @param to last date
   * @return requests, oldest first
   */
  public List<PaymentRequest> period(Long companyId, LocalDate from, LocalDate to) {
    return requests.findByCompanyIdAndRequestDateBetweenOrderByRequestDateAscIdAsc(
        companyId, from, to);
  }

  /**
   * Work list filter.
   *
   * @param stage stage or null
   * @param kind kind or null
   * @param from first request date or null
   * @param to last request date or null
   * @param text request, payee, reference or DV number, may be blank
   */
  public record Filter(
      RequestStage stage, RequestKind kind, LocalDate from, LocalDate to, String text) {}
}
