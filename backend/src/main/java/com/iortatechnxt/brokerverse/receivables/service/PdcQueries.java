package com.iortatechnxt.brokerverse.receivables.service;

import com.iortatechnxt.brokerverse.receivables.domain.PdcEvent;
import com.iortatechnxt.brokerverse.receivables.domain.PdcEventRepository;
import com.iortatechnxt.brokerverse.receivables.domain.PdcRepository;
import com.iortatechnxt.brokerverse.receivables.domain.PdcStatus;
import com.iortatechnxt.brokerverse.receivables.domain.PostDatedCheque;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PDC register as of a date: the status of each cheque is taken from its status history (events
 * dated after the date are ignored), as the PDC reports require.
 */
@Service
@Transactional(readOnly = true)
public class PdcQueries {

  private final PdcRepository pdcs;
  private final PdcEventRepository events;

  /**
   * Creates the query service.
   *
   * @param pdcs PDC repository
   * @param events PDC events
   */
  public PdcQueries(PdcRepository pdcs, PdcEventRepository events) {
    this.pdcs = pdcs;
    this.events = events;
  }

  /**
   * Cheques received on or before a date, each with its status as of that date.
   *
   * @param companyId company
   * @param asOf date
   * @return cheques by cheque date
   */
  public List<PdcAsOf> asOf(Long companyId, LocalDate asOf) {
    List<PostDatedCheque> received =
        pdcs.findByCompanyIdAndReceivedDateLessThanEqualOrderByChequeDateAscIdAsc(companyId, asOf);
    Map<Long, List<PdcEvent>> history =
        events
            .findByPdcIdInOrderByIdAsc(received.stream().map(PostDatedCheque::getId).toList())
            .stream()
            .collect(Collectors.groupingBy(PdcEvent::getPdcId));
    return received.stream()
        .map(p -> new PdcAsOf(p, statusAsOf(p, history.getOrDefault(p.getId(), List.of()), asOf)))
        .toList();
  }

  /**
   * Cheques received on or before a date and still held (on hand or due) as of that date.
   *
   * @param companyId company
   * @param asOf date
   * @return cheques
   */
  public List<PdcAsOf> heldAsOf(Long companyId, LocalDate asOf) {
    return asOf(companyId, asOf).stream().filter(p -> p.status().isHeld()).toList();
  }

  /**
   * Cheques received in a period (any status), with the status as of the end of the period.
   *
   * @param companyId company
   * @param from first received date
   * @param to last received date
   * @return cheques
   */
  public List<PdcAsOf> receivedBetween(Long companyId, LocalDate from, LocalDate to) {
    return asOf(companyId, to).stream()
        .filter(p -> !p.pdc().getReceivedDate().isBefore(from))
        .toList();
  }

  /**
   * Status of a cheque as of a date from its history.
   *
   * @param pdc cheque
   * @param history events in order
   * @param asOf date
   * @return status
   */
  static PdcStatus statusAsOf(PostDatedCheque pdc, List<PdcEvent> history, LocalDate asOf) {
    PdcStatus status = PdcStatus.ON_HAND;
    for (PdcEvent e : history) {
      if (!e.getEventDate().isAfter(asOf)) {
        status = e.getToStatus();
      }
    }
    return history.isEmpty() ? pdc.getStatus() : status;
  }

  /**
   * Cheque with its status as of a date.
   *
   * @param pdc cheque
   * @param status status as of the date
   */
  public record PdcAsOf(PostDatedCheque pdc, PdcStatus status) {}
}
